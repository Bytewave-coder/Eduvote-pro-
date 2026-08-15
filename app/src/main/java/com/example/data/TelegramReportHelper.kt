package com.example.data

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.example.ui.ExportChartHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TelegramReportHelper {
    private const val TAG = "TelegramReportHelper"
    private const val PREFS_NAME = "telegram_report_prefs"
    private const val KEY_REPORTED_IDS = "reported_election_ids"

    fun isReported(context: Context, electionId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_REPORTED_IDS, emptySet()) ?: emptySet()
        return set.contains(electionId)
    }

    fun markReported(context: Context, electionId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val set = prefs.getStringSet(KEY_REPORTED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
        set.add(electionId)
        prefs.edit().putStringSet(KEY_REPORTED_IDS, set).apply()
    }

    suspend fun sendElectionCompletedReport(context: Context, electionId: String, force: Boolean = false) {
        if (!force && isReported(context, electionId)) {
            Log.d(TAG, "Election $electionId report already sent, skipping.")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(context)
                val dao = db.eduVoteDao()

                val event = dao.getElectionById(electionId).firstOrNull() ?: return@withContext
                val candidateMap = dao.getCandidatesWithStudentsForElection(electionId).firstOrNull() ?: emptyMap()

                val candidateList = candidateMap.entries.map { 
                    CandidateWithStudent(it.key, it.value) 
                }.sortedByDescending { it.candidate.voteCount }

                val totalVotes = candidateList.sumOf { it.candidate.voteCount }

                // Determine winner
                val topCandidate = candidateList.firstOrNull()
                val isTie = if (candidateList.size > 1 && topCandidate != null) {
                    candidateList[1].candidate.voteCount == topCandidate.candidate.voteCount && topCandidate.candidate.voteCount > 0
                } else false

                val hasVotes = (topCandidate?.candidate?.voteCount ?: 0) > 0

                val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault())
                val startTimeStr = dateFormat.format(Date(event.startDateMillis))
                val completedTimeStr = event.completedTimeMillis?.let { dateFormat.format(Date(it)) } ?: "Just now"
                val currentTimeStr = dateFormat.format(Date())

                // Device & Battery details
                val (batteryPct, isCharging) = getBatteryInfo(context)
                val batteryState = if (isCharging) "$batteryPct% (🔌 Charging)" else "$batteryPct% (🔋 Discharging)"

                val reportSb = java.lang.StringBuilder()
                reportSb.append("🎉 *ELECTION COMPLETED & FINAL RESULTS REPORT* 🎉\n\n")

                reportSb.append("📌 *ELECTION DETAILS*\n")
                reportSb.append("• *Title*: ${event.title}\n")
                reportSb.append("• *Session ID*: `${event.id}`\n")
                reportSb.append("• *Target*: Class ${event.classTarget} - Section ${event.sectionTarget}\n")
                reportSb.append("• *Type*: ${event.electionType}\n")
                reportSb.append("• *Started At*: $startTimeStr\n")
                reportSb.append("• *Completed At*: $completedTimeStr\n\n")

                reportSb.append("🏆 *WINNER ANNOUNCEMENT*\n")
                if (!hasVotes) {
                    reportSb.append("⚠️ No votes were cast in this election session.\n\n")
                } else if (isTie) {
                    val tiedCandidates = candidateList.filter { it.candidate.voteCount == topCandidate!!.candidate.voteCount }
                    reportSb.append("🤝 *TIE RESULT* between ${tiedCandidates.size} candidates with ${topCandidate!!.candidate.voteCount} votes each:\n")
                    tiedCandidates.forEach { c ->
                        reportSb.append("   👑 *${c.student.name}* (${c.candidate.partyName}) - Roll: ${c.student.rollNumber}\n")
                    }
                    reportSb.append("\n")
                } else if (topCandidate != null) {
                    val winnerPct = if (totalVotes > 0) String.format(Locale.US, "%.1f", (topCandidate.candidate.voteCount.toDouble() / totalVotes) * 100) else "0.0"
                    reportSb.append("👑 *Winner*: ${topCandidate.student.name}\n")
                    reportSb.append("🚩 *Party*: ${topCandidate.candidate.partyName}\n")
                    reportSb.append("🗳 *Votes Received*: ${topCandidate.candidate.voteCount} ($winnerPct%)\n")
                    reportSb.append("🆔 *Roll No*: ${topCandidate.student.rollNumber} | *Class*: ${topCandidate.student.classNum}-${topCandidate.student.section}\n")
                    reportSb.append("🎓 *Admission No*: ${topCandidate.student.admissionNumber}\n\n")
                }

                reportSb.append("📊 *FULL CANDIDATE RESULTS*\n")
                if (candidateList.isEmpty()) {
                    reportSb.append("No candidates registered.\n")
                } else {
                    candidateList.forEachIndexed { index, c ->
                        val medal = when (index) {
                            0 -> "🥇"
                            1 -> "🥈"
                            2 -> "🥉"
                            else -> "🔹"
                        }
                        val pct = if (totalVotes > 0) String.format(Locale.US, "%.1f", (c.candidate.voteCount.toDouble() / totalVotes) * 100) else "0.0"
                        reportSb.append("$medal *${index + 1}. ${c.student.name}*\n")
                        reportSb.append("   └ Party: *${c.candidate.partyName}* | Votes: *${c.candidate.voteCount}* ($pct%)\n")
                        reportSb.append("   └ Roll: `${c.student.rollNumber}` | Adm: `${c.student.admissionNumber}`\n")
                    }
                }
                reportSb.append("\n")

                reportSb.append("📈 *ELECTION STATS SUMMARY*\n")
                reportSb.append("• *Total Votes Cast*: $totalVotes\n")
                reportSb.append("• *Total Candidates*: ${candidateList.size}\n")
                reportSb.append("• *Waiting Lock Period*: ${if (event.isWaitingPeriodActive()) "🔒 Active (6.5h Security Lock)" else "🔓 Unlocked/Finished"}\n\n")

                reportSb.append("📱 *DEVICE & HOST DETAILS*\n")
                reportSb.append("• *Device ID*: `${TelegramService.DEVICE_ID}`\n")
                reportSb.append("• *Model*: `${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}` (`${Build.PRODUCT}`)\n")
                reportSb.append("• *Android OS*: `Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})`\n")
                reportSb.append("• *Battery*: `$batteryState`\n")
                reportSb.append("• *Reported Time*: `$currentTimeStr`\n")

                // Send main report text to Telegram
                TelegramService.sendMessage(reportSb.toString())

                // Send visual Chart image if candidates exist
                if (candidateList.isNotEmpty()) {
                    val chartFile = ExportChartHelper.generateChart(context, event.title, candidateList)
                    if (chartFile.exists()) {
                        TelegramService.sendPhoto(chartFile, "📊 Election Results Chart: ${event.title}")
                    }
                }

                // Send Winner Spotlight image if winner exists and has photo/logo
                if (hasVotes && !isTie && topCandidate != null) {
                    val winnerFile = ExportChartHelper.generateWinnerImage(context, event.title, topCandidate)
                    if (winnerFile != null && winnerFile.exists()) {
                        TelegramService.sendPhoto(winnerFile, "🏆 Winner Spotlight: ${topCandidate.student.name} (${topCandidate.candidate.partyName})")
                    }
                }

                // Mark as reported to prevent duplicate messages
                markReported(context, electionId)
                Log.i(TAG, "Successfully sent Telegram completion report for election $electionId")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to send election completion report for $electionId", e)
            }
        }
    }

    private fun getBatteryInfo(context: Context): Pair<Int, Boolean> {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            val isCharging = bm?.isCharging ?: false
            Pair(if (level < 0) 100 else level, isCharging)
        } catch (e: Exception) {
            Pair(100, false)
        }
    }
}
