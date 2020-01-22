/*
    Copyright (C) 2019-2020 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

    This file is part of Bus When? (Twin Cities).

    Bus When? (Twin Cities) is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 3 of the License, or
    (at your option) any later version.

    Bus When? (Twin Cities) is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Bus When? (Twin Cities); if not, see <http://www.gnu.org/licenses/>.
*/

package com.sweetiepiggy.buswhentwincities

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.TypedValue
import android.util.TypedValue.complexToDimensionPixelSize
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.TilesOverlay.INVERT_COLORS
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.*

class MyMapFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {

    private var mMap: MapView? = null
    private var mVehicleBlockNumber: Int? = null
    private var mSelectedRouteLineBlockNumber: Int? = null
    private var mSelectedShapeId: Int? = null
    /** map from blockNumber to nexTrip */
    private var mNexTrips: MutableMap<Int?, PresentableNexTrip>? = null
    private lateinit var mModel: NexTripsViewModel
    private var mDoShowRoutes: Map<Pair<String?, String?>, Boolean> = mapOf()
    private var mStop: Stop? = null
    /** map from shapeId to shape */
    private var mShapes: Map<Int, List<GeoPoint>>? = null
    private var mInitCameraDone: Boolean = false
    private var mDoShowRoutesInitDone: Boolean = false
    private var mShapesInitDone: Boolean = false
    // note that Marker.position is the current position on the map which will
    // not match the NexTrip.position if the Marker is undergoing animation,
    // we keep track of the NexTrip.positions here so we can avoid jumpy
    // animation behavior that occurs if animation is started a second time
    // before the first animation is finished
    private val mMarkers: MutableMap<Int?, Pair<Marker, PresentableNexTrip>> = mutableMapOf()
    /** map from shapeId to route Polyline */
    private val mRouteLines: MutableMap<Int?, Polyline> = mutableMapOf()
    /** set of blockNumbers */
    private val mFindingShapeIdFor: MutableSet<Int> = mutableSetOf()
    private var mColorRoute = R.color.colorRoute
    private var mColorRouteUnselected = R.color.colorRouteUnselected

