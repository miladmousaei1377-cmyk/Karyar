package com.karyar.app.utils

import java.util.Calendar

object JalaliCalendar {
    private val monthNames = arrayOf(
        "فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور",
        "مهر","آبان","آذر","دی","بهمن","اسفند"
    )

    fun toShortDate(timestamp: Long): String {
        val (y, m, d) = convert(timestamp)
        return "$y/${m.toString().padStart(2,'0')}/${d.toString().padStart(2,'0')}"
    }

    fun toLongDate(timestamp: Long): String {
        val (y, m, d) = convert(timestamp)
        return "$d ${monthNames[m-1]} $y"
    }

    fun toDateTimeString(timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val h = cal.get(Calendar.HOUR_OF_DAY).toString().padStart(2,'0')
        val min = cal.get(Calendar.MINUTE).toString().padStart(2,'0')
        return "${toShortDate(timestamp)}  $h:$min"
    }

    private fun convert(ts: Long): Triple<Int,Int,Int> {
        val cal = Calendar.getInstance().apply { timeInMillis = ts }
        return gregorianToJalali(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH))
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
}
