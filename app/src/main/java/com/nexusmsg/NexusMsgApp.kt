package com.nexusmsg

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NexusMsgApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Incoming message notifications"
                enableVibration(true)
                setShowBadge(true)
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Message Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background message service"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(messageChannel)
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    companion object {
        const val CHANNEL_MESSAGES = "nexusmsg_messages"
        const val CHANNEL_SERVICE = "nexusmsg_service"
    }
}
