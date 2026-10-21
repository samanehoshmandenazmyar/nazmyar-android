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

    private const val KEY_LAST_UPDATE_INFO = "cached_last_app_update_info"

    fun saveLastKnownUpdate(info: com.example.domain.model.AppUpdateInfo) {
        try {
            val json = JSONObject().apply {
                put("version_code", info.versionCode)
                put("version_name", info.versionName)
                put("apk_url", info.apkUrl)
                put("update_message", info.updateMessage)
                put("is_forced", info.isForced)
                put("min_supported_version_code", info.minSupportedVersionCode)
                put("sha256", info.sha256)
                put("apk_size_bytes", info.apkSizeBytes)
                put("release_date", info.releaseDate)
                put("is_available", info.isUpdateAvailable)
            }
            prefs?.edit()?.putString(KEY_LAST_UPDATE_INFO, json.toString())?.apply()
        } catch (_: Exception) {}
    }

    fun getLastKnownUpdate(): com.example.domain.model.AppUpdateInfo? {
        val str = prefs?.getString(KEY_LAST_UPDATE_INFO, null) ?: return null
        return try {
            val obj = JSONObject(str)
            com.example.domain.model.AppUpdateInfo(
                versionCode = obj.optInt("version_code", obj.optInt("latestVersionCode", 8)),
                versionName = obj.optString("version_name", obj.optString("latestVersionName", "1.0.7")),
                apkUrl = obj.optString("apk_url", obj.optString("downloadUrl", "")),
                updateMessage = obj.optString("update_message", obj.optString("releaseNotes", "")),
                isForced = obj.optBoolean("is_forced", obj.optBoolean("isForceUpdate", false)),
                minSupportedVersionCode = obj.optInt("min_supported_version_code", obj.optInt("minRequiredVersionCode", 1)),
                sha256 = obj.optString("sha256", ""),
                apkSizeBytes = obj.optLong("apk_size_bytes", obj.optLong("apkSizeBytes", 0L)),
                releaseDate = obj.optString("release_date", obj.optString("releaseDate", "")),
                isUpdateAvailable = obj.optBoolean("is_available", false)
            )
        } catch (_: Exception) {
            null
        }
    }

    fun clearLastKnownUpdate() {
        prefs?.edit()?.remove(KEY_LAST_UPDATE_INFO)?.apply()
    }
}

