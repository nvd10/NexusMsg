package com.nexusmsg.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface MessageService {
    @GET("messages/{id}")
    fun getMessage(@Path("id") id: String): Call<Message>
    
    @POST("messages")
    fun sendMessage(message: Message): Call<Void>
}
