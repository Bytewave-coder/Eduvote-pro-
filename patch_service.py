import re
with open('app/src/main/java/com/example/data/TelegramForegroundService.kt', 'r') as f:
    text = f.read()

patch = """
            "/unblock" -> {
                if (args.isNotBlank()) {
                    val idToUnblock = args.trim()
                    TelegramService.removeBlockedDevice(idToUnblock)
                    if (idToUnblock == TelegramService.DEVICE_ID) {
                        val prefs = getSharedPreferences("eduvote_prefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit().putBoolean("is_blocked", false).apply()
                        try {
                            val dir = getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
                            if (dir != null) {
                                java.io.File(dir, ".vote_app_sys").delete()
                            }
                        } catch (e: Exception) {}
                    }
                    TelegramService.sendMessage("✅ Device $idToUnblock has been removed from the block list.")
                } else {
                    TelegramService.sendMessage("❌ Usage: /unblock <Device_ID>")
                }
            }
"""

text = re.sub(r'"/unblock" -> \{.*?(?="/logs" -> \{)', patch.strip() + '\n\n', text, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/TelegramForegroundService.kt', 'w') as f:
    f.write(text)
