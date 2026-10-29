package com.example.core.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.core.utils.DateTimeUtils
import com.example.domain.model.AppNotification
import com.example.domain.model.RecordType
import com.example.domain.model.User
import com.example.domain.model.UserRole
import java.util.UUID

object ParentAccessManager {

    private const val PREFS_NAME = "nezamyar_parent_access"
    private const val KEY_PREFIX_FATHER = "parent_father_"
    private const val KEY_PREFIX_MOTHER = "parent_mother_"
    private const val KEY_PREFIX_NOTIFIED = "parent_both_notified_"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun getPrefs(context: Context? = null): SharedPreferences? {
        if (prefs == null && context != null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        return prefs
    }

    /**
     * Checks if father of this student has logged into the app.
     * Evaluates local persistent SharedPreferences as well as registered user pool.
     */
    fun isFatherLoggedIn(
        context: Context? = null,
        studentCode: String,
        allUsers: List<User> = emptyList()
    ): Boolean {
        if (studentCode.isBlank()) return false
        val p = getPrefs(context)
        if (p?.getBoolean(KEY_PREFIX_FATHER + studentCode, false) == true) return true

        // Check in user list (synced remotely or locally)
        val hasUser = allUsers.any { u ->
            u.role == UserRole.PARENT &&
                (u.studentCode == studentCode || u.childStudentIds.contains(studentCode)) &&
                (u.id.endsWith("_dad") || u.rankTitle.contains("پدر") || u.gender.contains("پدر"))
        }
        if (hasUser) {
            p?.edit()?.putBoolean(KEY_PREFIX_FATHER + studentCode, true)?.apply()
            return true
        }
        return false
    }

    /**
     * Checks if mother of this student has logged into the app.
     * Evaluates local persistent SharedPreferences as well as registered user pool.
     */
    fun isMotherLoggedIn(
        context: Context? = null,
        studentCode: String,
        allUsers: List<User> = emptyList()
    ): Boolean {
        if (studentCode.isBlank()) return false
        val p = getPrefs(context)
        if (p?.getBoolean(KEY_PREFIX_MOTHER + studentCode, false) == true) return true

        // Check in user list (synced remotely or locally)
        val hasUser = allUsers.any { u ->
            u.role == UserRole.PARENT &&
                (u.studentCode == studentCode || u.childStudentIds.contains(studentCode)) &&
                (u.id.endsWith("_mom") || u.rankTitle.contains("مادر") || u.gender.contains("مادر"))
        }
        if (hasUser) {
            p?.edit()?.putBoolean(KEY_PREFIX_MOTHER + studentCode, true)?.apply()
            return true
        }
        return false
    }

    private const val KEY_PREFIX_SINGLE_PARENT = "parent_single_mode_"

    /**
     * Checks whether single parent login mode is enabled for a student.
     */
    fun isSingleParentModeEnabled(context: Context? = null, studentCode: String): Boolean {
        if (studentCode.isBlank()) return false
        val p = getPrefs(context)
        return p?.getBoolean(KEY_PREFIX_SINGLE_PARENT + studentCode, false) == true
    }

    /**
     * Toggles or sets single parent mode for a student.
     */
    fun setSingleParentMode(context: Context? = null, studentCode: String, enabled: Boolean) {
        if (studentCode.isBlank()) return
        val p = getPrefs(context)
        p?.edit()?.putBoolean(KEY_PREFIX_SINGLE_PARENT + studentCode, enabled)?.apply()
    }

    /**
     * Are BOTH parents logged into the app for this student?
     * If single parent mode is enabled, login of either father OR mother satisfies the requirement.
     */
    fun areBothParentsLoggedIn(
        context: Context? = null,
        studentCode: String,
        allUsers: List<User> = emptyList()
    ): Boolean {
        if (studentCode.isBlank()) return false
        val fatherIn = isFatherLoggedIn(context, studentCode, allUsers)
        val motherIn = isMotherLoggedIn(context, studentCode, allUsers)
        if (isSingleParentModeEnabled(context, studentCode)) {
            return fatherIn || motherIn
        }
        return fatherIn && motherIn
    }

    /**
     * Records a parent login.
     * Returns true if this specific login completed the dual-parent requirement for the first time
     * (triggering the celebration notification).
     */
    fun recordParentLogin(
        context: Context? = null,
        studentCode: String,
        isMother: Boolean
    ): Boolean {
        if (studentCode.isBlank()) return false
        val p = getPrefs(context)
        val key = if (isMother) KEY_PREFIX_MOTHER + studentCode else KEY_PREFIX_FATHER + studentCode
        p?.edit()?.putBoolean(key, true)?.apply()

        val fatherOk = p?.getBoolean(KEY_PREFIX_FATHER + studentCode, false) == true
        val motherOk = p?.getBoolean(KEY_PREFIX_MOTHER + studentCode, false) == true
        val alreadyNotified = p?.getBoolean(KEY_PREFIX_NOTIFIED + studentCode, false) == true

        if (fatherOk && motherOk && !alreadyNotified) {
            p?.edit()?.putBoolean(KEY_PREFIX_NOTIFIED + studentCode, true)?.apply()
            return true
        }
        return false
    }

    /**
     * Checks if both parents are logged in and if notification hasn't been sent yet, marks it and returns true.
     */
    fun checkAndMarkNotificationSent(context: Context? = null, studentCode: String): Boolean {
        if (studentCode.isBlank()) return false
        val p = getPrefs(context)
        val fatherOk = p?.getBoolean(KEY_PREFIX_FATHER + studentCode, false) == true
        val motherOk = p?.getBoolean(KEY_PREFIX_MOTHER + studentCode, false) == true
        val alreadyNotified = p?.getBoolean(KEY_PREFIX_NOTIFIED + studentCode, false) == true

        if (fatherOk && motherOk && !alreadyNotified) {
            p?.edit()?.putBoolean(KEY_PREFIX_NOTIFIED + studentCode, true)?.apply()
            return true
        }
        return false
    }

    /**
     * Generates celebratory unlock notifications for both parents and the student.
     */
    fun createBothParentsUnlockedNotifications(
        studentCode: String,
        studentName: String,
        studentId: String,
        schoolId: String
    ): List<AppNotification> {
        val title = "🎉 تبریک! ورود والدین دانش‌آموز تکمیل شد"
        val body = "اولیای گرامی «$studentName»، ورود هر دو ولی (پدر و مادر) در سامانه نظمیار تکمیل شد. از این پس دسترسی به کلیه گزارش‌ها، سوابق انضباطی، رفتارهای کلاسی و اطلاعات پرونده فرزندتان برای شما فعال می‌باشد."
        val date = DateTimeUtils.getCurrentPersianDate()
        val time = DateTimeUtils.getCurrentTime()

        return listOf(
            // To Father
            AppNotification(
                id = "notif_both_parents_dad_${UUID.randomUUID().toString().take(6)}",
                schoolId = schoolId,
                recipientUserId = "parent_${studentCode}_dad",
                title = title,
                body = body,
                type = RecordType.ACHIEVEMENT,
                studentId = studentId,
                studentName = studentName,
                recordId = "",
                date = date,
                time = time,
                read = false
            ),
            // To Mother
            AppNotification(
                id = "notif_both_parents_mom_${UUID.randomUUID().toString().take(6)}",
                schoolId = schoolId,
                recipientUserId = "parent_${studentCode}_mom",
                title = title,
                body = body,
                type = RecordType.ACHIEVEMENT,
                studentId = studentId,
                studentName = studentName,
                recordId = "",
                date = date,
                time = time,
                read = false
            ),
            // To Student
            AppNotification(
                id = "notif_both_parents_std_${UUID.randomUUID().toString().take(6)}",
                schoolId = schoolId,
                recipientUserId = studentId,
                title = "🎉 پیوستن کامل والدین به نظمیار",
                body = "پدر و مادر گرامی شما هر دو وارد سامانه نظمیار شدند و گزارش‌ها و سوابق مدرسه را دریافت خواهند کرد.",
                type = RecordType.ACHIEVEMENT,
                studentId = studentId,
                studentName = studentName,
                recordId = "",
                date = date,
                time = time,
                read = false
            )
        )
    }
}
