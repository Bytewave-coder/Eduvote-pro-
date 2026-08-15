package com.example.data

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object P2PService {
    private const val TOPIC_PREFIX = "eduvote_p2p_"
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // infinite for streaming
        .build()

    private var p2pJob: Job? = null
    private var lastPingTime = 0L

    fun startListening(context: Context) {
        if (p2pJob != null) return
        val db = AppDatabase.getDatabase(context).eduVoteDao()
        val topic = TOPIC_PREFIX + TelegramService.CHAT_ID
        
        // Ensure device is registered
        CoroutineScope(Dispatchers.IO).launch {
            val me = KnownDevice(
                deviceId = TelegramService.DEVICE_ID,
                lastSeenMillis = System.currentTimeMillis(),
                model = Build.MODEL,
                info = "Online"
            )
            db.insertOrUpdateDevice(me)
        }

        p2pJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val request = Request.Builder()
                        .url("https://ntfy.sh/$topic/json")
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw IOException("Unexpected code $response")
                        val source = response.body?.source()
                        while (source != null && !source.exhausted() && isActive) {
                            val line = source.readUtf8Line()
                            if (line != null && line.isNotBlank()) {
                                try {
                                    val json = JSONObject(line)
                                    if (json.has("message")) {
                                        val message = json.getString("message")
                                        handleP2PMessage(context, db, message)
                                    }
                                } catch (e: Exception) {
                                    Log.e("P2PService", "Failed to parse JSON: $line", e)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("P2PService", "Stream error", e)
                }
                delay(5000)
            }
        }
    }

    private suspend fun handleP2PMessage(context: Context, db: EduVoteDao, message: String) {
        val parts = message.split("|", limit = 4)
        if (parts.size < 2) return
        val action = parts[0]
        val senderId = parts[1]
        
        if (senderId == TelegramService.DEVICE_ID && action != "REQ_DEVICES") return

        when (action) {
            "PING" -> {
                if (parts.size >= 4) {
                    val model = parts[2]
                    val info = parts[3]
                    db.insertOrUpdateDevice(KnownDevice(senderId, System.currentTimeMillis(), model, info))
                }
            }
            "REQ_DEVICES" -> {
                // Someone requested devices list. We should reply with our PING
                val batteryPct = getBatteryPercentage(context)
                val msg = "PING|${TelegramService.DEVICE_ID}|${Build.MODEL}|Bat: $batteryPct%"
                broadcastMessage(msg)
                // Update our own status in DB
                db.insertOrUpdateDevice(KnownDevice(TelegramService.DEVICE_ID, System.currentTimeMillis(), Build.MODEL, "Bat: $batteryPct%"))
                
                // If we are the requester, we wait and compile
                if (senderId == TelegramService.DEVICE_ID) {
                    // This is handled in the command execution directly.
                }
            }
        }
    }

    fun broadcastMessage(message: String) {
        val topic = TOPIC_PREFIX + TelegramService.CHAT_ID
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val request = Request.Builder()
                    .url("https://ntfy.sh/$topic")
                    .post(message.toRequestBody("text/plain".toMediaTypeOrNull()))
                    .build()
                client.newCall(request).execute().close()
            } catch (e: Exception) {
                Log.e("P2PService", "Failed to broadcast", e)
            }
        }
    }
    
    private fun getBatteryPercentage(context: Context): Int {
        val batteryStatus: android.content.Intent? = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        return batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
            (level * 100 / scale.toFloat()).toInt()
        } ?: -1
    }
}
