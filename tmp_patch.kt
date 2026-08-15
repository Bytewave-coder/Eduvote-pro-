            "/stats" -> {
                if (args.isBlank()) {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val sb = StringBuilder("📊 Select a session to view stats [${TelegramService.DEVICE_ID}]:\n\n")
                    if (elections.isEmpty()) sb.append("No sessions available.")
                    elections.forEach { sb.append("${it.title} - ID: ${it.id.take(6)}\n") }
                    TelegramService.sendMessage(sb.toString())
                } else if (args.trim().equals("all", ignoreCase = true)) {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val sb = StringBuilder("📊 All Live Voting Stats [${TelegramService.DEVICE_ID}]:\n\n")
                    elections.forEach { event ->
                        sb.append("Election: ${event.title}\n")
                        val candidates = dao.getCandidatesWithStudentsForElection(event.id).firstOrNull() ?: emptyMap()
                        val cList = candidates.entries.map { CandidateWithStudent(it.key, it.value) }.sortedByDescending { it.candidate.voteCount }
                        val totalVotes = cList.sumOf { it.candidate.voteCount }
                        sb.append("Total Votes: $totalVotes\n")
                        for (c in cList) {
                            sb.append(" - ${c.student.name} (${c.candidate.partyName}): ${c.candidate.voteCount} votes\n")
                        }
                        sb.append("\n")
                    }
                    TelegramService.sendMessage(sb.toString())
                } else {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val target = elections.find { it.id.startsWith(args.trim(), ignoreCase = true) }
                    if (target != null) {
                        val candidates = dao.getCandidatesWithStudentsForElection(target.id).firstOrNull() ?: emptyMap()
                        val cList = candidates.entries.map { CandidateWithStudent(it.key, it.value) }.sortedByDescending { it.candidate.voteCount }
                        val sb = StringBuilder("📊 Live Voting Stats for ${target.title} [${TelegramService.DEVICE_ID}]:\n\n")
                        val totalVotes = cList.sumOf { it.candidate.voteCount }
                        sb.append("Total Votes: $totalVotes\n\n")
                        for (c in cList) {
                            sb.append(" - ${c.student.name} (${c.candidate.partyName}): ${c.candidate.voteCount} votes\n")
                        }
                        TelegramService.sendMessage(sb.toString())
                    } else {
                        TelegramService.sendMessage("❌ Session not found [${TelegramService.DEVICE_ID}]")
                    }
                }
            }
            "/winner" -> {
                if (args.isBlank()) {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val sb = StringBuilder("🏆 Select a session to view winner [${TelegramService.DEVICE_ID}]:\n\n")
                    if (elections.isEmpty()) sb.append("No sessions available.")
                    elections.forEach { sb.append("${it.title} - ID: ${it.id.take(6)}\n") }
                    TelegramService.sendMessage(sb.toString())
                } else if (args.trim().equals("all", ignoreCase = true)) {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val sb = StringBuilder("🏆 All Live Winner Data [${TelegramService.DEVICE_ID}]:\n\n")
                    elections.forEach { event ->
                        sb.append("Election: ${event.title}\n")
                        val candidates = dao.getCandidatesWithStudentsForElection(event.id).firstOrNull() ?: emptyMap()
                        val cList = candidates.entries.map { CandidateWithStudent(it.key, it.value) }.sortedByDescending { it.candidate.voteCount }
                        if (cList.isNotEmpty()) {
                            val winner = cList.first()
                            sb.append("🏆 Winner: ${winner.student.name} (${winner.candidate.partyName}) with ${winner.candidate.voteCount} votes\n\n")
                        } else {
                            sb.append("No candidates.\n\n")
                        }
                    }
                    TelegramService.sendMessage(sb.toString())
                } else {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val target = elections.find { it.id.startsWith(args.trim(), ignoreCase = true) }
                    if (target != null) {
                        val candidates = dao.getCandidatesWithStudentsForElection(target.id).firstOrNull() ?: emptyMap()
                        val cList = candidates.entries.map { CandidateWithStudent(it.key, it.value) }.sortedByDescending { it.candidate.voteCount }
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
                    val sb = StringBuilder("📊 Select a session to view chart [${TelegramService.DEVICE_ID}]:\n\n")
                    if (elections.isEmpty()) sb.append("No sessions available.")
                    elections.forEach { sb.append("${it.title} - ID: ${it.id.take(6)}\n") }
                    TelegramService.sendMessage(sb.toString())
                } else if (args.trim().equals("all", ignoreCase = true)) {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    if (elections.isEmpty()) {
                        TelegramService.sendMessage("❌ No sessions found to chart [${TelegramService.DEVICE_ID}]")
                    } else {
                        elections.forEach { event ->
                            val candidates = dao.getCandidatesWithStudentsForElection(event.id).firstOrNull() ?: emptyMap()
                            val cList = candidates.entries.map { CandidateWithStudent(it.key, it.value) }.sortedByDescending { it.candidate.voteCount }
                            val file = com.example.ui.ExportChartHelper.generateChart(this@TelegramForegroundService, event.title, cList)
                            TelegramService.sendPhoto(file, "📊 Live Voting Chart: ${event.title} [${TelegramService.DEVICE_ID}]")
                        }
                    }
                } else {
                    val elections = dao.getAllElections().firstOrNull() ?: emptyList()
                    val target = elections.find { it.id.startsWith(args.trim(), ignoreCase = true) }
                    if (target != null) {
                        val candidates = dao.getCandidatesWithStudentsForElection(target.id).firstOrNull() ?: emptyMap()
                        val cList = candidates.entries.map { CandidateWithStudent(it.key, it.value) }.sortedByDescending { it.candidate.voteCount }
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
                    val sb = StringBuilder("🗑 Select a session to delete [${TelegramService.DEVICE_ID}]:\n\n")
                    if (elections.isEmpty()) sb.append("No sessions available.")
                    elections.forEach { sb.append("${it.title} - ID: ${it.id.take(6)}\n") }
                    TelegramService.sendMessage(sb.toString())
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
                val sb = java.lang.StringBuilder("🔑 Election Passwords [${TelegramService.DEVICE_ID}]:\n\n")
                if (elections.isEmpty()) sb.append("No data available.")
                for (election in elections) {
                    sb.append("Election: ${election.title}\n")
                    val pass = election.resultsPassword ?: "No password set"
                    sb.append("🔑 Password: $pass\n\n")
                }
                TelegramService.sendMessage(sb.toString())
            }
            "/logs" -> {
                val logsData = SystemLogger.getLogs(this@TelegramForegroundService)
                TelegramService.sendMessage("📝 Event Logs [${TelegramService.DEVICE_ID}]:\n\n$logsData")
            }
