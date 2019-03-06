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
import android.location.LocationManager
import android.os.Bundle
import android.util.TypedValue
import android.util.TypedValue.complexToDimensionPixelSize
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions

class MyMapFragment : Fragment(), OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private var mMap: GoogleMap? = null
    private var mRouteAndTerminal: String? = null
    private var mDepartureText: String? = null
    private var mVehicleLatitude: Double = 0.toDouble()
    private var mVehicleLongitude: Double = 0.toDouble()

    companion object {
        fun newInstance() = MyMapFragment()
        private val MY_PERMISSIONS_REQUEST_FINE_LOCATION = 0
        private val KEY_ROUTE_AND_TERMINAL = "routeAndTerminal"
        private val KEY_DEPARTURE_TEXT = "departureText"
        private val KEY_VEHICLE_LATITUDE = "vehicleLatitude"
        private val KEY_VEHICLE_LONGITUDE = "vehicleLongitude"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.maps_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if (savedInstanceState == null) {
            val b = getArguments()
            b?.let { loadState(it) }
        } else {
            loadState(savedInstanceState)
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putString(KEY_ROUTE_AND_TERMINAL, mRouteAndTerminal)
        savedInstanceState.putString(KEY_DEPARTURE_TEXT, mDepartureText)
        savedInstanceState.putDouble(KEY_VEHICLE_LATITUDE, mVehicleLatitude)
        savedInstanceState.putDouble(KEY_VEHICLE_LONGITUDE, mVehicleLongitude)
    }

    private fun loadState(b: Bundle) {
        mRouteAndTerminal = b.getString(KEY_ROUTE_AND_TERMINAL)
        mDepartureText = b.getString(KEY_DEPARTURE_TEXT)
        mVehicleLatitude = b.getDouble(KEY_VEHICLE_LATITUDE)
        mVehicleLongitude = b.getDouble(KEY_VEHICLE_LONGITUDE)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        if (mRouteAndTerminal != null) {
            val latLng = LatLng(mVehicleLatitude, mVehicleLongitude)
            mMap?.addMarker(MarkerOptions().position(latLng).title(mRouteAndTerminal
    	        	+ " (" + mDepartureText + ")"))
	            ?.showInfoWindow()
        }
        if (ContextCompat.checkSelfPermission(getActivity()?.getApplicationContext()!!, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            zoomIncludingMyLocation()
        } else {
            ActivityCompat.requestPermissions(getActivity()!!,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION)
            zoomToVehicle()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_FINE_LOCATION -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    zoomIncludingMyLocation()
                }
                return
            }
        }
    }

    private fun zoomIncludingMyLocation() {
        mMap?.isMyLocationEnabled = true
        val locationManager = getActivity()?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val lastKnownLocation = locationManager
                .getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (lastKnownLocation == null) {
            zoomToVehicle()
        } else if (mRouteAndTerminal != null) {
            val myLocationLatLng = LatLng(lastKnownLocation.latitude,
                    lastKnownLocation.longitude)
            val vehicleLatLng = LatLng(mVehicleLatitude, mVehicleLongitude)
            val boundsBuilder = LatLngBounds.Builder()
            boundsBuilder.include(vehicleLatLng)
            boundsBuilder.include(myLocationLatLng)
            val bounds = boundsBuilder.build()
            val tv = TypedValue()
            getActivity()!!.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)
            val actionBarHeight = complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics())
            val width = getResources().getDisplayMetrics().widthPixels
            val height = getResources().getDisplayMetrics().heightPixels - actionBarHeight
            mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, 0))
            mMap?.moveCamera(CameraUpdateFactory.zoomTo(mMap?.getCameraPosition()!!.zoom - 0.5f))
        } else {
            val myLocationLatLng = LatLng(lastKnownLocation.latitude, lastKnownLocation.longitude)
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocationLatLng, 15f))
        }
    }

    private fun zoomToVehicle() {
        if (mRouteAndTerminal != null) {
            val latLng = LatLng(mVehicleLatitude, mVehicleLongitude)
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
        }
    }

    fun updateVehicle(b: Bundle) {
        loadState(b)
        val latLng = LatLng(mVehicleLatitude, mVehicleLongitude)
        mMap?.clear()
        mMap?.addMarker(MarkerOptions().position(latLng).title(mRouteAndTerminal
                + " (" + mDepartureText + ")"))
                ?.showInfoWindow()
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }
}
