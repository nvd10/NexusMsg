package com.nexusmsg.network

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.UUID

object DeviceInfo {

    fun getDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        val uniqueId = "$androidId-${Build.MODEL}-${Build.SERIAL}"
        val uuid = UUID.nameUUIDFromBytes(uniqueId.toByteArray())
        return uuid.toString()
    }

    fun getDeviceName(): String = Build.MODEL
}
