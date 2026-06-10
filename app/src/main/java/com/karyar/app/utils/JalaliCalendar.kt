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
        val g2j = intArrayOf(0,31,59,90,120,151,181,212,243,273,304,334)
        fun leap(y: Int) = (y%4==0 && y%100!=0) || y%400==0
        var gDayNo = 365*(gy-1)+(gy-1)/4-(gy-1)/100+(gy-1)/400+g2j[gm-1]+gd-1
        if (gm>2 && leap(gy)) gDayNo++
        var jDayNo = gDayNo - 79
        val jNP = jDayNo/12053; jDayNo %= 12053
        var jy = 979+33*jNP+4*(jDayNo/1461); jDayNo %= 1461
        val ml = intArrayOf(31,31,31,31,31,31,30,30,30,30,30,29)
        if (jDayNo>=366) { jy+=(jDayNo-1)/365; jDayNo=(jDayNo-1)%365 }
        var i=0; var s=0
        while(i<11 && jDayNo>=s+ml[i]) { s+=ml[i]; i++ }
        return Triple(jy, i+1, jDayNo-s+1)
    }
}
