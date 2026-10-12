package com.example.domain.repository

import com.example.domain.model.AppNotification
import com.example.domain.model.BehaviorRecord
import com.example.domain.model.BehaviorStats
import com.example.domain.model.SchoolClass
import com.example.domain.model.Student
import com.example.domain.model.User
import com.example.domain.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val currentUser: StateFlow<User?>
    val allUsers: StateFlow<List<User>>
    val isServerConnected: StateFlow<Boolean>
    val serverUrl: String
    val appUpdateInfo: StateFlow<com.example.domain.model.AppUpdateInfo?>
    suspend fun checkForUpdates(): com.example.domain.model.AppUpdateInfo?
    suspend fun publishAppUpdate(updateInfo: com.example.domain.model.AppUpdateInfo): Result<Unit>
    suspend fun deleteAppUpdate(): Result<Unit>
    suspend fun syncWithServer(): Boolean
    suspend fun login(
        name: String,
        studentCode: String,
        password: String,
        role: UserRole = UserRole.STUDENT,
        parentRelation: String = "پدر دانش‌آموز"
    ): Result<User>
    suspend fun register(
        name: String,
        studentCode: String,
        phone: String,
        email: String,
        password: String,
        role: UserRole,
        gender: String,
        grade: String = "پایه هفتم",
        className: String = "کلاس 7/1"
    ): Result<User>
    suspend fun recoverAccount(emailOrPhone: String): Result<User>
    suspend fun logout()
    fun getSavedUsers(): List<User>
    suspend fun updateUserProfile(updatedUser: User): Result<User>
}

interface StudentRepository {
    fun getStudents(): Flow<List<Student>>
    fun getStudentById(id: String): Flow<Student?>
    fun getStudentsByClass(className: String): Flow<List<Student>>
    fun searchStudents(query: String, grade: String?, className: String?): Flow<List<Student>>
    suspend fun addStudent(student: Student): Result<Unit>
    suspend fun updateStudent(student: Student): Result<Unit>
    suspend fun deleteStudent(studentId: String): Result<Unit>
    suspend fun assignRankToStudent(
        studentId: String,
        rankTitle: String,
        colorHex: String,
        permissions: List<String>
    ): Result<Unit>
    suspend fun assignMultipleRanksToStudent(
        studentId: String,
        ranks: List<String>,
        colors: List<String>,
        permissions: List<String>
    ): Result<Unit>
    fun getCustomRanks(): Flow<List<com.example.domain.model.CustomRank>>
    suspend fun saveCustomRank(rank: com.example.domain.model.CustomRank): Result<Unit>
    suspend fun deleteCustomRank(rankId: String): Result<Unit>
}

interface BehaviorRepository {
    fun getAllRecords(): Flow<List<BehaviorRecord>>
    fun getRecordsForStudent(studentId: String): Flow<List<BehaviorRecord>>
    suspend fun addRecord(record: BehaviorRecord): Result<Unit>
    fun getStats(): Flow<BehaviorStats>
}

interface ClassRepository {
    fun getClasses(): Flow<List<SchoolClass>>
}

interface NotificationRepository {
    fun getNotifications(recipientUserId: String? = null): Flow<List<AppNotification>>
    suspend fun markAsRead(notificationId: String)
    suspend fun markAllAsRead()
    suspend fun addNotification(notification: AppNotification)
}
