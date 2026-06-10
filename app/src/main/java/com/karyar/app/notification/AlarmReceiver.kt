package com.karyar.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1)
        val title = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "یادآوری کار"
        if (taskId != -1L) {
            NotificationHelper.showNotification(context, taskId, title)
        }
    }
}
