package com.karyar.app.utils

import java.util.Calendar

object JalaliCalendar {
    val monthNames = arrayOf(
        "فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور",
        "مهر","آبان","آذر","دی","بهمن","اسفند"
    )

    fun toShortDate(timestamp: Long): String {
        val (y, m, d) = fromTimestamp(timestamp)
        return "$y/${m.toString().padStart(2,'0')}/${d.toString().padStart(2,'0')}"
    }

    fun toLongDate(timestamp: Long): String {
        val (y, m, d) = fromTimestamp(timestamp)
        return "$d ${monthNames[m-1]} $y"
    }

    fun toDateTimeString(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val h = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2,'0')
        val min = cal.get(Calendar.MINUTE).toString().padStart(2,'0')
        return "${toShortDate(timestamp)}  $h:$min"
    }

    fun todayJalali(): Triple<Int,Int,Int> {
        return fromTimestamp(System.currentTimeMillis())
    }

    fun fromTimestamp(ts: Long): Triple<Int,Int,Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        return gregorianToJalali(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH))
    }

    fun toTimestamp(jy: Int, jm: Int, jd: Int): Long {
        val (gy, gm, gd) = jalaliToGregorian(jy, jm, jd)
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, gy)
            set(Calendar.MONTH, gm - 1)
            set(Calendar.DAY_OF_MONTH, gd)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun isLeapYear(jy: Int): Boolean {
        val leapRemainders = intArrayOf(1, 5, 9, 13, 17, 22, 26, 30)
        return (jy - 474) % 2820 % 128 in leapRemainders ||
                ((jy - 474) % 2820 % 128 == 0)
    }

    fun daysInMonth(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isLeapYear(jy)) 30 else 29
    }

    // Returns 0=Saturday..6=Friday for the first day of the given Jalali month
    fun firstDayOfWeek(jy: Int, jm: Int): Int {
        val ts = toTimestamp(jy, jm, 1)
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        // Calendar.DAY_OF_WEEK: 1=Sunday..7=Saturday
        return when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            Calendar.FRIDAY -> 6
            else -> 0
        }
    }

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int,Int,Int> {
        val gY = gy - 1600
        val gM = gm - 1
        val gD = gd - 1
        val isLeap = (gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0
        val gDaysInMonth = intArrayOf(31, if (isLeap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gDayNo = 365 * gY + (gY + 3) / 4 - (gY + 99) / 100 + (gY + 399) / 400
        for (i in 0 until gM) gDayNo += gDaysInMonth[i]
        gDayNo += gD
        var jDayNo = gDayNo - 79
        val jNP = jDayNo / 12053
        jDayNo %= 12053
        var jy = 979 + 33 * jNP + 4 * (jDayNo / 1461)
        jDayNo %= 1461
        if (jDayNo >= 366) { jy += (jDayNo - 1) / 365; jDayNo = (jDayNo - 1) % 365 }
        val jml = intArrayOf(31,31,31,31,31,31,30,30,30,30,30,29)
        var jm = 0; var jd = 0
        for (i in jml.indices) {
            if (jDayNo < jml[i]) { jm = i + 1; jd = jDayNo + 1; break }
            jDayNo -= jml[i]
        }
        return Triple(jy, jm, jd)
    }

    private fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int,Int,Int> {
        val jY = jy - 979
        val jM = jm - 1
        val jD = jd - 1
        var jDayNo = 365 * jY + (jY / 33) * 8 + (jY % 33 + 3) / 4
        val jml = intArrayOf(31,31,31,31,31,31,30,30,30,30,30,29)
        for (i in 0 until jM) jDayNo += jml[i]
        jDayNo += jD
        var gDayNo = jDayNo + 79
        var gy = 1600 + 400 * (gDayNo / 146097)
        gDayNo %= 146097
        var leap = true
        if (gDayNo >= 36525) {
            gDayNo--
            gy += 100 * (gDayNo / 36524)
            gDayNo %= 36524
            if (gDayNo >= 365) gDayNo++ else leap = false
        }
        gy += 4 * (gDayNo / 1461)
        gDayNo %= 1461
        if (gDayNo >= 366) { leap = false; gDayNo--; gy += gDayNo / 365; gDayNo %= 365 }
        val gDaysInMonth = intArrayOf(31, if (leap) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 0; var gd = 0
        for (i in gDaysInMonth.indices) {
            if (gDayNo < gDaysInMonth[i]) { gm = i + 1; gd = gDayNo + 1; break }
            gDayNo -= gDaysInMonth[i]
        }
        return Triple(gy, gm, gd)
    }
}
