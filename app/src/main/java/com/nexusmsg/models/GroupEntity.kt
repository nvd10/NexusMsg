package com.nexusmsg.models

data class GroupEntity(
    val id: String = "",
    val name: String = "",
    val memberCount: Int = 0,
    val lastMessage: String? = null,
    val lastActivity: Long = System.currentTimeMillis()
)
