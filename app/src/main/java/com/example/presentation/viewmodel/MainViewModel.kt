package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.AppContainer
import com.example.data.demo.DemoData
import com.example.domain.model.AppNotification
import com.example.domain.model.BehaviorRecord
import com.example.domain.model.BehaviorStats
import com.example.domain.model.CustomRank
import com.example.domain.model.RecordType
import com.example.domain.model.SchoolClass
import com.example.domain.model.SeverityLevel
import com.example.domain.model.Student
import com.example.domain.model.UserRole
import com.example.domain.model.toPersianDigits
import com.example.domain.model.sanitizePhoneNumber
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.BehaviorRepository
import com.example.domain.repository.ClassRepository
import com.example.domain.repository.NotificationRepository
import com.example.domain.repository.StudentRepository
import com.example.domain.model.matchesQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.random.Random

sealed class OperationState {
    data object Idle : OperationState()
    data object Loading : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}

class MainViewModel(
    private val studentRepository: StudentRepository = AppContainer.studentRepository,
    private val behaviorRepository: BehaviorRepository = AppContainer.behaviorRepository,
    private val classRepository: ClassRepository = AppContainer.classRepository,
    private val notificationRepository: NotificationRepository = AppContainer.notificationRepository,
    private val authRepository: AuthRepository = AppContainer.authRepository
) : ViewModel() {

    fun refreshData(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            authRepository.syncWithServer()
            onComplete?.invoke()
        }
    }

    // Students filter states
    val searchQuery = MutableStateFlow("")
    val selectedGrade = MutableStateFlow("همه")
    val selectedClass = MutableStateFlow("همه")

    val allStudents: StateFlow<List<Student>> = studentRepository.getStudents()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val students: StateFlow<List<Student>> = combine(
        studentRepository.getStudents(),
        searchQuery,
        selectedGrade,
        selectedClass
    ) { allStudents, query, grade, cls ->
        allStudents.filter { s ->
            val matchQuery = s.matchesQuery(query)
            val matchGrade = matchesGradeAndClassFilter(s, grade)
            val matchClass = cls == "همه" || cls == "همه کلاس‌ها" || s.className == cls
            matchQuery && matchGrade && matchClass
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        fun matchesGradeAndClassFilter(student: Student, targetGrade: String): Boolean {
            if (targetGrade == "همه" || targetGrade == "همه پایه‌ها" || targetGrade.isBlank()) return true

            val sGrade = student.grade.lowercase()
            val sClass = student.className.lowercase()
            val combined = "$sGrade $sClass"

            return when (targetGrade) {
                "هفتم", "پایه هفتم" -> sGrade.contains("هفتم") || sGrade.contains("7") || sGrade.contains("۷")
                "هشتم", "پایه هشتم" -> sGrade.contains("هشتم") || sGrade.contains("8") || sGrade.contains("۸")
                "نهم", "پایه نهم" -> sGrade.contains("نهم") || sGrade.contains("9") || sGrade.contains("۹")
                "هفتم یک" -> {
                    combined.contains("هفتم یک") || combined.contains("هفتم ۱") || combined.contains("هفتم 1") ||
                    ((sGrade.contains("هفتم") || sGrade.contains("7") || sGrade.contains("۷")) &&
                     (sClass.contains("7/1") || sClass.contains("۷/۱") || sClass.contains("یک") || sClass.contains("۱") || sClass.contains("/1") || (sClass.contains("1") && !sClass.contains("10") && !sClass.contains("11") && !sClass.contains("12"))))
                }
                "هفتم دو" -> {
                    combined.contains("هفتم دو") || combined.contains("هفتم ۲") || combined.contains("هفتم 2") ||
                    ((sGrade.contains("هفتم") || sGrade.contains("7") || sGrade.contains("۷")) &&
                     (sClass.contains("7/2") || sClass.contains("۷/۲") || sClass.contains("دو") || sClass.contains("۲") || sClass.contains("/2") || sClass.contains("2")))
                }
                "هشتم یک" -> {
                    combined.contains("هشتم یک") || combined.contains("هشتم ۱") || combined.contains("هشتم 1") ||
                    ((sGrade.contains("هشتم") || sGrade.contains("8") || sGrade.contains("۸")) &&
                     (sClass.contains("8/1") || sClass.contains("۸/۱") || sClass.contains("یک") || sClass.contains("۱") || sClass.contains("/1") || sClass.contains("1")))
                }
                "هشتم دو" -> {
                    combined.contains("هشتم دو") || combined.contains("هشتم ۲") || combined.contains("هشتم 2") ||
                    ((sGrade.contains("هشتم") || sGrade.contains("8") || sGrade.contains("۸")) &&
                     (sClass.contains("8/2") || sClass.contains("۸/۲") || sClass.contains("دو") || sClass.contains("۲") || sClass.contains("/2") || sClass.contains("2")))
                }
                "نهم یک" -> {
                    combined.contains("نهم یک") || combined.contains("نهم ۱") || combined.contains("نهم 1") ||
                    ((sGrade.contains("نهم") || sGrade.contains("9") || sGrade.contains("۹")) &&
                     (sClass.contains("9/1") || sClass.contains("۹/۱") || sClass.contains("یک") || sClass.contains("۱") || sClass.contains("/1") || sClass.contains("1")))
                }
                "نهم دو" -> {
                    combined.contains("نهم دو") || combined.contains("نهم ۲") || combined.contains("نهم 2") ||
                    ((sGrade.contains("نهم") || sGrade.contains("9") || sGrade.contains("۹")) &&
                     (sClass.contains("9/2") || sClass.contains("۹/۲") || sClass.contains("دو") || sClass.contains("۲") || sClass.contains("/2") || sClass.contains("2")))
                }
                else -> sGrade == targetGrade.lowercase() || sClass == targetGrade.lowercase() || combined.contains(targetGrade.lowercase())
            }
        }
    }

    val classes: StateFlow<List<SchoolClass>> = classRepository.getClasses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val records: StateFlow<List<BehaviorRecord>> = behaviorRepository.getAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stats: StateFlow<BehaviorStats> = behaviorRepository.getStats()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            BehaviorStats(10, 20, 10, 4, 3, 2, 0.82f)
        )

    val notifications: StateFlow<List<AppNotification>> = notificationRepository.getNotifications()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<com.example.domain.model.User>> = authRepository.allUsers
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.allUsers.value)

    private val _hiddenFromReportsRecordIds = MutableStateFlow<Set<String>>(emptySet())
    val hiddenFromReportsRecordIds: StateFlow<Set<String>> = _hiddenFromReportsRecordIds.asStateFlow()

    fun hideRecordFromReports(recordId: String) {
        _hiddenFromReportsRecordIds.value = _hiddenFromReportsRecordIds.value + recordId
    }

    val operationState = MutableStateFlow<OperationState>(OperationState.Idle)

    // Positive Registration Form State
    val posStudentId = MutableStateFlow("")
    val posCategory = MutableStateFlow("موفقیت تحصیلی")
    val posTitle = MutableStateFlow("")
    val posDescription = MutableStateFlow("")
    val posPoints = MutableStateFlow(10)
    val posNotifyParent = MutableStateFlow(true)

    // Disciplinary Registration Form State
    val discStudentId = MutableStateFlow("")
    val discCategory = MutableStateFlow("بی‌نظمی")
    val discTitle = MutableStateFlow("")
    val discDescription = MutableStateFlow("")
    val discLocation = MutableStateFlow("کلاس درس")
    val discSeverity = MutableStateFlow(SeverityLevel.MEDIUM)
    val discPoints = MutableStateFlow(-5)
    val discNotifyParent = MutableStateFlow(true)

    fun resetOperationState() {
        operationState.value = OperationState.Idle
    }

    fun notifyBothParentsUnlocked(studentCode: String, studentName: String, studentId: String, schoolId: String) {
        viewModelScope.launch {
            val notifs = com.example.core.auth.ParentAccessManager.createBothParentsUnlockedNotifications(
                studentCode = studentCode,
                studentName = studentName,
                studentId = studentId,
                schoolId = schoolId
            )
            notifs.forEach { notificationRepository.addNotification(it) }
        }
    }

    fun submitPositiveRecord(
        creatorName: String,
        creatorRole: UserRole,
        onSuccess: () -> Unit
    ) {
        val student = allStudents.value.firstOrNull { it.id == posStudentId.value }
            ?: students.value.firstOrNull { it.id == posStudentId.value }
        if (student == null) {
            operationState.value = OperationState.Error("لطفاً دانش‌آموز مورد نظر را انتخاب نمایید.")
            return
        }
        if (posTitle.value.trim().length < 3) {
            operationState.value = OperationState.Error("عنوان رفتار مثبت باید حداقل ۳ حرف باشد.")
            return
        }

        viewModelScope.launch {
            operationState.value = OperationState.Loading
            val record = BehaviorRecord(
                id = "rec_${UUID.randomUUID()}",
                studentId = student.id,
                studentName = student.fullName,
                studentGrade = student.grade,
                studentClass = student.className,
                schoolId = student.schoolId,
                type = if (posCategory.value.contains("موفقیت")) RecordType.ACHIEVEMENT else RecordType.POSITIVE,
                category = posCategory.value,
                title = posTitle.value.trim(),
                description = posDescription.value.trim(),
                date = com.example.core.utils.DateTimeUtils.getCurrentPersianDate(),
                time = com.example.core.utils.DateTimeUtils.getCurrentTime(),
                points = posPoints.value,
                createdBy = creatorName,
                createdByRole = creatorRole,
                parentNotified = posNotifyParent.value
            )

            behaviorRepository.addRecord(record)
            operationState.value = OperationState.Success("رفتار مثبت برای ${student.fullName} با موفقیت ثبت و به اولیاء ابلاغ شد.")
            // Clear form
            posTitle.value = ""
            posDescription.value = ""
            onSuccess()
        }
    }

    fun deleteBehaviorRecord(recordId: String) {
        viewModelScope.launch {
            behaviorRepository.deleteRecord(recordId)
        }
    }

    fun submitDisciplinaryRecord(
        creatorName: String,
        creatorRole: UserRole,
        onSuccess: () -> Unit
    ) {
        val student = allStudents.value.firstOrNull { it.id == discStudentId.value }
            ?: students.value.firstOrNull { it.id == discStudentId.value }
        if (student == null) {
            operationState.value = OperationState.Error("لطفاً دانش‌آموز مورد نظر را انتخاب نمایید.")
            return
        }
        if (discTitle.value.trim().length < 3) {
            operationState.value = OperationState.Error("عنوان مورد انضباطی باید حداقل ۳ حرف باشد.")
            return
        }

        viewModelScope.launch {
            operationState.value = OperationState.Loading
            val isWarning = discSeverity.value == SeverityLevel.LOW
            val record = BehaviorRecord(
                id = "rec_${UUID.randomUUID()}",
                studentId = student.id,
                studentName = student.fullName,
                studentGrade = student.grade,
                studentClass = student.className,
                schoolId = student.schoolId,
                type = if (isWarning) RecordType.WARNING else RecordType.DISCIPLINARY,
                category = discCategory.value,
                title = discTitle.value.trim(),
                description = discDescription.value.trim(),
                date = com.example.core.utils.DateTimeUtils.getCurrentPersianDate(),
                time = com.example.core.utils.DateTimeUtils.getCurrentTime(),
                location = discLocation.value.trim(),
                severity = discSeverity.value,
                points = discPoints.value,
                createdBy = creatorName,
                createdByRole = creatorRole,
                parentNotified = discNotifyParent.value
            )

            behaviorRepository.addRecord(record)
            val msg = if (isWarning) "تذکر انضباطی با موفقیت ثبت شد." else "مورد انضباطی ثبت و به اطلاع معاونت و اولیاء رسید."
            operationState.value = OperationState.Success(msg)
            // Clear form
            discTitle.value = ""
            discDescription.value = ""
            onSuccess()
        }
    }

    fun registerBehaviorRecord(
        studentId: String,
        type: RecordType,
        category: String,
        title: String,
        description: String,
        date: String = com.example.core.utils.DateTimeUtils.getCurrentPersianDate(),
        time: String = com.example.core.utils.DateTimeUtils.getCurrentTime(),
        location: String = "آموزشگاه",
        severity: SeverityLevel = SeverityLevel.MEDIUM,
        points: Int = if (type.isPositive) 2 else 1,
        createdBy: String = "معاونت مدرسه",
        createdByRole: UserRole = UserRole.VICE_PRINCIPAL,
        parentNotified: Boolean = true,
        imageUrl: String? = null
    ) {
        val student = allStudents.value.firstOrNull { it.id == studentId } 
            ?: students.value.firstOrNull { it.id == studentId } 
            ?: return
        viewModelScope.launch {
            val record = BehaviorRecord(
                id = "rec_${UUID.randomUUID()}",
                studentId = student.id,
                studentName = student.fullName,
                studentGrade = student.grade,
                studentClass = student.className,
                schoolId = student.schoolId,
                type = type,
                category = category,
                title = title.trim(),
                description = description.trim(),
                date = date,
                time = time,
                location = location,
                severity = severity,
                points = points,
                createdBy = createdBy,
                createdByRole = createdByRole,
                parentNotified = parentNotified,
                imageUrl = imageUrl
            )
            behaviorRepository.addRecord(record)

            try {
                val ctx = AppContainer.appContext
                com.example.core.notification.NotificationHelper.showBehaviorNotification(
                    context = ctx,
                    studentName = student.fullName,
                    behaviorTitle = title.trim(),
                    isPositive = type.isPositive
                )
            } catch (_: Exception) {}
        }
    }

    fun registerPrayerRecord(
        studentId: String,
        prayerTime: String,
        positiveCount: Int,
        negativeCount: Int,
        description: String,
        date: String = com.example.core.utils.DateTimeUtils.getCurrentPersianDate(),
        time: String = com.example.core.utils.DateTimeUtils.getCurrentTime(),
        createdBy: String = "مسئول اقامه نماز",
        createdByRole: UserRole = UserRole.VICE_PRINCIPAL,
        parentNotified: Boolean = true,
        imageUrl: String? = null
    ) {
        val net = positiveCount - negativeCount
        val type = when {
            net > 0 -> RecordType.POSITIVE
            net < 0 -> RecordType.DISCIPLINARY
            else -> RecordType.POSITIVE
        }
        val points = kotlin.math.abs(net)
        val netSummary = when {
            net > 0 -> "+$net مثبت"
            net < 0 -> "-${kotlin.math.abs(net)} منفی"
            else -> "خنثی (بدون تغییر امتیاز)"
        }
        val fullTitle = "اقامه نماز ($prayerTime)"
        val fullDesc = buildString {
            val userText = description.trim()
            if (userText.isNotBlank()) {
                append(userText)
                append("\n")
            }
            append("وعده نماز: $prayerTime | ارزیابی: $positiveCount مثبت، $negativeCount منفی | نتیجه اعمال‌شده: $netSummary")
        }

        registerBehaviorRecord(
            studentId = studentId,
            type = type,
            category = "نماز و فرائض دینی",
            title = fullTitle,
            description = fullDesc,
            date = date,
            time = time,
            location = "نمازخانه آموزشگاه",
            severity = SeverityLevel.LOW,
            points = points,
            createdBy = createdBy,
            createdByRole = createdByRole,
            parentNotified = parentNotified,
            imageUrl = imageUrl
        )
    }

    fun updateStudentFaceRegistration(
        studentId: String,
        frontUrl: String?,
        leftUrl: String?,
        rightUrl: String?
    ) {
        viewModelScope.launch {
            studentRepository.updateStudentFaceRegistration(studentId, frontUrl, leftUrl, rightUrl)
        }
    }

    fun addNewStudent(
        firstName: String,
        lastName: String,
        grade: String,
        className: String,
        studentCode: String,
        parentPhone: String,
        email: String = "",
        password: String = "1234",
        gender: String = "پسر",
        onComplete: () -> Unit
    ) {
        val cleanF = firstName.trim().replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
        val cleanL = lastName.trim().replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
        val trimmedCode = studentCode.trim()
        val trimmedPhone = parentPhone.sanitizePhoneNumber()
        val trimmedPass = password.trim().ifBlank { "1234" }

        if (cleanF.isBlank() || trimmedCode.isBlank()) {
            operationState.value = OperationState.Error("لطفاً نام و کد ۶ رقمی دانش‌آموزی را وارد کنید.")
            return
        }
        if (trimmedCode.length != 6 || !trimmedCode.all { it.isDigit() }) {
            operationState.value = OperationState.Error("کد دانش‌آموزی باید یک عدد ۶ رقمی معتبر باشد.")
            return
        }

        val candidateFullName = "$cleanF $cleanL".trim()
        val candidateNorm = com.example.data.demo.AllowedStudentsRegistry.normalize(candidateFullName)
        val duplicate = allStudents.value.any { s ->
            s.isRegistered && com.example.data.demo.AllowedStudentsRegistry.normalize(s.fullName) == candidateNorm
        }
        if (duplicate) {
            operationState.value = OperationState.Error("دانش‌آموزی با نام «$candidateFullName» قبلاً در سامانه ثبت شده است. داشتن دو حساب با یک نام مجاز نیست.")
            return
        }

        viewModelScope.launch {
            val student = Student(
                id = "std_${UUID.randomUUID().toString().take(8)}",
                firstName = cleanF,
                lastName = cleanL,
                studentCode = trimmedCode,
                grade = grade,
                className = className,
                gender = gender,
                schoolId = DemoData.DEFAULT_SCHOOL_ID,
                score = 100,
                parentPhone = trimmedPhone,
                email = email.trim(),
                password = trimmedPass,
                role = UserRole.STUDENT,
                rankTitle = "دانش‌آموز",
                rankColorHex = "#64748B",
                permissions = emptyList(),
                isRegistered = true
            )
            studentRepository.addStudent(student)
            operationState.value = OperationState.Success("${student.fullName} با موفقیت در سامانه ثبت شد و امکان ورود دارد.")
            onComplete()
        }
    }

    fun addPendingStudent(
        firstName: String,
        lastName: String,
        grade: String,
        className: String,
        onComplete: () -> Unit
    ) {
        val cleanF = firstName.trim().replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
        val cleanL = lastName.trim().replace("دانش‌آموز", "").replace("دانش آموز", "").trim()
        val generatedCode = Random.nextInt(100000, 999999).toString()

        if (cleanF.isBlank()) {
            operationState.value = OperationState.Error("لطفاً نام دانش‌آموز را وارد کنید.")
            return
        }

        val candidateFullName = "$cleanF $cleanL".trim()
        val candidateNorm = com.example.data.demo.AllowedStudentsRegistry.normalize(candidateFullName)
        val duplicate = allStudents.value.any { s ->
            com.example.data.demo.AllowedStudentsRegistry.normalize(s.fullName) == candidateNorm
        }
        if (duplicate) {
            operationState.value = OperationState.Error("دانش‌آموزی با نام «$candidateFullName» قبلاً در سامانه موجود است.")
            return
        }

        viewModelScope.launch {
            val student = Student(
                id = "std_${UUID.randomUUID().toString().take(8)}",
                firstName = cleanF,
                lastName = cleanL,
                studentCode = generatedCode,
                grade = grade,
                className = className,
                gender = "پسر",
                schoolId = DemoData.DEFAULT_SCHOOL_ID,
                score = 100,
                parentPhone = "09${Random.nextInt(100000000, 999999999)}",
                email = "",
                password = "1234",
                role = UserRole.STUDENT,
                rankTitle = "دانش‌آموز",
                rankColorHex = "#64748B",
                permissions = emptyList(),
                isRegistered = false,
                hasLoggedIn = false,
                addedByStaff = true
            )
            studentRepository.addStudent(student)
            operationState.value = OperationState.Success("${student.fullName} با موفقیت به عنوان دانش‌آموز (در انتظار ورود) به لیست اضافه شد.")
            onComplete()
        }
    }

    fun deleteStudent(studentId: String, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            studentRepository.deleteStudent(studentId)
            operationState.value = OperationState.Success("دانش‌آموز با موفقیت حذف شد.")
            onComplete?.invoke()
        }
    }

    val customRanks: StateFlow<List<CustomRank>> = studentRepository.getCustomRanks()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun assignRankToStudent(
        studentId: String,
        rankTitle: String,
        colorHex: String,
        permissions: List<String>,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = studentRepository.assignRankToStudent(studentId, rankTitle, colorHex, permissions)
            onComplete?.invoke(result.isSuccess)
        }
    }

    fun assignMultipleRanksToStudent(
        studentId: String,
        ranks: List<String>,
        colors: List<String>,
        permissions: List<String>,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = studentRepository.assignMultipleRanksToStudent(studentId, ranks, colors, permissions)
            onComplete?.invoke(result.isSuccess)
        }
    }

    fun saveCustomRank(
        rank: CustomRank,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = studentRepository.saveCustomRank(rank)
            onComplete?.invoke(result.isSuccess)
        }
    }

    fun deleteCustomRank(
        rankId: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = studentRepository.deleteCustomRank(rankId)
            onComplete?.invoke(result.isSuccess)
        }
    }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            notificationRepository.markAllAsRead()
        }
    }

    fun addNotification(notification: AppNotification) {
        viewModelScope.launch {
            notificationRepository.addNotification(notification)
        }
    }
}
