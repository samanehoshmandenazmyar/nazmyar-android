package com.example.domain.model

enum class UserRole(val farsiTitle: String, val badgeColorHex: Long) {
    MANAGER("مدیر مدرسه", 0xFF1E40AF),
    VICE_PRINCIPAL("معاون آموزشی/انضباطی", 0xFF2563EB),
    TEACHER("دبیر", 0xFF0D9488),
    COUNSELOR("مسئول پرورشی", 0xFF7C3AED),
    PARENT("ولی دانش‌آموز", 0xFF64748B),
    STUDENT("دانش‌آموز", 0xFF64748B);

    companion object {
        fun fromString(role: String?): UserRole {
            return entries.firstOrNull { it.name.equals(role, ignoreCase = true) } ?: MANAGER
        }
    }
}

data class User(
    val id: String,
    val name: String,
    val phone: String,
    val email: String,
    val role: UserRole,
    val schoolId: String,
    val active: Boolean = true,
    val assignedClasses: List<String> = emptyList(),
    val teachingSubjects: List<String> = emptyList(),
    val childStudentIds: List<String> = emptyList(),
    val avatarInitials: String = name.take(2),
    val tag: String? = null,
    val studentCode: String = "",
    val password: String = "",
    val gender: String = "پسر",
    val isRegisteredUser: Boolean = false,
    val rankTitle: String = if ((name.trim() == "سید حسین موسوی نژاد" && studentCode == "454545") || (tag == "سازنده" && id == "user_creator_hossein")) "سازنده" else role.farsiTitle,
    val rankBadgeColorHex: Long = if ((name.trim() == "سید حسین موسوی نژاد" && studentCode == "454545") || (tag == "سازنده" && id == "user_creator_hossein")) 0xFF1E40AF else 0xFF64748B,
    val rankColorHex: String = if ((name.trim() == "سید حسین موسوی نژاد" && studentCode == "454545") || (tag == "سازنده" && id == "user_creator_hossein")) "#1E40AF" else "#64748B",
    val permissions: List<String> = emptyList(),
    val isReadOnly: Boolean = (role == UserRole.PARENT),
    val profileImage: String? = null,
    val extraRanks: List<String> = emptyList(),
    val extraRankColors: List<String> = emptyList(),
    val nationalId: String = "",
    val bio: String = "",
    val hasLoggedIn: Boolean = false,
    val addedByStaff: Boolean = true,
    val faceRegistered: Boolean = false,
    val faceFrontUrl: String? = null,
    val faceLeftUrl: String? = null,
    val faceRightUrl: String? = null
) {
    val isCreator: Boolean
        get() = (id == "user_creator_hossein") ||
                (studentCode == "454545" && name.trim() == "سید حسین موسوی نژاد") ||
                (studentCode == "454545" && email.equals("hosseinvexter@gmail.com", ignoreCase = true)) ||
                (phone == "09923072572" || phone == "+989923072572") ||
                (name.trim() == "سید حسین موسوی نژاد")

    val allRanks: List<String>
        get() = (listOf(rankTitle) + extraRanks).filter { it.isNotBlank() }.distinct()
}

data class CustomRank(
    val id: String,
    val title: String,
    val colorHex: String = "#64748B",
    val permissions: List<String> = emptyList(),
    val description: String = ""
)

object RankPermission {
    const val REGISTER_POSITIVE = "REGISTER_POSITIVE"
    const val REGISTER_DISCIPLINARY = "REGISTER_DISCIPLINARY"
    const val VIEW_REPORTS = "VIEW_REPORTS"
    const val ACCESS_GAMES = "ACCESS_GAMES"
    const val VIEW_STUDENT_PROFILES = "VIEW_STUDENT_PROFILES"
    const val EDIT_CLASS_INFO = "EDIT_CLASS_INFO"
    const val SPECIAL_BADGE = "SPECIAL_BADGE"

    val all = listOf(
        REGISTER_POSITIVE to "امکان ثبت رفتار مثبت برای هم‌کلاسی‌ها (همیار معلم)",
        REGISTER_DISCIPLINARY to "امکان ثبت گزارش و تذکر انضباطی",
        VIEW_REPORTS to "مشاهده گزارش‌ها و آمار کلاسی",
        ACCESS_GAMES to "دسترسی آزاد به بخش بازی‌ها (شطرنج و سرگرمی)",
        VIEW_STUDENT_PROFILES to "مشاهده سوابق و پرونده رفتاری دانش‌آموزان",
        EDIT_CLASS_INFO to "همیار کلاسی (مدیریت مشخصات و اطلاعیه‌ها)",
        SPECIAL_BADGE to "نمایش نشان و کادر رنگی برجسته در سامانه"
    )
}

