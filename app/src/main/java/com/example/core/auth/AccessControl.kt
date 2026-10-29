package com.example.core.auth

import com.example.domain.model.RankPermission
import com.example.domain.model.User
import com.example.domain.model.UserRole

object AccessControl {

    /**
     * Checks if the given user is authorized to manage faces (register, edit, delete),
     * perform face scans, and log student behaviors and score changes.
     *
     * Permitted:
     * - School Principal / Manager (مدیر)
     * - Vice Principals (معاونین)
     * - Teachers (دبیران)
     * - Counselors (مسئولین پرورشی)
     * - System Creator (سازنده)
     * - Any staff / special individuals with explicit behavior registration permissions
     *
     * Strictly Prohibited:
     * - Students (دانش‌آموزان)
     * - Parents (پدر و مادر دانش‌آموز)
     */
    fun canManageFacesAndBehaviors(user: User?): Boolean {
        if (user == null) return false

        // Creator always has full access
        if (user.isCreator) return true

        // Check if user has explicit behavior or student management permissions from custom ranks
        if (user.permissions.contains(RankPermission.REGISTER_POSITIVE) ||
            user.permissions.contains(RankPermission.REGISTER_DISCIPLINARY) ||
            user.permissions.contains(RankPermission.VIEW_STUDENT_PROFILES) ||
            user.permissions.contains(RankPermission.EDIT_CLASS_INFO)) {
            return true
        }

        // Staff roles
        return user.role == UserRole.MANAGER ||
                user.role == UserRole.VICE_PRINCIPAL ||
                user.role == UserRole.COUNSELOR ||
                user.role == UserRole.TEACHER
    }

    /**
     * Reason message explaining why access is denied if an unauthorized user attempts the action.
     */
    fun getAccessDeniedMessage(user: User?): String {
        return when {
            user == null -> "لطفاً ابتدا وارد حساب کاربری خود شوید."
            user.role == UserRole.PARENT -> "اولیاء محترم امکان ثبت چهره یا تغییر موارد انضباطی دانش‌آموزان را ندارند."
            else -> "دسترسی یا رنک لازم برای این بخش به حساب شما اختصاص داده نشده است."
        }
    }
}
