package com.example.ui.screens

import java.util.Calendar

object JalaliCalendarHelper {
    // Gregorian to Persian
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gDaysInMonth = intArrayOf(0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val gy2 = gy - 1600
        var gDays = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
        for (i in 1 until gm) {
            gDays += gDaysInMonth[i]
        }
        if (gm > 2 && ((gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0)) {
            gDays++
        }
        gDays += gd - 1
        var jDays = gDays - 79
        val jNp = jDays / 12053
        jDays %= 12053
        var jy = 979 + 33 * jNp + 4 * (jDays / 1461)
        jDays %= 1461
        if (jDays >= 366) {
            jy += (jDays - 1) / 365
            jDays = (jDays - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (jDays < 186) {
            jm = 1 + (jDays / 31)
            jd = 1 + (jDays % 31)
        } else {
            jm = 7 + ((jDays - 186) / 30)
            jd = 1 + ((jDays - 186) % 30)
        }
        return Triple(jy, jm, jd)
    }

    // Persian to Gregorian
    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val jy2 = jy - 979
        var jDays = 365 * jy2 + (jy2 / 33) * 8 + ((jy2 % 33) + 3) / 4
        for (i in 1 until jm) {
            if (i < 7) {
                jDays += 31
            } else {
                jDays += 30
            }
        }
        jDays += jd - 1
        var gDays = jDays + 79
        var gy = 1600 + 400 * (gDays / 146097)
        gDays %= 146097
        var leap = true
        if (gDays >= 36525) {
            gDays--
            gy += 100 * (gDays / 36524)
            gDays %= 36524
            if (gDays >= 365) {
                gDays++
            } else {
                leap = false
            }
        }
        gy += 4 * (gDays / 1461)
        gDays %= 1461
        if (gDays >= 366) {
            leap = false
            gDays--
            gy += gDays / 365
            gDays %= 365
        }
        var gd = gDays + 1
        val gDaysInMonth = intArrayOf(
            0, 31,
            if (leap && ((gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0)) 29 else 28,
            31, 30, 31, 30, 31, 31, 30, 31, 30, 31
        )
        var gm = 1
        for (i in 1..12) {
            val dim = gDaysInMonth[i]
            if (gd <= dim) {
                gm = i
                break
            }
            gd -= dim
        }
        return Triple(gy, gm, gd)
    }

    fun getPersianTodayString(): String {
        val currentCal = Calendar.getInstance()
        val (jy, jm, jd) = gregorianToJalali(
            currentCal.get(Calendar.YEAR),
            currentCal.get(Calendar.MONTH) + 1,
            currentCal.get(Calendar.DAY_OF_MONTH)
        )
        val dayOfWeekName = when (currentCal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "شنبه"
            Calendar.SUNDAY -> "یکشنبه"
            Calendar.MONDAY -> "دوشنبه"
            Calendar.TUESDAY -> "سه‌شنبه"
            Calendar.WEDNESDAY -> "چهارشنبه"
            Calendar.THURSDAY -> "پنج‌شنبه"
            Calendar.FRIDAY -> "جمعه"
            else -> ""
        }
        return "$dayOfWeekName، $jd ${persianMonthNames[jm - 1]} $jy"
    }

    fun formatMillsToPersian(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val (jy, jm, jd) = gregorianToJalali(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        return String.format("%02d/%02d/%04d - %02d:%02d", jy, jm, jd, hour, minute)
    }

    val persianMonthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    )
}
