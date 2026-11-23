package com.shoptimemaster.app.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.shoptimemaster.app.data.local.database.AppDatabase
import com.shoptimemaster.app.data.local.entity.LocationEventEntity
import com.shoptimemaster.app.notification.ShopNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return

        val transition = event.geofenceTransition
        val type = when (transition) {
            Geofence.GEOFENCE_TRANSITION_ENTER, Geofence.GEOFENCE_TRANSITION_DWELL -> "ENTER"
            Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
            else -> return
        }

        val loc = event.triggeringLocation
        val db = AppDatabase.build(context)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                db.businessDataDao().upsertLocationEvent(
                    LocationEventEntity(
                        id = "loc_${UUID.randomUUID()}",
                        timestamp = System.currentTimeMillis(),
                        eventType = type,
                        latitude = loc?.latitude ?: 0.0,
                        longitude = loc?.longitude ?: 0.0,
                        accuracy = loc?.accuracy ?: 0f,
                        source = "GPS",
                        status = "PENDING_REVIEW",
                        distanceMeters = 0f
                    )
                )
                ShopNotificationManager.showGeofencePrompt(context, isArrival = (type == "ENTER"))
            } finally {
                pendingResult.finish()
            }
        }
    }
}
