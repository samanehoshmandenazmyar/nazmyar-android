package com.example.domain.model

data class SupportMessage(
    val id: String,
    val ticketId: String,
    val senderUserId: String,
    val senderName: String,
    val senderRoleTitle: String,
    val isFromSupport: Boolean,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val replyToId: String? = null,
    val replyToSenderName: String? = null,
    val replyToContent: String? = null,
    val isEdited: Boolean = false,
    val isPending: Boolean = false
)

data class SupportTicket(
    val id: String,
    val userId: String,
    val userName: String,
    val userPhoneOrCode: String,
    val userRoleTitle: String,
    val schoolName: String = "دبیرستان شهید رجایی",
    val status: String = "ACTIVE", // "ACTIVE", "CLOSED"
    val lastMessage: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val unreadBySupport: Boolean = true,
    val unreadByUser: Boolean = false,
    val isStarted: Boolean = false,
    val messages: List<SupportMessage> = emptyList()
)
