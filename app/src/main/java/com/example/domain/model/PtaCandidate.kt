package com.example.domain.model

data class PtaCandidate(
    val id: String,
    val name: String,
    val childRelation: String,
    val education: String,
    val bio: String,
    val imageRes: Int,
    val baseVotes: Int = 0
)
