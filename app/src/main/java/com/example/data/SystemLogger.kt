package com.example.data

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SystemLogger {
    private const val FILE_NAME = "app_logs.txt"

    fun logEvent(context: Context, eventType: String, details: String) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val logEntry = "[$timestamp] $eventType: $details\n"
            file.appendText(logEntry)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLogs(context: Context): String {
        return try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) {
                val lines = file.readLines()
                if (lines.size > 50) {
                    lines.takeLast(50).joinToString("\n")
                } else {
                    lines.joinToString("\n")
                }
            } else {
                "No logs available."
            }
        } catch (e: Exception) {
            "Error reading logs: ${e.message}"
        }
    }

    fun clearLogs(context: Context) {
        try {
            val file = File(context.filesDir, FILE_NAME)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