    companion object {
        fun newInstance() = MyMapFragment()
        private val MY_PERMISSIONS_REQUEST_LOCATION = 0
        private val KEY_BLOCK_NUMBER = "blockNumber"
        private val UNSELECTED_MARKER_ALPHA = 0.4f
        private val TWIN_CITIES_LATLNG = GeoPoint(44.950864, -93.187336)
        private val TWIN_CITIES_ZOOM = 11.0
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        Configuration.getInstance().apply{
            load(context, androidx.preference.PreferenceManager.getDefaultSharedPreferences(context))
            setUserAgentValue(activity?.packageName)
        }

        // mMap = activity?.findViewById<MapView>(R.id.map)!!.apply {
        mMap = MapView(inflater.context).apply {
            val isNightMode = (resources.configuration.uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES
            if (isNightMode && !androidx.preference.PreferenceManager.getDefaultSharedPreferences(context!!).getBoolean("map_always_light", false)) {
                overlayManager.tilesOverlay.setColorFilter(INVERT_COLORS)
            }

            setTileSource(TileSourceFactory.MAPNIK)
            // setTileSource(TileSourceFactory.USGS_SAT)
            // setTileSource(TileSourceFactory.USGS_TOPO)
            // setTileSource(TileSourceFactory.PUBLIC_TRANSPORT)
            setMultiTouchControls(true)
            setTilesScaledToDpi(true)
            setFlingEnabled(true)
            // zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            overlays?.add(RotationGestureOverlay(this).apply {
                setEnabled(true)
            })
            // overlays?.add(CompassOverlay(context, InternalCompassOrientationProvider(context), this).apply {
            // overlays?.add(CompassOverlay(context, this).apply {
            //     enableCompass()
            // })

            overlays?.add(CopyrightOverlay(context))
        }
        return mMap
        // return inflater.inflate(R.layout.maps_fragment, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mMap?.onDetach()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState == null) {
            arguments?.let { loadState(it) }
        } else {
            loadState(savedInstanceState)
        }

        if (PreferenceManager.getDefaultSharedPreferences(context!!).getBoolean("map_always_light", false)) {
            mColorRoute = R.color.colorRouteAlwaysLight
            mColorRouteUnselected = R.color.colorRouteUnselectedAlwaysLight
        }
        mModel = activity?.run {
            ViewModelProvider(this).get(NexTripsViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
        mModel.getDoShowRoutes().observe(this, Observer<Map<Pair<String?, String?>, Boolean>>{
            updateDoShowRoutes(it)
            if (!mDoShowRoutesInitDone) {
                mDoShowRoutesInitDone = true
                mModel.getStop().observe(this, Observer<Stop?>{
                    mStop = it
                    mStop?.let { stop ->
                        mMap?.overlays?.add(Marker(mMap).apply {
                            setPosition(GeoPoint(stop.stopLat, stop.stopLon))
                            setTitle(resources.getString(R.string.stop_number) + stop.stopId.toString())
                            setSnippet(stop.stopName)
                            setIcon(getDrawable(context!!, R.drawable.ic_stop))
                            setOnMarkerClickListener(object : Marker.OnMarkerClickListener {
                                override fun onMarkerClick(marker: Marker, mapView: MapView): Boolean {
                                    marker.showInfoWindow()
                                    // mapView.controller.animateTo(marker.position)
                                    return true
                                }
                            })
                        })
                    }
                    mModel.getNexTrips().observe(this, Observer<List<NexTrip>>{
                        updateNexTrips(it)
                    })
                    mModel.getShapes().observe(this, Observer<Map<Int, List<GeoPoint>>>{
                        updateShapes(it)
                    })
                })
            }
        })

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        // val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        // mapFragment!!.getMapAsync(this)
        onMapReady()
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        mVehicleBlockNumber?.let { savedInstanceState.putInt(KEY_BLOCK_NUMBER, it) }
    }

    private fun loadState(b: Bundle) {
        if (b.containsKey(KEY_BLOCK_NUMBER)) {
            mVehicleBlockNumber = b.getInt(KEY_BLOCK_NUMBER)
        }
    }

    public override fun onResume() {
        super.onResume()
        mMap?.onResume()
    }

    public override fun onPause() {
        super.onPause()
        mMap?.onPause()
    }

    fun onMapReady() {
        mMap?.controller?.apply {
            setZoom(TWIN_CITIES_ZOOM)
            setCenter(TWIN_CITIES_LATLNG)
        }
        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap?.overlays?.add(MyLocationNewOverlay(GpsMyLocationProvider(context), mMap).apply {
                enableMyLocation()
            })
            // initialize camera only if we previously thought we initialized the
            // camera but the map wasn't ready
            if (mInitCameraDone) {
                initCamera()
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION ->
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mMap?.overlays?.add(MyLocationNewOverlay(GpsMyLocationProvider(context), mMap).apply {
                        enableMyLocation()
                    })
                }
        }
        // initialize camera only if we previously thought we initialized the
        // camera but the map wasn't ready
        if (mInitCameraDone) {
            initCamera()
        }
    }

    fun selectVehicle(nexTrip: PresentableNexTrip) {
        if (nexTrip.blockNumber == null) {
            return
        }
        mVehicleBlockNumber = nexTrip.blockNumber
        android.util.Log.d("got here", "got here: shapeId of ${nexTrip.blockNumber} in selectVehicle(${nexTrip.blockNumber}) is ${nexTrip.shapeId}")

        selectRouteLine(nexTrip)

        mMap?.run {
            var markerToShow: Marker? = null
            for ((marker, taggedNexTrip) in mMarkers.values) {
                if (nexTrip.blockNumber == taggedNexTrip.blockNumber) {
                    marker.alpha = 1f
                    markerToShow = marker
                    zoomToPosition(taggedNexTrip.position!!)
                } else {
                    marker.alpha = UNSELECTED_MARKER_ALPHA
                    marker.closeInfoWindow()
                }
            }
            // workaround bug where closeInfoWindow() closes other info window
            markerToShow?.showInfoWindow()
        }
    }

    private fun deselectVehicle() {
        mVehicleBlockNumber = null
        mMap?.run {
            for ((marker, nexTrip) in mMarkers.values) {
                marker.alpha = if (mDoShowRoutes.get(Pair(nexTrip.route, nexTrip.terminal)) ?: true)
                    1f
                else
                    UNSELECTED_MARKER_ALPHA
            }
        }
    }

    private fun initCamera() {
        mStop?.let {
            mMap?.overlays?.add(Marker(mMap).apply{
                    setPosition(GeoPoint(it.stopLat, it.stopLon))
                    setTitle(resources.getString(R.string.stop_number) + it.stopId.toString())
                    setSnippet(it.stopName)
                    setIcon(getDrawable(context!!, R.drawable.ic_stop))
                    setOnMarkerClickListener(object : Marker.OnMarkerClickListener {
                        override fun onMarkerClick(marker: Marker, mapView: MapView): Boolean {
                            marker.showInfoWindow()
                            // mapView.controller.animateTo(marker.position)
                            return true
                        }
                    })
            })// ?.apply {
            //     if (mNexTrips.isNullOrEmpty()) showInfoWindow()
            // }
        }
        // in case map was not initialized when mNexTrips updated?
        if (!mNexTrips.isNullOrEmpty()) {
            updateMarkers()
            updateRouteLines(mNexTrips!!.values.map { it.nexTrip } )
        }
        if (!mNexTrips.isNullOrEmpty() || mStop != null) {
            zoomToAllVehicles()
            mInitCameraDone = true
        }
    }

    private fun zoomToAllVehicles() {
        val shownNexTrips = mMarkers.values.filter { (_, nexTrip) ->
            val routeAndTerminal = Pair(nexTrip.route, nexTrip.terminal)
            mDoShowRoutes.get(routeAndTerminal) ?: true
        }.map { it.second }
        val stop = mStop
        if (shownNexTrips.size == 1 && stop == null) {
            zoomToPosition(shownNexTrips.elementAt(0).position!!)
            return
        } else if (shownNexTrips.isEmpty() && stop != null) {
            mMap?.controller?.animateTo(GeoPoint(stop.stopLat, stop.stopLon), 15.0, null)
            return
        }

        val latLngs = shownNexTrips.map { it.position!! }
        val latLngsWithStop = if (stop == null)
                latLngs
            else latLngs + GeoPoint(stop.stopLat, stop.stopLon)

//        mFusedLocationClient.lastLocation
//            .addOnSuccessListener { myLocation: Location? ->
//                if (!latLngs.isEmpty()) {
//                    val latLngsWithMyLoc = if (myLocation != null)
//                        latLngsWithStop + GeoPoint(myLocation.latitude, myLocation.longitude)
//                    else latLngsWithStop
//                    zoomTo(latLngsWithMyLoc, 5.236f)
//                }
//            }
//            .addOnFailureListener { zoomTo(latLngsWithStop, 5.236f) }
        zoomTo(latLngsWithStop, 5.2356f)
    }

    private fun zoomToPosition(pos: GeoPoint) {
//        mFusedLocationClient.lastLocation
//            .addOnSuccessListener { myLocation: Location? ->
//                if (myLocation != null || mStop != null) {
//                    val locs = mutableListOf(pos)
//                    myLocation?.let { locs.add(GeoPoint(it.latitude, it.longitude)) }
//                    mStop?.let { locs.add(GeoPoint(it.stopLat, it.stopLon)) }
//                    zoomTo(locs, 3f)
//                } else {
//                    mMap?.controller?.animateTo(pos, 15.0, null)
//                }
//            }
//            .addOnFailureListener {
//                mMap?.controller?.animateTo(pos, 15.0, null)
//            }
            mStop?.let {
                zoomTo(listOf(pos, GeoPoint(it.stopLat, it.stopLon)), 3f)
            } ?: mMap?.controller?.animateTo(pos, 15.0, null)
        }

    private fun zoomTo(latLngs: List<GeoPoint>, paddingRatio: Float) {
        val a = activity
        if (!latLngs.isEmpty() && a != null) mMap?.run {
            // val bounds = LatLngBounds.Builder().apply {
            //     latLngs.forEach { include(it) }
            // }.build()
            val tv = TypedValue()
            a.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)
            val actionBarHeight = complexToDimensionPixelSize(tv.data, resources.displayMetrics)
            val width = (resources.displayMetrics.widthPixels)
            val height = resources.displayMetrics.heightPixels - actionBarHeight
            val mapWidth = if (width > height) (width * 0.618).toInt() else width
            val padding = (minOf(mapWidth, height) / paddingRatio).toInt()
            // FIXME: this crashes on orientation change?
            // java.lang.IllegalArgumentException: north must be in [-85.05112877980658,85.05112877980658]
            // maybe related: https://github.com/osmdroid/osmdroid/issues/1339
            // possibly view not inflated when this is called?
            try {
                zoomToBoundingBox(BoundingBox.fromGeoPoints(latLngs), true, padding)
            } catch (e: IllegalArgumentException) {
            }
            // animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, mapWidth, height, padding))
        }
    }

    fun updateNexTrips(nexTrips: List<NexTrip>) {
        android.util.Log.d("got here", "got here2: in updateNexTrips")
        val timeInMillis = Calendar.getInstance().timeInMillis
        val nexTripsWithActualPosition = nexTrips.filter {
            it.position != null && (it.isActual || (it.minutesUntilDeparture(timeInMillis)?.let { it < NexTrip.MINUTES_BEFORE_TO_SHOW_LOC } ?: false))
        }

        if (mNexTrips == null && !nexTripsWithActualPosition.isEmpty()) {
            mNexTrips = mutableMapOf()
        }
        mNexTrips?.clear()
        nexTripsWithActualPosition.forEach {
            mNexTrips!![it.blockNumber] = PresentableNexTrip(it, timeInMillis, context!!)
        }
        if (mNexTrips != null) {
            android.util.Log.d("got here", "got here2: calling updateMarkers")
            updateMarkers()
        }
        updateRouteLines(nexTrips)
        for (nexTrip in nexTrips) {
            if (nexTrip.shapeId != null) {
                mFindingShapeIdFor.remove(nexTrip.blockNumber)
            }
        }
        if (!mInitCameraDone && mModel.nexTripsLoaded()) {
            initCamera()
        }
    }

    private fun updateShapes(shapes: Map<Int, List<GeoPoint>>) {
        mShapes = shapes
        mMap?.run {
            for ((shapeId, shape) in shapes) {
                if (!mRouteLines.containsKey(shapeId)) {
                    val wantShapeId = mSelectedShapeId ?: mNexTrips?.get(mSelectedRouteLineBlockNumber)?.shapeId
                    val color = if (wantShapeId != shapeId)
                        mColorRouteUnselected else mColorRoute
                    val polyline = Polyline().apply {
                        setPoints(shape)
                        setColor(ContextCompat.getColor(context!!, color))
                    }
                    mRouteLines[shapeId] = polyline
                    overlays?.add(polyline)
                }
            }
            mShapesInitDone = true
        }
    }

    private fun updateDoShowRoutes(doShowRoutes: Map<Pair<String?, String?>, Boolean>) {
        mDoShowRoutes = doShowRoutes
    }

    private fun updateMarkers() {
        android.util.Log.d("got here", "got here: in updateMarkers(), mMap is null = ${mMap == null}")
        val blockNumbersToRemove = mutableListOf<Int?>()
        for ((blockNumber, markerAndNexTrip) in mMarkers) {
            if (!mNexTrips!!.containsKey(blockNumber)) {
                markerAndNexTrip.first.remove(mMap)
                blockNumbersToRemove.add(blockNumber)
            }
        }
        blockNumbersToRemove.forEach {
            mMarkers.remove(it)
//            mRouteLines.remove(it)
        }

        mMap?.run {
            for (nexTrip in mNexTrips!!.values) {
                android.util.Log.d("got here", "got here: blockNumber = ${nexTrip.blockNumber}")
                val marker = if (mMarkers.containsKey(nexTrip.blockNumber)) {
                    mMarkers[nexTrip.blockNumber]!!.first.apply {
                        position = nexTrip.position
                        // if (!NexTrip.distanceBetweenIsSmall(mMarkers[nexTrip.blockNumber]!!.second.position, nexTrip.position)) {
                        //     AnimationUtil.animateMarkerTo(this, nexTrip.position!!)
                        // }
                    }
                } else {
                    Marker(mMap).apply {
                        setIcon(getIcon(nexTrip.getVehicle(), nexTrip.routeDirection))
                        setPosition(nexTrip.position!!)
                        setFlat(true)
                        setAnchor(0.5f, getBusIconAnchorVertical(nexTrip.routeDirection))
                        val routeAndTerminal = Pair(nexTrip.route, nexTrip.terminal)
                        if (mVehicleBlockNumber != null || !(mDoShowRoutes.get(routeAndTerminal) ?: true)) {
                            setAlpha(UNSELECTED_MARKER_ALPHA)
                        }
                        mMap?.overlays?.add(this)
                        setOnMarkerClickListener(object : Marker.OnMarkerClickListener {
                            override fun onMarkerClick(marker: Marker, mapView: MapView): Boolean {
                                if (mVehicleBlockNumber != null && mVehicleBlockNumber != nexTrip.blockNumber) {
                                    deselectVehicle()
                                }
                                marker.showInfoWindow()
                                // mapView.controller.animateTo(marker.position)
                                return true
                            }
                        })
                    }
                }.apply {
                    // tag = nexTrip
                    setTitle("${nexTrip.routeAndTerminal} (${nexTrip.departureText})")
                    setSnippet(nexTrip.description)
                    // force title to refresh
                    if (isInfoWindowShown()) {
                        showInfoWindow()
                    }
                }
                mMarkers[nexTrip.blockNumber] = Pair(marker, nexTrip)
            }
        }
    }

    private fun updateRouteLines(nexTrips: List<NexTrip>) {
        android.util.Log.d("got here", "got here: in updateRouteLines()")

        mMap?.run {
            for (nexTrip in nexTrips) {
                if (nexTrip.shapeId != null && !(mShapes?.containsKey(nexTrip.shapeId) ?: false)) {
                    android.util.Log.d("got here", "got here: updateRouteLines: know shapeId of ${nexTrip.blockNumber} is ${nexTrip.shapeId} but need shape")
                    mModel.findShape(nexTrip.shapeId)
                }
            }

            // in case mMap==null when updateShapes last called
            if (!mShapesInitDone) {
                mShapes?.let { updateShapes(it) }
            }

            for ((shapeId, routeLine) in mRouteLines) {
                val wantShapeId = mSelectedShapeId ?: mNexTrips?.get(mSelectedRouteLineBlockNumber)?.shapeId
                val color = if (wantShapeId != shapeId)
                    mColorRouteUnselected else mColorRoute
                routeLine.setColor(ContextCompat.getColor(context!!, color))
            }
        }
    }

    private fun selectRouteLine(nexTrip: PresentableNexTrip) {
        if (nexTrip.blockNumber == null) {
            return
        }

        mSelectedRouteLineBlockNumber = nexTrip.blockNumber
        mSelectedShapeId = nexTrip.shapeId

        if (nexTrip.shapeId == null) {
            // get shapeId then shape then create polyline
            if (!mFindingShapeIdFor.contains(nexTrip.blockNumber)) {
                mFindingShapeIdFor.add(nexTrip.blockNumber)
                mModel.findShapeId(nexTrip.nexTrip)
            }
        }
        updateRouteLines(listOf(nexTrip.nexTrip))
    }

    fun onChangeHiddenRoutes(changedRoutes: Set<Pair<String?, String?>>) {
        if (mVehicleBlockNumber == null) {
            for ((marker, nexTrip) in mMarkers.values) {
                val routeAndTerminal = Pair(nexTrip.route, nexTrip.terminal)
                if (changedRoutes.contains(routeAndTerminal)) {
                    marker.alpha = if (mDoShowRoutes.get(routeAndTerminal) ?: true)
                        1f
                    else
                        UNSELECTED_MARKER_ALPHA
                }
            }
        }
    }

    private fun getIcon(vehicle: NexTrip.Vehicle, direction: NexTrip.Direction?): Drawable? =
        getDrawable(context!!, getIconId(vehicle, direction))

    private fun getIconId(vehicle: NexTrip.Vehicle, direction: NexTrip.Direction?): Int =
        when (vehicle) {
            NexTrip.Vehicle.BUS       -> getBusIconId(direction)
            NexTrip.Vehicle.LIGHTRAIL -> getLightrailIconId(direction)
            NexTrip.Vehicle.TRAIN     -> getTrainIconId(direction)
        }

    private fun getBusIconId(direction: NexTrip.Direction?): Int =
        when (direction) {
            NexTrip.Direction.SOUTH -> R.drawable.ic_baseline_directions_bus_south_30px
            NexTrip.Direction.EAST  -> R.drawable.ic_baseline_directions_bus_east_36px
            NexTrip.Direction.WEST  -> R.drawable.ic_baseline_directions_bus_west_36px
            NexTrip.Direction.NORTH -> R.drawable.ic_baseline_directions_bus_north_30px
            else -> R.drawable.ic_baseline_directions_bus_24px
        }

    private fun getLightrailIconId(direction: NexTrip.Direction?): Int =
        when (direction) {
            NexTrip.Direction.SOUTH -> R.drawable.ic_baseline_lightrail_south_30px
            NexTrip.Direction.EAST  -> R.drawable.ic_baseline_lightrail_east_36px
            NexTrip.Direction.WEST  -> R.drawable.ic_baseline_lightrail_west_36px
            NexTrip.Direction.NORTH -> R.drawable.ic_baseline_lightrail_north_30px
            else -> R.drawable.ic_baseline_lightrail_24px
        }

    private fun getTrainIconId(direction: NexTrip.Direction?): Int =
        when (direction) {
            NexTrip.Direction.SOUTH -> R.drawable.ic_baseline_train_south_30px
            NexTrip.Direction.EAST  -> R.drawable.ic_baseline_train_east_36px
            NexTrip.Direction.WEST  -> R.drawable.ic_baseline_train_west_36px
            NexTrip.Direction.NORTH -> R.drawable.ic_baseline_train_north_30px
            else -> R.drawable.ic_baseline_train_24px
        }

    private fun getBusIconAnchorVertical(direction: NexTrip.Direction?): Float =
        // anchor at bottom of bus, not bottom of arrow
        if (direction == NexTrip.Direction.SOUTH) 0.8f else 1f
}
