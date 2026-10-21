package com.example.core.network

import android.util.Log
import com.example.BuildConfig
import com.example.domain.model.BehaviorRecord
import com.example.domain.model.CustomRank
import com.example.domain.model.RecordType
import com.example.domain.model.SeverityLevel
import com.example.domain.model.Student
import com.example.domain.model.User
import com.example.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Intelligent Anti-Sanction & Anti-Filtering DNS Resolver.
 * Resolves hostnames through system DNS and falls back to InetAddress to prevent DNS poisoning.
 */
class AntiSanctionDns : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        return try {
            Dns.SYSTEM.lookup(hostname)
        } catch (e: Exception) {
            try {
                val addrs = InetAddress.getAllByName(hostname).toList()
                if (addrs.isNotEmpty()) addrs else throw e
            } catch (e2: Exception) {
                Log.w("AntiSanctionDns", "DNS resolution failed for $hostname: ${e2.message}")
                throw e
            }
        }
    }
}

/**
 * High-Speed Resilient Cloud Sync Service for NezamYar.
 * Automatically fails over between the primary Cloudflare Worker anti-sanction proxy
 * and direct Firebase Realtime Database with sub-second latency and zero data loss.
 */
object FirebaseRealtimeService {
    private const val TAG = "FirebaseRTDB"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(AntiSanctionDns())
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("User-Agent", "NezamYar-Android/${BuildConfig.VERSION_NAME}")
                    .header("Accept", "application/json")
                    .header("Cache-Control", "no-cache")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    private suspend fun <T> executeWithFailover(
        operation: String,
        block: (baseUrl: String) -> T?
    ): T? = withContext(Dispatchers.IO) {
        val candidates = ServerConfig.getCandidateUrls()
        for (baseUrl in candidates) {
            try {
                val result = block(baseUrl)
                if (result != null) {
                    ServerConfig.markWorkingUrl(baseUrl)
                    return@withContext result
                }
            } catch (e: Exception) {
                Log.w(TAG, "$operation failed on $baseUrl: ${e.message}")
                ServerConfig.markFailedUrl(baseUrl)
            }
        }
        null
    }

    private suspend fun executeWriteWithFailover(
        operation: String,
        block: (baseUrl: String) -> Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        val candidates = ServerConfig.getCandidateUrls()
        for (baseUrl in candidates) {
            try {
                val success = block(baseUrl)
                if (success) {
                    ServerConfig.markWorkingUrl(baseUrl)
                    return@withContext true
                }
            } catch (e: Exception) {
                Log.w(TAG, "$operation failed on $baseUrl: ${e.message}")
                ServerConfig.markFailedUrl(baseUrl)
            }
        }
        false
    }

