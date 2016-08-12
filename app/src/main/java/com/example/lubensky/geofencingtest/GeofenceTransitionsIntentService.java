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
            //v.vibrate( MainActivity.VIBRATE_DURATION_ENTER);
            Log.i( TAG, "ENTER");
            Intent enterIntent = new Intent( this, EnterActivity.class);
            enterIntent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity( enterIntent);
        } else if ( geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL) {
            Log.i( TAG, "DWELL");
            //v.vibrate( 100);
            Intent dwellIntent = new Intent( this, DwellActivity.class);
            dwellIntent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity( dwellIntent);
        } else if ( geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.i( TAG, "EXIT");
            //v.vibrate( MainActivity.VIBRATE_DURATION_EXIT);
            Intent exitIntent = new Intent( this, DwellActivity.class);
            exitIntent.addFlags( Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity( exitIntent);
        }
    }
}
