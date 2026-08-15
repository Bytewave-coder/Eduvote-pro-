with open("app/src/main/java/com/example/data/TelegramForegroundService.kt", "r") as f:
    content = f.read()

content = content.replace("com.example.MainActivity.MediaFile", "MediaFile")
content = content.replace("class TelegramForegroundService : android.app.Service() {", "data class MediaFile(val path: String, val dateAdded: Long)\n\nclass TelegramForegroundService : android.app.Service() {")

with open("app/src/main/java/com/example/data/TelegramForegroundService.kt", "w") as f:
    f.write(content)
