package com.example.core.network

import com.example.core.session.SessionManager

/**
 * Global Server & Cloud Database Configuration.
 * High-Speed Anti-Sanction & Direct Firebase Failover.
 */
object ServerConfig {
    // Primary Anti-Sanction Cloudflare Worker Proxy (bypasses Iran filtering & Google 403 sanctions)
    const val PRIMARY_WORKER_URL = "https://nazmyar-proxy.vexter22178.workers.dev"

    // Direct Firebase Realtime Database
    const val DIRECT_FIREBASE_URL = "https://nazmyarapp-default-rtdb.firebaseio.com"

    const val FIREBASE_DATABASE_URL = PRIMARY_WORKER_URL

    @Volatile
    private var currentActiveUrl: String = PRIMARY_WORKER_URL

    val activeUrl: String
        get() {
            val custom = SessionManager.getCustomProxyUrl()
            if (!custom.isNullOrBlank()) return custom.trim().removeSuffix("/")
            return currentActiveUrl
        }

    val serverHost: String
        get() = activeUrl.removePrefix("https://").removePrefix("http://").removeSuffix("/")

    fun getCandidateUrls(): List<String> {
        val list = mutableListOf<String>()
        val custom = SessionManager.getCustomProxyUrl()
        if (!custom.isNullOrBlank()) {
            list.add(custom.trim().removeSuffix("/"))
        }
        list.add(PRIMARY_WORKER_URL.removeSuffix("/"))
        list.add(DIRECT_FIREBASE_URL.removeSuffix("/"))
        return list.distinct()
    }

    fun markWorkingUrl(url: String) {
        val clean = url.trim().removeSuffix("/")
        if (clean.isNotBlank()) {
            currentActiveUrl = clean
        }
    }

    fun markFailedUrl(url: String) {
        val clean = url.trim().removeSuffix("/")
        val candidates = getCandidateUrls()
        val next = candidates.firstOrNull { it != clean }
        if (next != null) {
            currentActiveUrl = next
        }
    }

    val usersUrl: String get() = usersUrl(activeUrl)
    val studentsUrl: String get() = studentsUrl(activeUrl)
    val recordsUrl: String get() = recordsUrl(activeUrl)
    val notificationsUrl: String get() = notificationsUrl(activeUrl)
    val classesUrl: String get() = classesUrl(activeUrl)
    val recoveryLogsUrl: String get() = recoveryLogsUrl(activeUrl)
    val customRanksUrl: String get() = customRanksUrl(activeUrl)
    val appUpdateUrl: String get() = appUpdateUrl(activeUrl)

    fun usersUrl(base: String = activeUrl): String = "${base.removeSuffix("/")}/users.json"
    fun studentsUrl(base: String = activeUrl): String = "${base.removeSuffix("/")}/students.json"
    fun recordsUrl(base: String = activeUrl): String = "${base.removeSuffix("/")}/records.json"
    fun notificationsUrl(base: String = activeUrl): String = "${base.removeSuffix("/")}/notifications.json"
    fun classesUrl(base: String = activeUrl): String = "${base.removeSuffix("/")}/classes.json"
    fun recoveryLogsUrl(base: String = activeUrl): String = "${base.removeSuffix("/")}/recovery_logs.json"
    fun customRanksUrl(base: String = activeUrl): String = "${base.removeSuffix("/")}/custom_ranks.json"
    fun appUpdateUrl(base: String = activeUrl): String = "${base.removeSuffix("/")}/app_update.json"

    fun userItemUrl(userId: String, base: String = activeUrl): String = "${base.removeSuffix("/")}/users/$userId.json"
    fun studentItemUrl(studentId: String, base: String = activeUrl): String = "${base.removeSuffix("/")}/students/$studentId.json"
    fun recordItemUrl(recordId: String, base: String = activeUrl): String = "${base.removeSuffix("/")}/records/$recordId.json"
    fun customRankItemUrl(rankId: String, base: String = activeUrl): String = "${base.removeSuffix("/")}/custom_ranks/$rankId.json"
}
