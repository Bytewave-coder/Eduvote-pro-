import re
with open('app/src/main/java/com/example/data/TelegramForegroundService.kt', 'r') as f:
    text = f.read()

patch = """
        if (parts.size >= 2) {
            val potentialId = parts[1].uppercase()
            val idRegex = Regex("^[A-F0-9]{6}$")
            if (potentialId == "ALL" || idRegex.matches(potentialId)) {
                targetId = potentialId
                args = if (parts.size >= 3) command.substringAfter(parts[1]).trim() else ""
            } else {
                args = command.removePrefix("$cmd ").trim()
            }
        }
"""
text = re.sub(r'\s*if \(parts\.size >= 2\) \{.*?\}(?=\s*if \(cmd == "/help" \|\| cmd == "/start"\))', patch, text, flags=re.DOTALL)

with open('app/src/main/java/com/example/data/TelegramForegroundService.kt', 'w') as f:
    f.write(text)
