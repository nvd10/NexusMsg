package com.nexusmsg.models
data class Group(val id: String = "", val name: String = "", val members: List<String> = emptyList(), val lastMessage: String? = null, val lastMessageTime: Long = 0, val unreadCount: Int = 0)
