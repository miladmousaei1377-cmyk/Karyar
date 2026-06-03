package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", 0)
        val taskTitle = intent.getStringExtra("TASK_TITLE") ?: "یادآوری کار"
        val taskDesc = intent.getStringExtra("TASK_DESC") ?: "زمان انجام این کار فرا رسیده است"

        // Show notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "karha_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "یادآوری‌های کارها",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "نمایش یادآوری کارهای ثبت شده"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Action when clicking notification: Open App
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("TASK_ID_NAV", taskId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // System fallback info icon
            .setContentTitle(taskTitle)
            .setContentText(taskDesc)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(taskId, notification)

        // Disable reminder status in database so it is no longer shown as active
        if (taskId != 0) {
            val database = AppDatabase.getDatabase(context)
            CoroutineScope(Dispatchers.IO).launch {
                database.taskDao.updateReminderStatus(taskId, false)
            }
        }
    }
}
