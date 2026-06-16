package com.nexusmsg.network

import android.app.Service
import android.content.Intent
import android.os.IBinder

class MessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
