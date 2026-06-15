package com.nexusmsg.network

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.nexusmsg.NexusMsgApp
import com.nexusmsg.R
import com.nexusmsg.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that keeps the WebSocket connection alive.
 */

AAndroidEntryPoint
class MessageService : Service() {

    @Inject
    lateinit var webSocketClient: WebSocketClient

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createServiceNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1001, notification)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        webSocketClient.disconnect()
        super.onDestroy()
    }

    private fun createServiceNotification(): Notification {
        val channelId = NexusMsgApp.CHANNEL_SERVICE

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT || PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("NexusMsg")
            .setContentText("Connected to messaging service")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
