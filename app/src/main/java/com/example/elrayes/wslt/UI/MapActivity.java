package com.example.elrayes.wslt.UI;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import com.example.elrayes.wslt.Geofence.Geofencing;
import com.example.elrayes.wslt.PlaceAutoCompleteAdapter;
import com.example.elrayes.wslt.R;
import com.example.elrayes.wslt.Util.SharedPreferencesHelper;
import com.github.clans.fab.FloatingActionButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.PlaceBufferResponse;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener, View.OnClickListener, GoogleApiClient.OnConnectionFailedListener, View.OnLongClickListener {

    public static final int REQUEST_PERMISSION_CODE = 2000;
    public static final String FINE_LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final String COARSE_LOCATION_PERMISSION = Manifest.permission.ACCESS_COARSE_LOCATION;
    public static final int REQUEST_CHECK_SETTINGS = 1000;
    private static final String TAG = "MapActivity";
    private static final int ERROR_DIALOG_REQUEST = 1404;
    public static boolean locationPermissionGranted = false;
    public static boolean locationEnabled = false;
    private static boolean isAlarmEnabled = false;
    private static LatLng destination;
    private static Marker marker;
    FloatingActionButton fab;
    PlaceAutoCompleteAdapter adapter;
    AutoCompleteTextView search;
    GoogleMap map;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private GoogleApiClient mClient;
    private Geofencing mGeofencing;
    private AdapterView.OnItemClickListener mAutoCompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            final AutocompletePrediction item = adapter.getItem(i);
            final String placeId = item.getPlaceId();
            Places.getGeoDataClient(MapActivity.this).getPlaceById(placeId).addOnSuccessListener(new OnSuccessListener<PlaceBufferResponse>() {
                @Override
                public void onSuccess(PlaceBufferResponse places) {
                    if (places.getCount() > 0) {
                        if (marker != null)
                            marker.remove();
                        map.clear();
                        marker = map.addMarker(new MarkerOptions().position(places.get(0).getLatLng()));
                        destination = places.get(0).getLatLng();
                        moveCamera(places.get(0).getLatLng());
                        search.setHint(search.getText().toString());
                        search.setText(null);
                    }
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        search = findViewById(R.id.input_search);
        search.setOnItemClickListener(mAutoCompleteClickListener);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(this);
        fab.setOnLongClickListener(this);
        settingUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SharedPreferencesHelper.clearAllSavedSharedData(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        String data = SharedPreferencesHelper.retrieveDataFromSharedPref(this, "geofence");

        if (data == null)
            return;

        if (data.equals("off")) {
            if (map != null)
                map.clear();
            destination = null;
            marker = null;
            isAlarmEnabled = false;
        } else {
            isAlarmEnabled = true;
        }
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (locationPermissionGranted) {

                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), 15f));

                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void settingUp() {
        if (checkPlayServicesAvailability()) {
            if (checkPermissions()) {
                locationPermissionGranted = true;
                checkLocationEnabled();
            } else
                requestPermissions();
        }
    }

    private boolean checkLocationEnabled() {

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        SettingsClient client = LocationServices.getSettingsClient(this);
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                locationEnabled = true;
                init();
            }
        });

        task.addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings are not satisfied, but this can be fixed
                    // by showing the user a dialog.
                    try {
                        // Show the dialog by calling startResolutionForResult(),
                        // and check the result in onActivityResult().
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(MapActivity.this,
                                REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        // Ignore the error.
                    }
                }
            }
        });
        return false;
    }

    boolean checkPlayServicesAvailability() {
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);

        if (available == ConnectionResult.SUCCESS) {
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(this, available, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    void init() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        adapter = new PlaceAutoCompleteAdapter(
                this,
                Places.getGeoDataClient(this),
                new LatLngBounds(
                        new LatLng(-40, -168),
                        new LatLng(71, 136)),
                null);
        search.setAdapter(adapter);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                getDeviceLocation();
            }
        }, 1500);
        mClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .enableAutoManage(this, this)
                .build();
        mClient.connect();
        mGeofencing = Geofencing.getInstance(this, mClient);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        if (locationPermissionGranted) {
            //getDeviceLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;

            }
            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(false);
            map.setOnMapLongClickListener(this);
        }
    }

    private void requestPermissions() {

        // Creating String Array with Permissions.
        ActivityCompat.requestPermissions(this, new String[]
                {
                        FINE_LOCATION_PERMISSION,
                        COARSE_LOCATION_PERMISSION
                }, REQUEST_PERMISSION_CODE);

    }

    public boolean checkPermissions() {


        Log.d(TAG, "CheckPermissions: checking permissions");
        int FirstPermissionResult = ContextCompat.checkSelfPermission(getApplicationContext(), FINE_LOCATION_PERMISSION);
        int SecondPermissionResult = ContextCompat.checkSelfPermission(getApplicationContext(), COARSE_LOCATION_PERMISSION);

        Log.d(TAG, "CheckPermissions: " + Boolean.toString(FirstPermissionResult == PackageManager.PERMISSION_GRANTED) + " " +
                Boolean.toString(SecondPermissionResult == PackageManager.PERMISSION_GRANTED));
        return FirstPermissionResult == PackageManager.PERMISSION_GRANTED &&
                SecondPermissionResult == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {

            case REQUEST_PERMISSION_CODE:

                if (grantResults.length > 0) {

                    boolean FineLocationPermission = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean CoarseLocationPermission = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (FineLocationPermission && CoarseLocationPermission) {

                        Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show();
                        locationPermissionGranted = true;
                        checkLocationEnabled();
                    } else {
                        Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show();

                    }
                }

                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);

        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK: {
                        locationEnabled = true;
                        init();
                        break;
                    }
                    case Activity.RESULT_CANCELED: {
                        // The user was asked to change settings, but chose not to
                        Toast.makeText(this, "Turn on Location for the app to work", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    default: {
                        Toast.makeText(this, "Turn on Location for the app to work", Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                break;
        }

    }


    void moveCamera(LatLng latLng) {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        map.addMarker(new MarkerOptions().position(latLng));
    }

    @Override
    public void onMapLongClick(LatLng latLng) {

        if (!isAlarmEnabled) {
            if (marker != null)
                map.clear();
            marker = map.addMarker(new MarkerOptions().position(latLng));
            destination = latLng;
            CircleOptions circleOptions = new CircleOptions();

            // Specifying the center of the circle
            circleOptions.center(destination);

            // Radius of the circle
            circleOptions.radius(1000);

            // Border color of the circle
            circleOptions.strokeColor(Color.BLACK);

            // Fill color of the circle
            circleOptions.fillColor(0x30ff0000);

            // Border width of the circle
            circleOptions.strokeWidth(2);

            // Adding the circle to the GoogleMap
            map.addCircle(circleOptions);
        } else {
            Toast.makeText(this, "There is an alarm already enabled , long press on alarm icon to cancel it", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View view) {
        if (destination == null)
            Toast.makeText(this, "Please Select a destination by long pressing on map", Toast.LENGTH_SHORT).show();

        else if (isAlarmEnabled)
            Toast.makeText(this, "There is an alarm already enabled , long press on alarm icon to cancel it", Toast.LENGTH_SHORT).show();

        else {
            FragmentManager fragmentManager = getSupportFragmentManager();
            SetAlarmFragment setAlarmFragment = new SetAlarmFragment();
            setAlarmFragment.show(fragmentManager, "alarm");
        }
    }

    void setAlarm(int radius) {
        mGeofencing.updateGeofencesList(destination);
        mGeofencing.registerAllGeofences();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e(TAG, "API Client Connection Failed!");
    }

    @Override
    public boolean onLongClick(View view) {
        if (isAlarmEnabled) {
            Geofencing.getInstance(this, mClient).unRegisterAllGeofences();
            Toast.makeText(this, "Alarm has been disabled", Toast.LENGTH_SHORT).show();
            isAlarmEnabled = false;
            map.clear();
        }
        return true;
    }

    public void enableAlarm() {
        isAlarmEnabled = true;
    }
}
