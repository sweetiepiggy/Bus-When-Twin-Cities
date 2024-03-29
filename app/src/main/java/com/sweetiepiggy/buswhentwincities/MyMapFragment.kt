/*
    Copyright (C) 2019-2022 Sweetie Piggy Apps <sweetiepiggyapps@gmail.com>

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
import android.content.res.Configuration
import android.content.res.Configuration.UI_MODE_NIGHT_MASK
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.util.TypedValue
import android.util.TypedValue.complexToDimensionPixelSize
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import java.util.*

class MyMapFragment : Fragment(), OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private var mMap: GoogleMap? = null
    private var mVehicleTripId: String? = null
    private var mSelectedRouteLineTripId: String? = null
    private var mSelectedShapeId: Int? = null
    private var mNexTrips: List<NexTrip>? = null
    /** map from tripId to nexTrip */
    private var mVisibleNexTrips: MutableMap<String?, PresentableNexTrip>? = null
    private lateinit var mModel: NexTripsViewModel
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mDoShowRoutes: Map<Pair<String?, String?>, Boolean> = mapOf()
    private var mStop: Stop? = null
    /** map from shapeId to shape */
    private var mShapes: Map<Int, List<LatLng>>? = null
    private var mInitCameraDone: Boolean = false
    private var mDoShowRoutesInitDone: Boolean = false
    private var mShapesInitDone: Boolean = false
    // note that Marker.position is the current position on the map which will
    // not match the NexTrip.position if the Marker is undergoing animation,
    // we keep track of the NexTrip.positions here so we can avoid jumpy
    // animation behavior that occurs if animation is started a second time
    // before the first animation is finished
    private val mMarkers: MutableMap<String?, Pair<Marker, LatLng>> = mutableMapOf()
    /** map from shapeId to route Polyline */
    private val mRouteLines: MutableMap<Int?, Polyline> = mutableMapOf()
    /** set of tripIds */
    private val mFindingShapeIdFor: MutableSet<String> = mutableSetOf()
    private val mBusIcon: BitmapDescriptor by lazy {
        drawableToBitmap(requireContext(), R.drawable.ic_baseline_directions_bus_24px)
    }
    private val mBusSouthIcon: BitmapDescriptor by lazy {
        drawableToBitmap(requireContext(), R.drawable.ic_baseline_directions_bus_south_30px)
    }
    private val mBusEastIcon: BitmapDescriptor by lazy {
        drawableToBitmap(requireContext(), R.drawable.ic_baseline_directions_bus_east_36px)
    }
    private val mBusWestIcon: BitmapDescriptor by lazy {
        drawableToBitmap(requireContext(), R.drawable.ic_baseline_directions_bus_west_36px)
    }
    private val mBusNorthIcon: BitmapDescriptor by lazy {
        drawableToBitmap(requireContext(), R.drawable.ic_baseline_directions_bus_north_30px)
    }
    private val mTrainIcon: BitmapDescriptor by lazy {
        drawableToBitmap(requireContext(), R.drawable.ic_baseline_train_24px)
    }
    private val mTrainSouthIcon: BitmapDescriptor by lazy {
        drawableToBitmap(requireContext(), R.drawable.ic_baseline_train_south_30px)
    }
    private val mTrainEastIcon: BitmapDescriptor by lazy {
        drawableToBitmap(requireContext(), R.drawable.ic_baseline_train_east_36px)
    }
    private val mTrainWestIcon: BitmapDescriptor by lazy {
        drawableToBitmap(requireContext(), R.drawable.ic_baseline_train_west_36px)
    }
    private val mTrainNorthIcon: BitmapDescriptor by lazy {
        drawableToBitmap(requireContext(), R.drawable.ic_baseline_train_north_30px)
    }
    private val mLightrailIcon: BitmapDescriptor by lazy {
        drawableToBitmap(requireContext(), R.drawable.ic_baseline_lightrail_24px)
    }
    private val mLightrailSouthIcon: BitmapDescriptor by lazy {
        drawableToBitmap(requireContext(), R.drawable.ic_baseline_lightrail_south_30px)
    }
    private val mLightrailEastIcon: BitmapDescriptor by lazy {
        drawableToBitmap(requireContext(), R.drawable.ic_baseline_lightrail_east_36px)
    }
    private val mLightrailWestIcon: BitmapDescriptor by lazy {
        drawableToBitmap(requireContext(), R.drawable.ic_baseline_lightrail_west_36px)
    }
    private val mLightrailNorthIcon: BitmapDescriptor by lazy {
        drawableToBitmap(requireContext(), R.drawable.ic_baseline_lightrail_north_30px)
    }
    private var mColorRoute = R.color.colorRoute
    private var mColorRouteUnselected = R.color.colorRouteUnselected

    companion object {
        fun newInstance() = MyMapFragment()
        private val MY_PERMISSIONS_REQUEST_LOCATION = 0
        private val KEY_TRIP_ID = "tripId"
        private val UNSELECTED_MARKER_ALPHA = 0.3f
        private val TWIN_CITIES_LATLNG = LatLng(44.950864, -93.187336)
        private val TWIN_CITIES_ZOOM = 11f
        private val UNSELECTED_ROUTE_Z_INDEX = 0f
        private val UNSELECTED_VEHICLE_Z_INDEX = 1f
        private val ROUTE_Z_INDEX = 2f
        private val VEHICLE_Z_INDEX = 3f
        private val STOP_Z_INDEX = 4f

        private fun drawableToBitmap(context: Context, id: Int): BitmapDescriptor =
            BitmapDescriptorFactory.fromBitmap(getDrawable(context, id)?.toBitmap()!!)

    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.maps_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState == null) {
            arguments?.let { loadState(it) }
        } else {
            loadState(savedInstanceState)
        }

        if (PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("map_always_light", false)) {
            mColorRoute = R.color.colorRouteAlwaysLight
            mColorRouteUnselected = R.color.colorRouteUnselectedAlwaysLight
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        mModel = activity?.run {
            ViewModelProvider(this).get(NexTripsViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
        mModel.getDoShowRoutes().observe(viewLifecycleOwner, Observer<Map<Pair<String?, String?>, Boolean>>{
            updateDoShowRoutes(it)
            if (!mDoShowRoutesInitDone) {
                mDoShowRoutesInitDone = true
               mModel.getStop().observe(this, Observer<Stop?>{
                   mStop = it
                    mStop?.let { stop ->
                        mMap?.addMarker(MarkerOptions()
                                .position(LatLng(stop.stopLat, stop.stopLon))
                                .title(resources.getString(R.string.stop_number) + stop.stopId.toString())
                                .snippet(stop.stopName)
                                .icon(drawableToBitmap(requireContext(), R.drawable.ic_stop))
                    )}
                    mModel.getNexTrips().observe(viewLifecycleOwner, Observer<List<NexTrip>>{
                        updateNexTrips(it)
                    })
                    mModel.getShapes().observe(viewLifecycleOwner, Observer<Map<Int, List<LatLng>>>{
                        updateShapes(it)
                    })
               })
            }
        })

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        mVehicleTripId?.let { savedInstanceState.putString(KEY_TRIP_ID, it) }
    }

    private fun loadState(b: Bundle) {
        if (b.containsKey(KEY_TRIP_ID)) {
            mVehicleTripId = b.getString(KEY_TRIP_ID)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap.apply {
            uiSettings.setMapToolbarEnabled(false)
            setIndoorEnabled(false)
            // see: https://developers.google.com/maps/documentation/android-sdk/styling
            val isNightMode = (resources.configuration.uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES
            val mapstyle = if (isNightMode && !PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("map_always_light", false)) {
                R.raw.mapstyle_night
            } else {
                R.raw.mapstyle_default
            }
            setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), mapstyle))
            moveCamera(CameraUpdateFactory.newLatLngZoom(TWIN_CITIES_LATLNG, TWIN_CITIES_ZOOM))
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            // initialize camera only if we previously thought we initialized the
            // camera but the map wasn't ready
            if (mInitCameraDone) {
                initCamera()
            }
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION)
        }
        googleMap.setOnMarkerClickListener(object : GoogleMap.OnMarkerClickListener {
            override fun onMarkerClick(marker: Marker): Boolean {
                val nexTrip = (marker.tag as PresentableNexTrip?)
                mSelectedRouteLineTripId = null
                mSelectedShapeId = null
//                if (mVehicleTripId != null && mVehicleTripId != nexTrip?.tripId) {
                    deselectVehicle()
//                }
                nexTrip?.let { selectRouteLine(it) }
                marker.showInfoWindow()
                return true
            }
        })
        googleMap.setOnPolylineClickListener(object : GoogleMap.OnPolylineClickListener {
            override fun onPolylineClick(polyline: Polyline) {
                mSelectedRouteLineTripId = null
                mSelectedShapeId = (polyline.tag as Int)
                updateRouteLines()
                deselectVehicle()
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION ->
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mMap?.isMyLocationEnabled = true
                }
        }
        // initialize camera only if we previously thought we initialized the
        // camera but the map wasn't ready
        if (mInitCameraDone) {
            initCamera()
        }
    }

    fun selectVehicle(nexTrip: PresentableNexTrip) {
        if (nexTrip.tripId == null) {
            return
        }
        mVehicleTripId = nexTrip.tripId

        selectRouteLine(nexTrip)

        mMap?.run {
            for (marker in mMarkers.values.map { it.first }) {
                val taggedNexTrip = marker.tag as PresentableNexTrip
                if (nexTrip.tripId == taggedNexTrip.tripId) {
                    marker.apply {
                        setZIndex(VEHICLE_Z_INDEX)
                        alpha = 1f
                        showInfoWindow()
                    }
                    zoomToPosition(taggedNexTrip.position!!)
                } else {
                    marker.apply {
                        setZIndex(UNSELECTED_VEHICLE_Z_INDEX)
                        alpha = UNSELECTED_MARKER_ALPHA
                        hideInfoWindow()
                    }
                }
            }
        }
    }

    private fun deselectVehicle() {
        mVehicleTripId = null
        mMap?.run {
            val shownMarkers : MutableList<Marker> = mutableListOf()
            for (marker in mMarkers.values.map { it.first }) {
                val nexTrip = marker.tag as PresentableNexTrip
                if ((mDoShowRoutes.get(Pair(nexTrip.routeShortName, nexTrip.terminal)) ?: true) ||
                        mSelectedShapeId != null) {
                    marker.apply {
                        setZIndex(VEHICLE_Z_INDEX)
                        if (mSelectedShapeId != null && mSelectedShapeId != nexTrip.shapeId) {
                            alpha = UNSELECTED_MARKER_ALPHA
                        } else {
                            alpha = 1f
                            shownMarkers.add(marker)
                        }
                    }
                } else {
                    marker.apply {
                        setZIndex(UNSELECTED_VEHICLE_Z_INDEX)
                        alpha = UNSELECTED_MARKER_ALPHA
                    }
                }
            }
            if (mSelectedShapeId != null && shownMarkers.size == 1) {
                shownMarkers.first().showInfoWindow()
            }
        }
    }

    private fun initCamera() {
        mStop?.let {
            mMap?.addMarker(
                MarkerOptions().apply {
                    position(LatLng(it.stopLat, it.stopLon))
                    title(resources.getString(R.string.stop_number) + it.stopId.toString())
                    snippet(it.stopName)
                    icon(drawableToBitmap(requireContext(), R.drawable.ic_stop))
                    zIndex(STOP_Z_INDEX)
                }
            )// ?.apply {
            //     if (mVisibleNexTrips.isNullOrEmpty()) showInfoWindow()
            // }
        }
        // in case map was not initialized when mVisibleNexTrips updated?
        if (!mVisibleNexTrips.isNullOrEmpty()) {
            updateMarkers()
            updateRouteLines()
        }
        if (!mVisibleNexTrips.isNullOrEmpty() || mStop != null) {
            zoomToAllVehicles()
            mInitCameraDone = true
        }
    }

    private fun zoomToAllVehicles() {
        val shownMarkers = mMarkers.values.map { it.first }.filter {
            val nexTrip = it.tag as PresentableNexTrip
            val routeAndTerminal = Pair(nexTrip.routeShortName, nexTrip.terminal)
            mDoShowRoutes.get(routeAndTerminal) ?: true
        }
        val stop = mStop
        if (shownMarkers.size == 1 && stop == null) {
            zoomToPosition((shownMarkers.elementAt(0).tag as PresentableNexTrip).position!!)
            return
        } else if (shownMarkers.isEmpty() && stop != null) {
            mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(stop.stopLat, stop.stopLon), 15f))
            return
        }

        val latLngs = shownMarkers.map { (it.tag as PresentableNexTrip).position!! }
        val latLngsWithStop = if (stop == null)
                latLngs
            else latLngs + LatLng(stop.stopLat, stop.stopLon)

        mFusedLocationClient.lastLocation
            .addOnSuccessListener { myLocation: Location? ->
                if (!latLngs.isEmpty()) {
                    val latLngsWithMyLoc = if (myLocation != null)
                        latLngsWithStop + LatLng(myLocation.latitude, myLocation.longitude)
                    else latLngsWithStop
                    zoomTo(latLngsWithMyLoc, 5.236f)
                }
            }
            .addOnFailureListener { zoomTo(latLngsWithStop, 5.236f) }
    }

    private fun zoomToPosition(pos: LatLng) {
        mFusedLocationClient.lastLocation
            .addOnSuccessListener { myLocation: Location? ->
                if (myLocation != null || mStop != null) {
                    val locs = mutableListOf(pos)
                    myLocation?.let { locs.add(LatLng(it.latitude, it.longitude)) }
                    mStop?.let { locs.add(LatLng(it.stopLat, it.stopLon)) }
                    zoomTo(locs, 3f)
                } else {
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                }
            }
            .addOnFailureListener {
                val stop = mStop
                if (stop != null) {
                    val locs = mutableListOf(pos)
                    locs.add(LatLng(stop.stopLat, stop.stopLon))
                    zoomTo(locs, 3f)
                } else {
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
                }
            }
        }

    private fun zoomTo(latLngs: List<LatLng>, paddingRatio: Float) {
        val a = activity
        if (!latLngs.isEmpty() && a != null) mMap?.run {
            val bounds = LatLngBounds.Builder().apply {
                latLngs.forEach { include(it) }
            }.build()
            val tv = TypedValue()
            a.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)
            val actionBarHeight = complexToDimensionPixelSize(tv.data, resources.displayMetrics)
            val width = (resources.displayMetrics.widthPixels)
            val height = resources.displayMetrics.heightPixels - actionBarHeight
            val mapWidth = if (width > height) (width * 0.618).toInt() else width
            val padding = (minOf(mapWidth, height) / paddingRatio).toInt()
            animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, mapWidth, height, padding))
        }
    }

    fun updateNexTrips(nexTrips: List<NexTrip>) {
        mNexTrips = nexTrips
        val timeInMillis = Calendar.getInstance().timeInMillis
        val nexTripsWithActualPosition = nexTrips.filter {
            it.position != null &&
            (it.isActual ||
             (it.minutesUntilDeparture(timeInMillis)?.let {
                  it < NexTrip.MINUTES_BEFORE_TO_SHOW_LOC
              } ?: false)
            )
        }

        if (mVisibleNexTrips == null && !nexTripsWithActualPosition.isEmpty()) {
            mVisibleNexTrips = mutableMapOf()
        }
        mVisibleNexTrips?.clear()
        nexTripsWithActualPosition.forEach {
            if (!mVisibleNexTrips!!.contains(it.tripId)) {
                mVisibleNexTrips!![it.tripId] =
                    PresentableNexTrip(it, timeInMillis / 1000, requireContext())
            }
        }
        if (mVisibleNexTrips != null) {
            updateMarkers()
        }
        updateRouteLines()
        for (nexTrip in nexTrips) {
            if (nexTrip.shapeId != null) {
                mFindingShapeIdFor.remove(nexTrip.tripId)
            }
        }
        if (!mInitCameraDone && mModel.nexTripsLoaded()) {
            initCamera()
        }
    }

    private fun updateShapes(shapes: Map<Int, List<LatLng>>) {
        mShapes = shapes
        mMap?.run {
            val wantShapeId = mSelectedShapeId ?:
                mVisibleNexTrips?.get(mSelectedRouteLineTripId)?.shapeId ?:
                findShapeIdForTripId(mNexTrips, mSelectedRouteLineTripId)
            for ((shapeId, shape) in shapes) {
                if (!mRouteLines.containsKey(shapeId)) {
                    val color = if (wantShapeId == shapeId)
                        mColorRoute else mColorRouteUnselected
                    val zIndex = if (wantShapeId == shapeId)
                        ROUTE_Z_INDEX else UNSELECTED_ROUTE_Z_INDEX
                    mRouteLines[shapeId] = addPolyline(
                        PolylineOptions().apply {
                            addAll(shape)
                            color(ContextCompat.getColor(requireContext(), color))
                            startCap(RoundCap())
                            endCap(RoundCap())
                            zIndex(zIndex)
                            clickable(true)
                        }
                    ).apply { tag = shapeId }
                }
            }
            mShapesInitDone = true
        }
    }

    private fun findShapeIdForTripId(nexTrips: List<NexTrip>?, tripId: String?) =
        nexTrips?.filter {
            it.tripId == tripId
        }?.map {
            it.shapeId
        }?.distinct()?.let { possibleShapeIds ->
            if (possibleShapeIds.size == 1) {
                possibleShapeIds[0]
            } else {
                null
            }
        }

    private fun updateDoShowRoutes(doShowRoutes: Map<Pair<String?, String?>, Boolean>) {
        mDoShowRoutes = doShowRoutes
    }

    private fun updateMarkers() {
        val tripIdsToRemove = mutableListOf<String?>()
        for ((tripId, markerAndPosition) in mMarkers) {
            if (!mVisibleNexTrips!!.containsKey(tripId)) {
                markerAndPosition.first.remove()
                tripIdsToRemove.add(tripId)
            }
        }
        tripIdsToRemove.forEach {
            mMarkers.remove(it)
//            mRouteLines.remove(it)
        }

        mMap?.run {
            for (nexTrip in mVisibleNexTrips!!.values) {
                val marker = if (mMarkers.containsKey(nexTrip.tripId)) {
                    mMarkers[nexTrip.tripId]!!.first.apply {
                        if (!Vehicle.distanceBetweenIsSmall(mMarkers[nexTrip.tripId]!!.second, nexTrip.position)) {
                            AnimationUtil.animateMarkerTo(this, nexTrip.position!!)
                        }
                    }
                } else {
                    addMarker(
                        MarkerOptions().apply {
                            icon(getIcon(nexTrip.getVehicleKind(), nexTrip.routeDirection))
                            position(nexTrip.position!!)
                            flat(true)
                            anchor(0.5f, getBusIconAnchorVertical(nexTrip.routeDirection))
                        }
                    )?.apply {
                        val routeAndTerminal = Pair(nexTrip.routeShortName, nexTrip.terminal)
                        if (mVehicleTripId != null ||
                                (mSelectedShapeId == null &&
                                    !(mDoShowRoutes.get(routeAndTerminal) ?: true))) {
                            alpha = UNSELECTED_MARKER_ALPHA
                            setZIndex(UNSELECTED_VEHICLE_Z_INDEX)
                        } else {
                            alpha = if (mSelectedShapeId != null && mSelectedShapeId != nexTrip.shapeId)
                                UNSELECTED_MARKER_ALPHA else 1f
                            setZIndex(VEHICLE_Z_INDEX)
                        }
                    }
                }?.apply {
                    tag = nexTrip
                    title = "${nexTrip.routeAndTerminal} (${nexTrip.departureText})"
                    snippet = nexTrip.description
                    // force title to refresh
                    if (isInfoWindowShown()) {
                        showInfoWindow()
                    }
                }
                if (marker != null)
                    mMarkers[nexTrip.tripId] = Pair(marker, nexTrip.position!!)
            }
        }
    }

    private fun updateRouteLines() {
        mMap?.run {
            mNexTrips?.let { nexTrips ->
               for (nexTrip in nexTrips) {
                   if (nexTrip.shapeId != null && !(mShapes?.containsKey(nexTrip.shapeId) ?: false)) {
                       mModel.findShape(nexTrip.shapeId)
                   }
               }
            }
            // in case mMap==null when updateShapes last called
            if (!mShapesInitDone) {
                mShapes?.let { updateShapes(it) }
            }

            val wantShapeId = mSelectedShapeId ?:
                mVisibleNexTrips?.get(mSelectedRouteLineTripId)?.shapeId ?:
                findShapeIdForTripId(mNexTrips, mSelectedRouteLineTripId)
            for ((shapeId, routeLine) in mRouteLines) {
                val color = if (wantShapeId == shapeId)
                    mColorRoute else mColorRouteUnselected
                val zIndex = if (wantShapeId == shapeId)
                    ROUTE_Z_INDEX else UNSELECTED_ROUTE_Z_INDEX
                routeLine.apply {
                    setColor(ContextCompat.getColor(requireContext(), color))
                    setZIndex(zIndex)
                }
            }
        }
    }

    private fun selectRouteLine(nexTrip: PresentableNexTrip) {
        if (nexTrip.tripId == null) {
            return
        }

        mSelectedRouteLineTripId = nexTrip.tripId
        mSelectedShapeId = nexTrip.shapeId

        if (nexTrip.shapeId == null) {
            // get shapeId then shape then create polyline
            if (!mFindingShapeIdFor.contains(nexTrip.tripId)) {
                mFindingShapeIdFor.add(nexTrip.tripId)
                mModel.findShapeId(nexTrip.nexTrip)
            }
        }
        updateRouteLines()
    }

    fun onChangeHiddenRoutes(changedRoutes: Set<Pair<String?, String?>>) {
        if (mVehicleTripId == null) {
            for (marker in mMarkers.values.map { it.first }) {
                val nexTrip = marker.tag as PresentableNexTrip
                val routeAndTerminal = Pair(nexTrip.routeShortName, nexTrip.terminal)
                if (changedRoutes.contains(routeAndTerminal)) {
                    marker.alpha = if (mDoShowRoutes.get(routeAndTerminal) ?: true)
                        1f
                    else
                        UNSELECTED_MARKER_ALPHA
                }
            }
        }
    }

