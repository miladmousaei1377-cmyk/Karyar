package com.example.agent.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun VoiceButton(
    isRecording: Boolean,
    isSpeaking: Boolean,
    hasMicPermission: Boolean,
    onPressStart: () -> Unit,
    onPressEnd: () -> Unit,
    onCancelRecording: () -> Unit,
    onStopSpeaking: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val bgColor = when {
        isSpeaking -> MaterialTheme.colorScheme.tertiary
        isRecording -> MaterialTheme.colorScheme.error
        !hasMicPermission -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val iconColor = when {
        isSpeaking -> MaterialTheme.colorScheme.onTertiary
        isRecording -> MaterialTheme.colorScheme.onError
        !hasMicPermission -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    val modifier = if (isRecording) Modifier.scale(pulseScale) else Modifier

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(bgColor)
            .pointerInput(hasMicPermission, isSpeaking) {
                detectTapGestures(
                    onPress = {
                        if (isSpeaking) {
                            onStopSpeaking()
                        } else if (hasMicPermission) {
                            onPressStart()
                            tryAwaitRelease()
                            onPressEnd()
                        }
                    },
                    onLongPress = { if (isRecording) onCancelRecording() }
                )
            }
    ) {
        Icon(
            imageVector = when {
                isSpeaking -> Icons.Default.VolumeUp
                isRecording -> Icons.Default.MicOff
                else -> Icons.Default.Mic
            },
            contentDescription = "ضبط صدا",
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
