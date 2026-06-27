package com.karland.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1)
        val title = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "یادآوری کار"
        val alarmToneUri = intent.getStringExtra(NotificationHelper.EXTRA_ALARM_TONE)
        if (taskId != -1L) {
            if (alarmToneUri != null) {
                try {
                    val ringtone = RingtoneManager.getRingtone(context, Uri.parse(alarmToneUri))
                    ringtone?.play()
                } catch (e: Exception) { /* fallback: notification channel sound will play */ }
            }
            NotificationHelper.showNotification(context, taskId, title)
        }
    }
}
