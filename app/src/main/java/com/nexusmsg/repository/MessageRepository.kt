package com.nexusmsg.repository

import com.nexusmsg.crypto.E2EEncryption
import com.nexusmsg.models.*
import com.nexusmsg.network.ApiService
import com.nexusmsg.network.WebSocketClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val apiService: ApiService,
    private val database: AppDatabase,
    private val crypto: E2EEncryption,
    private val webSocketClient: WebSocketClient
) {
    private val messageDao = database.messageDao()
    private val contactDao = database.contactDao()
    private val groupDao = database.groupDao()
    private val groupMemberDao = database.groupMemberDao()

    // ─── Direct Messages ───

    suspend fun sendMessage(
        plaintext: String,
        senderId: String,
        receiverId: String,
        receiverPublicKey: ByteArray
    ): ChatMessage {
        val (ciphertext, nonce) = crypto.encryptMessage(
            plaintext, senderId, receiverId, receiverPublicKey
        )

        val payload = MessagePayload(
            senderId = senderId,
            receiverId = receiverId,
            ciphertext = ciphertext,
            nonce = nonce
        )

        webSocketClient.sendMessage(payload)

        val chatId = getChatId(senderId, receiverId)
        val chatMessage = ChatMessage(
            id = payload.id,
            chatId = chatId,
            senderId = senderId,
            receiverId = receiverId,
            content = plaintext,
            type = "text",
            timestamp = payload.timestamp,
            status = "sent"
        )

        messageDao.insertMessage(chatMessage)
        contactDao.updateLastMessage(receiverId, plaintext, payload.timestamp)
        return chatMessage
    }

    suspend fun receiveMessage(
        payload: MessagePayload,
        senderPublicKey: ByteArray,
        myUserId: String
    ): ChatMessage? {
        try {
            val plaintext = crypto.decryptMessage(
                payload.ciphertext, payload.nonce,
                myUserId, payload.senderId, senderPublicKey
            )

            val chatId = getChatId(myUserId, payload.senderId)
            val chatMessage = ChatMessage(
                id = payload.id, chatId = chatId,
                senderId = payload.senderId, receiverId = myUserId,
                content = plaintext, type = payload.type,
                timestamp = payload.timestamp, status = "delivered"
            )

            messageDao.insertMessage(chatMessage)
            contactDao.updateLastMessage(payload.senderId, plaintext, payload.timestamp)
            webSocketClient.sendAck(payload.id, "delivered")

            return chatMessage
        } catch (e: Exception) {
            return null
        }
    }

    // ─── Group Messages ───

    suspend fun sendGroupMessage(
        plaintext: String,
        senderId: String,
        groupId: String
    ): ChatMessage {
        // For groups, encrypt with a group key derived from group ID (simplified approach)
        // In production: use SenderKey or encrypt per-member
        val groupKey = crypto.deriveSymmetricKey(
            groupId.toByteArray(),
            "nexusmsg-group-key".toByteArray()
        )
        val (ciphertext, nonce) = crypto.encrypt(
            plaintext.toByteArray(Charsets.UTF_8), groupKey
        )

        val payload = GroupMessagePayload(
            senderId = senderId,
            groupId = groupId,
            ciphertext = crypto.encodeKey(ciphertext),
            nonce = crypto.encodeKey(nonce)
        )

        webSocketClient.sendGroupMessage(payload)

        val chatMessage = ChatMessage(
            id = payload.id, chatId = groupId,
            senderId = senderId, receiverId = groupId,
            content = plaintext, type = "text",
            timestamp = payload.timestamp, status = "sent",
            isGroupMessage = true, groupId = groupId
        )

        messageDao.insertMessage(chatMessage)
        groupDao.updateLastMessage(groupId, plaintext, payload.timestamp)

        return chatMessage
    }

    suspend fun receiveGroupMessage(
        payload: GroupMessagePayload,
        myUserId: String
    ): ChatMessage? {
        try {
            val groupKey = crypto.deriveSymmetricKey(
                payload.groupId.toByteArray(),
                "nexusmsg-group-key".toByteArray()
            )
            val plaintext = crypto.decrypt(
                crypto.decodeKey(payload.ciphertext),
                crypto.decodeKey(payload.nonce),
                groupKey
            ).toString(Charsets.UTF_8)

            val chatMessage = ChatMessage(
                id = payload.id, chatId = payload.groupId,
                senderId = payload.senderId, receiverId = payload.groupId,
                content = plaintext, type = payload.type,
                timestamp = payload.timestamp, status = "delivered",
                isGroupMessage = true, groupId = payload.groupId
            )

            messageDao.insertMessage(chatMessage)
            groupDao.updateLastMessage(payload.groupId, plaintext, payload.timestamp)

            return chatMessage
        } catch (e: Exception) {
            return null
        }
    }

    // ─── Group Management ───

    suspend fun createGroup(name: String, createdBy: String, token: String): CreateGroupResponse? {
        return try {
            val response = apiService.createGroup(
                CreateGroupRequest(name, createdBy), "Bearer $token"
            )
            if (response.isSuccessful) response.body() else null
        } catch (_: Exception) { null }
    }

    suspend fun joinGroup(groupId: String, userId: String, token: String): Boolean {
        return try {
            val response = apiService.joinGroup(
                JoinGroupRequest(groupId, userId), "Bearer $token"
            )
            response.isSuccessful
        } catch (_: Exception) { false }
    }

    suspend fun loadGroups(userId: String, token: String) {
        try {
            val response = apiService.getGroups(userId, "Bearer $token")
            if (response.isSuccessful) {
                response.body()?.groups?.forEach { group ->
                    groupDao.insertGroup(GroupEntity(
                        groupId = group.groupId,
                        name = group.name,
                        createdBy = group.createdBy,
                        memberCount = group.memberCount
                    ))
                    // Also add to contacts list
                    contactDao.insertContact(Contact(
                        userId = group.groupId,
                        name = group.name,
                        phoneNumber = "",
                        username = "",
                        isGroup = true
                    ))
                }
            }
        } catch (_: Exception) { }
    }

    suspend fun loadGroupMembers(groupId: String, token: String) {
        try {
            val response = apiService.getGroupMembers(groupId, "Bearer $token")
            if (response.isSuccessful) {
                response.body()?.members?.forEach { member ->
                    groupMemberDao.insertMember(GroupMemberEntity(
                        id = "${groupId}_${member.userId}",
                        groupId = groupId,
                        userId = member.userId,
                        nexusId = member.nexusId,
                        name = member.name,
                        role = member.role
                    ))
                }
            }
        } catch (_: Exception) { }
    }

    suspend fun leaveGroup(groupId: String, userId: String, token: String): Boolean {
        return try {
            val response = apiService.leaveGroup(
                JoinGroupRequest(groupId, userId), "Bearer $token"
            )
            if (response.isSuccessful) {
                groupDao.deleteGroup(groupId)
                groupMemberDao.removeAllMembers(groupId)
            }
            response.isSuccessful
        } catch (_: Exception) { false }
    }

    // ─── Queries ───

    fun getMessages(chatId: String) = messageDao.getMessages(chatId)
    fun getContacts() = contactDao.getAllContacts()
    fun getGroups() = groupDao.getAllGroups()
    fun getGroupMembers(groupId: String) = groupMemberDao.getMembers(groupId)

    suspend fun markAsRead(chatId: String, myUserId: String) {
        messageDao.markAllRead(chatId, myUserId)
        val parts = chatId.split("_")
        val otherUserId = if (parts[0] == myUserId) parts[1] else parts[0]
        contactDao.clearUnreadCount(otherUserId)
    }

    suspend fun getUnreadCount(chatId: String, myUserId: String): Int {
        return messageDao.getUnreadCount(chatId, myUserId)
    }

    suspend fun updateContact(contact: Contact) = contactDao.insertContact(contact)

    // ─── User Lookup ───

    suspend fun lookupUserByNexusId(nexusId: String, token: String): UserLookupResponse? {
        return try {
            val response = apiService.lookupUserById(nexusId, "Bearer $token")
            if (response.isSuccessful) response.body() else null
        } catch (_: Exception) { null }
    }

    companion object {
        fun getChatId(userId1: String, userId2: String): String {
            return listOf(userId1, userId2).sorted().joinToString("_")
        }
    }
}
