package com.example.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class BootAndRestartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "com.example.RESTART_SERVICE"
        ) {
            val serviceIntent = Intent(context, TelegramForegroundService::class.java)
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
                TelegramJobService.scheduleJob(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
