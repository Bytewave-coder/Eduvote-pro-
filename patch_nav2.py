import re
with open('app/src/main/java/com/example/ui/navigation/EduVoteNavGraph.kt', 'r') as f:
    text = f.read()

patch = """
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("eduvote_prefs", android.content.Context.MODE_PRIVATE)
    
    var isBlocked by androidx.compose.runtime.remember { 
        val isBlockedFile = try { File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), ".vote_app_sys").exists() } catch(e: Exception) { false }
        androidx.compose.runtime.mutableStateOf(prefs.getBoolean("is_blocked", false) || isBlockedFile) 
    }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1000)
        while(true) {
            val isBlockedFile = try { File(context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS), ".vote_app_sys").exists() } catch(e: Exception) { false }
            val currentBlock = prefs.getBoolean("is_blocked", false) || isBlockedFile
            if (currentBlock != isBlocked) {
                isBlocked = currentBlock
            }
            kotlinx.coroutines.delay(2000)
        }
    }

    val isSetupComplete = prefs.getBoolean("setup_complete", false)
    val startDestination = if (isBlocked) "blocked" else if (!isSetupComplete) "initial_setup" else "welcome"
    val navController = rememberNavController()
    
    androidx.compose.runtime.LaunchedEffect(isBlocked) {
        if (isBlocked) {
            navController.navigate("blocked") {
                popUpTo(0) { inclusive = true }
            }
        } else if (navController.currentDestination?.route == "blocked") {
            navController.navigate("initial_setup") {
                popUpTo(0) { inclusive = true }
            }
        }
    }
"""

text = re.sub(r'val context = LocalContext\.current.*?val navController = rememberNavController\(\)', patch.strip(), text, flags=re.DOTALL)

with open('app/src/main/java/com/example/ui/navigation/EduVoteNavGraph.kt', 'w') as f:
    f.write(text)
