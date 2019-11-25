package com.example.persometer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback{

    private static final String TAG = "MapsActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;

    private static final float DEFAULT_ZOOM = 15f;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final String API_KEY = BuildConfig.ApiKey;

    private boolean mLocationPermissionGranted = false;   //location permission flag (false by default)
    private GoogleMap mMap;
    private LatLng current;    //current position of the device

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        getLocationPermission();   //sets and gets the location permission flag

        Button BtnGoHome = (Button) findViewById(R.id.btn_goHome);
        BtnGoHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                finish();  //returns to MainActivity
            }
        });

        initMap();  //initialises map
    }

    //gets the current position of the device
    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the device's current location");

        FusedLocationProviderClient mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if(mLocationPermissionGranted) {
                Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location");
                            Location currentLocation = (Location) task.getResult();

                            current = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());

                            moveCamera(current,DEFAULT_ZOOM);

                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapsActivity.this, "Unable to get location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: Security exception: " + e.getMessage());
        }
    }

    //the map is being moved according to the current position of the device
    private void moveCamera(LatLng latLng, float zoom) {
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + " lng: " + latLng.longitude);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    //refreshes content every 200ms for a better
    public void content() {
        refresh(200);
    }

    private void refresh(int milisecs) {
        final Handler handler = new Handler();
        final Runnable runnable =  new Runnable() {
            @Override
            public void run() {
                content();
            }
        };
    }

    //all requests are being verified
    //if permissions were granted, the map is displayed
    //also a toast is displayed
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: Map is ready");
        mMap = googleMap;

        try {
            boolean success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle));

            if (!success) {
                Log.d(TAG, "Can't parse style properly");
            }

        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Can't find style. Error: ", e);
        }

        if (mLocationPermissionGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setAllGesturesEnabled(true);
            mMap.getUiSettings().setIndoorLevelPickerEnabled(true);
        }
    }

    //initialises the map
    private void initMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        Log.d(TAG, "initMap: Initializing map");
        mapFragment.getMapAsync(MapsActivity.this);
    }

    //gets permission to use GPS and Location
    private void getLocationPermission(){
        String[] permissions = {FINE_LOCATION,COARSE_LOCATION};

        Log.d(TAG, "getLocationPermissions: Getting location permissions");
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {   //checks if FINE_LOCATION permission was granted
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {   //checks if COARSE_LOCATION permission was granted
                mLocationPermissionGranted = true;    //sets the flag
                initMap();
            } else {
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
            }
        }  else {
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    //requests permission to access LOCATION
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionResult: Called");

        mLocationPermissionGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if(grantResults.length > 0) {
                    for(int i=0; i<grantResults.length; i++) {
                        if(grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionGranted = false;
                            Log.d(TAG, "onRequestPermissionResult: Permission failed");
                            return;
                        }
                    }

                    mLocationPermissionGranted = true;
                    Log.d(TAG, "onRequestPermissionResult: Permission granted");
                    initMap();
                }
            }
        }
    }
}
