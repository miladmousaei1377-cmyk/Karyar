package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

class TTSService(context: Context) {
    private var tts: TextToSpeech? = null
    private var isReady = false
    var isSpeaking = false
        private set

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                tts?.language = Locale("fa", "IR")
                tts?.setSpeechRate(1.0f)
            }
        }
    }

    suspend fun speak(text: String) {
        if (!isReady || text.isBlank()) return
        val clean = cleanMarkdown(text)
        isSpeaking = true

        suspendCancellableCoroutine { cont ->
            val id = UUID.randomUUID().toString()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == id) {
                        isSpeaking = false
                        cont.resume(Unit)
                    }
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    isSpeaking = false
                    cont.resume(Unit)
                }
                override fun onError(utteranceId: String?, errorCode: Int) {
                    isSpeaking = false
                    cont.resume(Unit)
                }
            })
            tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, id)
            cont.invokeOnCancellation { stop() }
        }
    }

    fun stop() {
        tts?.stop()
        isSpeaking = false
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private fun cleanMarkdown(text: String) = text
        .replace(Regex("""\*\*(.+?)\*\*""")) { it.groupValues[1] }
        .replace(Regex("""\*(.+?)\*""")) { it.groupValues[1] }
        .replace(Regex("""#{1,6}\s"""), "")
        .replace(Regex("""`(.+?)`""")) { it.groupValues[1] }
        .trim()
}
