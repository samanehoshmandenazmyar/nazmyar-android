package com.example.core.messenger

import android.content.Context
import android.util.Log
import com.example.R
import com.example.domain.model.AttendanceCardData
import com.example.domain.model.Attendee
import com.example.domain.model.ChatMessage
import com.example.domain.model.ClassGroup
import com.example.domain.model.MessageStatus
import com.example.domain.model.PersianDateHelper
import com.example.domain.model.User
import com.example.domain.model.UserRole
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object ClassMessengerManager {
    private const val TAG = "ClassMessengerManager"

    private var firestore: FirebaseFirestore? = null
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    // Dynamic groups loaded from Firestore (starts empty by default)
    val FIXED_GROUPS = emptyList<ClassGroup>()

    private val _allGroups = MutableStateFlow<List<ClassGroup>>(emptyList())
    val allGroups: StateFlow<List<ClassGroup>> = _allGroups.asStateFlow()

    private val _groupMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val groupMessages: StateFlow<Map<String, List<ChatMessage>>> = _groupMessages.asStateFlow()

    fun init(context: Context) {
        try {
            val dbId = context.applicationContext.getString(R.string.firestore_database_id)
            firestore = FirebaseFirestore.getInstance(dbId)
            setupFirestoreRealtimeListeners()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firestore: ${e.message}", e)
        }
    }

    private fun setupFirestoreRealtimeListeners() {
        val db = firestore ?: return

        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()

        // Listen to dynamic groups created in Firestore
        val groupsReg = db.collection("class_groups")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Groups listen failed: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val groupsList = snapshot.documents.mapNotNull { doc ->
                        try {
                            ClassGroup(
                                id = doc.getString("id") ?: doc.id,
                                title = doc.getString("title") ?: "گروه بدون نام",
                                gradeCode = doc.getString("gradeCode") ?: "",
                                gradeName = doc.getString("gradeName") ?: "کلاس",
                                avatarColorHex = doc.getLong("avatarColorHex") ?: 0xFF1976D2,
                                memberCount = (doc.getLong("memberCount") ?: 0L).toInt(),
                                description = doc.getString("description") ?: "",
                                isOfficialChannel = doc.getBoolean("isOfficialChannel") ?: false,
                                isPermanent = doc.getBoolean("isPermanent") ?: false
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Error parsing group doc ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    _allGroups.value = groupsList

                    // Attach message listeners to all dynamic groups
                    for (group in groupsList) {
                        listenToGroupMessages(group)
                    }
                }
            }
        listenerRegistrations.add(groupsReg)
    }

    private fun listenToGroupMessages(group: ClassGroup) {
        val db = firestore ?: return
        val reg = db.collection("class_groups")
            .document(group.id)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .limitToLast(60)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener
                if (snapshot != null) {
                    val messagesList = snapshot.documents.mapNotNull { doc ->
                        try {
                            val id = doc.getString("id") ?: doc.id
                            val classGroupId = doc.getString("classGroupId") ?: group.id
                            val senderId = doc.getString("senderId") ?: ""
                            val senderName = doc.getString("senderName") ?: ""
                            val senderRole = doc.getString("senderRole") ?: "دانش‌آموز"
                            val text = doc.getString("text") ?: ""
                            val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                            val timeFormatted = doc.getString("timeFormatted") ?: ""
                            val isAttendanceCard = doc.getBoolean("isAttendanceCard") ?: false

                            var attendanceData: AttendanceCardData? = null
                            if (isAttendanceCard) {
                                val attMap = doc.get("attendanceData") as? Map<*, *>
                                if (attMap != null) {
                                    val attId = attMap["id"] as? String ?: ""
                                    val dayOfWeek = attMap["dayOfWeek"] as? String ?: ""
                                    val dateText = attMap["dateText"] as? String ?: ""
                                    val classTitle = attMap["classTitle"] as? String ?: group.title
                                    val creatorName = attMap["creatorName"] as? String ?: ""
                                    val rawAttendees = attMap["attendees"] as? List<*> ?: emptyList<Any>()
                                    val attendeesList = rawAttendees.mapNotNull { a ->
                                        if (a is Map<*, *>) {
                                            Attendee(
                                                userId = a["userId"] as? String ?: "",
                                                userName = a["userName"] as? String ?: "",
                                                userRole = a["userRole"] as? String ?: "دانش‌آموز",
                                                timestamp = (a["timestamp"] as? Number)?.toLong() ?: 0L,
                                                timeFormatted = a["timeFormatted"] as? String ?: ""
                                            )
                                        } else null
                                    }
                                    attendanceData = AttendanceCardData(
                                        id = attId,
                                        dayOfWeek = dayOfWeek,
                                        dateText = dateText,
                                        classTitle = classTitle,
                                        creatorName = creatorName,
                                        attendees = attendeesList
                                    )
                                }
                            }

                            ChatMessage(
                                id = id,
                                classGroupId = classGroupId,
                                senderId = senderId,
                                senderName = senderName,
                                senderRole = senderRole,
                                text = text,
                                timestamp = timestamp,
                                timeFormatted = timeFormatted,
                                isAttendanceCard = isAttendanceCard,
                                attendanceData = attendanceData,
                                status = MessageStatus.SENT
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val currentMap = _groupMessages.value.toMutableMap()
                    currentMap[group.id] = messagesList
                    _groupMessages.value = currentMap
                }
            }
        listenerRegistrations.add(reg)
    }

    fun getGroupById(groupId: String): ClassGroup {
        return _allGroups.value.firstOrNull { it.id == groupId }
            ?: ClassGroup(
                id = groupId,
                title = "گفتگو",
                gradeCode = "",
                gradeName = "",
                avatarColorHex = 0xFF1976D2,
                memberCount = 0,
                description = ""
            )
    }

    fun createGroup(
        title: String,
        gradeCode: String = "",
        description: String = "",
        isOfficialChannel: Boolean = false
    ) {
        val db = firestore ?: return
        val groupId = if (isOfficialChannel) "channel_${UUID.randomUUID()}" else "group_${UUID.randomUUID()}"
        val groupMap = hashMapOf<String, Any>(
            "id" to groupId,
            "title" to title,
            "gradeCode" to gradeCode,
            "gradeName" to "کلاس",
            "avatarColorHex" to 0xFF1976D2,
            "memberCount" to 1,
            "description" to description,
            "isOfficialChannel" to isOfficialChannel,
            "isPermanent" to false
        )
        db.collection("class_groups").document(groupId).set(groupMap)
    }

    fun canUserAccessGroup(user: User?, groupId: String): Boolean {
        val group = getGroupById(groupId)
        return canUserAccessGroup(user, group)
    }

    fun canUserAccessGroup(user: User?, group: ClassGroup): Boolean {
        if (user == null) return false
        if (user.isCreator || user.role == UserRole.MANAGER || user.role == UserRole.VICE_PRINCIPAL ||
            user.role == UserRole.COUNSELOR || user.role == UserRole.TEACHER) {
            return true
        }

        if (group.isOfficialChannel) return true

        val assignedClassesStr = user.assignedClasses.joinToString(" ")
        val roleStr = user.rankTitle + " " + user.tag.orEmpty() + " " + user.name

        return when (group.id) {
            "channel_grade_7" -> assignedClassesStr.contains("7") || assignedClassesStr.contains("۷") || assignedClassesStr.contains("هفتم") || roleStr.contains("هفتم") || user.role == UserRole.STUDENT
            "channel_grade_8" -> assignedClassesStr.contains("8") || assignedClassesStr.contains("۸") || assignedClassesStr.contains("هشتم") || roleStr.contains("هشتم")
            "channel_grade_9" -> assignedClassesStr.contains("9") || assignedClassesStr.contains("۹") || assignedClassesStr.contains("نهم") || roleStr.contains("نهم")
            "class_7_1" -> assignedClassesStr.contains("7/1") || assignedClassesStr.contains("۷/۱")
            "class_7_2" -> assignedClassesStr.contains("7/2") || assignedClassesStr.contains("۷/۲")
            "class_8_1" -> assignedClassesStr.contains("8/1") || assignedClassesStr.contains("۸/۱")
            "class_8_2" -> assignedClassesStr.contains("8/2") || assignedClassesStr.contains("۸/۲")
            "class_9_1" -> assignedClassesStr.contains("9/1") || assignedClassesStr.contains("۹/۱")
            "class_9_2" -> assignedClassesStr.contains("9/2") || assignedClassesStr.contains("۹/۲")
            else -> true
        }
    }

    fun canUserPostInGroup(user: User?, groupId: String): Boolean {
        val group = getGroupById(groupId)
        return canUserPostInGroup(user, group)
    }

    fun canUserPostInGroup(user: User?, group: ClassGroup): Boolean {
        if (user == null) return false
        // Managers, Vice Principals, Counselors, Teachers, and Creator can post in ALL channels including official channel
        if (user.isCreator || user.role == UserRole.MANAGER || user.role == UserRole.VICE_PRINCIPAL ||
            user.role == UserRole.COUNSELOR || user.role == UserRole.TEACHER) {
            return true
        }

        // Official Channel is READ-ONLY for Students and Parents
        if (group.isOfficialChannel) return false

        // In class groups, students in that grade can post messages
        return canUserAccessGroup(user, group)
    }

    fun sendMessage(classGroupId: String, sender: User, text: String) {
        if (text.isBlank()) return
        val group = getGroupById(classGroupId)
        if (!canUserPostInGroup(sender, group)) return

        val msgId = "msg_${UUID.randomUUID()}"
        val timestamp = System.currentTimeMillis()
        val timeFormatted = PersianDateHelper.getCurrentTimeFormatted()

        val roleDisplay = when (sender.role) {
            UserRole.MANAGER -> "مدیر مدرسه"
            UserRole.VICE_PRINCIPAL -> "معاون"
            UserRole.COUNSELOR -> "مشاور"
            UserRole.TEACHER -> "دبیر"
            UserRole.PARENT -> "ولی دانش‌آموز"
            else -> sender.rankTitle.ifBlank { "دانش‌آموز" }
        }

        val localMsg = ChatMessage(
            id = msgId,
            classGroupId = classGroupId,
            senderId = sender.id,
            senderName = sender.name,
            senderRole = roleDisplay,
            text = text.trim(),
            timestamp = timestamp,
            timeFormatted = timeFormatted,
            status = MessageStatus.SENDING
        )

        // Optimistic UI update
        val currentList = _groupMessages.value[classGroupId]?.toMutableList() ?: mutableListOf()
        currentList.add(localMsg)
        val currentMap = _groupMessages.value.toMutableMap()
        currentMap[classGroupId] = currentList
        _groupMessages.value = currentMap

        val payload = hashMapOf<String, Any>(
            "id" to msgId,
            "classGroupId" to classGroupId,
            "senderId" to sender.id,
            "senderName" to sender.name,
            "senderRole" to roleDisplay,
            "text" to text.trim(),
            "timestamp" to timestamp,
            "timeFormatted" to timeFormatted,
            "isAttendanceCard" to false
        )

        val db = firestore
        if (db != null) {
            db.collection("class_groups")
                .document(classGroupId)
                .collection("messages")
                .document(msgId)
                .set(payload)
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to send message: ${e.message}", e)
                }
        }
    }

    fun deleteMessage(classGroupId: String, messageId: String, currentUser: User?): Boolean {
        if (currentUser == null) return false
        val currentList = _groupMessages.value[classGroupId] ?: return false
        val msg = currentList.firstOrNull { it.id == messageId } ?: return false

        val isStaffOrCreator = currentUser.isCreator ||
            currentUser.role == UserRole.MANAGER ||
            currentUser.role == UserRole.VICE_PRINCIPAL ||
            currentUser.role == UserRole.COUNSELOR

        val isAuthor = currentUser.id == msg.senderId

        if (!isStaffOrCreator && !isAuthor) return false

        // Remove locally
        val updatedList = currentList.filter { it.id != messageId }
        val currentMap = _groupMessages.value.toMutableMap()
        currentMap[classGroupId] = updatedList
        _groupMessages.value = currentMap

        // Remove from Firestore
        val db = firestore
        if (db != null) {
            db.collection("class_groups")
                .document(classGroupId)
                .collection("messages")
                .document(messageId)
                .delete()
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to delete message: ${e.message}", e)
                }
        }
        return true
    }

    fun sendAttendanceCard(classGroupId: String, sender: User) {
        val db = firestore ?: return
        val group = getGroupById(classGroupId)

        val msgId = "msg_${UUID.randomUUID()}"
        val cardId = "att_${UUID.randomUUID()}"
        val dayOfWeek = PersianDateHelper.getCurrentDayOfWeek()
        val dateText = PersianDateHelper.getCurrentPersianDate()
        val timestamp = System.currentTimeMillis()
        val timeFormatted = PersianDateHelper.getCurrentTimeFormatted()

        val attendanceDataMap = hashMapOf<String, Any>(
            "id" to cardId,
            "dayOfWeek" to dayOfWeek,
            "dateText" to dateText,
            "classTitle" to group.title,
            "creatorName" to sender.name,
            "attendees" to emptyList<Map<String, Any>>()
        )

        val payload = hashMapOf<String, Any>(
            "id" to msgId,
            "classGroupId" to classGroupId,
            "senderId" to sender.id,
            "senderName" to sender.name,
            "senderRole" to "دبیر",
            "text" to "📋 فرم ثبت حضور و غیاب ${group.title}",
            "timestamp" to timestamp,
            "timeFormatted" to timeFormatted,
            "isAttendanceCard" to true,
            "attendanceData" to attendanceDataMap
        )

        db.collection("class_groups")
            .document(classGroupId)
            .collection("messages")
            .document(msgId)
            .set(payload)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send attendance card: ${e.message}", e)
            }
    }

    fun toggleAttendance(messageId: String, classGroupId: String, user: User): Boolean {
        val db = firestore ?: return false
        val currentList = _groupMessages.value[classGroupId] ?: return false
        val msg = currentList.firstOrNull { it.id == messageId } ?: return false
        val card = msg.attendanceData ?: return false

        val currentAttendees = card.attendees.toMutableList()
        val existingIndex = currentAttendees.indexOfFirst { it.userId == user.id || it.userName == user.name }

        val markedPresent: Boolean
        if (existingIndex != -1) {
            currentAttendees.removeAt(existingIndex)
            markedPresent = false
        } else {
            currentAttendees.add(
                Attendee(
                    userId = user.id,
                    userName = user.name,
                    userRole = user.rankTitle.ifBlank { "دانش‌آموز" },
                    timestamp = System.currentTimeMillis(),
                    timeFormatted = PersianDateHelper.getCurrentFullTimeFormatted()
                )
            )
            markedPresent = true
        }

        val updatedAttendeesRaw = currentAttendees.map { a ->
            hashMapOf<String, Any>(
                "userId" to a.userId,
                "userName" to a.userName,
                "userRole" to a.userRole,
                "timestamp" to a.timestamp,
                "timeFormatted" to a.timeFormatted
            )
        }

        val updatedCardMap = hashMapOf<String, Any>(
            "id" to card.id,
            "dayOfWeek" to card.dayOfWeek,
            "dateText" to card.dateText,
            "classTitle" to card.classTitle,
            "creatorName" to card.creatorName,
            "attendees" to updatedAttendeesRaw
        )

        db.collection("class_groups")
            .document(classGroupId)
            .collection("messages")
            .document(messageId)
            .update("attendanceData", updatedCardMap)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to toggle attendance: ${e.message}", e)
            }

        return markedPresent
    }

    fun isUserAttended(messageId: String, classGroupId: String, userId: String, userName: String): Boolean {
        val messages = _groupMessages.value[classGroupId] ?: return false
        val msg = messages.firstOrNull { it.id == messageId } ?: return false
        val card = msg.attendanceData ?: return false
        return card.attendees.any { it.userId == userId || it.userName == userName }
    }

    fun getAttendees(messageId: String, classGroupId: String): List<Attendee> {
        val messages = _groupMessages.value[classGroupId] ?: return emptyList()
        val msg = messages.firstOrNull { it.id == messageId } ?: return emptyList()
        return msg.attendanceData?.attendees ?: emptyList()
    }
}
