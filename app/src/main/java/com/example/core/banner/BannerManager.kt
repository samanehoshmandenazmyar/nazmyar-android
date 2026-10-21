package com.example.core.banner

import android.content.Context
import android.content.SharedPreferences
import com.example.domain.model.HomeBanner
import com.example.domain.model.SchoolClasses
import com.example.domain.model.User
import com.example.domain.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

object BannerManager {
    private const val PREFS_NAME = "nezamyar_home_banners_prefs"
    private const val KEY_BANNERS = "home_banners_json_v2"

    private var prefs: SharedPreferences? = null

    val defaultBanners: List<HomeBanner> = emptyList()

    private val _banners = MutableStateFlow<List<HomeBanner>>(emptyList())
    val banners: StateFlow<List<HomeBanner>> = _banners.asStateFlow()

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        loadBanners()
    }

    @Synchronized
    private fun loadBanners() {
        val p = prefs ?: return
        val savedJson = p.getString(KEY_BANNERS, null)

        if (savedJson != null) {
            try {
                val array = JSONArray(savedJson)
                val list = mutableListOf<HomeBanner>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.getString("id")
                    // Filter out previous default banners if stored in local cache
                    if (id == "banner_default_1" || id == "banner_default_2") {
                        continue
                    }
                    val targetClassesList = mutableListOf<String>()
                    if (obj.has("targetClasses")) {
                        val tcArray = obj.getJSONArray("targetClasses")
                        for (j in 0 until tcArray.length()) {
                            targetClassesList.add(tcArray.getString(j))
                        }
                    } else if (obj.has("targetGrade")) {
                        val legacy = obj.optString("targetGrade", "ALL")
                        when (legacy) {
                            "GRADE_7" -> targetClassesList.addAll(listOf("هفتم یک", "هفتم دو"))
                            "GRADE_8" -> targetClassesList.addAll(listOf("هشتم یک", "هشتم دو"))
                            "GRADE_9" -> targetClassesList.addAll(listOf("نهم یک", "نهم دو"))
                            else -> {}
                        }
                    }

                    list.add(
                        HomeBanner(
                            id = id,
                            title = obj.getString("title"),
                            imageUrl = obj.getString("imageUrl"),
                            targetClasses = targetClassesList,
                            linkUrl = if (obj.has("linkUrl") && !obj.isNull("linkUrl")) obj.getString("linkUrl") else null,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                _banners.value = list
                saveBanners(list)
            } catch (_: Exception) {
                _banners.value = emptyList()
                saveBanners(emptyList())
            }
        } else {
            _banners.value = emptyList()
            saveBanners(emptyList())
        }
    }

    private fun saveBanners(list: List<HomeBanner>) {
        val p = prefs ?: return
        try {
            val array = JSONArray()
            for (b in list) {
                val obj = JSONObject()
                obj.put("id", b.id)
                obj.put("title", b.title)
                obj.put("imageUrl", b.imageUrl)

                val tcArray = JSONArray()
                for (c in b.targetClasses) {
                    tcArray.put(c)
                }
                obj.put("targetClasses", tcArray)

                obj.put("linkUrl", b.linkUrl)
                obj.put("createdAt", b.createdAt)
                array.put(obj)
            }
            p.edit().putString(KEY_BANNERS, array.toString()).apply()
        } catch (_: Exception) {}
    }

    @Synchronized
    fun addBanner(banner: HomeBanner) {
        val currentList = _banners.value.toMutableList()
        currentList.add(0, banner)
        _banners.value = currentList
        saveBanners(currentList)
    }

    @Synchronized
    fun deleteBanner(bannerId: String) {
        val currentList = _banners.value.toMutableList()
        currentList.removeAll { it.id == bannerId }
        _banners.value = currentList
        saveBanners(currentList)
    }

    @Synchronized
    fun resetToDefaults() {
        _banners.value = emptyList()
        saveBanners(emptyList())
    }

    @Synchronized
    fun clearAllBanners() {
        _banners.value = emptyList()
        saveBanners(emptyList())
    }

    fun getBannersForUser(
        user: User?,
        studentGrade: String? = null,
        studentClassName: String? = null,
        parentChildren: List<Pair<String, String>> = emptyList()
    ): List<HomeBanner> {
        val all = _banners.value
        // Managers and Creator can see all banners
        if (user?.isCreator == true || user?.role == UserRole.MANAGER || user?.role == UserRole.VICE_PRINCIPAL) {
            return all
        }

        // Student filtering
        if (user?.role == UserRole.STUDENT) {
            return all.filter { banner ->
                SchoolClasses.matchesStudent(banner.targetClasses, studentGrade, studentClassName)
            }
        }

        // Parent filtering
        if (user?.role == UserRole.PARENT && parentChildren.isNotEmpty()) {
            return all.filter { banner ->
                banner.targetClasses.isEmpty() || parentChildren.any { (g, c) ->
                    SchoolClasses.matchesStudent(banner.targetClasses, g, c)
                }
            }
        }

        // Teachers or others
        return all.filter { it.targetClasses.isEmpty() || it.targetClasses.contains("همه") }
    }
}
