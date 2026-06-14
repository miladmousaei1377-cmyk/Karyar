package com.karland.app

import android.app.Application
import com.karland.app.notification.NotificationHelper

class KaryarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
