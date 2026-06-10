package com.karyar.app

import android.app.Application
import com.karyar.app.notification.NotificationHelper

class KaryarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
    }
}
