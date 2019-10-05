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
import android.location.Location
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
    private var mVehicleBlockNumber: Int? = null
    private var mNexTrips: MutableMap<Int?, PresentableNexTrip>? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mDoShowRoutes: Map<Pair<String?, String?>, Boolean> = mapOf()
    private var mStop: Stop? = null
    private var mInitCameraDone: Boolean = false
    // note that Marker.position is the current position on the map which will
    // not match the NexTrip.position if the Marker is undergoing animation,
    // we keep track of the NexTrip.positions here so we can avoid jumpy
    // animation behavior that occurs if animation is started a second time
    // before the first animation is finished
    private val mMarkers: MutableMap<Int?, Pair<Marker, LatLng>> = mutableMapOf()
    private val mBusIcon: BitmapDescriptor by lazy {
        drawableToBitmap(context!!, R.drawable.ic_baseline_directions_bus_24px)
    }
    private val mBusSouthIcon: BitmapDescriptor by lazy {
        drawableToBitmap(context!!, R.drawable.ic_baseline_directions_bus_south_30px)
    }
    private val mBusEastIcon: BitmapDescriptor by lazy {
        drawableToBitmap(context!!, R.drawable.ic_baseline_directions_bus_east_36px)
    }
    private val mBusWestIcon: BitmapDescriptor by lazy {
        drawableToBitmap(context!!, R.drawable.ic_baseline_directions_bus_west_36px)
    }
    private val mBusNorthIcon: BitmapDescriptor by lazy {
        drawableToBitmap(context!!, R.drawable.ic_baseline_directions_bus_north_30px)
    }
    private val mTrainIcon: BitmapDescriptor by lazy {
        drawableToBitmap(context!!, R.drawable.ic_baseline_train_24px)
    }
    private val mTrainSouthIcon: BitmapDescriptor by lazy {
        drawableToBitmap(context!!, R.drawable.ic_baseline_train_south_30px)
    }
    private val mTrainEastIcon: BitmapDescriptor by lazy {
        drawableToBitmap(context!!, R.drawable.ic_baseline_train_east_36px)
    }
    private val mTrainWestIcon: BitmapDescriptor by lazy {
        drawableToBitmap(context!!, R.drawable.ic_baseline_train_west_36px)
    }
    private val mTrainNorthIcon: BitmapDescriptor by lazy {
        drawableToBitmap(context!!, R.drawable.ic_baseline_train_north_30px)
    }

    companion object {
        fun newInstance() = MyMapFragment()
        private val MY_PERMISSIONS_REQUEST_LOCATION = 0
        private val KEY_BLOCK_NUMBER = "blockNumber"
        private val UNSELECTED_MARKER_ALPHA = 0.3f
        private val TWIN_CITIES_LATLNG = LatLng(44.950864, -93.187336)
        private val TWIN_CITIES_ZOOM = 11f

        private fun drawableToBitmap(context: Context, id: Int): BitmapDescriptor =
            BitmapDescriptorFactory.fromBitmap(getDrawable(context, id)?.toBitmap())

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

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(context!!)

        val model = activity?.run {
            ViewModelProvider(this).get(NexTripsViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
        model.getNexTrips().observe(this, Observer<List<NexTrip>>{ updateNexTrips(it) })
        model.getDoShowRoutes().observe(this, Observer<Map<Pair<String?, String?>, Boolean>>{
            updateDoShowRoutes(it)
        })
        model.getStop().observe(this, Observer<Stop>{
            mStop = it
            mMap?.addMarker(MarkerOptions()
                    .position(LatLng(it.stopLat, it.stopLon))
                    .title(resources.getString(R.string.stop_number) + it.stopId.toString())
                    .snippet(it.stopName)
                    .icon(drawableToBitmap(context!!, R.drawable.ic_stop))
            )
            if (!mInitCameraDone) {
                initCamera()
            }
        })

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
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

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap.apply {
            uiSettings.setMapToolbarEnabled(false)
            setIndoorEnabled(false)
        }
        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            initCamera()
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION)
        }
        googleMap.setOnMarkerClickListener(object : GoogleMap.OnMarkerClickListener {
            override fun onMarkerClick(marker: Marker): Boolean {
                if (mVehicleBlockNumber != null &&
                        mVehicleBlockNumber != (marker.tag as PresentableNexTrip?)?.blockNumber) {
                    deselectVehicle()
                }
                marker.showInfoWindow()
                return true
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
        initCamera()
    }

    fun selectVehicle(blockNumber: Int) {
        mVehicleBlockNumber = blockNumber
        mMap?.run {
            for (marker in mMarkers.values.map { it.first }) {
                val nexTrip = marker.tag as PresentableNexTrip
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
            for (marker in mMarkers.values.map { it.first }) {
                val nexTrip = marker.tag as PresentableNexTrip
                marker.alpha = if (mDoShowRoutes.get(Pair(nexTrip.route, nexTrip.terminal)) ?: true)
                    1f
                else
                    UNSELECTED_MARKER_ALPHA
            }
        }
    }

    private fun initCamera() {
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(TWIN_CITIES_LATLNG, TWIN_CITIES_ZOOM))
        mStop?.let {
            mMap?.addMarker(MarkerOptions()
                    .position(LatLng(it.stopLat, it.stopLon))
                    .title(resources.getString(R.string.stop_number) + it.stopId.toString())
                    .snippet(it.stopName)
                    .icon(drawableToBitmap(context!!, R.drawable.ic_stop))
            )// ?.apply {
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
        val shownMarkers = mMarkers.values.map { it.first }.filter {
            val nexTrip = it.tag as PresentableNexTrip
            val routeAndTerminal = Pair(nexTrip.route, nexTrip.terminal)
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
            .addOnFailureListener { zoomTo(latLngs, 5.236f) }
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
                mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
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
        for ((blockNumber, markerAndPosition) in mMarkers) {
            if (!mNexTrips!!.containsKey(blockNumber)) {
                markerAndPosition.first.remove()
                blockNumbersToRemove.add(blockNumber)
            }
        }
        blockNumbersToRemove.forEach { mMarkers.remove(it) }

        mMap?.run {
            for (nexTrip in mNexTrips!!.values) {
                val marker = if (mMarkers.containsKey(nexTrip.blockNumber)) {
                    mMarkers[nexTrip.blockNumber]!!.first.apply {
                        if (!NexTrip.distanceBetweenIsSmall(mMarkers[nexTrip.blockNumber]!!.second, nexTrip.position)) {
                            AnimationUtil.animateMarkerTo(this, nexTrip.position!!)
                        }
                    }
                } else {
                    addMarker(MarkerOptions()
                        .icon(getIcon(nexTrip.isTrain(), nexTrip.routeDirection))
                        .position(nexTrip.position!!)
                        .flat(true)
                        .anchor(0.5f, getBusIconAnchorVertical(nexTrip.routeDirection))
                    ).apply {
                        val routeAndTerminal = Pair(nexTrip.route, nexTrip.terminal)
                        if (mVehicleBlockNumber != null || !(mDoShowRoutes.get(routeAndTerminal) ?: true)) {
                            alpha = UNSELECTED_MARKER_ALPHA
                        }
                    }
                }.apply {
                    tag = nexTrip
                    title = "${nexTrip.routeAndTerminal} (${nexTrip.departureText})"
                    snippet = nexTrip.description
                    // force title to refresh
                    if (isInfoWindowShown()) {
                        showInfoWindow()
                    }
                }
                mMarkers[nexTrip.blockNumber] = Pair(marker, nexTrip.position!!)
            }
        }
    }

    fun onChangeHiddenRoutes(changedRoutes: Set<Pair<String?, String?>>) {
        if (mVehicleBlockNumber == null) {
            for (marker in mMarkers.values.map { it.first }) {
                val nexTrip = marker.tag as PresentableNexTrip
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

    private fun getIcon(isTrain: Boolean, direction: NexTrip.Direction?): BitmapDescriptor =
        if (isTrain) getTrainIcon(direction) else getBusIcon(direction)

    private fun getTrainIcon(direction: NexTrip.Direction?): BitmapDescriptor =
        when (direction) {
            NexTrip.Direction.SOUTH -> mTrainSouthIcon
            NexTrip.Direction.EAST  -> mTrainEastIcon
            NexTrip.Direction.WEST  -> mTrainWestIcon
            NexTrip.Direction.NORTH -> mTrainNorthIcon
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

    private fun getBusIconAnchorVertical(direction: NexTrip.Direction?): Float =
        // anchor at bottom of bus, not bottom of arrow
        if (direction == NexTrip.Direction.SOUTH) 0.8f else 1f
}