data class Student(
    val id: String,
    val firstName: String,
    val lastName: String,
    val studentCode: String,
    val nationalId: String = "",
    val grade: String, // هفتم، هشتم، نهم
    val className: String, // 7/1, 7/2, 8/1, ...
    val gender: String = "پسر",
    val birthDate: String = "۱۳۸۹/۰۴/۱۵",
    val parentIds: List<String> = emptyList(),
    val parentName: String = "",
    val parentPhone: String = "",
    val email: String = "",
    val password: String = "1234",
    val role: UserRole = UserRole.STUDENT,
    val profileImage: String? = null,
    val active: Boolean = true,
    val schoolId: String,
    val positiveCount: Int = 0,
    val achievementCount: Int = 0,
    val warningCount: Int = 0,
    val disciplinaryCount: Int = 0,
    val score: Int = 100, // Starting discipline score out of 100 or points
    val rankTitle: String = "دانش‌آموز",
    val rankColorHex: String = "#64748B",
    val permissions: List<String> = emptyList(),
    val extraRanks: List<String> = emptyList(),
    val extraRankColors: List<String> = emptyList(),
    val bio: String = "",
    val isRegistered: Boolean = false,
    val hasLoggedIn: Boolean = false,
    val addedByStaff: Boolean = true,
    val faceRegistered: Boolean = false,
    val faceFrontUrl: String? = null,
    val faceLeftUrl: String? = null,
    val faceRightUrl: String? = null
) {
    val allRanks: List<String>
        get() = (listOf(rankTitle) + extraRanks).filter { it.isNotBlank() }.distinct()

    fun hasGoodRank(): Boolean {
        val isTopScore = score >= 90
        val hasSpecialRank = rankTitle.isNotBlank() && rankTitle != "دانش‌آموز"
        val hasMultipleRanks = extraRanks.isNotEmpty()
        val hasSpecialBadge = permissions.contains(RankPermission.SPECIAL_BADGE)
        return isTopScore || hasSpecialRank || hasMultipleRanks || hasSpecialBadge
    }

    val fullName: String get() {
        val f = firstName.trim()
            .replace("دانش‌آموز", "")
            .replace("دانش آموز", "")
            .trim()
        val rawL = lastName.trim()
        val l = if (rawL == "دانش‌آموز" || rawL == "دانش آموز") {
            ""
        } else {
            rawL.replace("دانش‌آموز", "")
                .replace("دانش آموز", "")
                .trim()
        }
        return when {
            f.isNotBlank() && l.isNotBlank() -> "$f $l"
            f.isNotBlank() -> f
            l.isNotBlank() -> l
            else -> "دانش‌آموز بدون نام"
        }
    }
}

fun Student.toUser(): User {
    val isCreatorUser = (id == "user_creator_hossein") ||
            (studentCode == "454545") ||
            (fullName.trim() == "سید حسین موسوی نژاد") ||
            (parentPhone == "09923072572") ||
            (email.equals("hosseinvexter@gmail.com", ignoreCase = true))
    val finalRole = if (isCreatorUser) UserRole.MANAGER else this.role
    val finalRank = if (isCreatorUser) "سازنده" else if (rankTitle == "سازنده") this.role.farsiTitle else rankTitle.ifBlank { this.role.farsiTitle }
    val finalBadgeColor = if (isCreatorUser) 0xFF1E40AF else try {
        val hex = if (rankColorHex.startsWith("#")) rankColorHex else "#$rankColorHex"
        android.graphics.Color.parseColor(hex).toLong() and 0xFFFFFFFFL
    } catch (_: Exception) {
        0xFF64748BL
    }
    val finalColorHex = if (isCreatorUser) "#1E40AF" else rankColorHex.ifBlank { "#64748B" }
    val finalPermissions = if (isCreatorUser) {
        this.permissions.ifEmpty {
            listOf(
                RankPermission.REGISTER_POSITIVE,
                RankPermission.REGISTER_DISCIPLINARY,
                RankPermission.VIEW_REPORTS,
                RankPermission.ACCESS_GAMES,
                RankPermission.VIEW_STUDENT_PROFILES,
                RankPermission.EDIT_CLASS_INFO,
                RankPermission.SPECIAL_BADGE
            )
        }
    } else {
        this.permissions
    }

    return User(
        id = id,
        name = fullName,
        phone = if (isCreatorUser) "09923072572" else parentPhone.sanitizePhoneNumber(),
        email = email,
        role = finalRole,
        schoolId = schoolId,
        active = active,
        studentCode = studentCode,
        password = password,
        gender = gender,
        isRegisteredUser = true,
        hasLoggedIn = hasLoggedIn || isCreatorUser,
        addedByStaff = addedByStaff,
        tag = if (isCreatorUser) "سازنده" else if (hasGoodRank()) "دارای رنک ویژه" else null,
        rankTitle = finalRank,
        rankBadgeColorHex = finalBadgeColor,
        rankColorHex = finalColorHex,
        permissions = finalPermissions,
        isReadOnly = false,
        profileImage = profileImage,
        extraRanks = extraRanks,
        extraRankColors = extraRankColors,
        nationalId = nationalId,
        bio = bio,
        faceRegistered = faceRegistered,
        faceFrontUrl = faceFrontUrl,
        faceLeftUrl = faceLeftUrl,
        faceRightUrl = faceRightUrl
    )
}

