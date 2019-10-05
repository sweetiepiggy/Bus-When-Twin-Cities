/*
    Copyright (C) 2019 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

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
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.location.Location
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
import java.util.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.CopyrightOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay

class MyMapFragment : Fragment(), ActivityCompat.OnRequestPermissionsResultCallback {

    private var mMap: MapView? = null
    private var mVehicleBlockNumber: Int? = null
    private var mNexTrips: MutableMap<Int?, PresentableNexTrip>? = null
    private var mDoShowRoutes: Map<Pair<String?, String?>, Boolean> = mapOf()
    private var mStop: Stop? = null
    private var mInitCameraDone: Boolean = false
    // note that Marker.position is the current position on the map which will
    // not match the NexTrip.position if the Marker is undergoing animation,
    // we keep track of the NexTrip.positions here so we can avoid jumpy
    // animation behavior that occurs if animation is started a second time
    // before the first animation is finished
    private val mMarkers: MutableMap<Int?, Pair<Marker, PresentableNexTrip>> = mutableMapOf()

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
            load(context, PreferenceManager.getDefaultSharedPreferences(context))
            setUserAgentValue(activity?.packageName)
        }

        // mMap = activity?.findViewById<MapView>(R.id.map)!!.apply {
        mMap = MapView(inflater.context).apply {
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

        val model = activity?.run {
            ViewModelProvider(this).get(NexTripsViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
        model.getNexTrips().observe(this, Observer<List<NexTrip>>{ updateNexTrips(it) })
        model.getDoShowRoutes().observe(this, Observer<Map<Pair<String?, String?>, Boolean>>{
            updateDoShowRoutes(it)
        })
        model.getStop().observe(this, Observer<Stop>{
            mStop = it
            mMap?.overlays?.add(Marker(mMap).apply {
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
            })
            if (!mInitCameraDone) {
                initCamera()
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

    fun selectVehicle(blockNumber: Int) {
        mVehicleBlockNumber = blockNumber
        mMap?.run {
            for ((marker, nexTrip) in mMarkers.values) {
                if (blockNumber == nexTrip.blockNumber) {
                    marker.alpha = 1f
                    marker.showInfoWindow()
                    zoomToPosition(nexTrip.position!!)
                } else {
                    marker.alpha = UNSELECTED_MARKER_ALPHA
                }
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
        mMap?.controller?.apply {
            setZoom(TWIN_CITIES_ZOOM)
            setCenter(TWIN_CITIES_LATLNG)
        }
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
        if (!mNexTrips.isNullOrEmpty()) {
            updateMarkers()
        }
        if (!mNexTrips.isNullOrEmpty() || mStop != null) {
            zoomToAllVehicles()
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
        if (!mInitCameraDone && mNexTrips != null) {
            initCamera()
            mInitCameraDone = true
        }
    }

    fun updateDoShowRoutes(doShowRoutes: Map<Pair<String?, String?>, Boolean>) {
        mDoShowRoutes = doShowRoutes
    }

    private fun updateMarkers() {
        val blockNumbersToRemove = mutableListOf<Int?>()
        for ((blockNumber, markerAndNexTrip) in mMarkers) {
            if (!mNexTrips!!.containsKey(blockNumber)) {
                markerAndNexTrip.first.remove(mMap)
                blockNumbersToRemove.add(blockNumber)
            }
        }
        blockNumbersToRemove.forEach { mMarkers.remove(it) }

        mMap?.run {
            for (nexTrip in mNexTrips!!.values) {
                val marker = if (mMarkers.containsKey(nexTrip.blockNumber)) {
                    mMarkers[nexTrip.blockNumber]!!.first.apply {
                        position = nexTrip.position
                        // if (!NexTrip.distanceBetweenIsSmall(mMarkers[nexTrip.blockNumber]!!.second.position, nexTrip.position)) {
                        //     AnimationUtil.animateMarkerTo(this, nexTrip.position!!)
                        // }
                    }
                } else {
                    Marker(mMap).apply {
                        setIcon(getIcon(nexTrip.isTrain(), nexTrip.routeDirection))
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

    private fun getIcon(isTrain: Boolean, direction: NexTrip.Direction?): Drawable? =
        getDrawable(context!!, if (isTrain) getTrainIconId(direction) else getBusIconId(direction))

    private fun getTrainIconId(direction: NexTrip.Direction?): Int =
        when (direction) {
            NexTrip.Direction.SOUTH -> R.drawable.ic_baseline_train_south_30px
            NexTrip.Direction.EAST  -> R.drawable.ic_baseline_train_east_36px
            NexTrip.Direction.WEST  -> R.drawable.ic_baseline_train_west_36px
            NexTrip.Direction.NORTH -> R.drawable.ic_baseline_train_north_30px
            else -> R.drawable.ic_baseline_train_24px
        }

    private fun getBusIconId(direction: NexTrip.Direction?): Int =
        when (direction) {
            NexTrip.Direction.SOUTH -> R.drawable.ic_baseline_directions_bus_south_30px
            NexTrip.Direction.EAST  -> R.drawable.ic_baseline_directions_bus_east_36px
            NexTrip.Direction.WEST  -> R.drawable.ic_baseline_directions_bus_west_36px
            NexTrip.Direction.NORTH -> R.drawable.ic_baseline_directions_bus_north_30px
            else -> R.drawable.ic_baseline_directions_bus_24px
        }

    private fun getBusIconAnchorVertical(direction: NexTrip.Direction?): Float =
        // anchor at bottom of bus, not bottom of arrow
        if (direction == NexTrip.Direction.SOUTH) 0.8f else 1f
}
