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

            val shamsi = DateTimeUtils.gregorianToJalali(year, month, day)
            return fromMonthNumber(shamsi[1])
        }

        fun fromDate(dateStr: String?, timestamp: Long = 0L): AcademicMonth {
            if (!dateStr.isNullOrBlank()) {
                val clean = dateStr
                    .replace("۰", "0").replace("۱", "1").replace("۲", "2")
                    .replace("۳", "3").replace("۴", "4").replace("۵", "5")
                    .replace("۶", "6").replace("۷", "7").replace("۸", "8")
                    .replace("۹", "9").trim()

                // Check by pattern YYYY/MM/DD or YYYY-MM-DD FIRST to follow exact برج
                val regex = Regex("""(\d{4})[/-](\d{1,2})[/-](\d{1,2})""")
                val match = regex.find(clean)
                if (match != null) {
                    val mNum = match.groupValues[2].toIntOrNull()
                    if (mNum != null && mNum in 1..12) {
                        return fromMonthNumber(mNum)
                    }
                }

                // Check by Persian month name
                for (m in entries) {
                    if (dateStr.contains(m.persianName)) {
                        return m
                    }
                }
            }

            if (timestamp > 0L) {
                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                val y = cal.get(Calendar.YEAR)
                val m = cal.get(Calendar.MONTH) + 1
                val d = cal.get(Calendar.DAY_OF_MONTH)
                val shamsi = DateTimeUtils.gregorianToJalali(y, m, d)
                return fromMonthNumber(shamsi[1])
            }

            return getCurrentMonth()
        }
    }
}
