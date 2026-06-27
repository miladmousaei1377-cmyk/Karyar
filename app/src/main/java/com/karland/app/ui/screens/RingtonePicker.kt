package com.karland.app.ui.screens

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class RingtoneItem(val title: String, val uri: Uri?)

@Composable
fun RingtonePickerDialog(
    currentUri: String?,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit
) {
    val context = LocalContext.current
    var selectedUri by remember { mutableStateOf(currentUri) }
    var playingUri by remember { mutableStateOf<String?>(null) }
    var activeRingtone by remember { mutableStateOf<Ringtone?>(null) }

    val ringtones = remember { loadSystemRingtones(context) }

    DisposableEffect(Unit) {
        onDispose { activeRingtone?.stop() }
    }

    AlertDialog(
        onDismissRequest = {
            activeRingtone?.stop()
            onDismiss()
        },
        title = { Text("آهنگ آلارم", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp)) {
                items(ringtones) { item ->
                    val uriStr = item.uri?.toString()
                    val isSelected = selectedUri == uriStr
                    val isPlaying = playingUri == uriStr && uriStr != null

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activeRingtone?.stop()
                                selectedUri = uriStr
                                if (item.uri != null) {
                                    val r = RingtoneManager.getRingtone(context, item.uri)
                                    r?.play()
                                    activeRingtone = r
                                    playingUri = uriStr
                                } else {
                                    activeRingtone = null
                                    playingUri = null
                                }
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                        if (isPlaying) {
                            Icon(
                                Icons.Default.VolumeUp, null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                activeRingtone?.stop()
                onSelect(selectedUri)
            }) { Text("انتخاب") }
        },
        dismissButton = {
            TextButton(onClick = {
                activeRingtone?.stop()
                onDismiss()
            }) { Text("بستن") }
        }
    )
}

private fun loadSystemRingtones(context: Context): List<RingtoneItem> {
    val list = mutableListOf<RingtoneItem>()
    list.add(RingtoneItem("بدون آلارم", null))
    try {
        val manager = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_ALARM) }
        val cursor = manager.cursor
        while (cursor.moveToNext()) {
            val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
            val uri = manager.getRingtoneUri(cursor.position)
            list.add(RingtoneItem(title, uri))
        }
        cursor.close()
        // If no alarm sounds found, fall back to notification sounds
        if (list.size <= 1) {
            val mgr2 = RingtoneManager(context).apply { setType(RingtoneManager.TYPE_NOTIFICATION) }
            val c2 = mgr2.cursor
            while (c2.moveToNext()) {
                val title = c2.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                val uri = mgr2.getRingtoneUri(c2.position)
                list.add(RingtoneItem(title, uri))
            }
            c2.close()
        }
    } catch (e: Exception) { /* return only "no alarm" if something fails */ }
    return list
}
