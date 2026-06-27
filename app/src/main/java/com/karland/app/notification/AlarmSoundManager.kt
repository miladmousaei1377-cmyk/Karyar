package com.karland.app.notification

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build

object AlarmSoundManager {
    private var currentRingtone: Ringtone? = null

    fun play(context: Context, uriString: String) {
        stop()
        try {
            val ringtone = RingtoneManager.getRingtone(context.applicationContext, Uri.parse(uriString))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone?.isLooping = true
            }
            ringtone?.play()
            currentRingtone = ringtone
        } catch (_: Exception) {}
    }

    fun stop() {
        try { currentRingtone?.stop() } catch (_: Exception) {}
        currentRingtone = null
    }
}
