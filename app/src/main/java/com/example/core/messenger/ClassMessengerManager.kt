package com.example.core.messenger

import android.content.Context
import android.util.Log
import com.example.R
import com.example.domain.model.AttendanceCardData
import com.example.domain.model.Attendee
import com.example.domain.model.ChatMessage
import com.example.domain.model.ClassGroup
import com.example.domain.model.PersianDateHelper
import com.example.domain.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object ClassMessengerManager {
    private const val TAG = "ClassMessengerManager"
    private const val PREFS_NAME = "nezamyar_class_messenger_prefs_v1"

    private var firestore: FirebaseFirestore? = null
    private val listenerRegistrations = mutableListOf<ListenerRegistration>()

    // 6 fixed, undeletable, default groups
    val FIXED_GROUPS = listOf(
        ClassGroup(
            id = "class_7_1",
            title = "کلاس هفتم یک",
            gradeCode = "7/1",
            gradeName = "پایه هفتم",
            avatarColorHex = 0xFF1E88E5, // Blue
            memberCount = 34,
            description = "گروه رسمی آموزشگاه - کلاس هفتم یک"
        ),
        ClassGroup(
            id = "class_7_2",
            title = "کلاس هفتم دو",
            gradeCode = "7/2",
            gradeName = "پایه هفتم",
            avatarColorHex = 0xFF00ACC1, // Cyan
            memberCount = 33,
            description = "گروه رسمی آموزشگاه - کلاس هفتم دو"
        ),
        ClassGroup(
            id = "class_8_1",
            title = "کلاس هشتم یک",
            gradeCode = "8/1",
            gradeName = "پایه هشتم",
            avatarColorHex = 0xFF43A047, // Green
            memberCount = 31,
            description = "گروه رسمی آموزشگاه - کلاس هشتم یک"
        ),
        ClassGroup(
            id = "class_8_2",
            title = "کلاس هشتم دو",
            gradeCode = "8/2",
            gradeName = "پایه هشتم",
            avatarColorHex = 0xFF7CB342, // Light Green
            memberCount = 32,
            description = "گروه رسمی آموزشگاه - کلاس هشتم دو"
        ),
        ClassGroup(
            id = "class_9_1",
            title = "کلاس نهم یک",
            gradeCode = "9/1",
            gradeName = "پایه نهم",
            avatarColorHex = 0xFFFB8C00, // Amber / Orange
            memberCount = 30,
            description = "گروه رسمی آموزشگاه - کلاس نهم یک"
        ),
        ClassGroup(
            id = "class_9_2",
            title = "کلاس نهم دو",
            gradeCode = "9/2",
            gradeName = "پایه نهم",
            avatarColorHex = 0xFF8E24AA, // Purple
            memberCount = 32,
            description = "گروه رسمی آموزشگاه - کلاس نهم دو"
        )
    )

    // Initially ALL groups are completely empty with zero messages
    private val _groupMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(
        FIXED_GROUPS.associate { it.id to emptyList<ChatMessage>() }
    )
    val groupMessages: StateFlow<Map<String, List<ChatMessage>>> = _groupMessages.asStateFlow()

    fun init(context: Context) {
        // Clear any old fake or legacy messages in SharedPreferences
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().clear().apply()
        } catch (_: Exception) {}

        // Reset memory state so all groups are empty by default
        _groupMessages.value = FIXED_GROUPS.associate { it.id to emptyList<ChatMessage>() }

        // Initialize Firebase Firestore with custom Database ID
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

        // Clear existing registrations if any
        listenerRegistrations.forEach { it.remove() }
        listenerRegistrations.clear()

        for (group in FIXED_GROUPS) {
            val reg = db.collection("class_groups")
                .document(group.id)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.w(TAG, "Listen failed for group ${group.id}: ${error.message}")
                        return@addSnapshotListener
                    }

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
                                    attendanceData = attendanceData
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing message doc ${doc.id}: ${e.message}")
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
    }

    fun getGroupById(groupId: String): ClassGroup {
        return FIXED_GROUPS.firstOrNull { it.id == groupId } ?: FIXED_GROUPS[0]
    }

    fun sendMessage(classGroupId: String, sender: User, text: String) {
        if (text.isBlank()) return
        val db = firestore ?: return

        val msgId = "msg_${UUID.randomUUID()}"
        val timestamp = System.currentTimeMillis()
        val timeFormatted = PersianDateHelper.getCurrentTimeFormatted()

        val payload = hashMapOf<String, Any>(
            "id" to msgId,
            "classGroupId" to classGroupId,
            "senderId" to sender.id,
            "senderName" to sender.name,
            "senderRole" to sender.rankTitle,
            "text" to text.trim(),
            "timestamp" to timestamp,
            "timeFormatted" to timeFormatted,
            "isAttendanceCard" to false
        )

        db.collection("class_groups")
            .document(classGroupId)
            .collection("messages")
            .document(msgId)
            .set(payload)
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to send message: ${e.message}", e)
            }
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
            "senderRole" to sender.rankTitle,
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
                    userRole = user.rankTitle,
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
