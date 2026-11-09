package com.example.core.support

import android.content.Context
import android.content.SharedPreferences
import com.example.core.network.FirebaseRealtimeService
import com.example.domain.model.SupportMessage
import com.example.domain.model.SupportTicket
import com.example.domain.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object SupportManager {
    private const val PREFS_NAME = "nezamyar_support_prefs"
    private const val KEY_TICKETS_JSON = "key_support_tickets_json"

    private lateinit var prefs: SharedPreferences
    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.IO)
    private var syncJob: Job? = null

    private val _tickets = MutableStateFlow<List<SupportTicket>>(emptyList())
    val tickets: StateFlow<List<SupportTicket>> = _tickets.asStateFlow()

    fun init(context: Context) {
        if (isInitialized) return
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isInitialized = true
        loadTickets()
        startLiveServerSync()
    }

    private fun startLiveServerSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive) {
                try {
                    val remoteTickets = FirebaseRealtimeService.fetchSupportTickets()
                    if (remoteTickets != null && remoteTickets.isNotEmpty()) {
                        mergeRemoteTickets(remoteTickets)
                    }
                } catch (_: Exception) {}
                delay(3000)
            }
        }
    }

    private fun mergeRemoteTickets(remote: List<SupportTicket>) {
        val currentLocal = _tickets.value.associateBy { it.id }.toMutableMap()

        remote.forEach { remoteTicket ->
            val localTicket = currentLocal[remoteTicket.id]
            if (localTicket == null) {
                currentLocal[remoteTicket.id] = remoteTicket
            } else {
                // If remote ticket has more messages or newer timestamp, update local ticket
                if (remoteTicket.messages.size >= localTicket.messages.size || remoteTicket.lastUpdated > localTicket.lastUpdated) {
                    val mergedMsgs = (localTicket.messages + remoteTicket.messages).distinctBy { it.id }.sortedBy { it.timestamp }
                    currentLocal[remoteTicket.id] = remoteTicket.copy(
                        messages = mergedMsgs,
                        lastMessage = mergedMsgs.lastOrNull()?.content ?: remoteTicket.lastMessage,
                        lastUpdated = mergedMsgs.lastOrNull()?.timestamp ?: remoteTicket.lastUpdated
                    )
                }
            }
        }

        val updatedList = currentLocal.values.sortedByDescending { it.lastUpdated }
        if (updatedList != _tickets.value) {
            _tickets.value = updatedList
            saveTicketsToPrefs(updatedList)
        }
    }

    private fun loadTickets() {
        val jsonStr = prefs.getString(KEY_TICKETS_JSON, null)
        if (jsonStr.isNullOrBlank()) {
            val defaultList = createInitialDemoTickets()
            _tickets.value = defaultList
            saveTicketsToPrefs(defaultList)
        } else {
            val parsed = parseJsonToTickets(jsonStr)
            _tickets.value = parsed.ifEmpty { createInitialDemoTickets() }
        }
    }

    private fun saveTicketsToPrefs(list: List<SupportTicket>) {
        val jsonArray = JSONArray()
        list.forEach { ticket ->
            val obj = JSONObject().apply {
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

                val msgsArray = JSONArray()
                ticket.messages.forEach { msg ->
                    val msgObj = JSONObject().apply {
                        put("id", msg.id)
                        put("ticketId", msg.ticketId)
                        put("senderUserId", msg.senderUserId)
                        put("senderName", msg.senderName)
                        put("senderRoleTitle", msg.senderRoleTitle)
                        put("isFromSupport", msg.isFromSupport)
                        put("content", msg.content)
                        put("timestamp", msg.timestamp)
                        if (msg.replyToId != null) put("replyToId", msg.replyToId)
                        if (msg.replyToSenderName != null) put("replyToSenderName", msg.replyToSenderName)
                        if (msg.replyToContent != null) put("replyToContent", msg.replyToContent)
                        put("isEdited", msg.isEdited)
                        put("isPending", msg.isPending)
                    }
                    msgsArray.put(msgObj)
                }
                put("messages", msgsArray)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString(KEY_TICKETS_JSON, jsonArray.toString()).apply()
    }

    private fun parseJsonToTickets(jsonStr: String): List<SupportTicket> {
        val list = mutableListOf<SupportTicket>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val msgsArray = obj.optJSONArray("messages") ?: JSONArray()
                val msgList = mutableListOf<SupportMessage>()

                for (j in 0 until msgsArray.length()) {
                    val mObj = msgsArray.getJSONObject(j)
                    msgList.add(
                        SupportMessage(
                            id = mObj.optString("id", UUID.randomUUID().toString()),
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
                            isEdited = mObj.optBoolean("isEdited", false),
                            isPending = mObj.optBoolean("isPending", false)
                        )
                    )
                }

                list.add(
                    SupportTicket(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        userId = obj.optString("userId", ""),
                        userName = obj.optString("userName", "کاربر پشتیبانی"),
                        userPhoneOrCode = obj.optString("userPhoneOrCode", ""),
                        userRoleTitle = obj.optString("userRoleTitle", "کاربر"),
                        schoolName = obj.optString("schoolName", "دبیرستان شهید رجایی"),
                        status = obj.optString("status", "ACTIVE"),
                        lastMessage = obj.optString("lastMessage", ""),
                        lastUpdated = obj.optLong("lastUpdated", System.currentTimeMillis()),
                        unreadBySupport = obj.optBoolean("unreadBySupport", false),
                        unreadByUser = obj.optBoolean("unreadByUser", false),
                        isStarted = obj.optBoolean("isStarted", false),
                        messages = msgList
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun createInitialDemoTickets(): List<SupportTicket> {
        val sampleTicketId = "ticket_guest_demo"
        val botWelcomeMsg = SupportMessage(
            id = "msg_welcome_1",
            ticketId = sampleTicketId,
            senderUserId = "bot_nezamyar",
            senderName = "ربات پشتیبانی نظم‌یار",
            senderRoleTitle = "پشتیبانی هوشمند",
            isFromSupport = true,
            content = "سلام! 👋 به پشتیبانی رسمی سامانه «نظم‌یار» خوش آمدید.\n\nلطفاً سوال، درخواست یا مشکل خود را مطرح نمایید. تیم پشتیبانی مدرسه در اسرع وقت پاسخگوی شما خواهند بود.",
            timestamp = System.currentTimeMillis() - 3600000,
            isPending = false
        )

        return listOf(
            SupportTicket(
                id = sampleTicketId,
                userId = "user_guest_1",
                userName = "مهمان / کاربر جدید",
                userPhoneOrCode = "09170000000",
                userRoleTitle = "مهمان سامانه",
                schoolName = "دبیرستان شهید رجایی",
                status = "ACTIVE",
                lastMessage = botWelcomeMsg.content,
                lastUpdated = System.currentTimeMillis() - 3600000,
                unreadBySupport = true,
                unreadByUser = false,
                isStarted = true,
                messages = listOf(botWelcomeMsg)
            )
        )
    }

    fun getOrCreateTicketForUser(
        userId: String,
        userName: String,
        phoneOrCode: String,
        roleTitle: String
    ): SupportTicket {
        val existing = _tickets.value.firstOrNull { it.userId == userId || (userId.startsWith("guest_") && it.userPhoneOrCode == phoneOrCode) }
        if (existing != null) {
            return existing
        }

        val newTicket = SupportTicket(
            id = "ticket_${userId}_${System.currentTimeMillis()}",
            userId = userId,
            userName = userName.ifBlank { "کاربر پشتیبانی" },
            userPhoneOrCode = phoneOrCode,
            userRoleTitle = roleTitle.ifBlank { "کاربر سامانه" },
            status = "ACTIVE",
            lastMessage = "",
            lastUpdated = System.currentTimeMillis(),
            unreadBySupport = false,
            unreadByUser = false,
            isStarted = false,
            messages = emptyList()
        )

        val updatedList = listOf(newTicket) + _tickets.value
        _tickets.value = updatedList
        saveTicketsToPrefs(updatedList)
        scope.launch {
            FirebaseRealtimeService.saveSupportTicket(newTicket)
        }
        return newTicket
    }

    fun startTicketSession(ticketId: String) {
        val list = _tickets.value.toMutableList()
        val index = list.indexOfFirst { it.id == ticketId }
        if (index != -1) {
            val ticket = list[index]
            if (!ticket.isStarted || ticket.messages.isEmpty()) {
                val welcomeMsg = SupportMessage(
                    id = "msg_welcome_${System.currentTimeMillis()}",
                    ticketId = ticketId,
                    senderUserId = "bot_nezamyar",
                    senderName = "ربات پشتیبانی نظم‌یار",
                    senderRoleTitle = "پشتیبانی هوشمند",
                    isFromSupport = true,
                    content = "سلام! 👋 به پشتیبانی رسمی سامانه «نظم‌یار» خوش آمدید.\n\nلطفاً سوال، درخواست یا مشکل خود را مطرح نمایید. تیم پشتیبانی مدرسه در اسرع وقت پاسخگوی شما خواهند بود.",
                    timestamp = System.currentTimeMillis(),
                    isPending = false
                )

                val updatedTicket = ticket.copy(
                    isStarted = true,
                    lastMessage = welcomeMsg.content,
                    lastUpdated = System.currentTimeMillis(),
                    messages = ticket.messages + welcomeMsg
                )
                list[index] = updatedTicket
                _tickets.value = list
                saveTicketsToPrefs(list)
                scope.launch {
                    FirebaseRealtimeService.saveSupportTicket(updatedTicket)
                }
            }
        }
    }

    fun sendMessageFromUser(
        ticketId: String,
        senderUser: User?,
        text: String,
        replyToMessage: SupportMessage? = null
    ) {
        val list = _tickets.value.toMutableList()
        val index = list.indexOfFirst { it.id == ticketId }
        if (index != -1 && text.isNotBlank()) {
            val ticket = list[index]
            val msgId = "msg_${System.currentTimeMillis()}"
            val msg = SupportMessage(
                id = msgId,
                ticketId = ticketId,
                senderUserId = senderUser?.id ?: ticket.userId,
                senderName = senderUser?.name ?: ticket.userName,
                senderRoleTitle = senderUser?.rankTitle ?: ticket.userRoleTitle,
                isFromSupport = false,
                content = text.trim(),
                timestamp = System.currentTimeMillis(),
                replyToId = replyToMessage?.id,
                replyToSenderName = replyToMessage?.senderName,
                replyToContent = replyToMessage?.content,
                isPending = true
            )

            val updatedTicket = ticket.copy(
                isStarted = true,
                lastMessage = text.trim(),
                lastUpdated = System.currentTimeMillis(),
                unreadBySupport = true,
                unreadByUser = false,
                messages = ticket.messages + msg
            )
            list[index] = updatedTicket
            _tickets.value = list
            saveTicketsToPrefs(list)
            scope.launch {
                try {
                    FirebaseRealtimeService.saveSupportTicket(updatedTicket)
                } catch (_: Exception) {}
                delay(600)
                markMessageSent(ticketId, msgId)
            }
        }
    }

    fun sendMessageFromSupport(
        ticketId: String,
        supportUser: User?,
        text: String,
        replyToMessage: SupportMessage? = null
    ) {
        val list = _tickets.value.toMutableList()
        val index = list.indexOfFirst { it.id == ticketId }
        if (index != -1 && text.isNotBlank()) {
            val ticket = list[index]
            val msgId = "msg_${System.currentTimeMillis()}"
            val msg = SupportMessage(
                id = msgId,
                ticketId = ticketId,
                senderUserId = supportUser?.id ?: "support_staff",
                senderName = supportUser?.name ?: "پشتیبانی مدرسه",
                senderRoleTitle = supportUser?.rankTitle ?: "پشتیبانی",
                isFromSupport = true,
                content = text.trim(),
                timestamp = System.currentTimeMillis(),
                replyToId = replyToMessage?.id,
                replyToSenderName = replyToMessage?.senderName,
                replyToContent = replyToMessage?.content,
                isPending = true
            )

            val updatedTicket = ticket.copy(
                lastMessage = text.trim(),
                lastUpdated = System.currentTimeMillis(),
                unreadBySupport = false,
                unreadByUser = true,
                messages = ticket.messages + msg
            )
            list[index] = updatedTicket
            _tickets.value = list
            saveTicketsToPrefs(list)
            scope.launch {
                try {
                    FirebaseRealtimeService.saveSupportTicket(updatedTicket)
                } catch (_: Exception) {}
                delay(600)
                markMessageSent(ticketId, msgId)
            }
        }
    }

    fun markMessageSent(ticketId: String, messageId: String) {
        val list = _tickets.value.toMutableList()
        val index = list.indexOfFirst { it.id == ticketId }
        if (index != -1) {
            val ticket = list[index]
            val msgs = ticket.messages.toMutableList()
            val mIndex = msgs.indexOfFirst { it.id == messageId }
            if (mIndex != -1 && msgs[mIndex].isPending) {
                msgs[mIndex] = msgs[mIndex].copy(isPending = false)
                val updatedTicket = ticket.copy(messages = msgs)
                list[index] = updatedTicket
                _tickets.value = list
                saveTicketsToPrefs(list)
            }
        }
    }

    fun editMessage(ticketId: String, messageId: String, newContent: String) {
        val list = _tickets.value.toMutableList()
        val index = list.indexOfFirst { it.id == ticketId }
        if (index != -1 && newContent.isNotBlank()) {
            val ticket = list[index]
            val msgs = ticket.messages.toMutableList()
            val mIndex = msgs.indexOfFirst { it.id == messageId }
            if (mIndex != -1) {
                msgs[mIndex] = msgs[mIndex].copy(
                    content = newContent.trim(),
                    isEdited = true
                )
                val updatedTicket = ticket.copy(
                    lastMessage = msgs.lastOrNull()?.content ?: ticket.lastMessage,
                    messages = msgs
                )
                list[index] = updatedTicket
                _tickets.value = list
                saveTicketsToPrefs(list)
                scope.launch {
                    FirebaseRealtimeService.saveSupportTicket(updatedTicket)
                }
            }
        }
    }

    fun deleteMessage(ticketId: String, messageId: String) {
        deleteMessages(ticketId, setOf(messageId))
    }

    fun deleteMessages(ticketId: String, messageIds: Set<String>) {
        val list = _tickets.value.toMutableList()
        val index = list.indexOfFirst { it.id == ticketId }
        if (index != -1 && messageIds.isNotEmpty()) {
            val ticket = list[index]
            val msgs = ticket.messages.filter { it.id !in messageIds }
            val updatedTicket = ticket.copy(
                lastMessage = msgs.lastOrNull()?.content ?: "",
                lastUpdated = System.currentTimeMillis(),
                messages = msgs
            )
            list[index] = updatedTicket
            _tickets.value = list
            saveTicketsToPrefs(list)
            scope.launch {
                FirebaseRealtimeService.saveSupportTicket(updatedTicket)
            }
        }
    }

    fun markTicketReadBySupport(ticketId: String) {
        val list = _tickets.value.toMutableList()
        val index = list.indexOfFirst { it.id == ticketId }
        if (index != -1 && list[index].unreadBySupport) {
            val updated = list[index].copy(unreadBySupport = false)
            list[index] = updated
            _tickets.value = list
            saveTicketsToPrefs(list)
            scope.launch {
                FirebaseRealtimeService.saveSupportTicket(updated)
            }
        }
    }

    fun markTicketReadByUser(ticketId: String) {
        val list = _tickets.value.toMutableList()
        val index = list.indexOfFirst { it.id == ticketId }
        if (index != -1 && list[index].unreadByUser) {
            val updated = list[index].copy(unreadByUser = false)
            list[index] = updated
            _tickets.value = list
            saveTicketsToPrefs(list)
            scope.launch {
                FirebaseRealtimeService.saveSupportTicket(updated)
            }
        }
    }

    fun deleteTicketLocally(ticketId: String) {
        val list = _tickets.value.filter { it.id != ticketId }
        _tickets.value = list
        saveTicketsToPrefs(list)
    }

    fun deleteTicketForEveryone(ticketId: String) {
        val list = _tickets.value.filter { it.id != ticketId }
        _tickets.value = list
        saveTicketsToPrefs(list)
        scope.launch {
            FirebaseRealtimeService.deleteSupportTicket(ticketId)
        }
    }
}

