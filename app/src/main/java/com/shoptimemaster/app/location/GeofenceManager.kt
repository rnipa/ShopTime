package com.shoptimemaster.app.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceManager(private val context: Context) {
    private val client: GeofencingClient = LocationServices.getGeofencingClient(context)

    private fun pendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun registerShopGeofence(
        shopId: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = 100f
    ) {
        val geofence = Geofence.Builder()
            .setRequestId(shopId)
            .setCircularRegion(latitude, longitude, radiusMeters.coerceIn(50f, 1000f))
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                    Geofence.GEOFENCE_TRANSITION_DWELL or
                    Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .setLoiteringDelay(60_000) // 60s confirmation delay to prevent false drift events
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        client.addGeofences(request, pendingIntent())
    }

    fun unregisterAll() {
        client.removeGeofences(pendingIntent())
    }
}
