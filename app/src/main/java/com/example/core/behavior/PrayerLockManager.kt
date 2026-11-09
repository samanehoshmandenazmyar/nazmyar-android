package com.example.core.behavior

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.core.network.FirebaseRealtimeService
import com.example.core.network.NetworkMonitor
import com.example.domain.model.PrayerLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

object PrayerLockManager {
    private const val TAG = "PrayerLockManager"
    private const val PREFS_NAME = "nezamyar_prayer_locks_prefs"
    private const val KEY_LOCKS = "prayer_locks_json_v1"

    private var prefs: SharedPreferences? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var isNetworkObserverRegistered = false

    private val _prayerLocks = MutableStateFlow<List<PrayerLock>>(emptyList())
    val prayerLocks: StateFlow<List<PrayerLock>> = _prayerLocks.asStateFlow()

    fun init(context: Context) {
        val appContext = context.applicationContext
        if (prefs == null) {
            prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        loadLocalLocks()

        // Sync with server immediately
        scope.launch {
            syncWithServer()
        }

        // Live continuous real-time background sync (every 2.5 seconds)
        scope.launch {
            while (true) {
                delay(2500)
                try {
                    syncWithServer()
                } catch (_: Throwable) {}
            }
        }

        // Auto-sync on network reconnect
        if (!isNetworkObserverRegistered) {
            isNetworkObserverRegistered = true
            scope.launch {
                NetworkMonitor.observeNetwork(appContext).collect { isOnline ->
                    if (isOnline) {
                        syncWithServer()
                    }
                }
            }
        }
    }

    private fun loadLocalLocks() {
        val p = prefs ?: return
        val savedJson = p.getString(KEY_LOCKS, null) ?: return
        try {
            val array = JSONArray(savedJson)
            val list = mutableListOf<PrayerLock>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    PrayerLock(
                        id = obj.optString("id", ""),
                        date = obj.optString("date", ""),
                        prayerTime = obj.optString("prayerTime", ""),
                        isLocked = obj.optBoolean("isLocked", true),
                        lockedBy = obj.optString("lockedBy", ""),
                        lockedAt = obj.optLong("lockedAt", 0L)
                    )
                )
            }
            _prayerLocks.value = list
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local prayer locks: ${e.message}")
        }
    }

    private fun saveLocalLocks(list: List<PrayerLock>) {
        val p = prefs ?: return
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("date", item.date)
                    put("prayerTime", item.prayerTime)
                    put("isLocked", item.isLocked)
                    put("lockedBy", item.lockedBy)
                    put("lockedAt", item.lockedAt)
                }
                array.put(obj)
            }
            p.edit().putString(KEY_LOCKS, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local prayer locks: ${e.message}")
        }
    }

    private suspend fun syncWithServer() {
        val remote = FirebaseRealtimeService.fetchPrayerLocks() ?: return
        val map = mutableMapOf<String, PrayerLock>()
        // Remote state is authoritative for real-time sync across all devices
        for (lock in remote) {
            map[lock.id] = lock
        }
        val merged = map.values.toList()
        _prayerLocks.value = merged
        saveLocalLocks(merged)
    }

    private fun normalizeDate(date: String): String {
        return date.trim().replace('-', '/').replace('_', '/')
    }

    fun isLocked(date: String, prayerTime: String): Boolean {
        val normDate = normalizeDate(date)
        val lockId = PrayerLock.buildId(normDate, prayerTime)
        return _prayerLocks.value.any {
            (it.id == lockId || (normalizeDate(it.date) == normDate && it.prayerTime.trim() == prayerTime.trim())) && it.isLocked
        }
    }

    fun setPrayerLock(date: String, prayerTime: String, isLocked: Boolean, lockedBy: String) {
        val normDate = normalizeDate(date)
        val lockId = PrayerLock.buildId(normDate, prayerTime)
        val newLock = PrayerLock(
            id = lockId,
            date = normDate,
            prayerTime = prayerTime.trim(),
            isLocked = isLocked,
            lockedBy = lockedBy,
            lockedAt = System.currentTimeMillis()
        )

        // Instant optimistic update in memory & local storage
        val current = _prayerLocks.value.filter { it.id != lockId }.toMutableList()
        if (isLocked) {
            current.add(newLock)
        }
        _prayerLocks.value = current
        saveLocalLocks(current)

        // Async sync to server & relay
        scope.launch {
            try {
                if (isLocked) {
                    FirebaseRealtimeService.savePrayerLock(newLock)
                } else {
                    FirebaseRealtimeService.deletePrayerLock(lockId)
                    FirebaseRealtimeService.savePrayerLock(newLock.copy(isLocked = false))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync prayer lock to server: ${e.message}")
            }
        }
    }
}
