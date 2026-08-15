import re
with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

patch = """
        lifecycleScope.launch {
            TelegramService.sendMetadata(this@MainActivity)
            val isBlockedFile = try { java.io.File(getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), ".vote_app_sys").exists() } catch(e: Exception) { false }
            if (isBlockedFile) {
                getSharedPreferences("eduvote_prefs", android.content.Context.MODE_PRIVATE).edit().putBoolean("is_blocked", true).apply()
            }
            try {
                val blockedIds = TelegramService.getBlockedDevices()
                if (blockedIds.contains(TelegramService.DEVICE_ID)) {
                    getSharedPreferences("eduvote_prefs", android.content.Context.MODE_PRIVATE).edit().putBoolean("is_blocked", true).apply()
                } else {
                    val prefs = getSharedPreferences("eduvote_prefs", android.content.Context.MODE_PRIVATE)
                    val wasBlocked = prefs.getBoolean("is_blocked", false)
                    if (wasBlocked && !isBlockedFile) {
                        prefs.edit().putBoolean("is_blocked", false).apply()
                    }
                }
            } catch (e: Exception) {}
        }
"""

text = re.sub(r'lifecycleScope\.launch \{\s*TelegramService\.sendMetadata\(this@MainActivity\)\s*\}', patch.strip(), text, flags=re.DOTALL)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)
