package com.nexusmsg.models
data class Contact(val id: String = "", val name: String = "", val phoneNumber: String? = null, val username: String? = null, val lastMessage: String? = null, val lastMessageTime: Long = 0, val isOnline: Boolean = false)
