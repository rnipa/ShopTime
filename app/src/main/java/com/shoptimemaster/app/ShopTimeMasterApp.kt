package com.shoptimemaster.app

import android.app.Application
import com.shoptimemaster.app.notification.ShopNotificationManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ShopTimeMasterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ShopNotificationManager.ensureChannels(this)
    }
}
