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
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.preference.PreferenceManager
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
    /** map from blockNumber to nexTrip */
    private var mNexTrips: MutableMap<Int?, PresentableNexTrip>? = null
    private lateinit var mModel: NexTripsViewModel
    private var mDoShowRoutes: Map<Pair<String?, String?>, Boolean> = mapOf()
    private var mStop: Stop? = null
    /** map from shapeId to shape */
    private var mShapes: Map<Int, List<GeoPoint>>? = null
    private var mInitCameraDone: Boolean = false
    private var mDoShowRoutesInitDone: Boolean = false
    // note that Marker.position is the current position on the map which will
    // not match the NexTrip.position if the Marker is undergoing animation,
    // we keep track of the NexTrip.positions here so we can avoid jumpy
    // animation behavior that occurs if animation is started a second time
    // before the first animation is finished
    private val mMarkers: MutableMap<Int?, Pair<Marker, PresentableNexTrip>> = mutableMapOf()
    /** map from blockNumber to route Polyline */
    private val mRouteLines: MutableMap<Int?, Polyline> = mutableMapOf()
    /** set of blockNumbers */
    private val mFindingShapeIdFor: MutableSet<Int> = mutableSetOf()
    /** set of shapeIds */
    private val mFindingShapeFor: MutableSet<Int> = mutableSetOf()

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
            initCamera()
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
        initCamera()
    }

    fun selectVehicle(nexTrip: PresentableNexTrip) {
        if (nexTrip.blockNumber == null) {
            return
        }
        val blockNumber = nexTrip.blockNumber
        android.util.Log.d("got here", "got here: shapeId of $blockNumber in selectVehicle(${blockNumber}) is ${nexTrip.shapeId}")
        mVehicleBlockNumber = nexTrip.blockNumber

        if (nexTrip.shapeId == null) {
            // get shapeId then shape then create polyline
            //     mShapeIds[blockNumber] = 110005
            if (!mFindingShapeIdFor.contains(blockNumber)) {
                mFindingShapeIdFor.add(blockNumber)
                mModel.findShapeId(nexTrip.nexTrip)
            }
        } else if (!(mShapes?.containsKey(nexTrip.shapeId) ?: false)) {
            // get shape then create polyline
            android.util.Log.d("got here", "got here: know shapeId of $blockNumber is ${nexTrip.shapeId} but need shape")
            if (!mFindingShapeFor.contains(nexTrip.shapeId)) {
                mFindingShapeFor.add(nexTrip.shapeId)
                mModel.findShape(nexTrip.shapeId)
//                FindShapeTask(shapeId).execute()
            }
        } else if (!mRouteLines.containsKey(blockNumber)) {
            // is this necessary? can this ever be reached?
            val polyline = Polyline().apply {
                setPoints(mShapes!![nexTrip.shapeId])
                setColor(ContextCompat.getColor(context!!, R.color.colorRoute))
            }
            mRouteLines[blockNumber] = polyline
            mMap?.overlays?.add(polyline)
        }

        mMap?.run {
            for ((marker, taggedNexTrip) in mMarkers.values) {
                if (blockNumber == taggedNexTrip.blockNumber) {
                    marker.alpha = 1f
                    marker.showInfoWindow()
                    zoomToPosition(taggedNexTrip.position!!)
                } else {
                    marker.alpha = UNSELECTED_MARKER_ALPHA
                }
            }
            for ((bn, routeLine) in mRouteLines) {
                val color = if (blockNumber == bn) R.color.colorRoute else R.color.colorRouteUnselected
                routeLine.setColor(ContextCompat.getColor(context!!, color))
//                routeLine.alpha = if (blockNumber == bn) 1f else UNSELECTED_MARKER_ALPHA
//                routeLine.setVisible(blockNumber == bn)
            }
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
//            .addOnFailureListener { zoomTo(latLngs, 5.236f) }
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
            mMap?.zoomToBoundingBox(BoundingBox.fromGeoPoints(latLngs), true, padding)
            // animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, mapWidth, height, padding))
        }
    }

    fun updateNexTrips(nexTrips: List<NexTrip>) {
        android.util.Log.d("got here", "got here: in updateNexTrips")
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
            updateMarkers()
        }
        if (!mInitCameraDone) {
            initCamera()
        }
    }

    private fun updateShapes(shapes: Map<Int, List<GeoPoint>>) {
        mShapes = shapes
    }

    private fun updateDoShowRoutes(doShowRoutes: Map<Pair<String?, String?>, Boolean>) {
        mDoShowRoutes = doShowRoutes
    }

    private fun updateMarkers() {
        android.util.Log.d("got here", "got here: in updateMarkers()")
        val blockNumbersToRemove = mutableListOf<Int?>()
        for ((blockNumber, markerAndNexTrip) in mMarkers) {
            if (!mNexTrips!!.containsKey(blockNumber)) {
                markerAndNexTrip.first.remove(mMap)
                blockNumbersToRemove.add(blockNumber)
            }
        }
        blockNumbersToRemove.forEach {
            mMarkers.remove(it)
            mRouteLines.remove(it)
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
                if (nexTrip.shapeId != null) {
                    android.util.Log.d("got here", "got here: shapeId for blockNumber ${nexTrip.blockNumber} is ${nexTrip.shapeId}")
                    mFindingShapeIdFor.remove(nexTrip.blockNumber)
                }
                if (mShapes?.containsKey(nexTrip.shapeId) ?: false) {
                    if (!mRouteLines.containsKey(nexTrip.blockNumber)) {
                        val polyline = Polyline().apply {
                            setPoints(mShapes!![nexTrip.shapeId])
                        }
                        mRouteLines[nexTrip.blockNumber] = polyline
                        mMap?.overlays?.add(polyline)
                    }
                    val color = if (nexTrip.blockNumber == mVehicleBlockNumber) R.color.colorRoute else R.color.colorRouteUnselected
                    mRouteLines[nexTrip.blockNumber]?.setColor(ContextCompat.getColor(context!!, color))
//                    mRouteLines[nexTrip.blockNumber]?.alpha = if (blockNumber == bn) 1f else UNSELECTED_MARKER_ALPHA
//                    mRouteLines[nexTrip.blockNumber]?.setVisible(nexTrip.blockNumber == mVehicleBlockNumber)
                }
            }
        }
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
