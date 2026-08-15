import re
with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

# Make isReadyFlow true initially and remove the Initializing UI
text = text.replace('val isReadyFlow = kotlinx.coroutines.flow.MutableStateFlow(false)', 'val isReadyFlow = kotlinx.coroutines.flow.MutableStateFlow(true)')
text = text.replace('isReadyFlow.value = true', '// isReadyFlow is already true')

# Update targetId parsing in MainActivity.kt
patch = """
            if (parts.size >= 2) {
                val potentialId = parts[1].uppercase()
                val idRegex = Regex("^[A-F0-9]{6}$")
                if (potentialId == "ALL" || idRegex.matches(potentialId)) {
                    targetId = potentialId
                    if (parts.size >= 3) {
                        args = command.substring(cmd.length + 1 + potentialId.length).trim()
                    }
                } else {
                    args = command.substring(cmd.length).trim()
                }
            }
"""
text = re.sub(r'\s*if \(parts\.size >= 2\) \{.*?\}\s*\}', patch, text, flags=re.DOTALL)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)
