package com.example.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

object SessionWaitingManager {
    private var tickingMediaPlayer: MediaPlayer? = null
    private const val WINNER_CHANNEL_ID = "winner_notification_channel"

    @Synchronized
    fun startTickingSound(context: Context) {
        if (tickingMediaPlayer == null) {
            try {
                tickingMediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.dragon_studio_clock_ticking_sfx_467486).apply {
                    isLooping = true
                    start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            try {
                if (tickingMediaPlayer?.isPlaying == false) {
                    tickingMediaPlayer?.start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Synchronized
    fun stopTickingSound() {
        try {
            tickingMediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            tickingMediaPlayer = null
        }
    }

    fun playAlarmSoundAndNotify(context: Context) {
        stopTickingSound()
        
        // Play alarm sound
        try {
            val alarmPlayer = MediaPlayer.create(context.applicationContext, R.raw.mixkit_classic_alarm_995)
            alarmPlayer?.setOnCompletionListener { mp ->
                mp.release()
            }
            alarmPlayer?.start()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Vibrate phone
        vibratePhone(context)

        // Send Notification
        sendWinnerNotification(context)
    }

    fun vibratePhone(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val wave = longArrayOf(0, 500, 250, 500, 250, 500)
                vibrator.vibrate(VibrationEffect.createWaveform(wave, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 500, 250, 500, 250, 500), -1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendWinnerNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    WINNER_CHANNEL_ID,
                    "Winner Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, WINNER_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("🏆 Session Winner Unlocked!")
                .setContentText("hey check out the winner candidate.")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            manager.notify(2001, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
