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

class MyMapFragment : Fragment(), OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private var mMap: GoogleMap? = null
    private var mVehicleBlockNumber: Int? = null
    private var mNexTrips: List<NexTrip>? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val mMarkers: MutableList<Marker> = mutableListOf()
    private val mBusIcon: BitmapDescriptor by lazy {
    	BitmapDescriptorFactory.fromBitmap(
            getDrawable(activity!!.applicationContext, R.drawable.ic_baseline_directions_bus_24px
        )!!.toBitmap())
    }

    companion object {
        fun newInstance() = MyMapFragment()
        private val MY_PERMISSIONS_REQUEST_LOCATION = 0
        private val KEY_BLOCK_NUMBER = "blockNumber"
        private val UNSELECTED_MARKER_ALPHA = 0.5f
        private val TWIN_CITIES_LATLNG = LatLng(44.950864, -93.187336)
        private val TWIN_CITIES_ZOOM = 11f
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

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(activity!!.getApplicationContext())

        val model = activity?.run {
            ViewModelProviders.of(this).get(NexTripsViewModel::class.java)
        } ?: throw Exception("Invalid Activity")
        model.getNexTrips().observe(this, Observer<List<NexTrip>>{ updateNexTrips(it) })

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        mVehicleBlockNumber?.let { savedInstanceState.putInt(KEY_BLOCK_NUMBER, it) }
    }

    private fun loadState(b: Bundle) {
        mVehicleBlockNumber = b.getInt(KEY_BLOCK_NUMBER)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // if (mRouteAndTerminal != null) {
        //     val latLng = LatLng(mVehicleLatitude, mVehicleLongitude)
        //     mMap?.addMarker(MarkerOptions().position(latLng).title(mRouteAndTerminal
    	//         	+ " (" + mDepartureText + ")"))
	    //         ?.showInfoWindow()
        // }
        if (ContextCompat.checkSelfPermission(activity!!.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
            initCamera()
            // zoomIncludingMyLocation()
        } else {
            ActivityCompat.requestPermissions(activity!!,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION)
            // zoomToVehicle()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_LOCATION ->
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mMap?.isMyLocationEnabled = true
                    // zoomIncludingMyLocation()
                }
        }
        initCamera()
    }
    // private fun zoomIncludingMyLocation() {
    //     val locationManager = getActivity()?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    //     val lastKnownLocation = locationManager
    //             .getLastKnownLocation(LocationManager.GPS_PROVIDER)
    //     if (lastKnownLocation == null) {
    //         zoomToVehicle()
    //     } else if (mRouteAndTerminal != null) {
    //         val myLocationLatLng = LatLng(lastKnownLocation.latitude,
    //                 lastKnownLocation.longitude)
    //         val vehicleLatLng = LatLng(mVehicleLatitude, mVehicleLongitude)
    //         val boundsBuilder = LatLngBounds.Builder()
    //         boundsBuilder.include(vehicleLatLng)
    //         boundsBuilder.include(myLocationLatLng)
    //         val bounds = boundsBuilder.build()
    //         val tv = TypedValue()
    //         getActivity()!!.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)
    //         val actionBarHeight = complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics())
    //         val width = getResources().getDisplayMetrics().widthPixels
    //         val height = getResources().getDisplayMetrics().heightPixels - actionBarHeight
    //         mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, 0))
    //         mMap?.moveCamera(CameraUpdateFactory.zoomTo(mMap?.getCameraPosition()!!.zoom - 0.5f))
    //     } else {
    //         val myLocationLatLng = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
    //         mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocationLatLng, 15f))
    //     }
    // }

    // private fun zoomToVehicle() {
    //     if (mRouteAndTerminal != null) {
    //         val latLng = LatLng(mVehicleLatitude, mVehicleLongitude)
    //         mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    //     } else {
    //         mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(TWIN_CITIES_LATLNG, TWIN_CITIES_ZOOM))
    //     }
    // }

    fun selectVehicle(blockNumber: Int) {
        mVehicleBlockNumber = blockNumber
        // mMap?.run {
        //     val latLng = LatLng(mVehicleLatitude, mVehicleLongitude)
        //     clear()
        //     addMarker(MarkerOptions().position(latLng).title(mRouteAndTerminal
        //     		+ " (" + mDepartureText + ")"))
        //         	?.showInfoWindow()
        //     animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        // }

        mMap?.run {
            for (marker in mMarkers) {
                val nexTrip = marker.tag as NexTrip
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

    private fun initCamera() {
        android.util.Log.d("abc", "got here: initCamera mNexTrips.isEmpty() == ${mNexTrips?.isEmpty()}")
        if (mNexTrips.isNullOrEmpty()) {
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(TWIN_CITIES_LATLNG, TWIN_CITIES_ZOOM))
        } else {
            updateMarkers()
            zoomToAllVehicles()
        }
    }

    private fun zoomToAllVehicles() {
//        mMap?.run {
//            for (marker in mMarkers) {
//                val nexTrip = marker.tag as NexTrip
//                if (blockNumber == nexTrip.blockNumber) {
//                    marker.alpha = 1f
//                    marker.showInfoWindow()
//                    zoomToVehicle(nexTrip)
//                } else {
//                    marker.alpha = UNSELECTED_MARKER_ALPHA
//                }
//            }
//        }

	    if (mMarkers.size == 1) {
            zoomToVehicle(mMarkers[0].tag as NexTrip)
            return
        }

    	val latLngs = mMarkers.map {
        	val nexTrip = it.tag as NexTrip
            LatLng(nexTrip.vehicleLatitude, nexTrip.vehicleLongitude)
        }

        mFusedLocationClient.lastLocation
            .addOnSuccessListener { myLocation: Location? ->
                if (!latLngs.isEmpty()) {
                    val latLngsWithMyLoc = if (myLocation != null)
                    	latLngs + LatLng(myLocation.latitude, myLocation.longitude)
                    else latLngs
                    // myLocation?.let { latLngs.add(LatLng(it.latitude, it.longitude)) }
                    android.util.Log.d("abc", "got here: zoomToAllVehicles with location: ${latLngs.size}, ${myLocation}")
                    zoomTo(latLngsWithMyLoc, 5.236f)
                }
            }
            .addOnFailureListener { android.util.Log.d("abc", "got here: zoomToAllVehicles no location"); zoomTo(latLngs, 5.236f) }
    }

    private fun zoomToVehicle(nexTrip: NexTrip) {
        val vehicleLatLng = LatLng(nexTrip.vehicleLatitude, nexTrip.vehicleLongitude)

        mFusedLocationClient.lastLocation
            .addOnSuccessListener { myLocation: Location? ->
                if (myLocation != null) {
                    zoomTo(listOf(vehicleLatLng, LatLng(myLocation.latitude, myLocation.longitude)), 3f)
                } else {
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(vehicleLatLng, 15f))
                }
//                myLocation?.let { latLngs.add(LatLng(it.latitude, it.longitude)) }
//                android.util.Log.d("abc", "got here: zoomToVehicle with location: ${myLocation}")
//                zoomTo(latLngs)
            }
            .addOnFailureListener {android.util.Log.d("abc", "got here: zoomToVehicle no location"); mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(vehicleLatLng, 15f)) }

        // mFusedLocationClient.lastLocation
        //     .addOnSuccessListener { myLocation: Location? ->
        //         mMap?.run {
        //             if (myLocation != null) {
        //                 val bounds = LatLngBounds.Builder().apply {
        //                     include(LatLng(nexTrip.vehicleLatitude, nexTrip.vehicleLongitude))
        //                     include(LatLng(myLocation.latitude, myLocation.longitude))
        //                 }.build()
        //                 val tv = TypedValue()
        //                 activity!!.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)
        //                 val actionBarHeight = complexToDimensionPixelSize(tv.data, resources.displayMetrics)
        //                 val width = resources.displayMetrics.widthPixels
        //                 val height = resources.displayMetrics.heightPixels - actionBarHeight
        //                 val padding = minOf(width, height) / 3
        //                 animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        //             } else {
        //                 val vehicleLatLng = LatLng(nexTrip.vehicleLatitude, nexTrip.vehicleLongitude)
        //                 animateCamera(CameraUpdateFactory.newLatLngZoom(vehicleLatLng, 15f))
        //             }
        //         }
        //     }
        //     .addOnFailureListener {
        //         mMap?.run {
        //             val vehicleLatLng = LatLng(nexTrip.vehicleLatitude, nexTrip.vehicleLongitude)
        //             animateCamera(CameraUpdateFactory.newLatLngZoom(vehicleLatLng, 15f))
        //         }
        //     }
        }

    private fun zoomTo(latLngs: List<LatLng>, paddingRatio: Float) {
        android.util.Log.d("abc", "got here: zoomTo(), mMap: $mMap")
        mMap?.run {
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
        val doInitCamera = mNexTrips == null && !nexTrips.isEmpty()
        mNexTrips = nexTrips.filter { it.isActual }
        updateMarkers()
        if (doInitCamera) initCamera()
    }

    private fun updateMarkers() {
        mMarkers.clear()
        mMap?.run {
            clear()
            // val boundsBuilder = LatLngBounds.Builder()
            for (nexTrip in (mNexTrips ?: listOf())) {
                val latLng = LatLng(nexTrip.vehicleLatitude, nexTrip.vehicleLongitude)
                val marker = addMarker(MarkerOptions()
                        // .tag(nexTrip)
                        // .visible(mVehicleBlockNumber == null || mVehicleBlockNumber == nexTrip.blockNumber)
                        .icon(mBusIcon)
                        .position(latLng)
                        .title("${nexTrip.route}${nexTrip.terminal} (${nexTrip.departureText})")
                        .snippet("${nexTrip.description}")
                    )?.apply {
                        tag = nexTrip
                        if (mVehicleBlockNumber == nexTrip.blockNumber || mNexTrips?.size == 1) {
                            showInfoWindow()
                        } else if (mVehicleBlockNumber != null) {
                            alpha = UNSELECTED_MARKER_ALPHA
                        }
                    }
                marker?.let { mMarkers.add(it) }
                // boundsBuilder.include(latLng)
            }
            // animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 0))
            // animateCamera(CameraUpdateFactory.zoomTo(getCameraPosition()!!.zoom - 0.5f))
        }
    }
}
