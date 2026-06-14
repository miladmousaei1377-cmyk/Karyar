package com.karland.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.karland.app.utils.JalaliCalendar

@Composable
fun JalaliDatePickerDialog(
    initialTimestamp: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val today = JalaliCalendar.todayJalali()
    val initial = initialTimestamp?.let { JalaliCalendar.fromTimestamp(it) } ?: today

    var viewYear by remember { mutableStateOf(initial.first) }
    var viewMonth by remember { mutableStateOf(initial.second) }
    var selectedYear by remember { mutableStateOf(initial.first) }
    var selectedMonth by remember { mutableStateOf(initial.second) }
    var selectedDay by remember { mutableStateOf(initial.third) }

    val weekDayHeaders = listOf("ش","ی","د","س","چ","پ","ج")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Selected date display
                Text(
                    text = "$selectedDay ${JalaliCalendar.monthNames[selectedMonth - 1]} $selectedYear",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(16.dp))

                // Month/year navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (viewMonth == 1) { viewMonth = 12; viewYear-- }
                        else viewMonth--
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "ماه قبل")
                    }

                    Text(
                        text = "${JalaliCalendar.monthNames[viewMonth - 1]} $viewYear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    IconButton(onClick = {
                        if (viewMonth == 12) { viewMonth = 1; viewYear++ }
                        else viewMonth++
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "ماه بعد")
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Weekday headers (Saturday first)
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekDayHeaders.forEach { header ->
                        Text(
                            text = header,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                // Calendar grid
                val firstDow = JalaliCalendar.firstDayOfWeek(viewYear, viewMonth)
                val daysInMonth = JalaliCalendar.daysInMonth(viewYear, viewMonth)
                val totalCells = firstDow + daysInMonth
                val rows = (totalCells + 6) / 7

                for (row in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val day = cellIndex - firstDow + 1

                            if (day < 1 || day > daysInMonth) {
                                Box(modifier = Modifier.weight(1f).height(40.dp))
                            } else {
                                val isSelected = day == selectedDay && viewMonth == selectedMonth && viewYear == selectedYear
                                val isToday = day == today.third && viewMonth == today.second && viewYear == today.first

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .padding(2.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isSelected -> MaterialTheme.colorScheme.primary
                                                else -> Color.Transparent
                                            }
                                        )
                                        .then(
                                            if (isToday && !isSelected)
                                                Modifier.clip(CircleShape)
                                            else Modifier
                                        )
                                        .clickable {
                                            selectedDay = day
                                            selectedMonth = viewMonth
                                            selectedYear = viewYear
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isToday && !isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = day.toString(),
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = day.toString(),
                                            fontSize = 14.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                    else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("انصراف")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val safeDay = selectedDay.coerceAtMost(JalaliCalendar.daysInMonth(selectedYear, selectedMonth))
                        onConfirm(JalaliCalendar.toTimestamp(selectedYear, selectedMonth, safeDay))
                    }) {
                        Text("تأیید")
                    }
                }
            }
        }
    }
}
