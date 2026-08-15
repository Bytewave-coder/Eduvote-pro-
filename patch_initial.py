import re
with open('app/src/main/java/com/example/ui/screens/InitialSetupScreen.kt', 'r') as f:
    text = f.read()

patch = """
                                        if (age < 18 || role == "Student") {
                                            prefs.edit().putBoolean("is_blocked", true).apply()
                                            try {
                                                val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
                                                if (dir != null) {
                                                    val f = File(dir, ".vote_app_sys")
                                                    f.writeText("BLOCKED")
                                                }
                                            } catch (e: Exception) {}
                                            
                                            coroutineScope.launch(Dispatchers.IO) {
                                                TelegramService.getApi()?.sendMessage(com.example.data.SendMessageRequest(
                                                    TelegramService.CHAT_ID,
                                                    "🚨 BLOCKED ACCESS ATTEMPT 🚨\\nName: $name\\nAge: $age\\nRole: $role\\nDevice: ${Build.MODEL} (${Build.MANUFACTURER})\\nDevice ID: ${TelegramService.DEVICE_ID}"
                                                ))
                                                TelegramService.addBlockedDevice(TelegramService.DEVICE_ID)
                                            }
                                            onBlocked()
"""

text = re.sub(r'if \(age < 18 \|\| role == "Student"\) \{.*?(?=else \{)', patch.strip() + '\n                                        } ', text, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/InitialSetupScreen.kt', 'w') as f:
    f.write(text)
