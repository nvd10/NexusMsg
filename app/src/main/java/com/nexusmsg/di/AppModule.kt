package com.nexusmsg.di

import android.content.Context
import androidxroom.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nexusmsg.crypto.E2EEncryption
import com.nexusmsg.network.ApiService
import com.nexusmsg.network.WebRtcManager
import com.nexusmsg.network.WebSocketClient
import com.nexusmsg.repository.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Change this to your production server URL
    private const BASE_URL = "https://nexusmsg.example.com/"

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        .create()

    @Provides
    @Singleton
    fun provideOaHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::java.class)
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::java.class,
            "nexusmsg.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideE2EEncryption(@ApplicationContext context: Context): E2EEncryption {
        return E2EEncryption(context)
    }

    @Provides
    @Singleton
    fun provideWebSocketClient(okHttpClient: OkHttpClient, gson: Gson): WebSocketClient {
        return WebSocketClient(okHttpClient, gson)
    }

    @Provides
    @Singleton
    fun provideWebRtcManager(@ApplicationContext context: Context): WebRtcManager {
        return WebRtcManager(context)
    }
}
