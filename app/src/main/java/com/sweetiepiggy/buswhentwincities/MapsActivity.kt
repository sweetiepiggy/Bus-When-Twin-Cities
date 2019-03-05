package com.sweetiepiggy.buswhentwincities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : FragmentActivity(), OnMapReadyCallback, GoogleMap.OnMapLoadedCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private var mMap: GoogleMap? = null
    private var mMapLoaded = false
    private var mDoZoomOnMapLoaded = false
    private var mRouteAndTerminal: String? = null
    private var mDepartureText: String? = null
    private var mVehicleLatitude: Double = 0.toDouble()
    private var mVehicleLongitude: Double = 0.toDouble()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        if (savedInstanceState == null) {
            val b = intent.extras
            if (b != null) {
                loadState(b)
            }
        } else {
            loadState(savedInstanceState)
        }

        title = mRouteAndTerminal

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment!!.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val latLng = LatLng(mVehicleLatitude, mVehicleLongitude)
        mMap!!.addMarker(MarkerOptions().position(latLng).title(mRouteAndTerminal
                + " (" + mDepartureText + ")"))
                .showInfoWindow()
        //        mMap.setMaxZoomPreference(16);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            zoomIncludingMyLocation()
        } else {
            //            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
            //                                                                    Manifest.permission.ACCESS_FINE_LOCATION)) {
            //            } else {
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION)

            //            }
            zoomToVehicle()
        }
    }

    override fun onMapLoaded() {
        mMapLoaded = true
        if (mDoZoomOnMapLoaded) zoomIncludingMyLocation()
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putString("routeAndTerminal", mRouteAndTerminal)
        savedInstanceState.putString("departureText", mDepartureText)
        savedInstanceState.putDouble("vehicleLatitude", mVehicleLatitude)
        savedInstanceState.putDouble("vehicleLongitude", mVehicleLongitude)
    }

    private fun loadState(b: Bundle) {
        mRouteAndTerminal = b.getString("routeAndTerminal")
        mDepartureText = b.getString("departureText")
        mVehicleLatitude = b.getDouble("vehicleLatitude")
        mVehicleLongitude = b.getDouble("vehicleLongitude")
    }

    public override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        loadState(savedInstanceState)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_FINE_LOCATION -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mMap!!.isMyLocationEnabled = true
                    zoomIncludingMyLocation()
                }
                return
            }
        }
    }

    private fun zoomIncludingMyLocation() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val lastKnownLocation = locationManager
                .getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (lastKnownLocation == null) {
            zoomToVehicle()
        } else if (!mMapLoaded) {
            zoomToVehicle()
            mDoZoomOnMapLoaded = true
        } else {
            val myLocationLatLng = LatLng(lastKnownLocation.latitude,
                    lastKnownLocation.longitude)
            val vehicleLatLng = LatLng(mVehicleLatitude, mVehicleLongitude)
            val boundsBuilder = LatLngBounds.Builder()
            boundsBuilder.include(vehicleLatLng)
            boundsBuilder.include(myLocationLatLng)
            val bounds = boundsBuilder.build()
            val padding = 128
            mMap!!.isMyLocationEnabled = true
            mMap!!.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        }
    }

    private fun zoomToVehicle() {
        val latLng = LatLng(mVehicleLatitude, mVehicleLongitude)
        mMap!!.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    companion object {
        private val MY_PERMISSIONS_REQUEST_FINE_LOCATION = 0
    }
}

