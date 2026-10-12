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
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.BehaviorRepository
import com.example.domain.repository.ClassRepository
import com.example.domain.repository.NotificationRepository
import com.example.domain.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

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

    val students: StateFlow<List<Student>> = combine(
        studentRepository.getStudents(),
        searchQuery,
        selectedGrade,
        selectedClass
    ) { allStudents, query, grade, cls ->
        allStudents.filter { s ->
            val matchQuery = query.isBlank() ||
                s.fullName.contains(query, ignoreCase = true) ||
                s.studentCode.contains(query) ||
                s.className.contains(query, ignoreCase = true)
            val matchGrade = matchesGradeAndClassFilter(s, grade)
            val matchClass = cls == "همه" || cls == "همه کلاس‌ها" || s.className == cls
            matchQuery && matchGrade && matchClass
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun matchesGradeAndClassFilter(student: Student, targetGrade: String): Boolean {
        if (targetGrade == "همه" || targetGrade == "همه پایه‌ها") return true

        val sGrade = student.grade.lowercase()
        val sClass = student.className.lowercase()
        val combined = "$sGrade $sClass"

        return when (targetGrade) {
            "هفتم یک" -> {
                combined.contains("هفتم یک") || combined.contains("هفتم ۱") || combined.contains("هفتم 1") ||
                ((sGrade.contains("هفتم") || sGrade.contains("7")) &&
                 (sClass.contains("7/1") || sClass.contains("یک") || sClass.contains("۱") || sClass.contains("/1") || (sClass.contains("1") && !sClass.contains("10") && !sClass.contains("11") && !sClass.contains("12"))))
            }
            "هفتم دو" -> {
                combined.contains("هفتم دو") || combined.contains("هفتم ۲") || combined.contains("هفتم 2") ||
                ((sGrade.contains("هفتم") || sGrade.contains("7")) &&
                 (sClass.contains("7/2") || sClass.contains("دو") || sClass.contains("۲") || sClass.contains("/2") || sClass.contains("2")))
            }
            "هشتم یک" -> {
                combined.contains("هشتم یک") || combined.contains("هشتم ۱") || combined.contains("هشتم 1") ||
                ((sGrade.contains("هشتم") || sGrade.contains("8")) &&
                 (sClass.contains("8/1") || sClass.contains("یک") || sClass.contains("۱") || sClass.contains("/1") || sClass.contains("1")))
            }
            "هشتم دو" -> {
                combined.contains("هشتم دو") || combined.contains("هشتم ۲") || combined.contains("هشتم 2") ||
                ((sGrade.contains("هشتم") || sGrade.contains("8")) &&
                 (sClass.contains("8/2") || sClass.contains("دو") || sClass.contains("۲") || sClass.contains("/2") || sClass.contains("2")))
            }
            "نهم یک" -> {
                combined.contains("نهم یک") || combined.contains("نهم ۱") || combined.contains("نهم 1") ||
                ((sGrade.contains("نهم") || sGrade.contains("9")) &&
                 (sClass.contains("9/1") || sClass.contains("یک") || sClass.contains("۱") || sClass.contains("/1") || sClass.contains("1")))
            }
            "نهم دو" -> {
                combined.contains("نهم دو") || combined.contains("نهم ۲") || combined.contains("نهم 2") ||
                ((sGrade.contains("نهم") || sGrade.contains("9")) &&
                 (sClass.contains("9/2") || sClass.contains("دو") || sClass.contains("۲") || sClass.contains("/2") || sClass.contains("2")))
            }
            else -> sGrade == targetGrade.lowercase() || sClass == targetGrade.lowercase() || combined.contains(targetGrade.lowercase())
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

    fun submitPositiveRecord(
        creatorName: String,
        creatorRole: UserRole,
        onSuccess: () -> Unit
    ) {
        val student = students.value.firstOrNull { it.id == posStudentId.value }
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
                date = "۱۴۰۳/۰۷/۱۶",
                time = "۱۱:۴۵",
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

    fun submitDisciplinaryRecord(
        creatorName: String,
        creatorRole: UserRole,
        onSuccess: () -> Unit
    ) {
        val student = students.value.firstOrNull { it.id == discStudentId.value }
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
                date = "۱۴۰۳/۰۷/۱۶",
                time = "۱۲:۱۵",
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
        val trimmedPhone = parentPhone.trim()
        val trimmedPass = password.trim().ifBlank { "1234" }

        if (cleanF.isBlank() || trimmedCode.isBlank()) {
            operationState.value = OperationState.Error("لطفاً نام و کد ۶ رقمی دانش‌آموزی را وارد کنید.")
            return
        }
        if (trimmedCode.length != 6 || !trimmedCode.all { it.isDigit() }) {
            operationState.value = OperationState.Error("کد دانش‌آموزی باید یک عدد ۶ رقمی معتبر باشد.")
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
                permissions = emptyList()
            )
            studentRepository.addStudent(student)
            operationState.value = OperationState.Success("${student.fullName} با موفقیت در سامانه ثبت شد و امکان ورود دارد.")
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
}
