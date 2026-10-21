package com.example.domain.model

object SchoolClasses {
    val ALL_CLASSES = listOf(
        "هفتم یک",
        "هفتم دو",
        "هشتم یک",
        "هشتم دو",
        "نهم یک",
        "نهم دو"
    )

    fun getStudentClassKey(grade: String?, className: String?): String {
        val sGrade = (grade ?: "").trim().lowercase()
        val sClass = (className ?: "").trim().lowercase()
        val combined = "$sGrade $sClass"

        return when {
            combined.contains("هفتم یک") || combined.contains("هفتم ۱") || combined.contains("7/1") || (sGrade.contains("هفتم") && (sClass.contains("یک") || sClass.contains("1"))) -> "هفتم یک"
            combined.contains("هفتم دو") || combined.contains("هفتم ۲") || combined.contains("7/2") || (sGrade.contains("هفتم") && (sClass.contains("دو") || sClass.contains("2"))) -> "هفتم دو"
            combined.contains("هشتم یک") || combined.contains("هشتم ۱") || combined.contains("8/1") || (sGrade.contains("هشتم") && (sClass.contains("یک") || sClass.contains("1"))) -> "هشتم یک"
            combined.contains("هشتم دو") || combined.contains("هشتم ۲") || combined.contains("8/2") || (sGrade.contains("هشتم") && (sClass.contains("دو") || sClass.contains("2"))) -> "هشتم دو"
            combined.contains("نهم یک") || combined.contains("نهم ۱") || combined.contains("9/1") || (sGrade.contains("نهم") && (sClass.contains("یک") || sClass.contains("1"))) -> "نهم یک"
            combined.contains("نهم دو") || combined.contains("نهم ۲") || combined.contains("9/2") || (sGrade.contains("نهم") && (sClass.contains("دو") || sClass.contains("2"))) -> "نهم دو"
            sGrade.contains("هفتم") -> "هفتم یک"
            sGrade.contains("هشتم") -> "هشتم یک"
            sGrade.contains("نهم") -> "نهم یک"
            else -> "هفتم یک"
        }
    }

    fun matchesStudent(targetClasses: List<String>, studentGrade: String?, studentClass: String?): Boolean {
        if (targetClasses.isEmpty() || targetClasses.size >= ALL_CLASSES.size || targetClasses.contains("همه")) {
            return true
        }
        val key = getStudentClassKey(studentGrade, studentClass)
        if (targetClasses.contains(key)) return true

        val g = (studentGrade ?: "").trim().lowercase()
        if (g.contains("هفتم") && (targetClasses.contains("هفتم یک") || targetClasses.contains("هفتم دو"))) return true
        if (g.contains("هشتم") && (targetClasses.contains("هشتم یک") || targetClasses.contains("هشتم دو"))) return true
        if (g.contains("نهم") && (targetClasses.contains("نهم یک") || targetClasses.contains("نهم دو"))) return true

        return false
    }

    fun formatClassesDisplay(targetClasses: List<String>): String {
        return if (targetClasses.isEmpty() || targetClasses.size >= ALL_CLASSES.size || targetClasses.contains("همه")) {
            "همه پایه‌ها و کلاس‌ها"
        } else {
            targetClasses.joinToString("، ")
        }
    }
}

data class HomeBanner(
    val id: String,
    val title: String,
    val imageUrl: String,
    val targetClasses: List<String> = emptyList(), // e.g. ["هفتم یک", "هفتم دو"] or empty for all
    val linkUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
