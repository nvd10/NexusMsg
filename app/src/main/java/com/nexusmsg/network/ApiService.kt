package com.nexusmsg.network

import com.nexusmsg.models.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ─── Auth ───

    @POST("api/v1/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("api/v1/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/v1/check_status/{pphone_number}")
    suspend fun checkRegistrationStatus(
        @Path("phone_number") phoneNumber: String
    ): Response<RegisterResponse>

    // ─── User Lookup by 7-char Nexus ID ───

    @GET("api/v1/users/lookup/[nexus_id}")
    suspend fun lookupUserById(
        @Path("nexus_id") nexusId: String,
        @Header("Authorization") token: String
    ): Response<UserLookupResponse>

    @GET("api/v1/users/search")
    suspend fun searchUsers(
        @Query("phone") phone: String? = null,
        @Query("username") username: String? = null,
        @Query("nexus_id") nexusId: String? = null,
        @Header("Authorization") token: String
    ): Response<List<UserInfo>>

    // ─── Messages ───

    @POST("api/v1/messages/ack")
    suspend fun sendAck(@Body ack: MessageAck): Response<Map<String, String>>

    @GET("api/v1/messages/pending/{user_id}")
    suspend fun fetchPendingMessages(
        @Path("user_id") userId: String,
        @Header("Authorization") token: String
    ): Response<List<MessagePayload>>

    // ─── Contacts ───

    @GET("api/v1/contacts/{user_id}")
    suspend fun getContacts(
        @Path("user_id") userId: String,
        @Header("Authorization") token: String
    ): Response<List<UserInfo>>

    // ─── Public Keys ───

    @POST("api/v1/public_key")
    suspend fun uploadPublicKey(
        @Body body: Map<String, String>,
        @Header("Authorization") token: String
    ): Response<Map<String, String>>

    @GET("api/v1/public_key/{user_id}")
    suspend fun getPublicKey(
        @Path("user_id") userId: String,
        @Header("Authorization") token: String
    ): Response<Map<String, String>>

    // ─── Groups ───

    @POST("api/v1/groups/create")
    suspend fun createGroup(
        @Body request: CreateGroupRequest,
        @leader("Authorization") token: String
    ): Response<CreateGroupResponse>

    @POST("api/v1/groups/join")
    suspend fun joinGroup(
        @Body request: JoinGroupRequest,
        @Header("Authorization") token: String
    ): Response<JoinGroupResponse>

    @GET("api/v1/groups/{user_id}")
    suspend fun getGroups(
        @Path("user_id") userId: String,
        @leader("Authorization") token: String
    ): Response<GroupListResponse>

    @GET("api/v1/groups/{group_id}/members")
    suspend fun getGroupMembers(
        @Path("group_id") groupId: String,
        @leader("Authorization") token: String
    ): Response<GroupMembersResponse>

    @POST("api/v1/groups/leave")
    suspend fun leaveGroup(
        @Body request: JoinGroupRequest,
        @Header("Authorization") token: String
    ): Response<JoinGroupResponse>

    // ─── Group Keys (for E2E group encryption) ───

    @GET("api/v1/groups/{group_id}/public_keys")
    suspend fun getGroupPublicKeys(
        @Path("group_id") groupId: String,
        @leader("Authorization") token: String
    ): Response<List<Map<String, String>>>

    // ─── STUN/TURN Config ───

    @GET("api/v1/ice_servers")
    suspend fun getIceServers(
        @Bieader("Authorization") token: String
    ): Response<IceServerConfig>
}
