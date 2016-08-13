package com.example.lubensky.geofencingtest;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
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
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, // for GoogleApiClient.Builder.addConnectionCallback
        GoogleApiClient.OnConnectionFailedListener, // for GoogleApiClient.Builder.addOnConnectionFailedListener
        LocationListener, // for LocationServices.FusedLocationApi.requestLocationUpdates
        ResultCallback<Status> { // for GeofencingApi.setResultCallback
    // home
    static final private LatLng POINT_OF_INTEREST = new LatLng( 51.071584, 13.731136);
    static final private int RADIUS = 50;
    static final private int VIBRATE_DURATION_ENTER = 10000;
    static final private int VIBRATE_DURATION_EXIT = 5000;
    static final private int LOCATION_UPDATE_INTERVAL = 5000;
    static final private float LOCATION_UPDATE_DISTANCE_THRESHOLD = 5.0f;
    static final public String GEOFENCE_TRANSITION_ACTION =
            "com.example.lubensky.geofencingtest.GEOFENCE_TRANSITION_ACTION";
    static final public String GEOFENCE_TRANSITION =
            "com.example.lubensky.geofencingtest.GEOFENCE_TRANSITION";
    static final private String TAG = "MainActivity";
    static final private String LAST_GEOFENCE_TRANSITION = "lastGeofenceTransition";
    static final private String LAST_STATUS = "lastStatus";

    static final int GETTING_CLOSER = 0;
    static final int MOVING_AWAY = 1;
    static final int DWELLING = 2;
    static final int OUT_OF_GEOFENCE = 3;

    private int lastGeofenceTransition = -1;
    private Location lastLocation = null;
    private float lastDistanceFromPointOfInterest = 0.0f;
    private int lastStatus = OUT_OF_GEOFENCE;

    private BroadcastReceiver receiver;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if( savedInstanceState != null) {
            lastGeofenceTransition = savedInstanceState.getInt( LAST_GEOFENCE_TRANSITION);
            lastStatus = savedInstanceState.getInt( LAST_STATUS);
        }

        setStatus( lastStatus);

        buildBroadcastReceiver();
        buildGoogleApiClient();
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

    private void onGeofenceTransition(int transition) {
        Log.i( TAG, "onGeofenceTransition: " + String.valueOf( transition));

        if (transition != lastGeofenceTransition) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            switch (transition) {
                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    Log.i(TAG, "enter geofence");
                    v.vibrate(VIBRATE_DURATION_ENTER);
                    startLocationUpdates();
                    break;
                case Geofence.GEOFENCE_TRANSITION_DWELL:
                    Log.i(TAG, "dwelling in geofence");
                    break;
                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    Log.i(TAG, "exiting geofence");
                    v.vibrate(MainActivity.VIBRATE_DURATION_EXIT);
                    stopLocationUpdates();
                    setStatus( OUT_OF_GEOFENCE);
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

    private void addGeofences() {
        try {
            LocationServices.GeofencingApi.addGeofences(
                    client,
                    getGeofencingRequest(),
                    getGeofencePendingIntent()).setResultCallback( this);
        } catch( SecurityException securityException) {
            logSecurityException( securityException);
        }
    }

    private PendingIntent getGeofencePendingIntent() {
        Intent intent = new Intent( this, GeofenceTransitionsIntentService.class);

        return PendingIntent.getService( this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        builder.setInitialTrigger( GeofencingRequest.INITIAL_TRIGGER_ENTER);

        Geofence geofence = new Geofence.Builder()
                .setRequestId( "POINT_OF_INTEREST")
                .setCircularRegion(
                        POINT_OF_INTEREST.latitude,
                        POINT_OF_INTEREST.longitude,
                        RADIUS)
                .setTransitionTypes(
                        Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .setExpirationDuration( Geofence.NEVER_EXPIRE)
                .build();

        builder.addGeofence( geofence);

        return builder.build();
    }

    private void startLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates
                    ( client, getLocationRequest(), this);
        } catch( SecurityException securityException) {
            logSecurityException( securityException);
        }
    }

    private void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates( client, this);
    }

    private LocationRequest getLocationRequest() {
        LocationRequest request = new LocationRequest();
        request.setInterval( LOCATION_UPDATE_INTERVAL);
        request.setPriority( LocationRequest.PRIORITY_HIGH_ACCURACY);

        return request;
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
        client.disconnect();
        LocalBroadcastManager.getInstance( this).unregisterReceiver( receiver);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState( Bundle savedInstanceState) {
        savedInstanceState.putInt( LAST_GEOFENCE_TRANSITION, lastGeofenceTransition);
        savedInstanceState.putInt( LAST_STATUS, lastStatus);

        super.onSaveInstanceState( savedInstanceState);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i( TAG, "connected");
        addGeofences();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

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
        Log.i( TAG, "location changed");

        float[] results = {0.0f};
        Location.distanceBetween(
                location.getLatitude(),
                location.getLongitude(),
                POINT_OF_INTEREST.latitude,
                POINT_OF_INTEREST.longitude,
                results);

        float distance = results[0];

        updateDistanceFromPointOfInterest( distance);
    }

    private void updateDistanceFromPointOfInterest( float distance) {
        float distanceDelta = distance - lastDistanceFromPointOfInterest;

        Log.i( TAG, String.valueOf( distance));
        Log.i( TAG, String.valueOf( lastDistanceFromPointOfInterest));
        Log.i( TAG, String.valueOf( distanceDelta));

        if ( Math.abs( distanceDelta) >= LOCATION_UPDATE_DISTANCE_THRESHOLD) {
            if( distanceDelta > 0) {
                setStatus( MOVING_AWAY);
            } else {
                setStatus( GETTING_CLOSER);
            }

            lastDistanceFromPointOfInterest = distance;
        } else {
            setStatus( DWELLING);
        }
    }

    private void setStatus( int status) {
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
            case OUT_OF_GEOFENCE:
                color = Color.WHITE;
                break;
            default:
                Log.e( TAG, "unknown status");
        }

        getWindow().getDecorView().setBackgroundColor( color);
    }
}
