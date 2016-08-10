package com.example.lubensky.geofencingtest;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

/**
 * Created by lubensky on 10.08.16.
 */
public class GeofenceTransitionsIntentService extends IntentService {

    protected static final String TAG = "GeofenceTransitionsIS";

    public GeofenceTransitionsIntentService() {
        super( TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent( intent);

        if( geofencingEvent.hasError()) {
            Log.e( TAG, "Geofencing error");

            return;
        }

        int geofenceTransition = geofencingEvent.getGeofenceTransition();
        Vibrator v = ( Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if( geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            v.vibrate( MainActivity.VIBRATE_DURATION_ENTER);
        } else if ( geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {

        } else if ( geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            v.vibrate( MainActivity.VIBRATE_DURATION_EXIT);
        }
    }
}
