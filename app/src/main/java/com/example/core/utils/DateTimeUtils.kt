package com.example.core.utils

import com.example.domain.model.toPersianDigits
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    fun getCurrentPersianDate(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // Convert Gregorian to Solar Hijri (Shamsi)
        val shamsi = gregorianToJalali(year, month, day)
        val formattedMonth = if (shamsi[1] < 10) "0${shamsi[1]}" else "${shamsi[1]}"
        val formattedDay = if (shamsi[2] < 10) "0${shamsi[2]}" else "${shamsi[2]}"
        return "${shamsi[0]}/$formattedMonth/$formattedDay".toPersianDigits()
    }

    fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date()).toPersianDigits()
    }

    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): IntArray {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val gy2 = gy - 1600
        val gm2 = gm - 1
        val gd2 = gd - 1

        var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400

        for (i in 0 until gm2) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm2 > 1 && ((gy2 % 4 == 0 && gy2 % 100 != 0) || (gy2 % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd2

        var jDayNo = gDayNo - 79

        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        while (jm < 11 && jDayNo >= jDaysInMonth[jm]) {
            jDayNo -= jDaysInMonth[jm]
            jm++
        }

        val jd = jDayNo + 1
        return intArrayOf(jy, jm + 1, jd)
    }

    private val persianMonthNames = listOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    fun getPersianMonthName(monthNumber: Int): String {
        return if (monthNumber in 1..12) persianMonthNames[monthNumber - 1] else "نامشخص"
    }

    /**
     * Normalizes a date string so that the month in parentheses strictly matches
     * the solar month (برج) indicated by the numeric date (YYYY/MM/DD).
     * For example, "۱۴۰۳/۰۸/۱۷ (اسفند)" -> "۱۴۰۳/۰۸/۱۷ (آبان)".
     */
    fun normalizeReportDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return getCurrentPersianDate()

        // Clean Persian and Arabic digits to ASCII digits
        val clean = dateStr
            .replace("۰", "0").replace("۱", "1").replace("۲", "2")
            .replace("۳", "3").replace("۴", "4").replace("۵", "5")
            .replace("۶", "6").replace("۷", "7").replace("۸", "8")
            .replace("۹", "9")

        // Match YYYY/MM/DD or YYYY-MM-DD
        val regex = Regex("""(\d{4})[/-](\d{1,2})[/-](\d{1,2})""")
        val match = regex.find(clean)
        if (match != null) {
            val y = match.groupValues[1]
            val m = match.groupValues[2].toIntOrNull() ?: 1
            val d = match.groupValues[3].toIntOrNull() ?: 1
            val monthName = getPersianMonthName(m)
            val formattedM = if (m < 10) "0$m" else "$m"
            val formattedD = if (d < 10) "0$d" else "$d"
            return "$y/$formattedM/$formattedD ($monthName)".toPersianDigits()
        }

        return dateStr
    }
}
