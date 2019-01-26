package com.sweetiepiggy.buswhentwincities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsActivity extends FragmentActivity
    implements OnMapReadyCallback, ActivityCompat.OnRequestPermissionsResultCallback {
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 0;

    private GoogleMap mMap;
    private String mRouteAndTerminal;
    private String mDepartureText;
    private double mVehicleLatitude;
    private double mVehicleLongitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (savedInstanceState == null) {
            Bundle b = getIntent().getExtras();
            if (b != null) {
                loadState(b);
            }
        } else {
            loadState(savedInstanceState);
        }

        setTitle(mRouteAndTerminal);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng latLng = new LatLng(mVehicleLatitude, mVehicleLongitude);
        mMap.addMarker(new MarkerOptions().position(latLng).title(mRouteAndTerminal
                                                                  + " (" + mDepartureText+ ")"))
            .showInfoWindow();
        mMap.setMaxZoomPreference(16);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            zoomIncludingMyLocation();
        } else {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
//                                                                    Manifest.permission.ACCESS_FINE_LOCATION)) {
//            } else {
                ActivityCompat.requestPermissions(this,
                                                  new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                                  MY_PERMISSIONS_REQUEST_FINE_LOCATION);

//            }
                zoomToVehicle();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString("routeAndTerminal", mRouteAndTerminal);
        savedInstanceState.putString("departureText", mDepartureText);
        savedInstanceState.putDouble("vehicleLatitude", mVehicleLatitude);
        savedInstanceState.putDouble("vehicleLongitude", mVehicleLongitude);
    }

    private void loadState(Bundle b) {
        mRouteAndTerminal = b.getString("routeAndTerminal");
        mDepartureText = b.getString("departureText");
        mVehicleLatitude = b.getDouble("vehicleLatitude");
        mVehicleLongitude = b.getDouble("vehicleLongitude");
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        loadState(savedInstanceState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        switch (requestCode) {
        case MY_PERMISSIONS_REQUEST_FINE_LOCATION:
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mMap.setMyLocationEnabled(true);
                zoomIncludingMyLocation();
            }
            return;
        }
    }

    private void zoomIncludingMyLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location lastKnownLocation = locationManager
            .getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnownLocation == null) {
            zoomToVehicle();
        } else {
            LatLng myLocationLatLng = new LatLng(lastKnownLocation.getLatitude(),
                                                 lastKnownLocation.getLongitude());
            LatLng vehicleLatLng = new LatLng(mVehicleLatitude, mVehicleLongitude);
            LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
            boundsBuilder.include(vehicleLatLng);
            boundsBuilder.include(myLocationLatLng);
            LatLngBounds bounds = boundsBuilder.build();
            int padding = 128;
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
        }
    }

    private void zoomToVehicle() {
        LatLng latLng = new LatLng(mVehicleLatitude, mVehicleLongitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }
}
