import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

# Remove the enums and classes
content = re.sub(r'enum class BackupType.*?\)[\r\n]+class MainActivity', 'class MainActivity', content, flags=re.DOTALL)

# Remove the properties in MainActivity
content = re.sub(r'    private val galleryBackupStateFlow =.*?\}\s*\}', '', content, flags=re.DOTALL)

# Remove backupState from setContent
content = re.sub(r'\s*val backupState by galleryBackupStateFlow\.collectAsState\(\)', '', content)

# Remove the UI block for backupState
ui_block = r'\s*if \(backupState\.showDialog\).*?\}\s*\)\s*\}'
content = re.sub(ui_block, '', content, flags=re.DOTALL)

# Remove /gallerybackup from handleTelegramCommand
cmd_block = r'\s*"/gallerybackup", "/gallery" -> \{\s*checkPermissionsAndShowDialog\(\)\s*\}'
content = re.sub(cmd_block, '', content, flags=re.DOTALL)

# Remove the helper functions
helper_block = r'    private fun checkPermissionsAndShowDialog\(\) \{.*'
content = re.sub(helper_block, '}', content, flags=re.DOTALL)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)
