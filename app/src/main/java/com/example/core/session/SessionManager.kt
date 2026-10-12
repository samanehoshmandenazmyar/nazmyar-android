package com.example.core.session

import android.content.Context
import android.content.SharedPreferences
import com.example.data.demo.DemoData
import com.example.domain.model.User
import com.example.domain.model.UserRole
import org.json.JSONArray
import org.json.JSONObject

object SessionManager {
    private const val PREFS_NAME = "nezamyar_session_prefs"
    private const val KEY_USER_JSON = "logged_in_user_json"
    private const val KEY_PROXY_URL = "custom_proxy_url"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun saveUser(user: User) {
        try {
            val json = JSONObject().apply {
                put("id", user.id)
                put("name", user.name)
                put("phone", user.phone)
                put("email", user.email)
                put("role", user.role.name)
                put("schoolId", user.schoolId)
                put("active", user.active)
                put("tag", user.tag ?: "")
                put("studentCode", user.studentCode)
                put("password", user.password)
                put("gender", user.gender)
                put("isRegisteredUser", user.isRegisteredUser)
                put("rankTitle", user.rankTitle)
                put("rankBadgeColorHex", user.rankBadgeColorHex)
                put("rankColorHex", user.rankColorHex)
                put("permissions", JSONArray(user.permissions))
                put("isReadOnly", user.isReadOnly)
            }
            prefs?.edit()?.putString(KEY_USER_JSON, json.toString())?.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSavedUser(): User? {
        val jsonStr = prefs?.getString(KEY_USER_JSON, null) ?: return null
        return try {
            val obj = JSONObject(jsonStr)
            val role = UserRole.fromString(obj.optString("role", "STUDENT"))
            val permsArray = obj.optJSONArray("permissions")
            val permissions = mutableListOf<String>()
            if (permsArray != null) {
                for (i in 0 until permsArray.length()) {
                    permissions.add(permsArray.optString(i))
                }
            }
            User(
                id = obj.optString("id"),
                name = obj.optString("name"),
                phone = obj.optString("phone"),
                email = obj.optString("email"),
                role = role,
                schoolId = obj.optString("schoolId", DemoData.DEFAULT_SCHOOL_ID),
                active = obj.optBoolean("active", true),
                tag = obj.optString("tag").ifBlank { null },
                studentCode = obj.optString("studentCode", ""),
                password = obj.optString("password", ""),
                gender = obj.optString("gender", "پسر"),
                isRegisteredUser = obj.optBoolean("isRegisteredUser", false),
                rankTitle = obj.optString("rankTitle", if (role == UserRole.STUDENT) "دانش‌آموز" else role.farsiTitle),
                rankBadgeColorHex = obj.optLong("rankBadgeColorHex", 0xFF64748B),
                rankColorHex = obj.optString("rankColorHex", "#64748B"),
                permissions = permissions,
                isReadOnly = obj.optBoolean("isReadOnly", false)
            )
        } catch (e: Exception) {
            null
        }
    }

    private const val KEY_SEEDED = "initial_seed_completed_v2"

    fun isInitialSeeded(): Boolean = prefs?.getBoolean(KEY_SEEDED, false) ?: false
    fun markInitialSeeded() {
        prefs?.edit()?.putBoolean(KEY_SEEDED, true)?.apply()
    }

    fun clearSession() {
        prefs?.edit()?.remove(KEY_USER_JSON)?.apply()
    }

    private const val KEY_SEEN_CELEBRATION_PREFIX = "seen_rank_celebration_"

    fun hasSeenRankCelebration(userId: String, rankTitle: String): Boolean {
        return prefs?.getBoolean("${KEY_SEEN_CELEBRATION_PREFIX}${userId}_${rankTitle}", false) ?: false
    }

    fun markRankCelebrationSeen(userId: String, rankTitle: String) {
        prefs?.edit()?.putBoolean("${KEY_SEEN_CELEBRATION_PREFIX}${userId}_${rankTitle}", true)?.apply()
    }

    fun saveCustomProxyUrl(url: String) {}

    fun getCustomProxyUrl(): String? = null
}
