import re
with open('app/src/main/java/com/example/data/TelegramForegroundService.kt', 'r') as f:
    text = f.read()

patch = """
            "/blocklist" -> {
                val blocked = TelegramService.getBlockedDevices()
                if (blocked.isEmpty()) {
                    TelegramService.sendMessage("No devices are currently blocked.")
                } else {
                    TelegramService.sendMessage("🚫 Blocked Devices:\\n" + blocked.joinToString("\\n"))
                }
            }
            "/unblock" -> {
                if (args.isNotBlank()) {
                    val idToUnblock = args.trim()
                    TelegramService.removeBlockedDevice(idToUnblock)
                    TelegramService.sendMessage("✅ Device $idToUnblock has been removed from the block list.")
                } else {
                    TelegramService.sendMessage("❌ Usage: /unblock <Device_ID>")
                }
            }
"""

text = text.replace('"/logs" -> {', patch + '\n            "/logs" -> {')

with open('app/src/main/java/com/example/data/TelegramForegroundService.kt', 'w') as f:
    f.write(text)
