package com.shoptimemaster.app.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import com.shoptimemaster.app.data.local.database.AppDatabase
import com.shoptimemaster.app.notification.ShopNotificationManager

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            WorkManager.getInstance(context).enqueue(
                OneTimeWorkRequestBuilder<SessionRecoveryWorker>().build()
            )
        }
    }
}

class SessionRecoveryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val db = AppDatabase.build(applicationContext)
        val active = db.timeSessionDao().getActiveSession()
        val now = System.currentTimeMillis()
        if (active != null && (now - active.checkIn) > 16 * 3_600_000L) {
            // Never fabricate exit timestamp; flag for user confirmation
            db.timeSessionDao().update(active.copy(status = "NEEDS_REVIEW"))
            ShopNotificationManager.showMissingSessionWarning(applicationContext)
        }
        val shop = db.shopDao().getPrimaryShop()
        if (shop?.latitude != null && shop.longitude != null) {
            GeofenceManager(applicationContext).registerShopGeofence(
                shop.id,
                shop.latitude,
                shop.longitude,
                shop.geofenceRadiusMeters
            )
        }
        return Result.success()
    }
}
