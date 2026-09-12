package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent == null) {
            Log.e("GeofenceReceiver", "Geofencing event is null")
            return
        }

        if (geofencingEvent.hasError()) {
            val errorMessage = "Geofencing error: ${geofencingEvent.errorCode}"
            Log.e("GeofenceReceiver", errorMessage)
            return
        }

        // Get the transition type.
        val geofenceTransition = geofencingEvent.geofenceTransition

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences
            if (triggeringGeofences != null) {
                for (geofence in triggeringGeofences) {
                    NotificationHelper.showNotification(
                        context,
                        title = "Safety Zone Alert",
                        content = "You have entered a designated high-priority safety zone: ${geofence.requestId}"
                    )
                }
            }
        }
    }
}
