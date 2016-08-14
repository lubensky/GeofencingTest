package com.example.lubensky.geofencingtest;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofenceStatusCodes;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, // for GoogleApiClient.Builder.addConnectionCallback
        GoogleApiClient.OnConnectionFailedListener, // for GoogleApiClient.Builder.addOnConnectionFailedListener
        LocationListener, // for LocationServices.FusedLocationApi.requestLocationUpdates
        ResultCallback<Status> { // for GeofencingApi.setResultCallback
    static final private String TAG = "MainActivity";
    static final private int RADIUS = 50;
    static final private int VIBRATE_DURATION_ENTER = 10000;
    static final private int VIBRATE_DURATION_EXIT = 5000;
    // can be small for high accuracy, but not too small for low accuracy, otherwise
    // GETTING_CLOSER and MOVING_AWAY will be triggered rarely
    static final private int LOCATION_UPDATE_INTERVAL = 5000;
    // prevent constant status changes from small / faulty position changes
    // home
    static final private float DEFAULT_LATITUDE = 51.071584f;
    static final private float DEFAULT_LONGITUDE = 13.731136f;
    // monkey works
//    static final private float DEFAULT_LATITUDE = 51.08685f;
//    static final private float DEFAULT_LONGITUDE = 13.76392f;
    // keys for preferences
    static final private String LAST_GEOFENCE_TRANSITION = "lastGeofenceTransition";
    static final private String LAST_STATUS = "lastStatus";
    // motion status
    static final private int GETTING_CLOSER = 0;
    static final private int MOVING_AWAY = 1;
    static final private int DWELLING = 2;
    static final private int LEAVING_GEOFENCE = 3;
    // for the GeofenceTransitionIntentSerive
    static final public String GEOFENCE_TRANSITION_ACTION =
            "com.example.lubensky.geofencingtest.GEOFENCE_TRANSITION_ACTION";
    static final public String GEOFENCE_TRANSITION =
            "com.example.lubensky.geofencingtest.GEOFENCE_TRANSITION";

    private int lastGeofenceTransition = -1;
    private float lastDistanceFromPointOfInterest = 0.0f;
    private int lastStatus = LEAVING_GEOFENCE;
    private LatLng pointOfInterest;
    // communication with GeofenceTransitionsIntentService
    private BroadcastReceiver receiver;
    private GoogleApiClient client;
    private PendingIntent geofencingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState != null) {
            lastGeofenceTransition = savedInstanceState.getInt( LAST_GEOFENCE_TRANSITION);
            lastStatus = savedInstanceState.getInt( LAST_STATUS);
        }

        restorePrefenrences();
        setMotionStatus( lastStatus);
        buildBroadcastReceiver();
        buildGoogleApiClient();
    }

    private void restorePrefenrences() {
        SharedPreferences preferences = getPreferences( MODE_PRIVATE);
        double latitude = preferences.getFloat( "latitude", DEFAULT_LATITUDE);
        double longitude = preferences.getFloat( "longitude", DEFAULT_LONGITUDE);

        pointOfInterest = new LatLng( latitude, longitude);

        EditText latitudeText = (EditText) findViewById( R.id.latitudeText);
        EditText longitudeText = (EditText) findViewById( R.id.longitudeText);
        latitudeText.setText( String.valueOf( latitude));
        longitudeText.setText( String.valueOf( longitude));
    }

    private void storePreferences() {
        SharedPreferences preferencs = getPreferences( MODE_PRIVATE);
        SharedPreferences.Editor editor = preferencs.edit();
        editor.putFloat( "latitude", (float)pointOfInterest.latitude);
        editor.putFloat( "longitude", (float)pointOfInterest.longitude);

        editor.apply();
    }

    private void readPointOfInterestFromUser() {
        EditText latitudeText = (EditText) findViewById( R.id.latitudeText);
        EditText longitudeText = (EditText) findViewById( R.id.longitudeText);
        double latitude = Double.valueOf( latitudeText.getText().toString());
        double longitude = Double.valueOf( longitudeText.getText().toString());

        pointOfInterest = new LatLng( latitude, longitude);
    }

    private void enterGeofence() {
        Log.i( TAG, "entering geofence");

        Vibrator v = (Vibrator) getSystemService( Context.VIBRATOR_SERVICE);
        v.vibrate( VIBRATE_DURATION_ENTER);
        startLocationUpdates();
    }

    private void exitGeofence() {
        Log.i( TAG, "exiting geofence");

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(MainActivity.VIBRATE_DURATION_EXIT);
        stopLocationUpdates();
        setMotionStatus(LEAVING_GEOFENCE);
    }

    private void startLocationUpdates() {
        try {
            LocationRequest request = new LocationRequest();
            request.setInterval( LOCATION_UPDATE_INTERVAL);
            request.setPriority( LocationRequest.PRIORITY_HIGH_ACCURACY);

            LocationServices.FusedLocationApi.requestLocationUpdates
                    ( client, request, this);
        } catch( SecurityException securityException) {
            logSecurityException( securityException);
        }
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates( client, this);
    }

    private void onGeofenceTransition(int transition) {
        Log.i( TAG, "geofence transition " + String.valueOf( transition));

        if (transition != lastGeofenceTransition) {
            switch (transition) {
                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    enterGeofence();
                    break;
                case Geofence.GEOFENCE_TRANSITION_DWELL:
                    Log.i(TAG, "dwelling in geofence");
                    break;
                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    exitGeofence();
                    break;
                default:
                    Log.e(TAG, "unknown geofence transition");
            }

            lastGeofenceTransition = transition;
        } else {
            Log.i(TAG, "no change in geofence transition");
        }
    }

    protected synchronized void buildGoogleApiClient() {
        client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void buildBroadcastReceiver() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int transition = intent.getIntExtra( GEOFENCE_TRANSITION, -1);

                onGeofenceTransition( transition);
            }
        };
    }

    private void setGeofence() {
        if( geofencingIntent != null) {
            LocationServices.GeofencingApi.removeGeofences( client, geofencingIntent);
            setMotionStatus( LEAVING_GEOFENCE);
            lastGeofenceTransition = -1;
            stopLocationUpdates();
        }

        try {
            Intent intent = new Intent( this, GeofenceTransitionsIntentService.class);
            geofencingIntent =  PendingIntent.getService
                    ( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            LocationServices.GeofencingApi.addGeofences(
                    client,
                    getGeofencingRequest(),
                    geofencingIntent).setResultCallback( this);
        } catch( SecurityException securityException) {
            logSecurityException( securityException);
        }
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        builder.setInitialTrigger( GeofencingRequest.INITIAL_TRIGGER_ENTER);

        Geofence geofence = new Geofence.Builder()
                .setRequestId( "POINT_OF_INTEREST")
                .setCircularRegion(
                        pointOfInterest.latitude,
                        pointOfInterest.longitude,
                        RADIUS)
                .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setExpirationDuration( Geofence.NEVER_EXPIRE)
                .build();

        builder.addGeofence( geofence);

        return builder.build();
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    @Override
    public void onStart() {
        super.onStart();

        LocalBroadcastManager.getInstance( this).registerReceiver
                ( receiver, new IntentFilter( GEOFENCE_TRANSITION_ACTION));
        client.connect();
    }

    @Override
    public void onStop() {
        super.onStop();

        client.disconnect();
        LocalBroadcastManager.getInstance( this).unregisterReceiver( receiver);

        storePreferences();
    }

    @Override
    public void onSaveInstanceState( Bundle savedInstanceState) {
        savedInstanceState.putInt( LAST_GEOFENCE_TRANSITION, lastGeofenceTransition);
        savedInstanceState.putInt( LAST_STATUS, lastStatus);

        super.onSaveInstanceState( savedInstanceState);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i( TAG, "connected to google api client");
        Button setPointOfInterestButton = (Button) findViewById( R.id.setPointOfInterestButton);
        setPointOfInterestButton.setEnabled( true);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i( TAG, "connection to google api client suspended");
        Button setPointOfInterestButton = (Button) findViewById( R.id.setPointOfInterestButton);
        setPointOfInterestButton.setEnabled( false);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.e( TAG, "connection to google api client failed: " +
                connectionResult.getErrorMessage());
    }

    @Override
    public void onResult(@NonNull Status status) {
        if (status.isSuccess()) {
            Log.i( TAG, "adding geofences successfull");
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            Log.e(TAG, "adding geofences failed: " +
                    GeofenceStatusCodes.getStatusCodeString( status.getStatusCode()));
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        float[] results = {0.0f};
        Location.distanceBetween(
                location.getLatitude(),
                location.getLongitude(),
                pointOfInterest.latitude,
                pointOfInterest.longitude,
                results);

        float distance = results[0];

        updateDistanceFromPointOfInterest( distance, location.getAccuracy());
    }

    private void updateDistanceFromPointOfInterest( float distance, float accuracy) {
        float distanceDelta = distance - lastDistanceFromPointOfInterest;

        if ( Math.abs( distanceDelta) >= accuracy) {
            Log.i( TAG, "moved " + String.valueOf( distanceDelta) + "m");

            if( distanceDelta > 0) {
                setMotionStatus( MOVING_AWAY);
            } else {
                setMotionStatus( GETTING_CLOSER);
            }

            lastDistanceFromPointOfInterest = distance;
        } else {
            setMotionStatus( DWELLING);
        }
    }

    private void setMotionStatus(int status) {
        lastStatus = status;

        int color = -1;

        switch( status) {
            case GETTING_CLOSER:
                color = Color.GREEN;
                break;
            case MOVING_AWAY:
                color = Color.RED;
                break;
            case DWELLING:
                color = Color.YELLOW;
                break;
            case LEAVING_GEOFENCE:
                color = Color.WHITE;
                break;
            default:
                Log.e( TAG, "unknown status");
        }

        getWindow().getDecorView().setBackgroundColor( color);
    }

    public void setPointOfInterest( View view) {
        readPointOfInterestFromUser();
        setGeofence();
    }
}
