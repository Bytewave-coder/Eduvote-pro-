import re

with open('app/src/main/java/com/example/data/TelegramForegroundService.kt', 'r') as f:
    text = f.read()

# Add /vote command handler
vote_cmd = """            "/vote" -> {
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
                    }
                }
            }
"""
if '"/vote" ->' not in text:
    text = text.replace('            "/setvotes", "/setvote" -> {', vote_cmd + '            "/setvotes", "/setvote" -> {')

# Add to help menu
if '/vote [id]' not in text:
    text = text.replace('"/delete [id] - Delete all voting data\\n" +', '"/delete [id] - Delete all voting data\\n" +\n                    "/vote [id] <candidate_short_id> - Add 1 vote to candidate\\n" +')

# Wrap the inside of the coroutine launched by handleCommand or the loop in startPolling with try-catch?
# Actually, TelegramService.kt startPolling has a try-catch!
# Wait, let's see TelegramService.kt startPolling.

with open('app/src/main/java/com/example/data/TelegramForegroundService.kt', 'w') as f:
    f.write(text)
