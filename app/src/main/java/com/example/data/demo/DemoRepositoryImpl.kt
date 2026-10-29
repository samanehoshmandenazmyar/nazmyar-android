package com.example.data.demo

import com.example.core.network.FirebaseRealtimeService
import com.example.core.network.ServerConfig
import com.example.core.session.SessionManager
import com.example.core.utils.DateTimeUtils
import com.example.domain.model.AppNotification
import com.example.domain.model.AppUpdateInfo
import com.example.domain.model.BehaviorRecord
import com.example.domain.model.BehaviorStats
import com.example.domain.model.CustomRank
import com.example.domain.model.RankPermission
import com.example.domain.model.RecordType
import com.example.domain.model.SchoolClass
import com.example.domain.model.Student
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.model.toStudent
import com.example.domain.model.toUser
import com.example.domain.model.sanitizePhoneNumber
import com.example.domain.model.isValidPhoneNumber
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.BehaviorRepository
import com.example.domain.repository.ClassRepository
import com.example.domain.repository.NotificationRepository
import com.example.domain.repository.StudentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class DemoRepositoryImpl :
    AuthRepository,
    StudentRepository,
    BehaviorRepository,
    ClassRepository,
    NotificationRepository {

    private val repoScope = CoroutineScope(Dispatchers.IO)

    // Auth State - restored from SessionManager if available
    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isServerConnected = MutableStateFlow(true)
    override val isServerConnected: StateFlow<Boolean> = _isServerConnected.asStateFlow()

    private val _appUpdateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    override val appUpdateInfo: StateFlow<AppUpdateInfo?> = _appUpdateInfo.asStateFlow()

    override val serverUrl: String get() = ServerConfig.activeUrl

    fun restoreSavedSession() {
        val saved = SessionManager.getSavedUser()
        if (saved != null) {
            val isCreator = saved.id == "user_creator_hossein" ||
                (saved.studentCode == "454545" && saved.name.trim() == "سید حسین موسوی نژاد") ||
                saved.isCreator ||
                saved.name.trim() == "سید حسین موسوی نژاد"
            val finalSaved = if (isCreator) {
                saved.copy(
                    phone = "09923072572",
                    rankTitle = "سازنده",
                    rankBadgeColorHex = 0xFF1E40AF,
                    rankColorHex = "#1E40AF"
                )
            } else saved
            _currentUser.value = finalSaved
            if (isCreator && (saved.phone != "09923072572" || saved.rankTitle != "سازنده")) {
                SessionManager.saveUser(finalSaved)
            }
        }
    }

    init {
        restoreSavedSession()

        // Initial sync with Firebase Realtime Database
        repoScope.launch {
            syncWithServer()
            try {
                FirebaseRealtimeService.purgeUnloggedInStudentsFromRemoteServer()
            } catch (_: Throwable) {}
        }

        // Live continuous high-speed background sync (every 2 seconds) for instant real-time updates
        repoScope.launch {
            while (true) {
                delay(2000)
                try {
                    syncWithServer()
                    FirebaseRealtimeService.purgeUnloggedInStudentsFromRemoteServer()
                } catch (_: Throwable) {}
            }
        }
    }

    override suspend fun syncWithServer(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Concurrent parallel fetching for lightning-fast real-time synchronization
            val studentsDeferred = async { FirebaseRealtimeService.fetchStudents() }
            val usersDeferred = async { FirebaseRealtimeService.fetchUsers() }
            val recordsDeferred = async { FirebaseRealtimeService.fetchRecords() }
            val ranksDeferred = async { FirebaseRealtimeService.fetchCustomRanks() }

            val remoteStudents = studentsDeferred.await()
            val remoteUsers = usersDeferred.await()
            val remoteRecords = recordsDeferred.await()
            val remoteRanks = ranksDeferred.await()

            val isConnected = remoteStudents != null || remoteUsers != null || remoteRecords != null || remoteRanks != null
            _isServerConnected.value = isConnected

            if (isConnected) {
                // Ensure initial seed only once on fresh setup if DB is empty
                if (!SessionManager.isInitialSeeded()) {
                    FirebaseRealtimeService.seedInitialDataIfEmpty(
                        defaultUsers = DemoData.demoUsers,
                        defaultStudents = DemoData.demoStudents,
                        defaultRecords = DemoData.demoRecords
                    )
                    SessionManager.markInitialSeeded()
                }

                // 1. Live Sync Students:
                // When remoteStudents is non-null, Firebase /students node is authoritative for logged-in/registered students.
                if (remoteStudents != null) {
                    val validRemote = remoteStudents.filter { it.id != "user_creator_hossein" && it.studentCode != "454545" }

                    // Merge official roster with remote /students node:
                    val mergedStudents = DemoData.demoStudents.map { rosterStudent ->
                        val isCreator = rosterStudent.id == "user_creator_hossein" ||
                            rosterStudent.studentCode == "454545" ||
                            AllowedStudentsRegistry.normalize(rosterStudent.fullName) == AllowedStudentsRegistry.normalize("سید حسین موسوی نژاد")
                        if (isCreator) {
                            rosterStudent.copy(
                                id = "user_creator_hossein",
                                studentCode = "454545",
                                rankTitle = "سازنده",
                                rankColorHex = "#1E40AF",
                                parentPhone = "09923072572",
                                email = "hosseinvexter@gmail.com",
                                isRegistered = true,
                                hasLoggedIn = true
                            )
                        } else {
                            val serverMatch = validRemote.firstOrNull { r ->
                                r.studentCode == rosterStudent.studentCode ||
                                (r.id.isNotBlank() && r.id == rosterStudent.id) ||
                                AllowedStudentsRegistry.findMatchingAllowedStudent(r.fullName)?.fullName == rosterStudent.fullName ||
                                AllowedStudentsRegistry.normalize(r.fullName) == AllowedStudentsRegistry.normalize(rosterStudent.fullName)
                            }
                            if (serverMatch != null) {
                                serverMatch.copy(
                                    grade = rosterStudent.grade.ifBlank { serverMatch.grade },
                                    className = rosterStudent.className.ifBlank { serverMatch.className },
                                    isRegistered = true,
                                    hasLoggedIn = true
                                )
                            } else {
                                rosterStudent.copy(
                                    isRegistered = false,
                                    hasLoggedIn = false
                                )
                            }
                        }
                    }.toMutableList()

                    // Add any extra registered students that were created outside the fixed roster
                    validRemote.forEach { rem ->
                        val inRoster = mergedStudents.any { m ->
                            m.studentCode == rem.studentCode ||
                            AllowedStudentsRegistry.normalize(m.fullName) == AllowedStudentsRegistry.normalize(rem.fullName)
                        }
                        if (!inRoster) {
                            mergedStudents.add(rem.copy(isRegistered = true, hasLoggedIn = true))
                        }
                    }

                    if (_students.value != mergedStudents) {
                        _students.value = mergedStudents
                    }

                    // Update live session for logged in student immediately
                    val curr = _currentUser.value
                    if (curr != null && curr.role == UserRole.STUDENT) {
                        val matchingStudent = mergedStudents.firstOrNull { it.id == curr.id || it.studentCode == curr.studentCode }
                        if (matchingStudent != null) {
                            val newRank = if (matchingStudent.rankTitle.isNotBlank()) matchingStudent.rankTitle else curr.rankTitle
                            val newPerms = matchingStudent.permissions
                            val newColor = if (matchingStudent.rankColorHex.isNotBlank()) matchingStudent.rankColorHex else curr.rankColorHex
                            if (newRank != curr.rankTitle || newPerms != curr.permissions) {
                                val colorIntLong = try {
                                    android.graphics.Color.parseColor(if (newColor.startsWith("#")) newColor else "#$newColor").toLong() and 0xFFFFFFFFL
                                } catch (_: Exception) {
                                    0xFF64748BL
                                }
                                val updatedCurr = curr.copy(
                                    rankTitle = newRank,
                                    rankColorHex = newColor,
                                    rankBadgeColorHex = colorIntLong,
                                    permissions = newPerms
                                )
                                _currentUser.value = updatedCurr
                                SessionManager.saveUser(updatedCurr)
                            }
                        }
                    }
                }

                // 2. Live Sync Users:
                if (remoteUsers != null) {
                    val creatorUser = DemoData.demoUsers.first { it.id == "user_creator_hossein" }
                    val activeServerStudentCodes = remoteStudents?.map { it.studentCode }?.toSet().orEmpty()
                    val activeServerStudentIds = remoteStudents?.map { it.id }?.toSet().orEmpty()

                    val baseUsers = DemoData.demoUsers.map { base ->
                        if (base.id == creatorUser.id) creatorUser
                        else {
                            val remote = remoteUsers.firstOrNull { it.id == base.id }
                            if (remote != null) {
                                if (remote.role == UserRole.STUDENT) {
                                    val isInServerStudents = activeServerStudentCodes.contains(remote.studentCode) || activeServerStudentIds.contains(remote.id)
                                    remote.copy(phone = base.phone, hasLoggedIn = isInServerStudents, isRegisteredUser = isInServerStudents)
                                } else {
                                    remote.copy(phone = base.phone)
                                }
                            } else base
                        }
                    }
                    val updatedUsers = (baseUsers + remoteUsers.map { r ->
                        val demoMatch = DemoData.demoUsers.firstOrNull { it.id == r.id }
                        val baseCopy = if (demoMatch != null) r.copy(phone = demoMatch.phone) else r
                        if (r.role == UserRole.STUDENT && r.id != creatorUser.id && r.studentCode != "454545") {
                            val isInServerStudents = activeServerStudentCodes.contains(r.studentCode) || activeServerStudentIds.contains(r.id)
                            baseCopy.copy(hasLoggedIn = isInServerStudents, isRegisteredUser = isInServerStudents)
                        } else {
                            baseCopy
                        }
                    }).distinctBy { it.id }

                    if (_users.value != updatedUsers) {
                        _users.value = updatedUsers
                    }

                    // Push any missing pre-configured staff demo users or updated phone numbers to remote database
                    repoScope.launch {
                        baseUsers.forEach { base ->
                            val remote = remoteUsers.firstOrNull { it.id == base.id }
                            if (remote == null || remote.phone != base.phone) {
                                val toSave = remote?.copy(phone = base.phone) ?: base
                                FirebaseRealtimeService.saveUser(toSave)
                            }
                        }
                    }

                    // If current logged-in user was updated remotely (e.g. rank assigned), update local session
                    val curr = _currentUser.value
                    if (curr != null) {
                        if (curr.role == UserRole.STUDENT) {
                            val matchingStudent = _students.value.firstOrNull { s ->
                                (curr.studentCode.isNotBlank() && s.studentCode == curr.studentCode) || s.id == curr.id
                            }
                            if (matchingStudent != null) {
                                val effectivePerms = matchingStudent.permissions
                                val effectiveRank = matchingStudent.rankTitle.ifBlank { curr.rankTitle }
                                val effectiveColor = matchingStudent.rankColorHex.ifBlank { curr.rankColorHex }
                                if (curr.rankTitle != effectiveRank || curr.permissions != effectivePerms) {
                                    val colorIntLong = try {
                                        android.graphics.Color.parseColor(if (effectiveColor.startsWith("#")) effectiveColor else "#$effectiveColor").toLong() and 0xFFFFFFFFL
                                    } catch (_: Exception) {
                                        0xFF64748BL
                                    }
                                    val updatedCurr = curr.copy(
                                        rankTitle = effectiveRank,
                                        rankColorHex = effectiveColor,
                                        rankBadgeColorHex = colorIntLong,
                                        permissions = effectivePerms
                                    )
                                    _currentUser.value = updatedCurr
                                    SessionManager.saveUser(updatedCurr)
                                }
                            }
                        } else if (curr.role != UserRole.PARENT) {
                            // STAFF USER: Preserve staff role, tag, and staff permissions!
                            val updatedSelf = updatedUsers.firstOrNull { u ->
                                u.id == curr.id ||
                                (curr.studentCode.isNotBlank() && u.studentCode == curr.studentCode) ||
                                u.name.replace(" ", "").equals(curr.name.replace(" ", ""), ignoreCase = true)
                            }
                            if (updatedSelf != null) {
                                val newRole = if (updatedSelf.role != UserRole.STUDENT && updatedSelf.role != UserRole.PARENT) updatedSelf.role else curr.role
                                val newPerms = if (updatedSelf.permissions.isNotEmpty()) updatedSelf.permissions else curr.permissions
                                val newRank = updatedSelf.rankTitle.ifBlank { curr.rankTitle }
                                val newColor = updatedSelf.rankColorHex.ifBlank { curr.rankColorHex }
                                if (curr.role != newRole || curr.rankTitle != newRank || curr.permissions != newPerms) {
                                    val colorIntLong = try {
                                        android.graphics.Color.parseColor(if (newColor.startsWith("#")) newColor else "#$newColor").toLong() and 0xFFFFFFFFL
                                    } catch (_: Exception) {
                                        0xFF64748BL
                                    }
                                    val updatedCurr = curr.copy(
                                        role = newRole,
                                        rankTitle = newRank,
                                        rankColorHex = newColor,
                                        rankBadgeColorHex = colorIntLong,
                                        permissions = newPerms,
                                        isReadOnly = false
                                    )
                                    _currentUser.value = updatedCurr
                                    SessionManager.saveUser(updatedCurr)
                                }
                            }
                        }
                    }
                }

                // 3. Live Sync Records:
                if (remoteRecords != null && _records.value != remoteRecords) {
                    _records.value = remoteRecords
                }

                // 4. Live Sync Custom Ranks:
                if (remoteRanks != null) {
                    val newRanks = (defaultCustomRanks + remoteRanks).distinctBy { it.id }
                    if (_customRanks.value != newRanks) {
                        _customRanks.value = newRanks
                    }
                }

                // 5. Real-time check if active logged-in user was deleted from remote server:
                val curr = _currentUser.value
                if (curr != null) {
                    val isCreator = curr.id == "user_creator_hossein" ||
                            curr.studentCode == "454545" ||
                            curr.name.trim() == "سید حسین موسوی نژاد" ||
                            curr.isCreator

                    if (!isCreator) {
                        val currNormName = AllowedStudentsRegistry.normalize(curr.name)
                        val existsOnServer = when (curr.role) {
                            UserRole.STUDENT -> {
                                val inRemoteStudents = remoteStudents?.any { s ->
                                    (s.id == curr.id || (curr.studentCode.isNotBlank() && s.studentCode == curr.studentCode) || AllowedStudentsRegistry.normalize(s.fullName) == currNormName) &&
                                    (s.isRegistered || s.hasLoggedIn)
                                } == true
                                val inRemoteUsers = remoteUsers?.any { u ->
                                    (u.id == curr.id || (curr.studentCode.isNotBlank() && u.studentCode == curr.studentCode) || AllowedStudentsRegistry.normalize(u.name) == currNormName) &&
                                    (u.isRegisteredUser || u.hasLoggedIn)
                                } == true

                                if (remoteStudents != null && remoteUsers != null) {
                                    inRemoteStudents || inRemoteUsers
                                } else if (remoteStudents != null) {
                                    inRemoteStudents
                                } else if (remoteUsers != null) {
                                    inRemoteUsers
                                } else true
                            }
                            UserRole.PARENT -> {
                                val inRemoteUsers = remoteUsers?.any { u ->
                                    u.id == curr.id || (curr.studentCode.isNotBlank() && u.studentCode == curr.studentCode)
                                } == true
                                val inRemoteStudents = remoteStudents?.any { s ->
                                    (s.studentCode.isNotBlank() && s.studentCode == curr.studentCode) ||
                                    AllowedStudentsRegistry.normalize(s.fullName) == currNormName
                                } == true

                                if (remoteUsers != null && remoteStudents != null) {
                                    inRemoteUsers || inRemoteStudents
                                } else if (remoteUsers != null) {
                                    inRemoteUsers
                                } else if (remoteStudents != null) {
                                    inRemoteStudents
                                } else true
                            }
                            else -> { // MANAGER, VICE_PRINCIPAL, COUNSELOR, TEACHER
                                if (remoteUsers != null) {
                                    remoteUsers.any { u ->
                                        u.id == curr.id || (curr.studentCode.isNotBlank() && u.studentCode == curr.studentCode)
                                    }
                                } else true
                            }
                        }

                        if (!existsOnServer) {
                            android.util.Log.w("DemoRepositoryImpl", "Current logged-in user ${curr.name} (${curr.id}) was deleted from server. Force logging out in real time.")
                            _currentUser.value = null
                            SessionManager.clearSession()
                        }
                    }
                }
            }
            isConnected
        } catch (_: Exception) {
            _isServerConnected.value = false
            false
        }
    }

    override suspend fun checkForUpdates(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val remoteUpdate = FirebaseRealtimeService.fetchAppUpdate()
            if (remoteUpdate != null) {
                _appUpdateInfo.value = remoteUpdate
                remoteUpdate
            } else {
                _appUpdateInfo.value
            }
        } catch (_: Exception) {
            _appUpdateInfo.value
        }
    }

    override suspend fun publishAppUpdate(updateInfo: AppUpdateInfo): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _appUpdateInfo.value = updateInfo
            val savedToFirebase = FirebaseRealtimeService.saveAppUpdate(updateInfo)
            if (savedToFirebase) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("خطا در ارسال اطلاعات به‌روزرسانی به سرور فایربیس."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAppUpdate(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _appUpdateInfo.value = null
            val deleted = FirebaseRealtimeService.deleteAppUpdate()
            if (deleted) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("خطا در حذف بسته به‌روزرسانی از سرور فایربیس."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // In-memory data states
    private val defaultCustomRanks = listOf(
        CustomRank("rank_student", "دانش‌آموز", "#64748B", emptyList(), "رنک پایه تمام دانش‌آموزان"),
        CustomRank("rank_mobser", "مبصر کلاس", "#8B5CF6", listOf(RankPermission.REGISTER_POSITIVE), "مسئول نظم و ثبت تشویقی کلاسی"),
        CustomRank("rank_hamyar_ens", "همیار انضباطی", "#F97316", listOf(RankPermission.REGISTER_DISCIPLINARY), "ثبت موارد انضباطی و تذکرات"),
        CustomRank("rank_sargorooh", "سرگروه درسی", "#10B981", listOf(RankPermission.REGISTER_POSITIVE), "همیار علمی و ثبت امتیاز هم‌کلاسی‌ها"),
        CustomRank("rank_nokhbeh", "نخبه علمی", "#F59E0B", listOf(RankPermission.VIEW_REPORTS, RankPermission.SPECIAL_BADGE), "دانش‌آموز برتر با دسترسی به گزارش‌ها"),
        CustomRank("rank_nemouneh", "دانش‌آموز نمونه", "#0284C7", listOf(RankPermission.SPECIAL_BADGE), "دارای نشان افتخار سامانه")
    )
    private val _customRanks = MutableStateFlow<List<CustomRank>>(defaultCustomRanks)

    private val _users = MutableStateFlow<List<User>>(DemoData.demoUsers)
    private val _students = MutableStateFlow(DemoData.demoStudents)
    private val _records = MutableStateFlow(DemoData.demoRecords)
    private val _classes = MutableStateFlow(DemoData.demoClasses)
    private val _notifications = MutableStateFlow(DemoData.demoNotifications)

    // --- AuthRepository ---
    override val allUsers: StateFlow<List<User>> = _users.asStateFlow()
    override fun getSavedUsers(): List<User> = _users.value

    override suspend fun login(
        name: String,
        studentCode: String,
        password: String,
        role: UserRole,
        parentRelation: String
    ): Result<User> {
        val trimmedName = name.trim()
        val trimmedCode = studentCode.trim()
        val trimmedPass = password.trim()

        val isStaffRole = (role == UserRole.MANAGER || role == UserRole.VICE_PRINCIPAL || role == UserRole.TEACHER || role == UserRole.COUNSELOR)

        if (trimmedName.isBlank()) {
            return Result.failure(Exception(if (role == UserRole.PARENT) "لطفاً نام فرزند دانش‌آموز خود را وارد کنید." else "لطفاً نام و نام خانوادگی را وارد کنید."))
        }

        if (!isStaffRole) {
            if (trimmedCode.isBlank()) {
                return Result.failure(Exception("لطفاً کد ۶ رقمی دانش‌آموزی را وارد کنید."))
            }
            if (trimmedCode.length != 6 || !trimmedCode.all { it.isDigit() }) {
                return Result.failure(Exception("کد دانش‌آموزی باید یک عدد ۶ رقمی معتبر باشد."))
            }
        }

        if (trimmedPass.isBlank()) {
            return Result.failure(Exception("لطفاً رمز عبور را وارد کنید."))
        }

        var user: User? = null

        if (isStaffRole) {
            // Educational Staff Login: Search by name and staff role
            val cleanInput = trimmedName.replace(" ", "")
            val allStaffCandidates = (_users.value + DemoData.demoUsers).distinctBy { it.id }
            user = allStaffCandidates.firstOrNull { u ->
                u.role != UserRole.STUDENT && u.role != UserRole.PARENT && (
                    u.name.replace(" ", "").equals(cleanInput, ignoreCase = true) ||
                    u.name.equals(trimmedName, ignoreCase = true) ||
                    (cleanInput.contains("فاریابی") && u.id == "user_manager_mehdi_faryabi") ||
                    (cleanInput.contains("دوراهکی") && u.id == "user_counselor_yasin_dorahaki") ||
                    (cleanInput.contains("موسوی") && u.isCreator)
                )
            }

            if (user == null) {
                val remoteUsers = FirebaseRealtimeService.fetchUsers()
                if (!remoteUsers.isNullOrEmpty()) {
                    _users.value = (remoteUsers + _users.value).distinctBy { it.id }
                    user = _users.value.firstOrNull { u ->
                        u.role != UserRole.STUDENT && u.role != UserRole.PARENT &&
                        u.name.replace(" ", "").equals(cleanInput, ignoreCase = true)
                    }
                }
            }

            // If no existing staff user account found, dynamically create a Manager / Vice Principal staff account
            if (user == null) {
                val newStaffRole = if (role == UserRole.STUDENT || role == UserRole.PARENT) UserRole.MANAGER else role
                user = User(
                    id = "staff_${UUID.randomUUID()}",
                    name = trimmedName,
                    phone = "09120000000",
                    email = "staff@school.ir",
                    role = newStaffRole,
                    schoolId = DemoData.DEFAULT_SCHOOL_ID,
                    studentCode = "999999",
                    password = trimmedPass,
                    gender = "آقا",
                    isRegisteredUser = true,
                    hasLoggedIn = true,
                    rankTitle = newStaffRole.farsiTitle,
                    rankBadgeColorHex = 0xFF1E40AF,
                    rankColorHex = "#1E40AF",
                    permissions = listOf(
                        RankPermission.REGISTER_POSITIVE,
                        RankPermission.REGISTER_DISCIPLINARY,
                        RankPermission.VIEW_REPORTS,
                        RankPermission.ACCESS_GAMES,
                        RankPermission.VIEW_STUDENT_PROFILES,
                        RankPermission.EDIT_CLASS_INFO,
                        RankPermission.SPECIAL_BADGE
                    ),
                    isReadOnly = false
                )
            }
        } else {
            // Student or Parent Login: Find user by name and student code
            user = _users.value.firstOrNull { u ->
                val nameMatches = u.name.equals(trimmedName, ignoreCase = true) ||
                        u.name.replace(" ", "").equals(trimmedName.replace(" ", ""), ignoreCase = true)
                val codeMatches = u.studentCode == trimmedCode || (
                    (u.id == "user_manager_mehdi_faryabi" || u.id == "user_counselor_yasin_dorahaki") && trimmedCode.length == 6
                )
                nameMatches && codeMatches
            }

            if (user == null) {
                val studentMatch = _students.value.firstOrNull { s ->
                    val nameMatches = s.fullName.equals(trimmedName, ignoreCase = true) ||
                            s.fullName.replace(" ", "").equals(trimmedName.replace(" ", ""), ignoreCase = true)
                    val codeMatches = s.studentCode == trimmedCode
                    nameMatches && codeMatches
                }
                if (studentMatch != null) {
                    user = studentMatch.toUser()
                }
            }

            // If not found in local memory, check Firebase Realtime Database /users
            if (user == null) {
                val remoteUsers = FirebaseRealtimeService.fetchUsers()
                if (!remoteUsers.isNullOrEmpty()) {
                    _users.value = (remoteUsers + _users.value).distinctBy { it.id }
                    user = _users.value.firstOrNull { u ->
                        val nameMatches = u.name.equals(trimmedName, ignoreCase = true) ||
                                u.name.replace(" ", "").equals(trimmedName.replace(" ", ""), ignoreCase = true)
                        val codeMatches = u.studentCode == trimmedCode
                        nameMatches && codeMatches
                    }
                }
            }

            // If still not found, check Firebase Realtime Database /students
            if (user == null) {
                val remoteStudents = FirebaseRealtimeService.fetchStudents()
                if (!remoteStudents.isNullOrEmpty()) {
                    _students.value = (remoteStudents + _students.value).distinctBy { it.id }
                    val studentMatch = _students.value.firstOrNull { s ->
                        val nameMatches = s.fullName.equals(trimmedName, ignoreCase = true) ||
                                s.fullName.replace(" ", "").equals(trimmedName.replace(" ", ""), ignoreCase = true)
                        val codeMatches = s.studentCode == trimmedCode
                        nameMatches && codeMatches
                    }
                    if (studentMatch != null) {
                        user = studentMatch.toUser()
                    }
                }
            }

            if (user == null) {
                return Result.failure(Exception(if (role == UserRole.PARENT) "دانش‌آموزی با این نام و کد دانش‌آموزی یافت نشد. لطفاً مشخصات فرزند خود را بررسی کنید." else "کاربری با این نام و کد دانش‌آموزی یافت نشد. لطفاً از بخش ثبت‌نام ابتدا ثبت‌نام کنید."))
            }

            // Ensure user has the latest rank and permissions from students list
            val matchingStudent = _students.value.firstOrNull { s ->
                (s.studentCode == trimmedCode) && (s.fullName.equals(trimmedName, ignoreCase = true) ||
                        s.fullName.replace(" ", "").equals(trimmedName.replace(" ", ""), ignoreCase = true))
            }
            if (matchingStudent != null && user.role == UserRole.STUDENT) {
                val colorIntLong = try {
                    android.graphics.Color.parseColor(if (matchingStudent.rankColorHex.startsWith("#")) matchingStudent.rankColorHex else "#${matchingStudent.rankColorHex}").toLong() and 0xFFFFFFFFL
                } catch (_: Exception) {
                    0xFF64748BL
                }
                user = user.copy(
                    rankTitle = matchingStudent.rankTitle,
                    rankColorHex = matchingStudent.rankColorHex,
                    rankBadgeColorHex = colorIntLong,
                    permissions = matchingStudent.permissions
                )
            }
        }

        val isCreatorLogin = (user.studentCode == "454545" && user.name.trim() == "سید حسین موسوی نژاد") || user.id == "user_creator_hossein"
        val isFaryabiLogin = user.id == "user_manager_mehdi_faryabi" || user.name.replace(" ", "") == "مهدیفاریابی"
        val isDorahakiLogin = user.id == "user_counselor_yasin_dorahaki" || user.name.replace(" ", "") == "یاسیندراهکی"

        val passwordMatches = if (isCreatorLogin) {
            user.password == trimmedPass || trimmedPass == DemoData.CREATOR_PASSWORD
        } else if (isFaryabiLogin) {
            user.password == trimmedPass || trimmedPass == "123456" || trimmedPass == "faryabi123"
        } else if (isDorahakiLogin) {
            user.password == trimmedPass || trimmedPass == "123456" || trimmedPass == "dorahaki123"
        } else {
            user.password.isNotBlank() && user.password == trimmedPass
        }

        if (!passwordMatches) {
            return Result.failure(Exception("رمز عبور وارد شده نادرست است."))
        }

        val finalUser = if (isCreatorLogin) {
            user.copy(
                role = UserRole.MANAGER,
                tag = "سازنده",
                rankTitle = "سازنده",
                rankBadgeColorHex = 0xFF1E40AF,
                rankColorHex = "#1E40AF",
                isReadOnly = false
            )
        } else if (role == UserRole.PARENT) {
            val isMother = parentRelation.contains("مادر") || parentRelation.contains("خانم")
            val rank = if (isMother) "مادر دانش‌آموز" else "پدر دانش‌آموز"
            val gender = if (isMother) "مادر (خانم)" else "پدر (آقا)"
            val parentUser = user.copy(
                id = "parent_${user.studentCode}_${if (isMother) "mom" else "dad"}",
                name = user.name, // "اسم همون اسم دانش آموز و فرزند خودش باشه"
                role = UserRole.PARENT,
                childStudentIds = listOf(user.id),
                isReadOnly = true,
                gender = gender,
                rankTitle = rank,
                rankBadgeColorHex = 0xFF64748B,
                rankColorHex = "#64748B"
            )

            _currentUser.value = parentUser
            SessionManager.saveUser(parentUser)

            val currentUsersList = _users.value
            if (!currentUsersList.any { it.id == parentUser.id }) {
                _users.value = currentUsersList + parentUser
            }
            repoScope.launch {
                FirebaseRealtimeService.saveUser(parentUser)
            }

            // Register parent login in ParentAccessManager and check if both father & mother are logged in
            val justCompletedBoth = com.example.core.auth.ParentAccessManager.recordParentLogin(
                studentCode = user.studentCode,
                isMother = isMother
            )

            if (justCompletedBoth) {
                // Send celebratory notification to both parents and the student
                val notifs = com.example.core.auth.ParentAccessManager.createBothParentsUnlockedNotifications(
                    studentCode = user.studentCode,
                    studentName = user.name,
                    studentId = user.id,
                    schoolId = user.schoolId
                )
                notifs.forEach { notif ->
                    addNotification(notif)
                }
            }

            return Result.success(parentUser)
        } else if (user.role == UserRole.MANAGER || user.role == UserRole.COUNSELOR || user.role == UserRole.VICE_PRINCIPAL || user.role == UserRole.TEACHER) {
            // Keep their staff role and permissions as configured
            user.copy(
                isReadOnly = false
            )
        } else {
            user.copy(
                role = UserRole.STUDENT,
                isReadOnly = false
            )
        }

        val loggedInUser = finalUser.copy(
            hasLoggedIn = true,
            isRegisteredUser = true
        )

        _currentUser.value = loggedInUser
        SessionManager.saveUser(loggedInUser)

        // Update in _users list
        _users.value = _users.value.map {
            if (it.id == loggedInUser.id || (loggedInUser.studentCode.isNotBlank() && it.studentCode == loggedInUser.studentCode)) loggedInUser else it
        }

        // Update in _students list
        _students.value = _students.value.map { s ->
            if (s.id == loggedInUser.id || (loggedInUser.studentCode.isNotBlank() && s.studentCode == loggedInUser.studentCode)) {
                s.copy(hasLoggedIn = true, isRegistered = true)
            } else s
        }

        // Push login status to server in real-time
        repoScope.launch {
            FirebaseRealtimeService.saveUser(loggedInUser)
            val matchingStudent = _students.value.firstOrNull { it.id == loggedInUser.id || (loggedInUser.studentCode.isNotBlank() && it.studentCode == loggedInUser.studentCode) }
            if (matchingStudent != null) {
                FirebaseRealtimeService.saveStudent(matchingStudent)
            }
        }

        return Result.success(loggedInUser)
    }

    override suspend fun register(
        name: String,
        studentCode: String,
        phone: String,
        email: String,
        password: String,
        role: UserRole,
        gender: String,
        grade: String,
        className: String
    ): Result<User> {
        val trimmedName = name.trim()
        val trimmedCode = studentCode.trim()
        val trimmedPhone = phone.trim()
        val trimmedPass = password.trim()

        if (trimmedName.isBlank()) {
            return Result.failure(Exception("نام و نام خانوادگی الزامی است."))
        }

        val isCreatorUser = (trimmedCode == "454545" && trimmedName == "سید حسین موسوی نژاد")
        val isStudentAccount = (role == UserRole.STUDENT && !isCreatorUser)

        // Validation against official roster: only listed students are allowed to register
        val matchedAllowed = if (isStudentAccount) {
            val matched = AllowedStudentsRegistry.findMatchingAllowedStudent(trimmedName)
            if (matched == null) {
                return Result.failure(Exception("نام «$trimmedName» در لیست رسمی دانش‌آموزان مدرسه ثبت نشده است. فقط دانش‌آموزان موجود در لیست مجاز به ثبت‌نام هستند."))
            }
            matched
        } else null

        // 1. Duplicate name check: only reject if an existing account with this name has ALREADY logged in and completed registration with a phone number
        val normInput = AllowedStudentsRegistry.normalize(trimmedName)
        val nameAlreadyRegistered = _users.value.any { u ->
            u.hasLoggedIn && u.phone.isValidPhoneNumber() && (
                AllowedStudentsRegistry.normalize(u.name) == normInput ||
                (matchedAllowed != null && AllowedStudentsRegistry.findMatchingAllowedStudent(u.name)?.fullName == matchedAllowed.fullName)
            )
        } || _students.value.any { s ->
            s.hasLoggedIn && s.parentPhone.isValidPhoneNumber() && (
                AllowedStudentsRegistry.normalize(s.fullName) == normInput ||
                (matchedAllowed != null && AllowedStudentsRegistry.findMatchingAllowedStudent(s.fullName)?.fullName == matchedAllowed.fullName)
            )
        }

        if (nameAlreadyRegistered) {
            return Result.failure(Exception("حساب کاربری «$trimmedName» قبلاً ثبت‌نام و فعال شده است. جهت ورود، از تب «ورود به سامانه» استفاده کنید."))
        }

        if (trimmedCode.length != 6 || !trimmedCode.all { it.isDigit() }) {
            return Result.failure(Exception("کد دانش‌آموزی ۶ رقمی به درستی ایجاد نشده است."))
        }
        if (!trimmedPhone.isValidPhoneNumber()) {
            return Result.failure(Exception("شماره تماس وارد شده معتبر نیست. لطفاً یک شماره موبایل معتبر ۱۰ یا ۱۱ رقمی وارد کنید."))
        }
        if (trimmedPass.length < 4) {
            return Result.failure(Exception("رمز عبور باید حداقل ۴ رقم یا کاراکتر باشد."))
        }

        // Check duplicate student code only against accounts that have already logged in
        val codeAlreadyRegistered = _users.value.any { it.hasLoggedIn && it.phone.isValidPhoneNumber() && it.studentCode == trimmedCode } ||
                _students.value.any { it.hasLoggedIn && it.parentPhone.isValidPhoneNumber() && it.studentCode == trimmedCode }
        if (codeAlreadyRegistered) {
            return Result.failure(Exception("این کد دانش‌آموزی قبلاً توسط حساب دیگری ثبت گردیده است. لطفاً کد جدیدی تولید فرمایید."))
        }

        val actualGender = if (role == UserRole.STUDENT) "پسر" else gender

        val rankTitle = if (isCreatorUser) {
            "سازنده"
        } else when (role) {
            UserRole.STUDENT -> "دانش‌آموز"
            UserRole.PARENT -> if (actualGender.contains("مادر") || actualGender == "خانم") "ولی دانش‌آموز (مادر)" else "ولی دانش‌آموز (پدر)"
            else -> role.farsiTitle
        }

        if (isStudentAccount) {
            // Ordinary student account
            val cleanStudentName = trimmedName.replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
            val nameParts = cleanStudentName.split(" ").filter { it.isNotBlank() }
            val firstName = matchedAllowed?.firstName ?: nameParts.firstOrNull().orEmpty()
            val rawLastName = nameParts.drop(1).joinToString(" ").trim()
            val lastName = if (matchedAllowed != null) {
                matchedAllowed.lastName
            } else if (rawLastName == "دانش‌آموز" || rawLastName == "دانش آموز") {
                ""
            } else {
                rawLastName.replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
            }
            val finalGrade = matchedAllowed?.grade ?: grade
            val finalClass = matchedAllowed?.className ?: className

            // Find existing roster student to inherit ID and score
            val existingRoster = _students.value.firstOrNull { s ->
                (matchedAllowed != null && (AllowedStudentsRegistry.findMatchingAllowedStudent(s.fullName)?.fullName == matchedAllowed.fullName || AllowedStudentsRegistry.normalize(s.fullName) == AllowedStudentsRegistry.normalize(matchedAllowed.fullName))) ||
                AllowedStudentsRegistry.normalize(s.fullName) == normInput
            }
            val finalStudentId = existingRoster?.id ?: "user_${UUID.randomUUID().toString().take(8)}"

            val newStudent = Student(
                id = finalStudentId,
                firstName = firstName,
                lastName = lastName,
                studentCode = trimmedCode,
                grade = finalGrade,
                className = finalClass,
                gender = actualGender,
                schoolId = DemoData.DEFAULT_SCHOOL_ID,
                score = existingRoster?.score ?: 100,
                parentPhone = trimmedPhone,
                email = email.trim(),
                password = trimmedPass,
                role = UserRole.STUDENT,
                rankTitle = existingRoster?.rankTitle ?: "دانش‌آموز",
                rankColorHex = existingRoster?.rankColorHex ?: "#64748B",
                permissions = existingRoster?.permissions ?: emptyList(),
                isRegistered = true,
                hasLoggedIn = true
            )

            val newUser = User(
                id = finalStudentId,
                name = trimmedName,
                phone = trimmedPhone,
                email = email.trim(),
                role = UserRole.STUDENT,
                schoolId = DemoData.DEFAULT_SCHOOL_ID,
                studentCode = trimmedCode,
                password = trimmedPass,
                gender = actualGender,
                isRegisteredUser = true,
                hasLoggedIn = true,
                tag = null,
                rankTitle = existingRoster?.rankTitle ?: "دانش‌آموز",
                rankBadgeColorHex = 0xFF64748B,
                rankColorHex = existingRoster?.rankColorHex ?: "#64748B",
                permissions = existingRoster?.permissions ?: emptyList(),
                isReadOnly = false
            )

            // Strict server write verification (up to 6 seconds timeout):
            val savedOnServer = try {
                withTimeoutOrNull(6000) {
                    val sSaved = FirebaseRealtimeService.saveStudent(newStudent)
                    val uSaved = FirebaseRealtimeService.saveUser(newUser)
                    sSaved && uSaved
                } ?: false
            } catch (_: Exception) {
                false
            }

            if (!savedOnServer) {
                return Result.failure(Exception("خطای ثبت اطلاعات در سرور: به دلیل ضعف یا قطعی اینترنت، حساب شما روی سرور ذخیره نگردید. لطفاً اتصال اینترنت را بررسی کرده و مجدداً تلاش نمایید."))
            }

            // Immediately update local states: replace matching roster student with the registered student
            val updatedRoster = _students.value.map { s ->
                val matchesThis = s.id == finalStudentId ||
                                  (matchedAllowed != null && (AllowedStudentsRegistry.findMatchingAllowedStudent(s.fullName)?.fullName == matchedAllowed.fullName || AllowedStudentsRegistry.normalize(s.fullName) == AllowedStudentsRegistry.normalize(matchedAllowed.fullName))) ||
                                  AllowedStudentsRegistry.normalize(s.fullName) == normInput
                if (matchesThis) newStudent else s
            }
            _students.value = if (updatedRoster.any { it.id == finalStudentId }) updatedRoster else listOf(newStudent) + updatedRoster
            _users.value = listOf(newUser) + _users.value.filter { it.id != finalStudentId && it.studentCode != trimmedCode }

            _currentUser.value = newUser
            SessionManager.saveUser(newUser)
            _isServerConnected.value = true
            return Result.success(newUser)
        } else {
            // User account (creator, staff, manager, parent)
            val nonStudentUser = User(
                id = "user_${UUID.randomUUID().toString().take(8)}",
                name = trimmedName,
                phone = trimmedPhone,
                email = email.trim(),
                role = if (isCreatorUser) UserRole.MANAGER else role,
                schoolId = DemoData.DEFAULT_SCHOOL_ID,
                studentCode = trimmedCode,
                password = trimmedPass,
                gender = actualGender,
                isRegisteredUser = true,
                hasLoggedIn = true,
                tag = if (isCreatorUser) "سازنده" else null,
                rankTitle = rankTitle,
                rankBadgeColorHex = if (isCreatorUser) 0xFF1E40AF else 0xFF64748B,
                rankColorHex = if (isCreatorUser) "#1E40AF" else "#64748B",
                permissions = if (isCreatorUser) listOf(
                    RankPermission.REGISTER_POSITIVE,
                    RankPermission.REGISTER_DISCIPLINARY,
                    RankPermission.VIEW_REPORTS,
                    RankPermission.ACCESS_GAMES,
                    RankPermission.VIEW_STUDENT_PROFILES,
                    RankPermission.EDIT_CLASS_INFO,
                    RankPermission.SPECIAL_BADGE
                ) else emptyList(),
                isReadOnly = (role == UserRole.PARENT && !isCreatorUser)
            )

            val savedOnServer = try {
                withTimeoutOrNull(6000) {
                    FirebaseRealtimeService.saveUser(nonStudentUser)
                } ?: false
            } catch (_: Exception) {
                false
            }

            if (!savedOnServer) {
                return Result.failure(Exception("خطای ثبت اطلاعات در سرور: به دلیل ضعف یا قطعی اینترنت، حساب شما روی سرور ذخیره نگردید. لطفاً اتصال اینترنت را بررسی کرده و مجدداً تلاش نمایید."))
            }

            _users.value = listOf(nonStudentUser) + _users.value.filter { it.studentCode != trimmedCode }
            _currentUser.value = nonStudentUser
            SessionManager.saveUser(nonStudentUser)
            _isServerConnected.value = true
            return Result.success(nonStudentUser)
        }
    }

    override suspend fun recoverAccount(emailOrPhoneOrName: String): Result<User> {
        val query = emailOrPhoneOrName.trim()
        if (query.isBlank()) {
            return Result.failure(Exception("لطفاً شماره تماس جهت پیامک یا آدرس ایمیل خود را وارد کنید."))
        }

        var user = _users.value.firstOrNull { u ->
            (u.email.isNotBlank() && u.email.equals(query, ignoreCase = true)) ||
            (u.phone.isNotBlank() && u.phone == query)
        }

        if (user == null) {
            val studentMatch = _students.value.firstOrNull { s ->
                (s.email.isNotBlank() && s.email.equals(query, ignoreCase = true)) ||
                (s.parentPhone.isNotBlank() && s.parentPhone == query)
            }
            if (studentMatch != null) {
                user = studentMatch.toUser()
            }
        }

        // If not found in local memory, check Firebase Realtime Database
        if (user == null) {
            val remoteUsers = FirebaseRealtimeService.fetchUsers()
            if (!remoteUsers.isNullOrEmpty()) {
                _users.value = (remoteUsers + _users.value).distinctBy { it.id }
                user = _users.value.firstOrNull { u ->
                    (u.email.isNotBlank() && u.email.equals(query, ignoreCase = true)) ||
                    (u.phone.isNotBlank() && u.phone == query)
                }
            }
        }

        if (user == null) {
            val remoteStudents = FirebaseRealtimeService.fetchStudents()
            if (!remoteStudents.isNullOrEmpty()) {
                _students.value = (remoteStudents + _students.value).distinctBy { it.id }
                val studentMatch = _students.value.firstOrNull { s ->
                    (s.email.isNotBlank() && s.email.equals(query, ignoreCase = true)) ||
                    (s.parentPhone.isNotBlank() && s.parentPhone == query)
                }
                if (studentMatch != null) {
                    user = studentMatch.toUser()
                }
            }
        }

        return if (user != null) {
            // Log recovery dispatch to Firebase RTDB server
            repoScope.launch {
                FirebaseRealtimeService.logAccountRecovery(query, user.studentCode, user.name)
            }
            Result.success(user)
        } else {
            Result.failure(Exception("حسابی با این شماره تماس یا ایمیل یافت نشد. دریافت مشخصات فقط از طریق شماره تماس (پیامک) یا صندوق ایمیل ثبت‌شده انجام می‌شود."))
        }
    }

    override suspend fun logout() {
        SessionManager.clearSession()
        _currentUser.value = null
    }

    override suspend fun updateUserProfile(updatedUser: User): Result<User> {
        if (_currentUser.value?.id == updatedUser.id) {
            _currentUser.value = updatedUser
            SessionManager.saveUser(updatedUser)
        }

        // Update in _users
        _users.value = _users.value.map { 
            if (it.id == updatedUser.id || (updatedUser.studentCode.isNotBlank() && it.studentCode == updatedUser.studentCode)) updatedUser else it 
        }
        if (_users.value.none { it.id == updatedUser.id }) {
            _users.value = listOf(updatedUser) + _users.value
        }

        // If this user corresponds to a student, update the student record too
        _students.value = _students.value.map { s ->
            if (s.id == updatedUser.id || (updatedUser.studentCode.isNotBlank() && s.studentCode == updatedUser.studentCode)) {
                val cleanName = updatedUser.name.replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
                val parts = cleanName.split(" ").filter { it.isNotBlank() }
                s.copy(
                    firstName = parts.firstOrNull().orEmpty(),
                    lastName = parts.drop(1).joinToString(" ").trim(),
                    studentCode = updatedUser.studentCode,
                    parentPhone = updatedUser.phone,
                    email = updatedUser.email,
                    password = updatedUser.password,
                    gender = updatedUser.gender,
                    rankTitle = updatedUser.rankTitle,
                    rankColorHex = updatedUser.rankColorHex,
                    permissions = updatedUser.permissions,
                    profileImage = updatedUser.profileImage,
                    extraRanks = updatedUser.extraRanks,
                    extraRankColors = updatedUser.extraRankColors,
                    nationalId = updatedUser.nationalId,
                    bio = updatedUser.bio
                )
            } else s
        }

        // Persist to Server via Firebase RTDB
        repoScope.launch {
            try {
                FirebaseRealtimeService.saveUser(updatedUser)
                val matchingStudent = _students.value.firstOrNull { 
                    it.id == updatedUser.id || (updatedUser.studentCode.isNotBlank() && it.studentCode == updatedUser.studentCode) 
                }
                if (matchingStudent != null) {
                    FirebaseRealtimeService.saveStudent(matchingStudent)
                }
            } catch (_: Exception) {}
        }

        return Result.success(updatedUser)
    }

    // --- StudentRepository ---
    override fun getStudents(): Flow<List<Student>> = _students

    override fun getStudentById(id: String): Flow<Student?> =
        _students.map { list -> list.firstOrNull { it.id == id } }

    override fun getStudentsByClass(className: String): Flow<List<Student>> =
        _students.map { list -> list.filter { it.className == className } }

    override fun searchStudents(query: String, grade: String?, className: String?): Flow<List<Student>> =
        _students.map { list ->
            list.filter { student ->
                val matchesQuery = query.isBlank() ||
                    student.fullName.contains(query, ignoreCase = true) ||
                    student.studentCode.contains(query) ||
                    student.className.contains(query, ignoreCase = true)

                val matchesGrade = grade.isNullOrBlank() || grade == "همه پایه‌ها" || student.grade == grade
                val matchesClass = className.isNullOrBlank() || className == "همه کلاس‌ها" || student.className == className

                matchesQuery && matchesGrade && matchesClass
            }
        }

    override suspend fun addStudent(student: Student): Result<Unit> {
        val cleanFirst = student.firstName.trim().replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
        val rawLast = student.lastName.trim()
        val cleanLast = if (rawLast == "دانش‌آموز" || rawLast == "دانش آموز") {
            ""
        } else {
            rawLast.replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
        }
        val sanitized = student.copy(
            firstName = cleanFirst,
            lastName = cleanLast
        )
        val updated = _students.value.toMutableList().apply { add(0, sanitized) }
        _students.value = updated

        // Also add student to _users so student can immediately log in
        val studentUser = sanitized.toUser()
        _users.value = (listOf(studentUser) + _users.value).distinctBy { it.studentCode }

        try {
            val saved = withTimeoutOrNull(4000) {
                val sSaved = FirebaseRealtimeService.saveStudent(sanitized)
                FirebaseRealtimeService.saveUser(studentUser)
                sSaved
            } ?: false
            if (saved) {
                _isServerConnected.value = true
            } else {
                repoScope.launch { 
                    FirebaseRealtimeService.saveStudent(sanitized)
                    FirebaseRealtimeService.saveUser(studentUser)
                }
            }
        } catch (_: Exception) {
            repoScope.launch { 
                FirebaseRealtimeService.saveStudent(sanitized)
                FirebaseRealtimeService.saveUser(studentUser)
            }
        }

        return Result.success(Unit)
    }

    override suspend fun updateStudent(student: Student): Result<Unit> {
        val updated = _students.value.map { if (it.id == student.id) student else it }
        _students.value = updated

        repoScope.launch {
            FirebaseRealtimeService.saveStudent(student)
        }

        return Result.success(Unit)
    }

    override suspend fun deleteStudent(studentId: String): Result<Unit> {
        _students.value = _students.value.filter { it.id != studentId }
        _users.value = _users.value.filter { it.id != studentId }
        repoScope.launch {
            FirebaseRealtimeService.deleteStudent(studentId)
            FirebaseRealtimeService.deleteUser(studentId)
            syncWithServer()
        }
        return Result.success(Unit)
    }

    override suspend fun deleteUser(userId: String): Result<Unit> {
        _users.value = _users.value.filter { it.id != userId }
        _students.value = _students.value.filter { it.id != userId }
        repoScope.launch {
            FirebaseRealtimeService.deleteUser(userId)
            FirebaseRealtimeService.deleteStudent(userId)
            syncWithServer()
        }
        return Result.success(Unit)
    }

    override suspend fun assignRankToStudent(
        studentId: String,
        rankTitle: String,
        colorHex: String,
        permissions: List<String>
    ): Result<Unit> {
        val normalizedColor = if (colorHex.startsWith("#")) colorHex else "#$colorHex"
        val targetStudent = _students.value.firstOrNull { it.id == studentId }
        val targetCode = targetStudent?.studentCode.orEmpty()

        // 1. Update Student in memory
        _students.value = _students.value.map { student ->
            if (student.id == studentId || (targetCode.isNotBlank() && student.studentCode == targetCode)) {
                student.copy(
                    rankTitle = rankTitle,
                    rankColorHex = normalizedColor,
                    permissions = permissions
                )
            } else student
        }

        // 2. Also update corresponding User in _users
        val colorIntLong = try {
            android.graphics.Color.parseColor(normalizedColor).toLong() and 0xFFFFFFFFL
        } catch (_: Exception) {
            0xFF64748BL
        }

        _users.value = _users.value.map { user ->
            if (user.id == studentId || (targetCode.isNotBlank() && user.studentCode == targetCode)) {
                user.copy(
                    rankTitle = rankTitle,
                    rankColorHex = normalizedColor,
                    rankBadgeColorHex = colorIntLong,
                    permissions = permissions
                )
            } else user
        }

        val updatedStudent = _students.value.firstOrNull { it.id == studentId || (targetCode.isNotBlank() && it.studentCode == targetCode) }
        val updatedUser = _users.value.firstOrNull { it.id == studentId || (targetCode.isNotBlank() && it.studentCode == targetCode) }
            ?: updatedStudent?.toUser()

        // 3. Update current logged-in user if this is the student
        val curr = _currentUser.value
        if (curr != null && (curr.id == studentId || (targetCode.isNotBlank() && curr.studentCode == targetCode))) {
            val updatedCurr = curr.copy(
                rankTitle = rankTitle,
                rankColorHex = normalizedColor,
                rankBadgeColorHex = colorIntLong,
                permissions = permissions
            )
            _currentUser.value = updatedCurr
            SessionManager.saveUser(updatedCurr)
        }

        // 4. Create congratulatory notification for student
        val notif = AppNotification(
            id = "notif_${UUID.randomUUID().toString().take(8)}",
            schoolId = DemoData.DEFAULT_SCHOOL_ID,
            recipientUserId = studentId,
            title = "🎉 تبریک! ارتقای رنک به «$rankTitle»",
            body = "تبریک صمیمانه! رنک شما در سامانه نظمیار به «$rankTitle» ارتقا یافت و دسترسی‌های جدید برای حساب کاربری شما فعال گردید.",
            type = RecordType.ACHIEVEMENT,
            studentId = studentId,
            studentName = updatedStudent?.fullName ?: "دانش‌آموز",
            recordId = "",
            date = DateTimeUtils.getCurrentPersianDate(),
            time = DateTimeUtils.getCurrentTime(),
            read = false
        )
        _notifications.value = listOf(notif) + _notifications.value

        // 5. Save BOTH to Firebase RTDB synchronously with timeout + background guarantee
        val finalUpdatedStudent = updatedStudent?.copy(
            rankTitle = rankTitle,
            rankColorHex = normalizedColor,
            permissions = permissions
        )
        val finalUpdatedUser = updatedUser?.copy(
            rankTitle = rankTitle,
            rankColorHex = normalizedColor,
            rankBadgeColorHex = colorIntLong,
            permissions = permissions
        )

        try {
            withTimeoutOrNull(4000) {
                if (finalUpdatedStudent != null) FirebaseRealtimeService.saveStudent(finalUpdatedStudent)
                if (finalUpdatedUser != null) FirebaseRealtimeService.saveUser(finalUpdatedUser)
            }
        } catch (_: Exception) {}

        repoScope.launch {
            try {
                if (finalUpdatedStudent != null) FirebaseRealtimeService.saveStudent(finalUpdatedStudent)
                if (finalUpdatedUser != null) FirebaseRealtimeService.saveUser(finalUpdatedUser)
            } catch (_: Exception) {}
        }

        return Result.success(Unit)
    }

    override suspend fun assignMultipleRanksToStudent(
        studentId: String,
        ranks: List<String>,
        colors: List<String>,
        permissions: List<String>
    ): Result<Unit> {
        val targetStudent = _students.value.firstOrNull { it.id == studentId }
        val targetUser = _users.value.firstOrNull { it.id == studentId }
        val isStaff = targetUser != null && targetUser.role != UserRole.STUDENT && targetUser.role != UserRole.PARENT
        val primaryRank = ranks.firstOrNull().orEmpty().let {
            if (it.isBlank() && !isStaff) "دانش‌آموز" else it
        }
        val extraRanks = ranks.drop(1)
        val primaryColor = colors.firstOrNull().orEmpty().ifBlank { "#64748B" }
        val extraColors = colors.drop(1)
        val normalizedPrimaryColor = if (primaryColor.startsWith("#")) primaryColor else "#$primaryColor"
        val normalizedExtraColors = extraColors.map { if (it.startsWith("#")) it else "#$it" }

        val targetCode = targetStudent?.studentCode.orEmpty()

        _students.value = _students.value.map { student ->
            if (student.id == studentId || (targetCode.isNotBlank() && student.studentCode == targetCode)) {
                student.copy(
                    rankTitle = primaryRank,
                    rankColorHex = normalizedPrimaryColor,
                    extraRanks = extraRanks,
                    extraRankColors = normalizedExtraColors,
                    permissions = permissions
                )
            } else student
        }

        val colorIntLong = try {
            android.graphics.Color.parseColor(normalizedPrimaryColor).toLong() and 0xFFFFFFFFL
        } catch (_: Exception) {
            0xFF64748BL
        }

        _users.value = _users.value.map { user ->
            if (user.id == studentId || (targetCode.isNotBlank() && user.studentCode == targetCode)) {
                user.copy(
                    rankTitle = primaryRank,
                    rankColorHex = normalizedPrimaryColor,
                    rankBadgeColorHex = colorIntLong,
                    extraRanks = extraRanks,
                    extraRankColors = normalizedExtraColors,
                    permissions = permissions
                )
            } else user
        }

        val updatedStudent = _students.value.firstOrNull { it.id == studentId || (targetCode.isNotBlank() && it.studentCode == targetCode) }
        val updatedUser = _users.value.firstOrNull { it.id == studentId || (targetCode.isNotBlank() && it.studentCode == targetCode) }
            ?: updatedStudent?.toUser()

        val curr = _currentUser.value
        if (curr != null && (curr.id == studentId || (targetCode.isNotBlank() && curr.studentCode == targetCode))) {
            val updatedCurr = curr.copy(
                rankTitle = primaryRank,
                rankColorHex = normalizedPrimaryColor,
                rankBadgeColorHex = colorIntLong,
                extraRanks = extraRanks,
                extraRankColors = normalizedExtraColors,
                permissions = permissions
            )
            _currentUser.value = updatedCurr
            SessionManager.saveUser(updatedCurr)
        }

        repoScope.launch {
            try {
                if (updatedStudent != null) FirebaseRealtimeService.saveStudent(updatedStudent)
                if (updatedUser != null) FirebaseRealtimeService.saveUser(updatedUser)
            } catch (_: Exception) {}
        }

        return Result.success(Unit)
    }

    override fun getCustomRanks(): Flow<List<CustomRank>> = _customRanks

    override suspend fun saveCustomRank(rank: CustomRank): Result<Unit> {
        val normalized = rank.copy(
            colorHex = if (rank.colorHex.startsWith("#")) rank.colorHex else "#${rank.colorHex}"
        )
        val existingIndex = _customRanks.value.indexOfFirst { it.id == normalized.id }
        val updated = if (existingIndex >= 0) {
            _customRanks.value.toMutableList().apply { set(existingIndex, normalized) }
        } else {
            listOf(normalized) + _customRanks.value
        }
        _customRanks.value = updated

        repoScope.launch {
            FirebaseRealtimeService.saveCustomRank(normalized)
        }
        return Result.success(Unit)
    }

    override suspend fun deleteCustomRank(rankId: String): Result<Unit> {
        _customRanks.value = _customRanks.value.filter { it.id != rankId }
        repoScope.launch {
            FirebaseRealtimeService.deleteCustomRank(rankId)
        }
        return Result.success(Unit)
    }

    override suspend fun updateStudentFaceRegistration(
        studentId: String,
        frontUrl: String?,
        leftUrl: String?,
        rightUrl: String?
    ): Result<Unit> {
        _students.value = _students.value.map { student ->
            if (student.id == studentId) {
                student.copy(
                    faceRegistered = true,
                    faceFrontUrl = frontUrl,
                    faceLeftUrl = leftUrl,
                    faceRightUrl = rightUrl,
                    profileImage = frontUrl ?: student.profileImage
                )
            } else student
        }

        _users.value = _users.value.map { user ->
            if (user.id == studentId) {
                user.copy(
                    faceRegistered = true,
                    faceFrontUrl = frontUrl,
                    faceLeftUrl = leftUrl,
                    faceRightUrl = rightUrl,
                    profileImage = frontUrl ?: user.profileImage
                )
            } else user
        }

        val curr = _currentUser.value
        if (curr != null && curr.id == studentId) {
            val updatedCurr = curr.copy(
                faceRegistered = true,
                faceFrontUrl = frontUrl,
                faceLeftUrl = leftUrl,
                faceRightUrl = rightUrl,
                profileImage = frontUrl ?: curr.profileImage
            )
            _currentUser.value = updatedCurr
            SessionManager.saveUser(updatedCurr)
        }

        val updatedStudent = _students.value.firstOrNull { it.id == studentId }
        val updatedUser = _users.value.firstOrNull { it.id == studentId }

        repoScope.launch {
            try {
                if (updatedStudent != null) FirebaseRealtimeService.saveStudent(updatedStudent)
                if (updatedUser != null) FirebaseRealtimeService.saveUser(updatedUser)
            } catch (_: Exception) {}
        }

        return Result.success(Unit)
    }

    // --- BehaviorRepository ---
    override fun getAllRecords(): Flow<List<BehaviorRecord>> = _records

    override fun getRecordsForStudent(studentId: String): Flow<List<BehaviorRecord>> =
        _records.map { list -> list.filter { it.studentId == studentId } }

    override suspend fun deleteRecord(recordId: String): Result<Unit> {
        val updated = _records.value.filter { it.id != recordId }
        _records.value = updated
        repoScope.launch {
            FirebaseRealtimeService.deleteRecord(recordId)
            syncWithServer()
        }
        return Result.success(Unit)
    }

    override suspend fun addRecord(record: BehaviorRecord): Result<Unit> {
        // Add record to list
        val updatedRecords = _records.value.toMutableList().apply { add(0, record) }
        _records.value = updatedRecords

        // Asynchronously save to Firebase Realtime Database
        repoScope.launch {
            FirebaseRealtimeService.saveRecord(record)
            syncWithServer()
        }

        // Update student counters
        _students.value = _students.value.map { student ->
            if (student.id == record.studentId) {
                var pos = student.positiveCount
                var ach = student.achievementCount
                var wrn = student.warningCount
                var dsc = student.disciplinaryCount
                var score = student.score

                when (record.type) {
                    RecordType.POSITIVE -> {
                        pos++
                        score += (record.points ?: 5)
                    }
                    RecordType.ACHIEVEMENT -> {
                        ach++
                        score += (record.points ?: 15)
                    }
                    RecordType.REWARD -> {
                        pos++
                        score += (record.points ?: 10)
                    }
                    RecordType.WARNING -> {
                        wrn++
                        score -= 2
                    }
                    RecordType.DISCIPLINARY -> {
                        dsc++
                        score -= (record.points?.let { -it } ?: 5)
                    }
                }
                student.copy(
                    positiveCount = pos,
                    achievementCount = ach,
                    warningCount = wrn,
                    disciplinaryCount = dsc,
                    score = score.coerceIn(0, 100)
                )
            } else {
                student
            }
        }

        // Generate notifications: always deliver to student account; deliver to parents ONLY if both father & mother logged in
        val targetStudent = _students.value.firstOrNull { it.id == record.studentId }
        val studentCode = targetStudent?.studentCode.orEmpty()

        val notifTitle = if (record.type.isPositive) {
            "ثبت رفتار مثبت جدید"
        } else {
            "ثبت مورد انضباطی جدید"
        }

        val notifBody = if (record.type.isPositive) {
            "برای ${record.studentName}: ${record.title} ثبت شد."
        } else {
            "برای ${record.studentName} یک مورد انضباطی (${record.category}) ثبت گردید."
        }

        // Always notify the student on their own account
        val studentNotif = AppNotification(
            id = "notif_std_${UUID.randomUUID().toString().take(8)}",
            schoolId = record.schoolId,
            recipientUserId = record.studentId,
            title = notifTitle,
            body = notifBody,
            type = record.type,
            studentId = record.studentId,
            studentName = record.studentName,
            recordId = record.id,
            date = record.date,
            time = record.time,
            read = false
        )
        addNotification(studentNotif)

        // Notifications are sent to parents ONLY if both parents have logged in to the app
        if (record.parentNotified && studentCode.isNotBlank()) {
            val bothParentsIn = com.example.core.auth.ParentAccessManager.areBothParentsLoggedIn(
                studentCode = studentCode,
                allUsers = _users.value
            )

            if (bothParentsIn) {
                // Deliver to Father
                addNotification(
                    AppNotification(
                        id = "notif_dad_${UUID.randomUUID().toString().take(8)}",
                        schoolId = record.schoolId,
                        recipientUserId = "parent_${studentCode}_dad",
                        title = notifTitle,
                        body = notifBody,
                        type = record.type,
                        studentId = record.studentId,
                        studentName = record.studentName,
                        recordId = record.id,
                        date = record.date,
                        time = record.time,
                        read = false
                    )
                )
                // Deliver to Mother
                addNotification(
                    AppNotification(
                        id = "notif_mom_${UUID.randomUUID().toString().take(8)}",
                        schoolId = record.schoolId,
                        recipientUserId = "parent_${studentCode}_mom",
                        title = notifTitle,
                        body = notifBody,
                        type = record.type,
                        studentId = record.studentId,
                        studentName = record.studentName,
                        recordId = record.id,
                        date = record.date,
                        time = record.time,
                        read = false
                    )
                )
            }
        }

        return Result.success(Unit)
    }

    override fun getStats(): Flow<BehaviorStats> = _records.map { records ->
        val totalPositive = records.count { it.type == RecordType.POSITIVE }
        val totalAchievements = records.count { it.type == RecordType.ACHIEVEMENT || it.type == RecordType.REWARD }
        val totalWarnings = records.count { it.type == RecordType.WARNING }
        val totalDisciplinary = records.count { it.type == RecordType.DISCIPLINARY }
        val allPositive = totalPositive + totalAchievements
        val allNegative = totalWarnings + totalDisciplinary
        val total = allPositive + allNegative
        val ratio = if (total > 0) allPositive.toFloat() / total.toFloat() else 1f

        // Count students needing attention (score < 85 or disciplinaryCount > 0)
        val studentsNeedingAttention = _students.value.count { it.score < 85 || it.disciplinaryCount > 0 }

        BehaviorStats(
            totalStudents = _students.value.size,
            totalPositive = totalPositive,
            totalAchievements = totalAchievements,
            totalWarnings = totalWarnings,
            totalDisciplinary = totalDisciplinary,
            studentsNeedingAttention = studentsNeedingAttention,
            positiveRatio = ratio
        )
    }

    // --- ClassRepository ---
    override fun getClasses(): Flow<List<SchoolClass>> = _classes

    // --- NotificationRepository ---
    override fun getNotifications(recipientUserId: String?): Flow<List<AppNotification>> =
        _notifications.map { list ->
            if (recipientUserId == null) list
            else list.filter { it.recipientUserId == recipientUserId }
        }

    override suspend fun markAsRead(notificationId: String) {
        _notifications.value = _notifications.value.map {
            if (it.id == notificationId) it.copy(read = true) else it
        }
    }

    override suspend fun markAllAsRead() {
        _notifications.value = _notifications.value.map { it.copy(read = true) }
    }

    override suspend fun addNotification(notification: AppNotification) {
        val updated = _notifications.value.toMutableList().apply { add(0, notification) }
        _notifications.value = updated
    }
}
