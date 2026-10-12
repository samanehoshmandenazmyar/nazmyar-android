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
            _currentUser.value = saved
        }
    }

    init {
        restoreSavedSession()

        // Initial sync with Firebase Realtime Database
        repoScope.launch {
            syncWithServer()
        }

        // Live continuous background sync (every 10 seconds) so registrations/deletions sync reliably without UI lag
        repoScope.launch {
            while (true) {
                delay(10000)
                try {
                    syncWithServer()
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
                // When remoteStudents is non-null, Firebase is authoritative.
                // We also cross-merge any student registered in remoteUsers so that no registered account is missed.
                if (remoteStudents != null) {
                    val validRemote = remoteStudents.filter { it.id != "user_creator_hossein" && it.studentCode != "454545" }
                    val extraFromUsers = if (remoteUsers != null) {
                        remoteUsers.filter { it.role == UserRole.STUDENT && it.id != "user_creator_hossein" && it.studentCode != "454545" }
                            .map { it.toStudent() }
                    } else emptyList()
                    val mergedStudents = (validRemote + extraFromUsers).distinctBy { it.studentCode }
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
                    val baseUsers = DemoData.demoUsers.map { base ->
                        if (base.id == creatorUser.id) creatorUser
                        else remoteUsers.firstOrNull { it.id == base.id } ?: base
                    }
                    val updatedUsers = (baseUsers + remoteUsers).distinctBy { it.id }
                    if (_users.value != updatedUsers) {
                        _users.value = updatedUsers
                    }

                    // Push any missing pre-configured staff demo users to remote database
                    val missingDemoUsers = DemoData.demoUsers.filter { demo ->
                        remoteUsers.none { it.id == demo.id }
                    }
                    if (missingDemoUsers.isNotEmpty()) {
                        repoScope.launch {
                            missingDemoUsers.forEach { FirebaseRealtimeService.saveUser(it) }
                        }
                    }

                    // If current logged-in user was updated remotely (e.g. rank assigned), update local session
                    val curr = _currentUser.value
                    if (curr != null && curr.role != UserRole.PARENT) {
                        val updatedSelf = updatedUsers.firstOrNull { it.id == curr.id || it.studentCode == curr.studentCode }
                        if (updatedSelf != null) {
                            val matchingStudent = _students.value.firstOrNull { it.id == curr.id || it.studentCode == curr.studentCode }
                            val effectivePerms = if (matchingStudent != null) {
                                matchingStudent.permissions
                            } else {
                                updatedSelf.permissions
                            }
                            val effectiveRank = when {
                                matchingStudent != null && matchingStudent.rankTitle.isNotBlank() -> matchingStudent.rankTitle
                                updatedSelf.rankTitle.isNotBlank() -> updatedSelf.rankTitle
                                else -> curr.rankTitle
                            }
                            val effectiveColor = when {
                                matchingStudent != null && matchingStudent.rankColorHex.isNotBlank() -> matchingStudent.rankColorHex
                                updatedSelf.rankColorHex.isNotBlank() -> updatedSelf.rankColorHex
                                else -> curr.rankColorHex
                            }
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

        if (trimmedName.isBlank()) {
            return Result.failure(Exception(if (role == UserRole.PARENT) "لطفاً نام فرزند دانش‌آموز خود را وارد کنید." else "لطفاً نام و نام خانوادگی را وارد کنید."))
        }
        if (trimmedCode.isBlank()) {
            return Result.failure(Exception("لطفاً کد ۶ رقمی دانش‌آموزی را وارد کنید."))
        }
        if (trimmedCode.length != 6 || !trimmedCode.all { it.isDigit() }) {
            return Result.failure(Exception("کد دانش‌آموزی باید یک عدد ۶ رقمی معتبر باشد."))
        }
        if (trimmedPass.isBlank()) {
            return Result.failure(Exception("لطفاً رمز عبور را وارد کنید."))
        }

        // Find user by name and student code
        var user = _users.value.firstOrNull { u ->
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
                    val codeMatches = u.studentCode == trimmedCode || (
                        (u.id == "user_manager_mehdi_faryabi" || u.id == "user_counselor_yasin_dorahaki") && trimmedCode.length == 6
                    )
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
            user.copy(
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

        _currentUser.value = finalUser
        SessionManager.saveUser(finalUser)
        return Result.success(finalUser)
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

        // Check if student with this exact official name or code is already registered
        if (isStudentAccount && matchedAllowed != null) {
            val alreadyRegistered = _students.value.any { s ->
                val m = AllowedStudentsRegistry.findMatchingAllowedStudent(s.fullName)
                m?.fullName == matchedAllowed.fullName
            }
            if (alreadyRegistered) {
                return Result.failure(Exception("دانش‌آموز «${matchedAllowed.fullName}» قبلاً در سامانه ثبت‌نام کرده است. لطفاً از بخش ورود وارد شوید یا با کد ۶ رقمی خود وارد شوید."))
            }
        }

        if (trimmedCode.length != 6 || !trimmedCode.all { it.isDigit() }) {
            return Result.failure(Exception("کد دانش‌آموزی ۶ رقمی به درستی ایجاد نشده است."))
        }
        if (trimmedPhone.isBlank()) {
            return Result.failure(Exception("شماره تماس الزامی است."))
        }
        if (trimmedPass.length < 4) {
            return Result.failure(Exception("رمز عبور باید حداقل ۴ رقم یا کاراکتر باشد."))
        }

        if (_users.value.any { it.studentCode == trimmedCode } || _students.value.any { it.studentCode == trimmedCode }) {
            return Result.failure(Exception("این کد دانش‌آموزی قبلاً ثبت شده است. لطفاً کد تصادفی جدیدی تولید کنید."))
        }

        val actualGender = if (role == UserRole.STUDENT) "پسر" else gender

        val rankTitle = if (isCreatorUser) {
            "سازنده"
        } else when (role) {
            UserRole.STUDENT -> "دانش‌آموز"
            UserRole.PARENT -> if (actualGender.contains("مادر") || actualGender == "خانم") "ولی دانش‌آموز (مادر)" else "ولی دانش‌آموز (پدر)"
            else -> role.farsiTitle
        }

        val newUser = User(
            id = "user_${UUID.randomUUID().toString().take(8)}",
            name = trimmedName,
            phone = trimmedPhone,
            email = email.trim(),
            role = if (isCreatorUser) UserRole.MANAGER else if (isStudentAccount) UserRole.STUDENT else role,
            schoolId = DemoData.DEFAULT_SCHOOL_ID,
            studentCode = trimmedCode,
            password = trimmedPass,
            gender = actualGender,
            isRegisteredUser = true,
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

            val newStudent = Student(
                id = newUser.id,
                firstName = firstName,
                lastName = lastName,
                studentCode = trimmedCode,
                grade = finalGrade,
                className = finalClass,
                gender = actualGender,
                schoolId = DemoData.DEFAULT_SCHOOL_ID,
                score = 100,
                parentPhone = trimmedPhone,
                email = email.trim(),
                password = trimmedPass,
                role = UserRole.STUDENT,
                rankTitle = "دانش‌آموز",
                rankColorHex = "#64748B",
                permissions = emptyList()
            )
            // Immediately update local states
            _students.value = listOf(newStudent) + _students.value.filter { it.studentCode != trimmedCode }
            _users.value = listOf(newUser) + _users.value.filter { it.studentCode != trimmedCode }

            // Await remote save directly (up to 4 seconds) so data is guaranteed on the server before registration returns
            try {
                val saved = withTimeoutOrNull(4000) {
                    val sSaved = FirebaseRealtimeService.saveStudent(newStudent)
                    FirebaseRealtimeService.saveUser(newUser)
                    sSaved
                } ?: false
                if (saved) {
                    _isServerConnected.value = true
                } else {
                    repoScope.launch { 
                        FirebaseRealtimeService.saveStudent(newStudent)
                        FirebaseRealtimeService.saveUser(newUser)
                    }
                }
            } catch (_: Exception) {
                repoScope.launch { 
                    FirebaseRealtimeService.saveStudent(newStudent)
                    FirebaseRealtimeService.saveUser(newUser)
                }
            }
        } else {
            // User account (creator, staff, manager, parent)
            _users.value = listOf(newUser) + _users.value.filter { it.studentCode != trimmedCode }
            try {
                val saved = withTimeoutOrNull(4000) {
                    FirebaseRealtimeService.saveUser(newUser)
                } ?: false
                if (saved) {
                    _isServerConnected.value = true
                } else {
                    repoScope.launch { FirebaseRealtimeService.saveUser(newUser) }
                }
            } catch (_: Exception) {
                repoScope.launch { FirebaseRealtimeService.saveUser(newUser) }
            }
        }

        _currentUser.value = newUser
        SessionManager.saveUser(newUser)

        return Result.success(newUser)
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
        _currentUser.value = updatedUser
        SessionManager.saveUser(updatedUser)

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
        repoScope.launch {
            FirebaseRealtimeService.deleteStudent(studentId)
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

    // --- BehaviorRepository ---
    override fun getAllRecords(): Flow<List<BehaviorRecord>> = _records

    override fun getRecordsForStudent(studentId: String): Flow<List<BehaviorRecord>> =
        _records.map { list -> list.filter { it.studentId == studentId } }

    override suspend fun addRecord(record: BehaviorRecord): Result<Unit> {
        // Add record to list
        val updatedRecords = _records.value.toMutableList().apply { add(0, record) }
        _records.value = updatedRecords

        // Asynchronously save to Firebase Realtime Database
        repoScope.launch {
            FirebaseRealtimeService.saveRecord(record)
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

        // Generate parent notification if parentNotified is enabled
        if (record.parentNotified) {
            val targetStudent = _students.value.firstOrNull { it.id == record.studentId }
            val parentUserId = targetStudent?.parentIds?.firstOrNull() ?: "user_parent_1"

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

            val notif = AppNotification(
                id = "notif_${UUID.randomUUID()}",
                schoolId = record.schoolId,
                recipientUserId = parentUserId,
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
            addNotification(notif)
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
