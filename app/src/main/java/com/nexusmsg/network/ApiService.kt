package com.nexusmsg.network

import retrofit2.Call
import retrofit2.http.*

data class CreateGroupRequest(
    val name: String,
    val members: List<String>
)

data class GroupResponse(
    val id: String,
    val name: String,
    val members: List<String>
)

data class Message(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long
)

data class IceServer(
    val urls: List<String>,
    val username: String? = null,
    val credential: String? = null
)

interface ApiService {
    
    // Create a new group
    @POST("groups")
    fun createGroup(
        @Body request: CreateGroupRequest
    ): Call<GroupResponse>
    
    // Get group details
    @GET("groups/{groupId}")
    fun getGroup(
        @Path("groupId") groupId: String
    ): Call<GroupResponse>
    
    // Add user to group
    @POST("groups/{groupId}/members")
    fun addMember(
        @Path("groupId") groupId: String,
        @Query("userId") userId: String
    ): Call<Void>
    
    // Remove user from group
    @DELETE("groups/{groupId}/members")
    fun removeMember(
        @Path("groupId") groupId: String,
        @Query("userId") userId: String
    ): Call<Void>
    
    // Get messages for a group
    @GET("groups/{groupId}/messages")
    fun getMessages(
        @Path("groupId") groupId: String,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Call<List<Message>>
    
    // Send message to group
    @POST("groups/{groupId}/messages")
    fun sendMessage(
        @Path("groupId") groupId: String,
        @Body message: Message
    ): Call<Message>
    
    // Get ICE servers for WebRTC
    @GET("ice-servers")
    fun getIceServers(): Call<List<IceServer>>
}
