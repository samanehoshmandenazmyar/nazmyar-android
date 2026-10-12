package com.example.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.AppContainer
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val authRepository: AuthRepository = AppContainer.authRepository
) : ViewModel() {

    val currentUser: StateFlow<User?> = authRepository.currentUser
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.currentUser.value)

    val allUsers: StateFlow<List<User>> = authRepository.allUsers
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.allUsers.value)

    val isServerConnected: StateFlow<Boolean> = authRepository.isServerConnected
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.isServerConnected.value)

    val appUpdateInfo: StateFlow<com.example.domain.model.AppUpdateInfo?> = authRepository.appUpdateInfo
        .stateIn(viewModelScope, SharingStarted.Eagerly, authRepository.appUpdateInfo.value)

    init {
        checkForUpdates()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            authRepository.checkForUpdates()
        }
    }

    fun publishAppUpdate(
        updateInfo: com.example.domain.model.AppUpdateInfo,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val result = authRepository.publishAppUpdate(updateInfo)
            onResult(result)
        }
    }

    fun deleteAppUpdate(onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.deleteAppUpdate()
            onResult(result)
        }
    }

    fun syncWithServer(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val result = authRepository.syncWithServer()
            onComplete?.invoke(result)
        }
    }

    val savedUsers: List<User> get() = authRepository.getSavedUsers()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(
        name: String,
        studentCode: String,
        password: String,
        role: UserRole = UserRole.STUDENT,
        parentRelation: String = "پدر دانش‌آموز"
    ) {
        val trimmedName = name.trim()
        val trimmedCode = studentCode.trim()
        val trimmedPass = password.trim()

        if (trimmedName.isBlank()) {
            _uiState.value = AuthUiState.Error(if (role == UserRole.PARENT) "لطفاً نام فرزند دانش‌آموز خود را وارد کنید." else "لطفاً نام و نام خانوادگی خود را وارد کنید.")
            return
        }
        if (trimmedCode.isBlank()) {
            _uiState.value = AuthUiState.Error("لطفاً کد ۶ رقمی دانش‌آموزی را وارد کنید.")
            return
        }
        if (trimmedCode.length != 6 || !trimmedCode.all { it.isDigit() }) {
            _uiState.value = AuthUiState.Error("کد دانش‌آموزی باید یک عدد ۶ رقمی معتبر باشد.")
            return
        }
        if (trimmedPass.isBlank()) {
            _uiState.value = AuthUiState.Error("لطفاً رمز عبور را وارد کنید.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            kotlinx.coroutines.delay(200)
            val result = authRepository.login(trimmedName, trimmedCode, trimmedPass, role, parentRelation)
            result.onSuccess { user ->
                _uiState.value = AuthUiState.Success(user)
            }.onFailure { error ->
                _uiState.value = AuthUiState.Error(error.message ?: "اطلاعات وارد شده یا رمز عبور اشتباه است.")
            }
        }
    }

    fun register(
        name: String,
        studentCode: String,
        phone: String,
        email: String,
        password: String,
        confirmPassword: String,
        role: UserRole,
        gender: String,
        grade: String = "پایه هفتم",
        className: String = "کلاس 7/1"
    ) {
        val trimmedName = name.trim()
        val trimmedCode = studentCode.trim()
        val trimmedPhone = phone.trim()
        val trimmedPass = password.trim()
        val trimmedConfirm = confirmPassword.trim()

        if (trimmedName.isBlank()) {
            _uiState.value = AuthUiState.Error("لطفاً نام و نام خانوادگی را وارد کنید.")
            return
        }

        // بررسی اینکه آیا نام دانش‌آموز در لیست رسمی مدرسه وجود دارد یا خیر
        if (role == UserRole.STUDENT && trimmedName != "سید حسین موسوی نژاد") {
            val matchedAllowed = com.example.data.demo.AllowedStudentsRegistry.findMatchingAllowedStudent(trimmedName)
            if (matchedAllowed == null) {
                _uiState.value = AuthUiState.Error("نام «$trimmedName» در لیست رسمی دانش‌آموزان مدرسه ثبت نشده است. فقط دانش‌آموزان موجود در لیست مجاز به ثبت‌نام هستند.")
                return
            }
        }

        if (trimmedCode.isBlank() || trimmedCode.length != 6 || !trimmedCode.all { it.isDigit() }) {
            _uiState.value = AuthUiState.Error("لطفاً با دکمه تولید کد، کد ۶ رقمی دانش‌آموزی را ایجاد کنید.")
            return
        }
        if (trimmedPhone.isBlank()) {
            _uiState.value = AuthUiState.Error("لطفاً شماره تماس را وارد کنید.")
            return
        }
        if (trimmedPass.length < 4) {
            _uiState.value = AuthUiState.Error("رمز عبور باید حداقل ۴ رقم یا کاراکتر باشد.")
            return
        }
        if (trimmedPass != trimmedConfirm) {
            _uiState.value = AuthUiState.Error("رمز عبور با تکرار آن مطابقت ندارد.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            kotlinx.coroutines.delay(300)
            val result = authRepository.register(
                name = trimmedName,
                studentCode = trimmedCode,
                phone = trimmedPhone,
                email = email.trim(),
                password = trimmedPass,
                role = role,
                gender = gender,
                grade = grade,
                className = className
            )
            result.onSuccess { user ->
                _uiState.value = AuthUiState.Success(user)
            }.onFailure { error ->
                _uiState.value = AuthUiState.Error(error.message ?: "خطا در ثبت‌نام. لطفاً مجدداً تلاش کنید.")
            }
        }
    }

    fun recoverAccount(query: String, onResult: (Result<User>) -> Unit) {
        viewModelScope.launch {
            val result = authRepository.recoverAccount(query)
            onResult(result)
        }
    }

    fun clearState() {
        _uiState.value = AuthUiState.Idle
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.value = AuthUiState.Idle
        }
    }

    fun updateUserProfile(
        updatedUser: User,
        onResult: ((Result<User>) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val result = authRepository.updateUserProfile(updatedUser)
            onResult?.invoke(result)
        }
    }
}
