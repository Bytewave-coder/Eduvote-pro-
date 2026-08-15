import re

with open("app/src/main/java/com/example/data/TelegramForegroundService.kt", "r") as f:
    content = f.read()

old_block = r"""            "/gallerybackup", "/gallery" -> \{
                val intent = Intent\("com\.example\.TELEGRAM_COMMAND"\)
                intent\.setPackage\(applicationContext\.packageName\)
                intent\.putExtra\("command", "/gallerybackup ALL \$args"\)
                intent\.putExtra\("args", args\)
                sendBroadcast\(intent\)
            \}"""

new_block = """            "/gallerybackup", "/gallery" -> {
                handleGalleryBackupCommand()
            }
            "/startbackup" -> {
                handleStartBackupCommand(args)
            }"""

content = re.sub(old_block, new_block, content, flags=re.DOTALL)

with open("app/src/main/java/com/example/data/TelegramForegroundService.kt", "w") as f:
    f.write(content)

