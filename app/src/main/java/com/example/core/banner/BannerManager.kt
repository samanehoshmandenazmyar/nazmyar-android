package com.example.core.banner

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.core.network.FirebaseRealtimeService
import com.example.core.network.NetworkMonitor
import com.example.domain.model.HomeBanner
import com.example.domain.model.SchoolClasses
import com.example.domain.model.User
import com.example.domain.model.UserRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object BannerManager {
    private const val TAG = "BannerManager"
    private const val PREFS_NAME = "nezamyar_home_banners_prefs"
    private const val KEY_BANNERS = "home_banners_json_v2"

    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isNetworkObserverRegistered = false

    val defaultBanners: List<HomeBanner> = emptyList()

    private val _banners = MutableStateFlow<List<HomeBanner>>(emptyList())
    val banners: StateFlow<List<HomeBanner>> = _banners.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun init(context: Context) {
        val appContext = context.applicationContext
        if (prefs == null) {
            prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        loadBanners()

        // Sync with server immediately
        scope.launch {
            syncWithServer(appContext)
        }

        // Live continuous real-time background sync (every 2 seconds)
        scope.launch {
            while (true) {
                kotlinx.coroutines.delay(2000)
                try {
                    syncWithServer(appContext)
                } catch (_: Throwable) {}
            }
        }

        // Auto-sync whenever internet connectivity is restored
        if (!isNetworkObserverRegistered) {
            isNetworkObserverRegistered = true
            scope.launch {
                NetworkMonitor.observeNetwork(appContext).collect { isOnline ->
                    if (isOnline) {
                        syncWithServer(appContext)
                    }
                }
            }
        }
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
            } catch (_: Exception) {
                _banners.value = emptyList()
            }
        } else {
            _banners.value = emptyList()
        }
    }

    private fun saveBannersLocally(list: List<HomeBanner>) {
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

    suspend fun syncWithServer(context: Context? = null) {
        if (_isSyncing.value) return
        _isSyncing.value = true
        try {
            val remoteBanners = FirebaseRealtimeService.fetchBanners()
            if (remoteBanners != null) {
                if (remoteBanners.isNotEmpty()) {
                    _banners.value = remoteBanners
                    saveBannersLocally(remoteBanners)
                } else if (_banners.value.isNotEmpty()) {
                    // Server is empty but we have local banners - seed server
                    for (b in _banners.value) {
                        FirebaseRealtimeService.saveBanner(b)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing banners with server: ${e.message}")
        } finally {
            _isSyncing.value = false
        }
    }

    @Synchronized
    fun addBanner(banner: HomeBanner) {
        val currentList = _banners.value.toMutableList()
        currentList.removeAll { it.id == banner.id }
        currentList.add(0, banner)
        _banners.value = currentList
        saveBannersLocally(currentList)

        // Asynchronously persist to server
        scope.launch {
            try {
                FirebaseRealtimeService.saveBanner(banner)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist banner to server: ${e.message}")
            }
        }
    }

    @Synchronized
    fun deleteBanner(bannerId: String) {
        val currentList = _banners.value.toMutableList()
        currentList.removeAll { it.id == bannerId }
        _banners.value = currentList
        saveBannersLocally(currentList)

        // Asynchronously delete from server
        scope.launch {
            try {
                FirebaseRealtimeService.deleteBanner(bannerId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete banner on server: ${e.message}")
            }
        }
    }

    @Synchronized
    fun resetToDefaults() {
        clearAllBanners()
    }

    @Synchronized
    fun clearAllBanners() {
        _banners.value = emptyList()
        saveBannersLocally(emptyList())

        // Asynchronously clear on server
        scope.launch {
            try {
                FirebaseRealtimeService.clearAllBanners()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear banners on server: ${e.message}")
            }
        }
    }

    /**
     * Resizes, compresses and Base64-encodes picked banner image from user gallery.
     * Guarantees persistence across device restarts and server synchronization.
     */
    fun compressAndEncodeImage(context: Context, uri: Uri): String {
        return try {
            val contentResolver = context.contentResolver

            // Sample bounds to prevent OutOfMemory on huge gallery photos
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }

            var inSampleSize = 1
            val maxDimension = 1200
            if (boundsOptions.outWidth > maxDimension || boundsOptions.outHeight > maxDimension) {
                val halfWidth = boundsOptions.outWidth / 2
                val halfHeight = boundsOptions.outHeight / 2
                while ((halfWidth / inSampleSize) >= maxDimension || (halfHeight / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val rawBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return uri.toString()

            // Scale to target 16:9 banner size (max width 960, max height 540)
            val maxTargetWidth = 960
            val maxTargetHeight = 540
            val scale = minOf(
                maxTargetWidth.toFloat() / rawBitmap.width,
                maxTargetHeight.toFloat() / rawBitmap.height,
                1.0f
            )

            val scaledBitmap = if (scale < 1.0f) {
                val newW = (rawBitmap.width * scale).toInt().coerceAtLeast(1)
                val newH = (rawBitmap.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(rawBitmap, newW, newH, true).also {
                    if (it != rawBitmap) rawBitmap.recycle()
                }
            } else {
                rawBitmap
            }

            // Compress to JPEG 75% quality for lightweight cloud payload (~40KB - 80KB)
            val byteStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, byteStream)
            val bytes = byteStream.toByteArray()

            // Also save locally in app private storage as permanent backup
            try {
                val bannersDir = File(context.filesDir, "banners").apply { if (!exists()) mkdirs() }
                val localFile = File(bannersDir, "banner_${System.currentTimeMillis()}.jpg")
                FileOutputStream(localFile).use { fos ->
                    fos.write(bytes)
                }
            } catch (_: Exception) {}

            scaledBitmap.recycle()

            val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$base64String"
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress and encode image: ${e.message}")
            uri.toString()
        }
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
