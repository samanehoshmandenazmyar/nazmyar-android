package com.example.domain.model

data class PrayerLock(
    val id: String = "",
    val date: String = "",
    val prayerTime: String = "",
    val isLocked: Boolean = true,
    val lockedBy: String = "",
    val lockedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun buildId(date: String, prayerTime: String): String {
            val safeDate = date.replace('/', '_').replace('-', '_').trim()
            val safePrayer = when (prayerTime.trim()) {
                "نماز ظهر و عصر" -> "noon"
                "نماز صبح" -> "morning"
                "نماز مغرب و عشاء" -> "evening"
                "نماز جمعه و جماعت" -> "congregation"
                else -> prayerTime.hashCode().toString()
            }
            return "${safeDate}_${safePrayer}"
        }
    }
}
