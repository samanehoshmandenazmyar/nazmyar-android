package com.example.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ClassGroup(
    val id: String,
    val title: String,
    val gradeCode: String,
    val gradeName: String,
    val avatarColorHex: Long,
    val memberCount: Int,
    val description: String
)

data class Attendee(
    val userId: String,
    val userName: String,
    val userRole: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String
)

data class AttendanceCardData(
    val id: String,
    val dayOfWeek: String,
    val dateText: String,
    val classTitle: String,
    val creatorName: String,
    val attendees: List<Attendee> = emptyList()
)

data class ChatMessage(
    val id: String,
    val classGroupId: String,
    val senderId: String,
    val senderName: String,
    val senderRole: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String,
    val isAttendanceCard: Boolean = false,
    val attendanceData: AttendanceCardData? = null
)

object PersianDateHelper {
    private val WEEK_DAYS = arrayOf(
        "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنج‌شنبه", "جمعه", "شنبه"
    )

    private val MONTH_NAMES = arrayOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    fun getCurrentDayOfWeek(): String {
        val calendar = Calendar.getInstance()
        val dayIndex = calendar.get(Calendar.DAY_OF_WEEK) - 1 // 0 is Sunday
        return WEEK_DAYS.getOrElse(dayIndex) { "امروز" }
    }

    fun getCurrentTimeFormatted(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date()).toPersianDigits()
    }

    fun getCurrentFullTimeFormatted(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date()).toPersianDigits()
    }

    /**
     * Converts a Gregorian calendar date to Shamsi / Solar Hijri date string.
     */
    fun getCurrentPersianDate(): String {
        val calendar = Calendar.getInstance()
        val gYear = calendar.get(Calendar.YEAR)
        val gMonth = calendar.get(Calendar.MONTH) + 1
        val gDay = calendar.get(Calendar.DAY_OF_MONTH)

        val shamsi = gregorianToJalali(gYear, gMonth, gDay)
        val monthName = MONTH_NAMES.getOrElse(shamsi[1] - 1) { "مهر" }
        return "${shamsi[2].toPersianDigits()} $monthName ${shamsi[0].toPersianDigits()}"
    }

    private fun gregorianToJalali(gy: Int, gm: Int, gd: Int): IntArray {
        val gDaysInMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        var gy2 = if (gm > 2) (gy + 1) else gy
        var gDayNo = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gDaysInMonth[gm - 1]
        var jy = -1595 + (33 * (gDayNo / 12053))
        gDayNo %= 12053
        jy += 4 * (gDayNo / 1461)
        gDayNo %= 1461
        if (gDayNo > 365) {
            jy += ((gDayNo - 1) / 365)
            gDayNo = (gDayNo - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (gDayNo < 186) {
            jm = 1 + (gDayNo / 31)
            jd = 1 + (gDayNo % 31)
        } else {
            jm = 7 + ((gDayNo - 186) / 30)
            jd = 1 + ((gDayNo - 186) % 30)
        }
        return intArrayOf(jy, jm, jd)
    }
}
