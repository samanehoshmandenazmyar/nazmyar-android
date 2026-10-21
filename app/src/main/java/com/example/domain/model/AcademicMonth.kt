package com.example.domain.model

import com.example.core.utils.DateTimeUtils
import java.util.Calendar

/**
 * Representation of Iranian Academic Year Months (سال تحصیلی ایران).
 * Starts with Mehr (مهر - Month 7) and goes through the Iranian school year.
 */
enum class AcademicMonth(
    val monthNumber: Int, // 1 to 12 in Solar Hijri (Shamsi)
    val persianName: String,
    val season: String,
    val isSummer: Boolean = false
) {
    MEHR(7, "مهر", "پاییز"),
    ABAN(8, "آبان", "پاییز"),
    AZAR(9, "آذر", "پاییز"),
    DEY(10, "دی", "زمستان"),
    BAHMAN(11, "بهمن", "زمستان"),
    ESFAND(12, "اسفند", "زمستان"),
    FARVARDIN(1, "فروردین", "بهار"),
    ORDIBEHESHT(2, "اردیبهشت", "بهار"),
    KHORDAD(3, "خرداد", "بهار"),
    TIR(4, "تیر", "تابستان", true),
    MORDAD(5, "مرداد", "تابستان", true),
    SHAHRIVAR(6, "شهریور", "تابستان", true);

    companion object {
        // Ordered according to the Iranian School Year
        val ACADEMIC_YEAR_ORDER = listOf(
            MEHR, ABAN, AZAR, DEY, BAHMAN, ESFAND, FARVARDIN, ORDIBEHESHT, KHORDAD, TIR, MORDAD, SHAHRIVAR
        )

        fun fromMonthNumber(number: Int): AcademicMonth {
            return entries.firstOrNull { it.monthNumber == number } ?: MEHR
        }

        fun getCurrentMonth(): AcademicMonth {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            // Convert Gregorian to Solar Hijri
            val shamsiMonth = getShamsiMonthNumber(year, month, day)
            return fromMonthNumber(shamsiMonth)
        }

        fun fromDate(dateStr: String?, timestamp: Long = 0L): AcademicMonth {
            if (!dateStr.isNullOrBlank()) {
                val clean = dateStr
                    .replace("۰", "0").replace("۱", "1").replace("۲", "2")
                    .replace("۳", "3").replace("۴", "4").replace("۵", "5")
                    .replace("۶", "6").replace("۷", "7").replace("۸", "8")
                    .replace("۹", "9").trim()

                // Check by Persian month name
                for (m in entries) {
                    if (dateStr.contains(m.persianName)) {
                        return m
                    }
                }

                // Check by pattern YYYY/MM/DD or YYYY-MM-DD
                val parts = clean.split("/", "-", " ")
                if (parts.size >= 2) {
                    val mNum = parts[1].toIntOrNull()
                    if (mNum != null && mNum in 1..12) {
                        return fromMonthNumber(mNum)
                    }
                }
            }

            if (timestamp > 0L) {
                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH) + 1
                val d = cal.get(Calendar.DAY_OF_MONTH)
                val shamsiMonth = getShamsiMonthNumber(y, m, d)
                return fromMonthNumber(shamsiMonth)
            }

            return getCurrentMonth()
        }

        private fun getShamsiMonthNumber(gy: Int, gm: Int, gd: Int): Int {
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

            var jm = 0
            while (jm < 11 && jDayNo >= jDaysInMonth[jm]) {
                jDayNo -= jDaysInMonth[jm]
                jm++
            }
            return jm + 1
        }
    }
}
