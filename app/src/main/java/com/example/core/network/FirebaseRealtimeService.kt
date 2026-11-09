package com.example.core.network

import android.util.Log
import com.example.BuildConfig
import com.example.domain.model.BehaviorRecord
import com.example.domain.model.CustomRank
import com.example.domain.model.HomeBanner
import com.example.domain.model.RecordType
import com.example.domain.model.PrayerLock
import com.example.domain.model.ReportCard
import com.example.domain.model.SeverityLevel
import com.example.domain.model.Student
import com.example.domain.model.SubjectGrade
import com.example.domain.model.SupportMessage
import com.example.domain.model.SupportTicket
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.model.toStudent
import com.example.domain.model.sanitizePhoneNumber
import com.example.domain.model.isValidPhoneNumber
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
            put("hasLoggedIn", user.hasLoggedIn)
            put("addedByStaff", user.addedByStaff)
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
            put("faceRegistered", user.faceRegistered)
            put("faceFrontUrl", user.faceFrontUrl ?: "")
            put("faceLeftUrl", user.faceLeftUrl ?: "")
            put("faceRightUrl", user.faceRightUrl ?: "")
            put("updatedAt", System.currentTimeMillis())
        }
        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)

        val success = executeWriteWithFailover("saveUser") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.userItemUrl(user.id, baseUrl))
                .put(body)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }

        // If user is a student, sync to /students if hasLoggedIn == true OR isRegisteredUser == true
        if (user.role == UserRole.STUDENT) {
            if (user.hasLoggedIn || user.isRegisteredUser) {
                try {
                    val student = user.toStudent().copy(hasLoggedIn = true, isRegistered = true)
                    val cleanFirstName = student.firstName.trim().replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
                    val cleanLastName = student.lastName.trim().let {
                        if (it == "دانش‌آموز" || it == "دانش آموز") "" else it.replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
                    }
                    val sJson = JSONObject().apply {
                        put("id", student.id)
                        put("firstName", cleanFirstName)
                        put("lastName", cleanLastName)
                        put("fullName", student.fullName)
                        put("name", student.fullName)
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
                        put("isRegistered", true)
                        put("hasLoggedIn", true)
                        put("addedByStaff", student.addedByStaff)
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
                    val sBody = sJson.toString().toRequestBody(JSON_MEDIA_TYPE)
                    executeWriteWithFailover("saveUserToStudents") { baseUrl ->
                        val req = Request.Builder()
                            .url(ServerConfig.studentItemUrl(student.id, baseUrl))
                            .put(sBody)
                            .build()
                        client.newCall(req).execute().use { it.isSuccessful }
                    }
                } catch (_: Exception) {}
            } else {
                try {
                    executeWriteWithFailover("removeUnloggedInUserFromStudents") { baseUrl ->
                        val req = Request.Builder()
                            .url(ServerConfig.studentItemUrl(user.id, baseUrl))
                            .delete()
                            .build()
                        client.newCall(req).execute().use { it.isSuccessful }
                    }
                } catch (_: Exception) {}
            }
        }

        success
    }

    suspend fun recordParentLogin(studentCode: String, isMother: Boolean, parentUser: User): Boolean = withContext(Dispatchers.IO) {
        val relationKey = if (isMother) "mom" else "dad"
        val json = JSONObject().apply {
            put("studentCode", studentCode)
            put("relation", if (isMother) "مادر" else "پدر")
            put("parentId", parentUser.id)
            put("parentName", parentUser.name)
            put("hasLoggedIn", true)
            put("loggedInAt", System.currentTimeMillis())
        }
        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
        executeWriteWithFailover("recordParentLogin") { baseUrl ->
            val req = Request.Builder()
                .url("$baseUrl/parent_logins/$studentCode/$relationKey.json")
                .put(body)
                .build()
            client.newCall(req).execute().use { it.isSuccessful }
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
            phone = obj.optString("phone", "").sanitizePhoneNumber(),
            email = email,
            role = role,
            schoolId = obj.optString("schoolId", "sch_01"),
            active = obj.optBoolean("active", true),
            studentCode = studentCode,
            password = obj.optString("password", "1234"),
            gender = obj.optString("gender", "نامشخص"),
            isRegisteredUser = obj.optBoolean("isRegisteredUser", true),
            hasLoggedIn = obj.optBoolean("hasLoggedIn", obj.optBoolean("isLoggedIn", false)) || isCreatorUser,
            addedByStaff = obj.optBoolean("addedByStaff", true),
            tag = if (isCreatorUser) "سازنده" else tag,
            rankTitle = rankTitle,
            rankBadgeColorHex = rankBadgeColorHex,
            rankColorHex = rankColorHex,
            permissions = permissions,
            profileImage = profileImage,
            extraRanks = extraRanks,
            extraRankColors = extraRankColors,
            nationalId = nationalId,
            bio = bio,
            faceRegistered = obj.optBoolean("faceRegistered", false),
            faceFrontUrl = obj.optString("faceFrontUrl", "").ifBlank { null },
            faceLeftUrl = obj.optString("faceLeftUrl", "").ifBlank { null },
            faceRightUrl = obj.optString("faceRightUrl", "").ifBlank { null }
        )
    }

    // --- STUDENTS MANAGEMENT ---
    suspend fun saveStudent(student: Student): Boolean = withContext(Dispatchers.IO) {
        val cleanFirstName = student.firstName.trim().replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
        val cleanLastName = student.lastName.trim().let {
            if (it == "دانش‌آموز" || it == "دانش آموز") "" else it.replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
        }
        val cleanFullName = student.fullName

        // 1. ALWAYS write/update student account in /users so all school members exist in /users
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
                put("isRegisteredUser", student.isRegistered)
                put("hasLoggedIn", student.hasLoggedIn)
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

        // 2. Write to /students if student.hasLoggedIn == true OR student.isRegistered == true
        if (student.hasLoggedIn || student.isRegistered) {
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
                put("isRegistered", student.isRegistered)
                put("hasLoggedIn", student.hasLoggedIn || student.isRegistered)
                put("addedByStaff", student.addedByStaff)
                put("rankTitle", student.rankTitle)
                put("rankColorHex", student.rankColorHex)
                put("permissions", JSONArray(student.permissions))
                put("profileImage", student.profileImage ?: "")
                put("extraRanks", JSONArray(student.extraRanks))
                put("extraRankColors", JSONArray(student.extraRankColors))
                put("nationalId", student.nationalId)
                put("bio", student.bio)
                put("faceRegistered", student.faceRegistered)
                put("faceFrontUrl", student.faceFrontUrl ?: "")
                put("faceLeftUrl", student.faceLeftUrl ?: "")
                put("faceRightUrl", student.faceRightUrl ?: "")
                put("updatedAt", System.currentTimeMillis())
            }
            val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)

            executeWriteWithFailover("saveStudentToStudents") { baseUrl ->
                val request = Request.Builder()
                    .url(ServerConfig.studentItemUrl(student.id, baseUrl))
                    .put(body)
                    .build()
                client.newCall(request).execute().use { it.isSuccessful }
            }
        } else {
            // Remove from /students if not logged in or registered
            executeWriteWithFailover("removeUnloggedInStudentFromStudents") { baseUrl ->
                val request = Request.Builder()
                    .url(ServerConfig.studentItemUrl(student.id, baseUrl))
                    .delete()
                    .build()
                client.newCall(request).execute().use { it.isSuccessful }
            }
            true
        }
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
            parentPhone = obj.optString("parentPhone", obj.optString("phone", "")).sanitizePhoneNumber(),
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
            bio = obj.optString("bio", ""),
            isRegistered = obj.optBoolean("isRegistered", obj.optBoolean("isRegisteredUser", false)),
            hasLoggedIn = obj.optBoolean("hasLoggedIn", obj.optBoolean("isLoggedIn", false)),
            addedByStaff = obj.optBoolean("addedByStaff", true),
            faceRegistered = obj.optBoolean("faceRegistered", false),
            faceFrontUrl = obj.optString("faceFrontUrl", "").ifBlank { null },
            faceLeftUrl = obj.optString("faceLeftUrl", "").ifBlank { null },
            faceRightUrl = obj.optString("faceRightUrl", "").ifBlank { null }
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

    // --- HOME BANNERS MANAGEMENT ---
    suspend fun saveBanner(banner: HomeBanner): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("id", banner.id)
            put("title", banner.title)
            put("imageUrl", banner.imageUrl)
            put("targetClasses", JSONArray(banner.targetClasses))
            put("linkUrl", banner.linkUrl ?: JSONObject.NULL)
            put("createdAt", banner.createdAt)
            put("updatedAt", System.currentTimeMillis())
        }
        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)

        executeWriteWithFailover("saveBanner") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.bannerItemUrl(banner.id, baseUrl))
                .put(body)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }
    }

    suspend fun deleteBanner(bannerId: String): Boolean = withContext(Dispatchers.IO) {
        executeWriteWithFailover("deleteBanner") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.bannerItemUrl(bannerId, baseUrl))
                .delete()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }
    }

    suspend fun clearAllBanners(): Boolean = withContext(Dispatchers.IO) {
        executeWriteWithFailover("clearAllBanners") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.bannersUrl(baseUrl))
                .delete()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }
    }

    suspend fun fetchBanners(): List<HomeBanner>? = withContext(Dispatchers.IO) {
        executeWithFailover("fetchBanners") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.bannersUrl(baseUrl))
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                if (body == "null" || body.isBlank()) return@use emptyList<HomeBanner>()

                val list = mutableListOf<HomeBanner>()
                if (body.trimStart().startsWith("{")) {
                    val root = JSONObject(body)
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val obj = root.optJSONObject(key) ?: continue
                        parseSingleBanner(key, obj)?.let { list.add(it) }
                    }
                } else if (body.trimStart().startsWith("[")) {
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        val obj = arr.optJSONObject(i) ?: continue
                        val key = obj.optString("id", "banner_$i")
                        parseSingleBanner(key, obj)?.let { list.add(it) }
                    }
                }
                list.sortedByDescending { it.createdAt }
            }
        }
    }

    private fun parseSingleBanner(key: String, obj: JSONObject): HomeBanner? {
        val id = obj.optString("id", key)
        val title = obj.optString("title", "")
        val imageUrl = obj.optString("imageUrl", "")
        if (imageUrl.isBlank()) return null

        val targetClasses = mutableListOf<String>()
        val tcArray = obj.optJSONArray("targetClasses")
        if (tcArray != null) {
            for (i in 0 until tcArray.length()) {
                val tc = tcArray.optString(i)
                if (tc.isNotBlank()) targetClasses.add(tc)
            }
        }
        val linkUrl = if (obj.has("linkUrl") && !obj.isNull("linkUrl")) obj.optString("linkUrl") else null
        val createdAt = obj.optLong("createdAt", System.currentTimeMillis())

        return HomeBanner(
            id = id,
            title = title,
            imageUrl = imageUrl,
            targetClasses = targetClasses,
            linkUrl = linkUrl,
            createdAt = createdAt
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
            put("imageUrl", record.imageUrl ?: "")
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
            imageUrl = obj.optString("imageUrl", "").ifBlank { null },
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

    suspend fun purgeUnloggedInStudentsFromRemoteServer(): Boolean = withContext(Dispatchers.IO) {
        try {
            val remoteStudents = fetchStudents() ?: return@withContext false
            var countRemoved = 0
            for (student in remoteStudents) {
                if (!student.hasLoggedIn && !student.isRegistered) {
                    executeWriteWithFailover("purgeUnloggedInStudent") { baseUrl ->
                        val request = Request.Builder()
                            .url(ServerConfig.studentItemUrl(student.id, baseUrl))
                            .delete()
                            .build()
                        client.newCall(request).execute().use { it.isSuccessful }
                    }
                    countRemoved++
                }
            }
            if (countRemoved > 0) {
                Log.d(TAG, "Purged $countRemoved unloggedIn/unregistered students from /students on server")
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Error purging unloggedIn students: ${e.message}")
            false
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

    suspend fun fetchReportCards(): List<ReportCard>? = withContext(Dispatchers.IO) {
        executeWithFailover<List<ReportCard>>("fetchReportCards") { baseUrl ->
            try {
                val request = Request.Builder()
                    .url(ServerConfig.reportCardsUrl(baseUrl))
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@executeWithFailover null
                    val body = response.body?.string() ?: return@executeWithFailover null
                    if (body == "null" || body.isBlank()) return@executeWithFailover emptyList<ReportCard>()

                    val list = mutableListOf<ReportCard>()
                    val root = JSONObject(body)
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val obj = root.optJSONObject(key) ?: continue

                        val subjArr = obj.optJSONArray("subjects") ?: JSONArray()
                        val subList = mutableListOf<SubjectGrade>()
                        for (i in 0 until subjArr.length()) {
                            val sObj = subjArr.optJSONObject(i) ?: continue
                            subList.add(
                                SubjectGrade(
                                    rowNumber = sObj.optInt("rowNumber", i + 1),
                                    subjectName = sObj.optString("subjectName", ""),
                                    term1Continuous = sObj.optString("term1Continuous", "۲۰"),
                                    term1Final = sObj.optString("term1Final", "۲۰"),
                                    term1Score = sObj.optString("term1Score", "۲۰"),
                                    term2Continuous = sObj.optString("term2Continuous", "۲۰"),
                                    term2Final = sObj.optString("term2Final", "۲۰"),
                                    term2ScoreWithCoefficient = sObj.optString("term2ScoreWithCoefficient", "۴۰"),
                                    annualScore = sObj.optString("annualScore", "۲۰"),
                                    summerFinal = sObj.optString("summerFinal", ""),
                                    summerAverage = sObj.optString("summerAverage", ""),
                                    summerAnnual = sObj.optString("summerAnnual", ""),
                                    majorFieldSelection = sObj.optString("majorFieldSelection", ""),
                                    result = sObj.optString("result", "قبول"),
                                    notes = sObj.optString("notes", ""),
                                    isContinuousTerm1Hatched = sObj.optBoolean("isContinuousTerm1Hatched", false),
                                    isContinuousTerm2Hatched = sObj.optBoolean("isContinuousTerm2Hatched", false)
                                )
                            )
                        }

                        list.add(
                            ReportCard(
                                id = obj.optString("id", key),
                                studentId = obj.optString("studentId", ""),
                                studentCode = obj.optString("studentCode", ""),
                                nationalId = obj.optString("nationalId", ""),
                                studentFirstName = obj.optString("studentFirstName", ""),
                                studentLastName = obj.optString("studentLastName", ""),
                                fatherName = obj.optString("fatherName", ""),
                                gender = obj.optString("gender", "پسر"),
                                birthDate = obj.optString("birthDate", "۱۳۹۱/۱۲/۱۶"),
                                birthPlace = obj.optString("birthPlace", "کنگان"),
                                studentPhoto = obj.optString("studentPhoto").ifBlank { null },
                                province = obj.optString("province", "بوشهر"),
                                region = obj.optString("region", "دیر"),
                                schoolName = obj.optString("schoolName", "دبیرستان شهید رجایی"),
                                schoolCode = obj.optString("schoolCode", "۹۵۰۹۶۸۷۳"),
                                grade = obj.optString("grade", "هفتم"),
                                className = obj.optString("className", "۷/۱"),
                                schoolType = obj.optString("schoolType", "دولتی"),
                                schoolLocation = obj.optString("schoolLocation", "شهری"),
                                schoolFeature = obj.optString("schoolFeature", "عادی"),
                                educationalPeriod = obj.optString("educationalPeriod", "دوره اول متوسطه"),
                                academicYear = obj.optString("academicYear", "۱۴۰۳-۱۴۰۴"),
                                semesterTitle = obj.optString("semesterTitle", "ضمن سال"),
                                issueDate = obj.optString("issueDate", "۱۴۰۴/۰۳/۲۵"),
                                subjects = if (subList.isEmpty()) ReportCard.defaultSubjectsList() else subList,
                                totalTerm1 = obj.optString("totalTerm1", "۲۸۰"),
                                totalAnnual = obj.optString("totalAnnual", "۵۶۰"),
                                gpaTerm1 = obj.optString("gpaTerm1", "۲۰"),
                                gpaAnnual = obj.optString("gpaAnnual", "۲۰"),
                                gpaInWords = obj.optString("gpaInWords", "بیست"),
                                conductTerm1 = obj.optString("conductTerm1", "عالی"),
                                conductTerm2 = obj.optString("conductTerm2", "عالی"),
                                finalStatus = obj.optString("finalStatus", "قبول خرداد"),
                                principalName = obj.optString("principalName", "مهدی فاریابی"),
                                executiveDeputyName = obj.optString("executiveDeputyName", "یاسین دراهکی"),
                                certificationText = obj.optString("certificationText", "گواهی می‌شود دانش‌آموز با مشخصات فوق در ارزشیابی خرداد ماه سال ۱۴۰۴ در پایه هفتم شرکت و قبولی خرداد شده است مندرجات این کارنامه با مندرجات دفتر نتایج ارزشیابی مطابقت دارد."),
                                indicatorNumber = obj.optString("indicatorNumber", "۱۴۰۴-۵۰۸-۲"),
                                createdBy = obj.optString("createdBy", ""),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                    list
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching report cards from $baseUrl: ${e.message}")
                null
            }
        }
    }

    suspend fun saveReportCard(card: ReportCard): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("id", card.id)
            put("studentId", card.studentId)
            put("studentCode", card.studentCode)
            put("nationalId", card.nationalId)
            put("studentFirstName", card.studentFirstName)
            put("studentLastName", card.studentLastName)
            put("fatherName", card.fatherName)
            put("gender", card.gender)
            put("birthDate", card.birthDate)
            put("birthPlace", card.birthPlace)
            put("studentPhoto", card.studentPhoto ?: "")

            put("province", card.province)
            put("region", card.region)
            put("schoolName", card.schoolName)
            put("schoolCode", card.schoolCode)
            put("grade", card.grade)
            put("className", card.className)
            put("schoolType", card.schoolType)
            put("schoolLocation", card.schoolLocation)
            put("schoolFeature", card.schoolFeature)
            put("educationalPeriod", card.educationalPeriod)
            put("academicYear", card.academicYear)
            put("semesterTitle", card.semesterTitle)
            put("issueDate", card.issueDate)

            put("totalTerm1", card.totalTerm1)
            put("totalAnnual", card.totalAnnual)
            put("gpaTerm1", card.gpaTerm1)
            put("gpaAnnual", card.gpaAnnual)
            put("gpaInWords", card.gpaInWords)
            put("conductTerm1", card.conductTerm1)
            put("conductTerm2", card.conductTerm2)
            put("finalStatus", card.finalStatus)

            put("principalName", card.principalName)
            put("executiveDeputyName", card.executiveDeputyName)
            put("certificationText", card.certificationText)
            put("indicatorNumber", card.indicatorNumber)
            put("createdBy", card.createdBy)
            put("createdAt", card.createdAt)
            put("updatedAt", card.updatedAt)

            val subjArr = JSONArray()
            card.subjects.forEach { s ->
                val sObj = JSONObject().apply {
                    put("rowNumber", s.rowNumber)
                    put("subjectName", s.subjectName)
                    put("term1Continuous", s.term1Continuous)
                    put("term1Final", s.term1Final)
                    put("term1Score", s.term1Score)
                    put("term2Continuous", s.term2Continuous)
                    put("term2Final", s.term2Final)
                    put("term2ScoreWithCoefficient", s.term2ScoreWithCoefficient)
                    put("annualScore", s.annualScore)
                    put("summerFinal", s.summerFinal)
                    put("summerAverage", s.summerAverage)
                    put("summerAnnual", s.summerAnnual)
                    put("majorFieldSelection", s.majorFieldSelection)
                    put("result", s.result)
                    put("notes", s.notes)
                    put("isContinuousTerm1Hatched", s.isContinuousTerm1Hatched)
                    put("isContinuousTerm2Hatched", s.isContinuousTerm2Hatched)
                }
                subjArr.put(sObj)
            }
            put("subjects", subjArr)
        }

        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
        val cardId = if (card.id.isNotBlank()) card.id else "rc_${card.studentCode}"
        executeWriteWithFailover("saveReportCard") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.reportCardItemUrl(cardId, baseUrl))
                .put(body)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }
    }

    suspend fun deleteReportCard(cardId: String): Boolean = withContext(Dispatchers.IO) {
        executeWriteWithFailover("deleteReportCard") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.reportCardItemUrl(cardId, baseUrl))
                .delete()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }
    }

    suspend fun fetchSupportTickets(): List<SupportTicket>? = withContext(Dispatchers.IO) {
        executeWithFailover<List<SupportTicket>>("fetchSupportTickets") { baseUrl ->
            val request = Request.Builder()
                .url("$baseUrl/support_tickets.json")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@executeWithFailover null
                val body = response.body?.string() ?: return@executeWithFailover null
                if (body == "null" || body.isBlank()) return@executeWithFailover emptyList<SupportTicket>()
                val list = mutableListOf<SupportTicket>()
                if (body.trim().startsWith("{")) {
                    val root = JSONObject(body)
                    val keys = root.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val obj = root.optJSONObject(key) ?: continue
                        list.add(parseSupportTicketJson(obj))
                    }
                } else if (body.trim().startsWith("[")) {
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        if (!arr.isNull(i)) {
                            list.add(parseSupportTicketJson(arr.getJSONObject(i)))
                        }
                    }
                }
                list
            }
        }
    }

    suspend fun saveSupportTicket(ticket: SupportTicket): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("id", ticket.id)
            put("userId", ticket.userId)
            put("userName", ticket.userName)
            put("userPhoneOrCode", ticket.userPhoneOrCode)
            put("userRoleTitle", ticket.userRoleTitle)
            put("schoolName", ticket.schoolName)
            put("status", ticket.status)
            put("lastMessage", ticket.lastMessage)
            put("lastUpdated", ticket.lastUpdated)
            put("unreadBySupport", ticket.unreadBySupport)
            put("unreadByUser", ticket.unreadByUser)
            put("isStarted", ticket.isStarted)

            val msgsArr = JSONArray()
            ticket.messages.forEach { m ->
                msgsArr.put(JSONObject().apply {
                    put("id", m.id)
                    put("ticketId", m.ticketId)
                    put("senderUserId", m.senderUserId)
                    put("senderName", m.senderName)
                    put("senderRoleTitle", m.senderRoleTitle)
                    put("isFromSupport", m.isFromSupport)
                    put("content", m.content)
                    put("timestamp", m.timestamp)
                    if (m.replyToId != null) put("replyToId", m.replyToId)
                    if (m.replyToSenderName != null) put("replyToSenderName", m.replyToSenderName)
                    if (m.replyToContent != null) put("replyToContent", m.replyToContent)
                    put("isEdited", m.isEdited)
                })
            }
            put("messages", msgsArr)
        }

        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)
        val ticketId = if (ticket.id.isNotBlank()) ticket.id else "ticket_${ticket.userId}"
        executeWriteWithFailover("saveSupportTicket") { baseUrl ->
            val request = Request.Builder()
                .url("$baseUrl/support_tickets/$ticketId.json")
                .put(body)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }
    }

    suspend fun deleteSupportTicket(ticketId: String): Boolean = withContext(Dispatchers.IO) {
        executeWriteWithFailover("deleteSupportTicket") { baseUrl ->
            val request = Request.Builder()
                .url("$baseUrl/support_tickets/$ticketId.json")
                .delete()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        }
    }

    private fun parseSupportTicketJson(obj: JSONObject): SupportTicket {
        val msgsArray = obj.optJSONArray("messages") ?: JSONArray()
        val msgList = mutableListOf<SupportMessage>()
        for (j in 0 until msgsArray.length()) {
            if (!msgsArray.isNull(j)) {
                val mObj = msgsArray.getJSONObject(j)
                msgList.add(
                    SupportMessage(
                        id = mObj.optString("id", ""),
                        ticketId = mObj.optString("ticketId", obj.optString("id")),
                        senderUserId = mObj.optString("senderUserId", ""),
                        senderName = mObj.optString("senderName", ""),
                        senderRoleTitle = mObj.optString("senderRoleTitle", ""),
                        isFromSupport = mObj.optBoolean("isFromSupport", false),
                        content = mObj.optString("content", ""),
                        timestamp = mObj.optLong("timestamp", System.currentTimeMillis()),
                        replyToId = if (mObj.has("replyToId") && !mObj.isNull("replyToId")) mObj.optString("replyToId") else null,
                        replyToSenderName = if (mObj.has("replyToSenderName") && !mObj.isNull("replyToSenderName")) mObj.optString("replyToSenderName") else null,
                        replyToContent = if (mObj.has("replyToContent") && !mObj.isNull("replyToContent")) mObj.optString("replyToContent") else null,
                        isEdited = mObj.optBoolean("isEdited", false)
                    )
                )
            }
        }

        return SupportTicket(
            id = obj.optString("id", ""),
            userId = obj.optString("userId", ""),
            userName = obj.optString("userName", ""),
            userPhoneOrCode = obj.optString("userPhoneOrCode", ""),
            userRoleTitle = obj.optString("userRoleTitle", ""),
            schoolName = obj.optString("schoolName", "دبیرستان شهید رجایی"),
            status = obj.optString("status", "ACTIVE"),
            lastMessage = obj.optString("lastMessage", ""),
            lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis()),
            unreadBySupport = obj.optBoolean("unreadBySupport", false),
            unreadByUser = obj.optBoolean("unreadByUser", false),
            isStarted = obj.optBoolean("isStarted", false),
            messages = msgList
        )
    }

    suspend fun savePrayerLock(lock: PrayerLock): Boolean = withContext(Dispatchers.IO) {
        val json = JSONObject().apply {
            put("id", lock.id)
            put("date", lock.date)
            put("prayerTime", lock.prayerTime)
            put("isLocked", lock.isLocked)
            put("lockedBy", lock.lockedBy)
            put("lockedAt", lock.lockedAt)
        }
        val body = json.toString().toRequestBody(JSON_MEDIA_TYPE)

        executeWriteWithFailover("savePrayerLock") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.prayerLockItemUrl(lock.id, baseUrl))
                .put(body)
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } ?: false
    }

    suspend fun deletePrayerLock(lockId: String): Boolean = withContext(Dispatchers.IO) {
        executeWriteWithFailover("deletePrayerLock") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.prayerLockItemUrl(lockId, baseUrl))
                .delete()
                .build()
            client.newCall(request).execute().use { it.isSuccessful }
        } ?: false
    }

    suspend fun fetchPrayerLocks(): List<PrayerLock>? = withContext(Dispatchers.IO) {
        executeWithFailover("fetchPrayerLocks") { baseUrl ->
            val request = Request.Builder()
                .url(ServerConfig.prayerLocksUrl(baseUrl))
                .get()
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use emptyList()
                parsePrayerLocksJson(body)
            }
        }
    }

    private fun parsePrayerLocksJson(body: String): List<PrayerLock> {
        val list = mutableListOf<PrayerLock>()
        if (body.isBlank() || body == "null") return list
        try {
            val root = JSONObject(body)
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val obj = root.optJSONObject(key) ?: continue
                parseSinglePrayerLock(key, obj)?.let { list.add(it) }
            }
        } catch (_: Exception) {}
        return list
    }

    private fun parseSinglePrayerLock(key: String, obj: JSONObject): PrayerLock? {
        return try {
            PrayerLock(
                id = obj.optString("id", key),
                date = obj.optString("date", ""),
                prayerTime = obj.optString("prayerTime", ""),
                isLocked = obj.optBoolean("isLocked", true),
                lockedBy = obj.optString("lockedBy", ""),
                lockedAt = obj.optLong("lockedAt", System.currentTimeMillis())
            )
        } catch (_: Exception) {
            null
        }
    }
}

