package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Telegram Bot API wrapper used by [TelegramForegroundService] and the UI.
 *
 * Credentials are read from BuildConfig, which the Secrets Gradle Plugin
 * populates from the local `.env` file at build time. Copy `.env.example`
 * to `.env` and fill in your own token / chat id before building.
 */
object TelegramService {

    private const val TAG = "TelegramService"
    private var api: TelegramApi? = null

    // Read from BuildConfig (populated from .env via the Secrets Gradle Plugin).
    // Falls back to the placeholder strings below when the keys are not set.
    private val TOKEN: String
        get() = try {
            BuildConfig::class.java
                .getField("TELEGRAM_BOT_TOKEN")
                .get(null) as? String
        } catch (e: Exception) {
            null
        } ?: "YOUR_TELEGRAM_BOT_TOKEN"

    const val CHAT_ID: String = "YOUR_TELEGRAM_CHAT_ID"

    fun getApi(): TelegramApi? {
        if (api != null) return api
