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
import androidx.lifecycle.ViewModelProviders
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
    private var mDoShowRoutes: Map<String?, Boolean> = mapOf()
    private val mMarkers: MutableMap<Int?, Marker> = mutableMapOf()
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
            ViewModelProviders.of(this).get(NexTripsViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
        model.getNexTrips().observe(this, Observer<List<NexTrip>>{ updateNexTrips(it) })
        model.getDoShowRoutes().observe(this, Observer<Map<String?, Boolean>>{ updateDoShowRoutes(it) })

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
        mMap = googleMap
        googleMap.uiSettings.setMapToolbarEnabled(false)
        if (ContextCompat.checkSelfPermission(context!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            initCamera()
        } else {
            ActivityCompat.requestPermissions(activity!!,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION)
        }
        googleMap.setOnMarkerClickListener(object : GoogleMap.OnMarkerClickListener {
            override fun onMarkerClick(marker: Marker): Boolean {
                if (mVehicleBlockNumber != null &&
            			mVehicleBlockNumber != (marker.tag as PresentableNexTrip).blockNumber) {
                    deselectVehicle()
                }
                return false
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
            for (marker in mMarkers.values) {
                val nexTrip = marker.tag as PresentableNexTrip
                if (blockNumber == nexTrip.blockNumber) {
                    marker.alpha = 1f
                    marker.showInfoWindow()
                    zoomToVehicle(nexTrip)
                } else {
                    marker.alpha = UNSELECTED_MARKER_ALPHA
                }
            }
        }
    }

    private fun deselectVehicle() {
        mVehicleBlockNumber = null
        mMap?.run {
            for (marker in mMarkers.values) {
                val nexTrip = marker.tag as PresentableNexTrip
                marker.alpha = if (mDoShowRoutes.get(nexTrip.routeAndTerminal) ?: true)
                	1f
                else
                	UNSELECTED_MARKER_ALPHA
            }
        }
    }

    private fun initCamera() {
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(TWIN_CITIES_LATLNG, TWIN_CITIES_ZOOM))
        if (!mNexTrips.isNullOrEmpty()) {
            updateMarkers()
            zoomToAllVehicles()
        }
    }

    private fun zoomToAllVehicles() {
	    if (mMarkers.size == 1) {
            zoomToVehicle(mMarkers.values.elementAt(0).tag as PresentableNexTrip)
            return
        }

    	val latLngs = mMarkers.values.map { (it.tag as PresentableNexTrip).position!! }

        mFusedLocationClient.lastLocation
            .addOnSuccessListener { myLocation: Location? ->
                if (!latLngs.isEmpty()) {
                    val latLngsWithMyLoc = if (myLocation != null)
                    	latLngs + LatLng(myLocation.latitude, myLocation.longitude)
                    else latLngs
                    zoomTo(latLngsWithMyLoc, 5.236f)
                }
            }
            .addOnFailureListener { zoomTo(latLngs, 5.236f) }
    }

    private fun zoomToVehicle(nexTrip: PresentableNexTrip) {
        mFusedLocationClient.lastLocation
            .addOnSuccessListener { myLocation: Location? ->
                if (myLocation != null) {
                    zoomTo(listOf(nexTrip.position!!, LatLng(myLocation.latitude, myLocation.longitude)), 3f)
                } else {
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(nexTrip.position!!, 15f))
                }
            }
            .addOnFailureListener {
                mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(nexTrip.position!!, 15f))
            }
        }

    private fun zoomTo(latLngs: List<LatLng>, paddingRatio: Float) {
        if (!latLngs.isEmpty()) mMap?.run {
            val bounds = LatLngBounds.Builder().apply {
                latLngs.forEach { include(it) }
            }.build()
            val tv = TypedValue()
            activity!!.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)
            val actionBarHeight = complexToDimensionPixelSize(tv.data, resources.displayMetrics)
            val width = (resources.displayMetrics.widthPixels)
            val height = resources.displayMetrics.heightPixels - actionBarHeight
            val mapWidth = if (width > height) (width * 0.618).toInt() else width
            val padding = (minOf(mapWidth, height) / paddingRatio).toInt()
            animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, mapWidth, height, padding))
        }
    }

    fun updateNexTrips(nexTrips: List<NexTrip>) {
        val nexTripsWithActualPosition = nexTrips.filter { it.isActual && it.position != null }
        val doInitCamera = mNexTrips == null && !nexTripsWithActualPosition.isEmpty()
        if (mNexTrips == null) mNexTrips = mutableMapOf()
        mNexTrips!!.clear()
        val timeInMillis = Calendar.getInstance().timeInMillis
        nexTripsWithActualPosition.forEach {
            mNexTrips!![it.blockNumber] = PresentableNexTrip(it, timeInMillis, context!!)
        }
        updateMarkers()
        if (doInitCamera) initCamera()
    }

    fun updateDoShowRoutes(doShowRoutes: Map<String?, Boolean>) {
        mDoShowRoutes = doShowRoutes
    }

    private fun updateMarkers() {
        val blockNumbersToRemove = mutableListOf<Int?>()
        for ((blockNumber, marker) in mMarkers) {
            if (!mNexTrips!!.containsKey(blockNumber)) {
                marker.remove()
                blockNumbersToRemove.add(blockNumber)
            }
        }
        blockNumbersToRemove.forEach { mMarkers.remove(it) }

        mMap?.run {
            for (nexTrip in mNexTrips!!.values) {
                val marker = if (mMarkers.containsKey(nexTrip.blockNumber)) {
                    mMarkers[nexTrip.blockNumber]!!.apply {
                        AnimationUtil.animateMarkerTo(this, nexTrip.position!!)
                    }
                } else {
                    addMarker(MarkerOptions()
                        .icon(getBusIcon(nexTrip.routeDirection))
                        .position(nexTrip.position!!)
                    	.flat(true)
                    ).apply { if (mVehicleBlockNumber != null) alpha = UNSELECTED_MARKER_ALPHA }
                }.apply {
                    tag = nexTrip
                    title = "${nexTrip.routeAndTerminal} (${nexTrip.departureText})"
                    snippet = nexTrip.description
                    // force title to refresh
                    if (isInfoWindowShown()) {
                        showInfoWindow()
                    }
                }
                mMarkers[nexTrip.blockNumber] = marker
            }
        }
    }

    fun onChangeHiddenRoutes(changedRoutes: Set<String>) {
        if (mVehicleBlockNumber == null) {
            for (marker in mMarkers.values) {
                val nexTrip = marker.tag as PresentableNexTrip
                if (changedRoutes.contains(nexTrip.routeAndTerminal)) {
                    marker.alpha = if (mDoShowRoutes.get(nexTrip.routeAndTerminal) ?: true)
            	    	1f
                    else
        	        	UNSELECTED_MARKER_ALPHA
                }
            }
        }
    }

    private fun getBusIcon(direction: NexTrip.Direction?): BitmapDescriptor =
    	when (direction) {
            NexTrip.Direction.SOUTH -> mBusSouthIcon
            NexTrip.Direction.EAST  -> mBusEastIcon
            NexTrip.Direction.WEST  -> mBusWestIcon
            NexTrip.Direction.NORTH -> mBusNorthIcon
            else -> mBusIcon
        }
}