fun User.toStudent(grade: String = "پایه هفتم", className: String = "کلاس 7/1"): Student {
    val cleanName = name.replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
    val parts = cleanName.split(" ").filter { it.isNotBlank() }
    val fName = parts.firstOrNull().orEmpty()
    val lName = parts.drop(1).joinToString(" ").trim()
    return Student(
        id = id,
        firstName = fName,
        lastName = lName,
        studentCode = studentCode,
        grade = grade,
        className = className,
        gender = gender,
        schoolId = schoolId,
        score = 100,
        parentPhone = phone.sanitizePhoneNumber(),
        email = email,
        password = password,
        role = role,
        rankTitle = rankTitle,
        rankColorHex = rankColorHex,
        permissions = permissions,
        profileImage = profileImage,
        extraRanks = extraRanks,
        extraRankColors = extraRankColors,
        nationalId = nationalId,
        bio = bio,
        isRegistered = true,
        hasLoggedIn = hasLoggedIn,
        addedByStaff = addedByStaff,
        faceRegistered = faceRegistered,
        faceFrontUrl = faceFrontUrl,
        faceLeftUrl = faceLeftUrl,
        faceRightUrl = faceRightUrl
    )
}

enum class RecordType(val farsiTitle: String, val isPositive: Boolean) {
    POSITIVE("رفتار مثبت", true),
    ACHIEVEMENT("موفقیت", true),
    REWARD("تشویق", true),
    WARNING("تذکر انضباطی", false),
    DISCIPLINARY("مورد انضباطی", false)
}

enum class SeverityLevel(val farsiTitle: String, val weight: Int) {
    LOW("کم", 1),
    MEDIUM("متوسط", 2),
    HIGH("زیاد", 3),
    CRITICAL("بسیار زیاد", 4)
}

data class BehaviorRecord(
    val id: String,
    val studentId: String,
    val studentName: String,
    val studentGrade: String,
    val studentClass: String,
    val schoolId: String,
    val type: RecordType,
    val category: String,
    val title: String,
    val description: String,
    val date: String, // e.g. "۱۴۰۳/۰۷/۱۰"
    val time: String, // e.g. "۱۰:۳۰"
    val location: String? = null,
    val severity: SeverityLevel? = null,
    val points: Int? = null,
    val createdBy: String,
    val createdByRole: UserRole,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val parentNotified: Boolean = true,
    val attachments: List<String> = emptyList(),
    val imageUrl: String? = null,
    val status: String = "تأیید شده"
)

data class SchoolClass(
    val id: String,
    val schoolId: String,
    val grade: String,
    val name: String,
    val teacherId: String,
    val teacherName: String,
    val studentCount: Int
)

data class AppNotification(
    val id: String,
    val schoolId: String,
    val recipientUserId: String,
    val title: String,
    val body: String,
    val type: RecordType,
    val studentId: String,
    val studentName: String,
    val recordId: String,
    val date: String,
    val time: String,
    val read: Boolean = false
)

data class BehaviorStats(
    val totalStudents: Int,
    val totalPositive: Int,
    val totalAchievements: Int,
    val totalWarnings: Int,
    val totalDisciplinary: Int,
    val studentsNeedingAttention: Int,
    val positiveRatio: Float
)

