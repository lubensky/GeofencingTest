package com.example.lubensky.geofencingtest;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;


public class MainActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks, // for GoogleApiClient.Builder.addConnectionCallback
        GoogleApiClient.OnConnectionFailedListener, // for GoogleApiClient.Builder.addOnConnectionFailedListener
        ResultCallback<Status> { // for GeofencingApi.setResultCallback
    // home
    static final private LatLng POINT_OF_INTEREST = new LatLng( 51.071584, 13.731136);
    static final private int RADIUS = 50;
    static final public int VIBRATE_DURATION_ENTER = 10000;
    static final public int VIBRATE_DURATION_EXIT = 5000;
    static final private int LOITERING_DELAY = 1000;
    static final private String TAG = "MainActivity";

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buildGoogleApiClient();
    }

    protected synchronized void buildGoogleApiClient() {
        client = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void addGeofences() {
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
                        Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT |
                                    Geofence.GEOFENCE_TRANSITION_DWELL)
                .setLoiteringDelay( LOITERING_DELAY)
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

        client.connect();
    }

    @Override
    public void onStop() {
        super.onStop();

        client.disconnect();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
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

    }
}
