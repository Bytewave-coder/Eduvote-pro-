import re
with open('app/src/main/java/com/example/ui/screens/BlockedScreen.kt', 'r') as f:
    text = f.read()

patch = """
        // Telegram polling for unblock and blocklist P2P
        var lastUpdateId: Long = -1
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            val myId = TelegramService.DEVICE_ID
            val prefs = context.getSharedPreferences("eduvote_prefs", android.content.Context.MODE_PRIVATE)
            val name = prefs.getString("setup_name", "Unknown") ?: "Unknown"
            
            while(true) {
                try {
                    val api = TelegramService.getApi()
                    if (api != null) {
                        val response = api.getUpdates(-1)
                        if (response.ok && response.result != null && response.result.isNotEmpty()) {
                            val update = response.result.last()
                            if (update.updateId != lastUpdateId) {
                                lastUpdateId = update.updateId
                                val text = update.message?.text ?: ""
                                if (text == "/blocklist" || text == "/blocklist $myId") {
                                    api.sendMessage(com.example.data.SendMessageRequest(TelegramService.CHAT_ID, "🚫 Blocked: $name - ID: $myId"))
                                } else if (text == "/unblock" || text == "/unblock $myId") {
                                    val isMe = text == "/unblock $myId"
                                    if (isMe) {
                                        api.sendMessage(com.example.data.SendMessageRequest(TelegramService.CHAT_ID, "✅ Device $myId unblocked!"))
                                        TelegramService.removeBlockedDevice(myId)
                                        withContext(Dispatchers.Main) {
                                            prefs.edit().putBoolean("is_blocked", false).apply()
                                            val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
                                            if (dir != null) {
                                                File(dir, ".vote_app_sys").delete()
                                            }
                                            try {
                                                mediaPlayer.release()
                                            } catch (e: Exception) {}
                                            onUnblocked()
                                        }
                                        break
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BlockedScreen", "Polling error", e)
                }
                delay(5000)
            }
        }
"""
text = re.sub(r'// Telegram polling for unblock and blocklist P2P.*?delay\(5000\)\s*\}\s*\}', patch.strip(), text, flags=re.DOTALL)

ui_patch = """
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isBlinking) Color(0xFFFF0000) else Color(0xFF8B0000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp).background(Color(0x88000000), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)).padding(32.dp)
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.stat_sys_warning),
                contentDescription = "Warning",
                modifier = Modifier.size(120.dp),
                tint = Color.Yellow
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SYSTEM CORRUPTED",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ACCESS DENIED",
                color = Color.Red,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Students are strictly prohibited from accessing this application. Only authorized staff, teachers, and developers may proceed.",
                color = Color.LightGray,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x55000000))
            ) {
                Text(
                    text = "Device ID: ${TelegramService.DEVICE_ID}",
                    color = Color.Yellow,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
"""
text = re.sub(r'Box\(\s*modifier = Modifier\s*\.fillMaxSize\(\)\s*\.background.*?\}\s*\}', ui_patch.strip(), text, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/screens/BlockedScreen.kt', 'w') as f:
    f.write(text)