//     private inner class FindShapeTask(private val shapeId: Int): AsyncTask<Void, Void, List<LatLng>?>() {
//         override fun doInBackground(vararg params: Void): List<LatLng>? {
//             var shape: List<LatLng>? = null
//             DbAdapter().run {
//                 open(requireContext())
// //                shape = getShape(shapeId)
//                 close()
//             }
//             return shape
//         }

//         override fun onPostExecute(shape: List<LatLng>?) {
//             if (shape != null) {
//                 mShapes[shapeId] = shape
//                 mFindingShapeFor.remove(shapeId)
//             } else {
// //                DownloadShapeTask(this@MyMapFragment, shapeId).execute()
//             }
//         }
//     }

    private fun getIcon(vehicle: NexTrip.VehicleKind, direction: NexTrip.Direction?): BitmapDescriptor =
        when (vehicle) {
            NexTrip.VehicleKind.BUS       -> getBusIcon(direction)
            NexTrip.VehicleKind.LIGHTRAIL -> getLightrailIcon(direction)
            NexTrip.VehicleKind.TRAIN     -> getTrainIcon(direction)
        }

    private fun getTrainIcon(direction: NexTrip.Direction?): BitmapDescriptor =
        when (direction) {
            NexTrip.Direction.SOUTH -> mTrainSouthIcon
            NexTrip.Direction.EAST -> mTrainEastIcon
            NexTrip.Direction.WEST -> mTrainWestIcon
            NexTrip.Direction.NORTH -> mTrainNorthIcon
            else -> mTrainIcon
        }

    private fun getLightrailIcon(direction: NexTrip.Direction?): BitmapDescriptor =
        when (direction) {
            NexTrip.Direction.SOUTH -> mLightrailSouthIcon
            NexTrip.Direction.EAST  -> mLightrailEastIcon
            NexTrip.Direction.WEST  -> mLightrailWestIcon
            NexTrip.Direction.NORTH -> mLightrailNorthIcon
            else -> mTrainIcon
        }

    private fun getBusIcon(direction: NexTrip.Direction?): BitmapDescriptor =
        when (direction) {
            NexTrip.Direction.SOUTH -> mBusSouthIcon
            NexTrip.Direction.EAST  -> mBusEastIcon
            NexTrip.Direction.WEST  -> mBusWestIcon
            NexTrip.Direction.NORTH -> mBusNorthIcon
            else -> mBusIcon
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