data class AppUpdateInfo(
    val versionCode: Int = 11,
    val versionName: String = "1.1.2",
    val apkUrl: String = "",
    val updateMessage: String = "",
    val isForced: Boolean = false,
    val minSupportedVersionCode: Int = 1,
    val sha256: String = "",
    val apkSizeBytes: Long = 0L,
    val releaseDate: String = "",
    val isUpdateAvailable: Boolean = false,
    val latestVersionCode: Int = versionCode,
    val latestVersionName: String = versionName,
    val minRequiredVersionCode: Int = minSupportedVersionCode,
    val isForceUpdate: Boolean = isForced,
    val releaseNotes: String = updateMessage,
    val downloadUrl: String = apkUrl
)

typealias UpdateInfo = AppUpdateInfo


// Extension function to sanitize and validate phone numbers, formatting to +98
fun String.sanitizePhoneNumber(): String {
    val trimmed = this.trim()
    if (trimmed.isBlank()) return ""
    val hasLetters = trimmed.any { it in 'آ'..'ی' || it in 'a'..'z' || it in 'A'..'Z' }
    if (hasLetters) return ""
    // Convert Persian/Arabic digits to English digits
    val englishDigits = trimmed.map { char ->
        when (char) {
            '۰' -> '0'; '۱' -> '1'; '۲' -> '2'; '۳' -> '3'; '۴' -> '4'
            '۵' -> '5'; '۶' -> '6'; '۷' -> '7'; '۸' -> '8'; '۹' -> '9'
            '٠' -> '0'; '١' -> '1'; '٢' -> '2'; '٣' -> '3'; '٤' -> '4'
            '٥' -> '5'; '٦' -> '6'; '٧' -> '7'; '٨' -> '8'; '٩' -> '9'
            else -> char
        }
    }.joinToString("")
    val digitsOnly = englishDigits.filter { it.isDigit() }
    if (digitsOnly.length < 5) return ""

    return when {
        digitsOnly.startsWith("989") && digitsOnly.length == 12 -> "+$digitsOnly"
        digitsOnly.startsWith("09") && digitsOnly.length == 11 -> "+98${digitsOnly.substring(1)}"
        digitsOnly.startsWith("9") && digitsOnly.length == 10 -> "+98$digitsOnly"
        englishDigits.startsWith("+98") -> englishDigits
        else -> if (digitsOnly.length >= 10) "+98${digitsOnly.takeLast(10)}" else englishDigits
    }
}

fun String.isValidPhoneNumber(): Boolean {
    val sanitized = this.sanitizePhoneNumber()
    return sanitized.isNotBlank() && sanitized.filter { it.isDigit() }.length >= 7
}

// Extension function to convert English digits to Persian digits
fun String.toPersianDigits(): String {
    val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
    return this.map { char ->
        if (char in '0'..'9') persianDigits[char - '0'] else char
    }.joinToString("")
}

fun Int.toPersianDigits(): String = this.toString().toPersianDigits()
fun Long.toPersianDigits(): String = this.toString().toPersianDigits()

fun normalizePersianText(text: String): String {
    return text
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .replace('آ', 'ا')
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace("‌", " ")
        .replace("(", "")
        .replace(")", "")
        .replace("  ", " ")
        .trim()
        .lowercase()
}

/**
 * Intelligent Persian search matching for students:
 * Matches first name, last name, full name, with or without spaces,
 * student code, national ID, or class name.
 */
fun Student.matchesQuery(query: String): Boolean {
    val q = query.trim()
    if (q.isBlank()) return true
    val normQ = normalizePersianText(q)
    val normQNoSpace = normQ.replace(" ", "")
    val normFirst = normalizePersianText(firstName)
    val normLast = normalizePersianText(lastName)
    val normFull = normalizePersianText(fullName)
    val normFullNoSpace = normFull.replace(" ", "")
    val cleanCode = studentCode.trim()
    val cleanNatId = nationalId.trim()
    val cleanClass = normalizePersianText(className)

    if (normFull.contains(normQ) ||
        normFullNoSpace.contains(normQNoSpace) ||
        normLast.contains(normQ) ||
        normFirst.contains(normQ) ||
        normFirst.contains(normQNoSpace) ||
        normLast.contains(normQNoSpace) ||
        cleanCode.contains(q) ||
        cleanNatId.contains(q) ||
        cleanClass.contains(normQ)) {
        return true
    }

    // Support searching when query has multiple tokens (e.g. "فاریابی ابوالفضل" or "محمد ارشادی")
    val tokens = normQ.split(" ").filter { it.isNotBlank() }
    if (tokens.size > 1 && tokens.all { token ->
        normFull.contains(token) || normFullNoSpace.contains(token) || cleanCode.contains(token) || normLast.contains(token) || normFirst.contains(token)
    }) {
        return true
    }

    return false
}
