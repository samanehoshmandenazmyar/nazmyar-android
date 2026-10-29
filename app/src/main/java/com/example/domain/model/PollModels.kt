package com.example.domain.model

import java.util.UUID

data class PollOption(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val imageUrl: String? = null,
    val imageRes: Int? = null,
    val baseVotes: Int = 0
)

data class Poll(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val creatorName: String = "مدیریت مدرسه",
    val creatorRole: String = "مدیر",
    val options: List<PollOption> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val targetRole: String = "همه",
    val isPermanent: Boolean = true,
    val expiryDate: String = ""
)