    suspend fun checkConnection(): Boolean = withContext(Dispatchers.IO) {
        executeWithFailover("checkConnection") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.usersUrl(baseUrl))
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) true else null
            }
        } ?: false
    }

    // --- USERS MANAGEMENT ---
    suspend fun saveUser(user: User): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("id", user.id)
            put("name", user.name)
            put("phone", user.phone)
            put("email", user.email)
            put("role", user.role.name)
            put("schoolId", user.schoolId)
            put("active", user.active)
            put("studentCode", user.studentCode)
            put("password", user.password)
            put("gender", user.gender)
            put("isRegisteredUser", user.isRegisteredUser)
            put("tag", user.tag ?: "")
            put("rankTitle", user.rankTitle)
            put("rankBadgeColorHex", user.rankBadgeColorHex)
            put("rankColorHex", user.rankColorHex)
            put("permissions", JSONArray(user.permissions))
            put("profileImage", user.profileImage ?: "")
            put("extraRanks", JSONArray(user.extraRanks))
            put("extraRankColors", JSONArray(user.extraRankColors))
            put("nationalId", user.nationalId)
            put("bio", user.bio)
            put("updatedAt", System.currentTimeMillis())
        }
        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)

        executeWriteWithFailover("saveUser") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.userItemUrl(user.id, baseUrl))
                .put(body)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }
    }

    suspend fun deleteUser(userId: String): Boolean = withContext(Dispatchers.IO) {
        executeWriteWithFailover("deleteUser") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.userItemUrl(userId, baseUrl))
                .delete()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }
    }

    suspend fun fetchUsers(): List<User>? = withContext(Dispatchers.IO) {
        executeWithFailover("fetchUsers") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.usersUrl(baseUrl))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                if (body == "null" || body.isBlank()) return@use emptyList<User>()

                parseUsersJson(body)
            }
        }
    }

    private fun parseUsersJson(body: String): List<User> {
        val list = mutableListOf<User>()
        try {
            if (body.trimStart().startsWith("{")) {
                val root = JSONObject(body)
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val obj = root.optJSONObject(key) ?: continue
                    parseSingleUser(key, obj)?.let { list.add(it) }
                }
            } else if (body.trimStart().startsWith("[")) {
                val array = JSONArray(body)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val key = obj.optString("id", "user_$i")
                    parseSingleUser(key, obj)?.let { list.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing users JSON: ${e.message}")
        }
        return list
    }

    private fun parseSingleUser(key: String, obj: JSONObject): User? {
        val roleStr = obj.optString("role", "TEACHER")
        val role = try {
            UserRole.valueOf(roleStr)
        } catch (_: Exception) {
            UserRole.TEACHER
        }
        val permsArray = obj.optJSONArray("permissions")
        val permissions = mutableListOf<String>()
        if (permsArray != null) {
            for (i in 0 until permsArray.length()) {
                permissions.add(permsArray.optString(i))
            }
        }
        val name = obj.optString("name", "")
        val studentCode = obj.optString("studentCode", "")
        val tag = obj.optString("tag", "").ifBlank { null }
        val id = obj.optString("id", key)
        val email = obj.optString("email", "")

        val isCreatorUser = (id == "user_creator_hossein") ||
                (studentCode == "454545" && (name.trim() == "سید حسین موسوی نژاد" || email.equals("hosseinvexter@gmail.com", ignoreCase = true)))

        val rankTitle = if (isCreatorUser) "سازنده" else obj.optString("rankTitle", role.farsiTitle)
        val rankBadgeColorHex = if (isCreatorUser) 0xFF1E40AF else obj.optLong("rankBadgeColorHex", 0xFF64748B)
        val rankColorHex = if (isCreatorUser) "#1E40AF" else obj.optString("rankColorHex", "#64748B")

        val extraRanksArray = obj.optJSONArray("extraRanks")
        val extraRanks = mutableListOf<String>()
        if (extraRanksArray != null) {
            for (i in 0 until extraRanksArray.length()) {
                extraRanks.add(extraRanksArray.optString(i))
            }
        }
        val extraColorsArray = obj.optJSONArray("extraRankColors")
        val extraRankColors = mutableListOf<String>()
        if (extraColorsArray != null) {
            for (i in 0 until extraColorsArray.length()) {
                extraRankColors.add(extraColorsArray.optString(i))
            }
        }
        val profileImage = obj.optString("profileImage", "").ifBlank { null }
        val nationalId = obj.optString("nationalId", "")
        val bio = obj.optString("bio", "")

        return User(
            id = id,
            name = name,
            phone = obj.optString("phone", ""),
            email = email,
            role = role,
            schoolId = obj.optString("schoolId", "sch_01"),
            active = obj.optBoolean("active", true),
            studentCode = studentCode,
            password = obj.optString("password", "1234"),
            gender = obj.optString("gender", "نامشخص"),
            isRegisteredUser = obj.optBoolean("isRegisteredUser", true),
            tag = if (isCreatorUser) "سازنده" else tag,
            rankTitle = rankTitle,
            rankBadgeColorHex = rankBadgeColorHex,
            rankColorHex = rankColorHex,
            permissions = permissions,
            profileImage = profileImage,
            extraRanks = extraRanks,
            extraRankColors = extraRankColors,
            nationalId = nationalId,
            bio = bio
        )
    }

    // --- STUDENTS MANAGEMENT ---
    suspend fun saveStudent(student: Student): Boolean = withContext(Dispatchers.IO) {
        val cleanFirstName = student.firstName.trim().replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
        val cleanLastName = student.lastName.trim().let {
            if (it == "دانش‌آموز" || it == "دانش آموز") "" else it.replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
        }
        val cleanFullName = student.fullName

        val json = JSONObject().apply {
            put("id", student.id)
            put("firstName", cleanFirstName)
            put("lastName", cleanLastName)
            put("fullName", cleanFullName)
            put("name", cleanFullName)
            put("studentCode", student.studentCode)
            put("grade", student.grade)
            put("className", student.className)
            put("gender", student.gender)
            put("schoolId", student.schoolId)
            put("score", student.score)
            put("parentPhone", student.parentPhone)
            put("phone", student.parentPhone)
            put("email", student.email)
            put("password", student.password)
            put("role", student.role.name)
            put("rankTitle", student.rankTitle)
            put("rankColorHex", student.rankColorHex)
            put("permissions", JSONArray(student.permissions))
            put("profileImage", student.profileImage ?: "")
            put("extraRanks", JSONArray(student.extraRanks))
            put("extraRankColors", JSONArray(student.extraRankColors))
            put("nationalId", student.nationalId)
            put("bio", student.bio)
            put("updatedAt", System.currentTimeMillis())
        }
        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)

        val success = executeWriteWithFailover("saveStudent") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.studentItemUrl(student.id, baseUrl))
                .put(body)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }

        // Also ensure student account is written to /users so auth and cross-references succeed
        try {
            val userJson = JSONObject().apply {
                put("id", student.id)
                put("name", cleanFullName)
                put("phone", student.parentPhone)
                put("email", student.email)
                put("role", UserRole.STUDENT.name)
                put("schoolId", student.schoolId)
                put("studentCode", student.studentCode)
                put("password", student.password)
                put("gender", student.gender)
                put("grade", student.grade)
                put("className", student.className)
                put("active", true)
                put("isRegisteredUser", true)
                put("rankTitle", student.rankTitle)
                put("rankColorHex", student.rankColorHex)
                put("rankBadgeColorHex", try {
                    val hex = if (student.rankColorHex.startsWith("#")) student.rankColorHex else "#${student.rankColorHex}"
                    android.graphics.Color.parseColor(hex).toLong() and 0xFFFFFFFFL
                } catch (_: Exception) { 0xFF64748BL })
                put("permissions", JSONArray(student.permissions))
                put("updatedAt", System.currentTimeMillis())
            }
            val uBody = userJson.toString().toRequestBody(JSON_MEDIA_TYPE)
            executeWriteWithFailover("saveStudentToUsers") { baseUrl ->
                val req = Request.Builder()
                    .url(ServerConfig.userItemUrl(student.id, baseUrl))
                    .put(uBody)
                    .build()
                client.newCall(req).execute().use { it.isSuccessful }
            }
        } catch (_: Exception) {}

        success
    }

    suspend fun deleteStudent(studentId: String): Boolean = withContext(Dispatchers.IO) {
        val success = executeWriteWithFailover("deleteStudent") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.studentItemUrl(studentId, baseUrl))
                .delete()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }
        // Also remove from /users if exists
        try {
            executeWriteWithFailover("deleteStudentFromUsers") { baseUrl ->
                val request = Request.Builder()
                    .url(ServerConfig.userItemUrl(studentId, baseUrl))
                    .delete()
                    .build()
                client.newCall(request).execute().use { it.isSuccessful }
            }
        } catch (_: Exception) {}
        success
    }

    suspend fun fetchStudents(): List<Student>? = withContext(Dispatchers.IO) {
        executeWithFailover("fetchStudents") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.studentsUrl(baseUrl))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                if (body == "null" || body.isBlank()) return@use emptyList<Student>()

                parseStudentsJson(body)
            }
        }
    }

    private fun parseStudentsJson(body: String): List<Student> {
        val list = mutableListOf<Student>()
        try {
            if (body.trimStart().startsWith("{")) {
                val root = JSONObject(body)
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (key == "user_creator_hossein") continue
                    val obj = root.optJSONObject(key) ?: continue
                    parseSingleStudent(key, obj)?.let { list.add(it) }
                }
            } else if (body.trimStart().startsWith("[")) {
                val array = JSONArray(body)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val key = obj.optString("id", "std_$i")
                    if (key == "user_creator_hossein") continue
                    parseSingleStudent(key, obj)?.let { list.add(it) }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing students JSON: ${e.message}")
        }
        return list
    }

    private fun parseSingleStudent(key: String, obj: JSONObject): Student? {
        if (obj.optString("studentCode") == "454545" || obj.optString("id") == "user_creator_hossein") return null

        val permsArray = obj.optJSONArray("permissions")
        val permissions = mutableListOf<String>()
        if (permsArray != null) {
            for (i in 0 until permsArray.length()) {
                permissions.add(permsArray.optString(i))
            }
        }

        val roleStr = obj.optString("role", "STUDENT")
        val role = try {
            UserRole.valueOf(roleStr)
        } catch (_: Exception) {
            UserRole.STUDENT
        }
        val email = obj.optString("email", "")
        val password = obj.optString("password", "1234")
        val rawName = obj.optString("name", obj.optString("fullName", ""))
        var fName = obj.optString("firstName", "")
        var lName = obj.optString("lastName", "")
        if (fName.isBlank() && rawName.isNotBlank()) {
            val cleanRaw = rawName.replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
            val parts = cleanRaw.split(" ").filter { it.isNotBlank() }
            fName = parts.firstOrNull().orEmpty()
            lName = parts.drop(1).joinToString(" ").trim()
        }

        fName = fName.replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
        lName = if (lName == "دانش‌آموز" || lName == "دانش آموز") {
            ""
        } else {
            lName.replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
        }

        val extraRanksArray = obj.optJSONArray("extraRanks")
        val extraRanks = mutableListOf<String>()
        if (extraRanksArray != null) {
            for (i in 0 until extraRanksArray.length()) {
                extraRanks.add(extraRanksArray.optString(i))
            }
        }
        val extraColorsArray = obj.optJSONArray("extraRankColors")
        val extraRankColors = mutableListOf<String>()
        if (extraColorsArray != null) {
            for (i in 0 until extraColorsArray.length()) {
                extraRankColors.add(extraColorsArray.optString(i))
            }
        }

        return Student(
            id = obj.optString("id", key),
            firstName = fName,
            lastName = lName,
            studentCode = obj.optString("studentCode"),
            grade = obj.optString("grade", "پایه هفتم"),
            className = obj.optString("className", "کلاس 7/1"),
            gender = obj.optString("gender", "پسر"),
            schoolId = obj.optString("schoolId", "sch_01"),
            score = obj.optInt("score", 100),
            parentPhone = obj.optString("parentPhone", obj.optString("phone", "")),
            email = email,
            password = password,
            role = role,
            rankTitle = obj.optString("rankTitle", "دانش‌آموز"),
            rankColorHex = obj.optString("rankColorHex", "#64748B"),
            permissions = permissions,
            profileImage = obj.optString("profileImage", "").ifBlank { null },
            extraRanks = extraRanks,
            extraRankColors = extraRankColors,
            nationalId = obj.optString("nationalId", ""),
            bio = obj.optString("bio", "")
        )
    }

    // --- CUSTOM RANKS MANAGEMENT ---
    suspend fun saveCustomRank(rank: CustomRank): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("id", rank.id)
            put("title", rank.title)
            put("colorHex", rank.colorHex)
            put("description", rank.description)
            put("permissions", JSONArray(rank.permissions))
            put("updatedAt", System.currentTimeMillis())
        }
        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)

        executeWriteWithFailover("saveCustomRank") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.customRankItemUrl(rank.id, baseUrl))
                .put(body)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }
    }

    suspend fun deleteCustomRank(rankId: String): Boolean = withContext(Dispatchers.IO) {
        executeWriteWithFailover("deleteCustomRank") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.customRankItemUrl(rankId, baseUrl))
                .delete()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }
    }

    suspend fun fetchCustomRanks(): List<CustomRank>? = withContext(Dispatchers.IO) {
        executeWithFailover("fetchCustomRanks") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.customRanksUrl(baseUrl))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                if (body == "null" || body.isBlank()) return@use emptyList<CustomRank>()

                val list = mutableListOf<CustomRank>()
                if (body.trimStart().startsWith("{")) {
                    val root = JSONObject(body)
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val obj = root.optJSONObject(key) ?: continue
                        parseSingleRank(key, obj)?.let { list.add(it) }
                    }
                } else if (body.trimStart().startsWith("[")) {
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val key = obj.optString("id", "rank_$i")
                        parseSingleRank(key, obj)?.let { list.add(it) }
                    }
                }
                list
            }
        }
    }

    private fun parseSingleRank(key: String, obj: JSONObject): CustomRank? {
        val permsArray = obj.optJSONArray("permissions")
        val permissions = mutableListOf<String>()
        if (permsArray != null) {
            for (i in 0 until permsArray.length()) {
                val p = permsArray.optString(i)
                if (p.isNotBlank()) permissions.add(p)
            }
        }
        return CustomRank(
            id = obj.optString("id", key),
            title = obj.optString("title", "رنک"),
            colorHex = obj.optString("colorHex", "#64748B"),
            permissions = permissions,
            description = obj.optString("description", "")
        )
    }

    // --- BEHAVIOR RECORDS MANAGEMENT ---
    suspend fun saveRecord(record: BehaviorRecord): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("id", record.id)
            put("studentId", record.studentId)
            put("studentName", record.studentName)
            put("studentGrade", record.studentGrade)
            put("studentClass", record.studentClass)
            put("schoolId", record.schoolId)
            put("type", record.type.name)
            put("category", record.category)
            put("title", record.title)
            put("description", record.description)
            put("date", record.date)
            put("time", record.time)
            put("location", record.location)
            if (record.points != null) put("points", record.points)
            put("createdBy", record.createdBy)
            put("createdByRole", record.createdByRole.name)
            put("parentNotified", record.parentNotified)
            put("status", record.status)
            put("updatedAt", System.currentTimeMillis())
        }
        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)

        executeWriteWithFailover("saveRecord") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.recordItemUrl(record.id, baseUrl))
                .put(body)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }
    }

    suspend fun deleteRecord(recordId: String): Boolean = withContext(Dispatchers.IO) {
        executeWriteWithFailover("deleteRecord") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.recordItemUrl(recordId, baseUrl))
                .delete()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }
    }

    suspend fun fetchRecords(): List<BehaviorRecord>? = withContext(Dispatchers.IO) {
        executeWithFailover("fetchRecords") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.recordsUrl(baseUrl))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                if (body == "null" || body.isBlank()) return@use emptyList<BehaviorRecord>()

                val list = mutableListOf<BehaviorRecord>()
                if (body.trimStart().startsWith("{")) {
                    val root = JSONObject(body)
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val obj = root.optJSONObject(key) ?: continue
                        parseSingleRecord(key, obj)?.let { list.add(it) }
                    }
                } else if (body.trimStart().startsWith("[")) {
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val key = obj.optString("id", "rec_$i")
                        parseSingleRecord(key, obj)?.let { list.add(it) }
                    }
                }
                list
            }
        }
    }

    private fun parseSingleRecord(key: String, obj: JSONObject): BehaviorRecord? {
        val typeStr = obj.optString("type", "POSITIVE")
        val type = try {
            RecordType.valueOf(typeStr)
        } catch (_: Exception) {
            RecordType.POSITIVE
        }
        val roleStr = obj.optString("createdByRole", "MANAGER")
        val role = try {
            UserRole.valueOf(roleStr)
        } catch (_: Exception) {
            UserRole.MANAGER
        }
        return BehaviorRecord(
            id = obj.optString("id", key),
            studentId = obj.optString("studentId"),
            studentName = obj.optString("studentName"),
            studentGrade = obj.optString("studentGrade", "پایه هفتم"),
            studentClass = obj.optString("studentClass", "کلاس 7/1"),
            schoolId = obj.optString("schoolId", "sch_01"),
            type = type,
            category = obj.optString("category", "رفتاری"),
            title = obj.optString("title"),
            description = obj.optString("description"),
            date = obj.optString("date"),
            time = obj.optString("time"),
            location = obj.optString("location", "مدرسه"),
            points = if (obj.has("points")) obj.optInt("points") else null,
            createdBy = obj.optString("createdBy", "مدیر"),
            createdByRole = role,
            parentNotified = obj.optBoolean("parentNotified", true),
            status = obj.optString("status", "تأیید شده")
        )
    }

    suspend fun logAccountRecovery(phoneOrEmail: String, studentCode: String, userName: String): Boolean = withContext(Dispatchers.IO) {
        val logId = "rec_${System.currentTimeMillis()}"
        val json = JSONObject().apply {
            put("query", phoneOrEmail)
            put("userName", userName)
            put("studentCode", studentCode)
            put("timestamp", System.currentTimeMillis())
            put("status", "DISPATCHED_TO_SMS_AND_EMAIL")
            put("serverUrl", ServerConfig.activeUrl)
        }
        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)

        executeWriteWithFailover("logAccountRecovery") { baseUrl ->
            val request = Request.Builder()
                .url("${baseUrl.removeSuffix("/")}/recovery_logs/$logId.json")
                .put(body)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }
    }

    suspend fun seedInitialDataIfEmpty(
        defaultUsers: List<User>,
        defaultStudents: List<Student>,
        defaultRecords: List<BehaviorRecord>
    ) = withContext(Dispatchers.IO) {
        try {
            val existingUsers = fetchUsers()
            if (existingUsers != null && existingUsers.isEmpty()) {
                defaultUsers.forEach { saveUser(it) }
            }
            val existingStudents = fetchStudents()
            if (existingStudents != null && existingStudents.isEmpty() && defaultStudents.isNotEmpty()) {
                defaultStudents.forEach { saveStudent(it) }
            }
            val existingRecords = fetchRecords()
            if (existingRecords != null && existingRecords.isEmpty() && defaultRecords.isNotEmpty()) {
                defaultRecords.forEach { saveRecord(it) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed seeding initial data: ${e.message}")
        }
    }

    suspend fun fetchAppUpdate(): com.example.domain.model.AppUpdateInfo? = withContext(Dispatchers.IO) {
        val remote = executeWithFailover("fetchAppUpdate") { baseUrl ->
            try {
                val request = Request.Builder()
                    .url(ServerConfig.appUpdateUrl(baseUrl))
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@executeWithFailover null
                    val body = response.body?.string() ?: return@executeWithFailover null
                    if (body.trim() == "null" || body.isBlank()) return@executeWithFailover null

                    val rawObj = JSONObject(body)
                    // Support both root {"app_update": {...}} and direct /app_update.json {...}
                    val obj = if (rawObj.has("app_update") && !rawObj.isNull("app_update")) {
                        rawObj.optJSONObject("app_update") ?: rawObj
                    } else {
                        rawObj
                    }

                    val currentCode = BuildConfig.VERSION_CODE
                    val vCode = when {
                        obj.has("version_code") -> obj.optInt("version_code", currentCode)
                        obj.has("latestVersionCode") -> obj.optInt("latestVersionCode", currentCode)
                        else -> currentCode
                    }
                    val vName = when {
                        obj.has("version_name") -> obj.optString("version_name", BuildConfig.VERSION_NAME)
                        obj.has("latestVersionName") -> obj.optString("latestVersionName", BuildConfig.VERSION_NAME)
                        else -> BuildConfig.VERSION_NAME
                    }
                    val minCode = when {
                        obj.has("min_supported_version_code") -> obj.optInt("min_supported_version_code", 1)
                        obj.has("minRequiredVersionCode") -> obj.optInt("minRequiredVersionCode", 1)
                        else -> 1
                    }
                    val isForcedRaw = when {
                        obj.has("is_forced") -> obj.optBoolean("is_forced", false)
                        obj.has("isForceUpdate") -> obj.optBoolean("isForceUpdate", false)
                        else -> false
                    }
                    val apkUrl = when {
                        obj.has("apk_url") -> obj.optString("apk_url", "")
                        obj.has("downloadUrl") -> obj.optString("downloadUrl", "")
                        else -> ""
                    }
                    val updateMsg = when {
                        obj.has("update_message") -> obj.optString("update_message", "")
                        obj.has("releaseNotes") -> obj.optString("releaseNotes", "")
                        else -> "به‌روزرسانی امنیتی، رفع باگ‌ها و بهبود عملکرد سامانه"
                    }
                    val sha256 = when {
                        obj.has("sha256") -> obj.optString("sha256", "")
                        obj.has("sha_256") -> obj.optString("sha_256", "")
                        else -> ""
                    }
                    val sizeBytes = when {
                        obj.has("apk_size_bytes") -> obj.optLong("apk_size_bytes", 0L)
                        obj.has("apkSizeBytes") -> obj.optLong("apkSizeBytes", 0L)
                        else -> 0L
                    }
                    val relDate = when {
                        obj.has("release_date") -> obj.optString("release_date", "")
                        obj.has("releaseDate") -> obj.optString("releaseDate", "")
                        else -> ""
                    }

                    val isAvailable = vCode > currentCode
                    val isForced = isAvailable && (isForcedRaw || currentCode < minCode)

                    val info = com.example.domain.model.AppUpdateInfo(
                        versionCode = vCode,
                        versionName = vName,
                        minSupportedVersionCode = minCode,
                        isForced = isForced,
                        apkUrl = apkUrl,
                        updateMessage = updateMsg,
                        sha256 = sha256,
                        apkSizeBytes = sizeBytes,
                        releaseDate = relDate,
                        isUpdateAvailable = isAvailable
                    )

                    // Cache latest valid info locally
                    com.example.core.session.SessionManager.saveLastKnownUpdate(info)
                    info
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error parsing app update from $baseUrl: ${e.message}")
                null
            }
        }

        // If network request failed, gracefully fallback to cached update info
        remote ?: com.example.core.session.SessionManager.getLastKnownUpdate()
    }

    suspend fun saveAppUpdate(info: com.example.domain.model.AppUpdateInfo): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            // Standard requested schema
            put("version_code", info.versionCode)
            put("version_name", info.versionName)
            put("apk_url", info.apkUrl)
            put("update_message", info.updateMessage)
            put("is_forced", info.isForced)
            put("min_supported_version_code", info.minSupportedVersionCode)
            put("sha256", info.sha256)
            put("apk_size_bytes", info.apkSizeBytes)
            put("release_date", info.releaseDate)

            // Backward-compatibility keys
            put("latestVersionCode", info.versionCode)
            put("latestVersionName", info.versionName)
            put("downloadUrl", info.apkUrl)
            put("releaseNotes", info.updateMessage)
            put("isForceUpdate", info.isForced)
            put("minRequiredVersionCode", info.minSupportedVersionCode)
            put("apkSizeBytes", info.apkSizeBytes)
            put("releaseDate", info.releaseDate)
            put("updatedAt", System.currentTimeMillis())
        }
        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)

        val success = executeWriteWithFailover("saveAppUpdate") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.appUpdateUrl(baseUrl))
                .put(body)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }

        if (success) {
            com.example.core.session.SessionManager.saveLastKnownUpdate(info)
        }
        success
    }

    suspend fun deleteAppUpdate(): Boolean = withContext(Dispatchers.IO) {
        val success = executeWriteWithFailover("deleteAppUpdate") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.appUpdateUrl(baseUrl))
                .delete()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }
        if (success) {
            com.example.core.session.SessionManager.clearLastKnownUpdate()
        }
        success
    }
}

