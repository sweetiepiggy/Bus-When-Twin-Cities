package com.sweetiepiggy.buswhentwincities

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {

    private var mMap: GoogleMap? = null
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
            val options = GoogleMapOptions()
            options.zoomControlsEnabled(true)
            // options.mapToolbarEnabled(true)
            // options.compassEnabled(true)

            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            val fragment = SupportMapFragment.newInstance(options)
            supportFragmentManager.beginTransaction()
            		.add(R.id.container, fragment)
            		.commitNow()
            fragment.getMapAsync(this)
        } else {
            loadState(savedInstanceState)
        }
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

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val latLng = LatLng(mVehicleLatitude, mVehicleLongitude)
        mMap?.addMarker(MarkerOptions().position(latLng).title(mRouteAndTerminal
                + " (" + mDepartureText + ")"))
                ?.showInfoWindow()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            zoomIncludingMyLocation()
        } else {
            ActivityCompat.requestPermissions(this,
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
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val lastKnownLocation = locationManager
                .getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (lastKnownLocation == null) {
            zoomToVehicle()
        } else {
            val myLocationLatLng = LatLng(lastKnownLocation.latitude,
                    lastKnownLocation.longitude)
            val vehicleLatLng = LatLng(mVehicleLatitude, mVehicleLongitude)
            val boundsBuilder = LatLngBounds.Builder()
            boundsBuilder.include(vehicleLatLng)
            boundsBuilder.include(myLocationLatLng)
            val bounds = boundsBuilder.build()
            mMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0))
            mMap?.moveCamera(CameraUpdateFactory.zoomTo(mMap?.getCameraPosition()!!.zoom - 0.5f))
        }
    }

    private fun zoomToVehicle() {
        val latLng = LatLng(mVehicleLatitude, mVehicleLongitude)
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    }

    companion object {
        private val MY_PERMISSIONS_REQUEST_FINE_LOCATION = 0
    }
}

