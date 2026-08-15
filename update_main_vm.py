import re

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'r') as f:
    text = f.read()

import_line = "import com.example.ui.theme.AppUiStyle"
if import_line not in text:
    text = text.replace("import kotlinx.coroutines.launch", "import kotlinx.coroutines.launch\\n" + import_line)

old_init = """    private val prefs = application.getSharedPreferences("edu_vote_prefs", android.content.Context.MODE_PRIVATE)

    // Global password can be removed or kept, let's keep it to not break anything unless needed, but add per election"""

new_init = """    private val prefs = application.getSharedPreferences("edu_vote_prefs", android.content.Context.MODE_PRIVATE)

    private val _uiStyle = MutableStateFlow(AppUiStyle.valueOf(prefs.getString("ui_style", "DEFAULT") ?: "DEFAULT"))
    val uiStyle: StateFlow<AppUiStyle> = _uiStyle.asStateFlow()

    fun setUiStyle(style: AppUiStyle) {
        _uiStyle.value = style
        prefs.edit().putString("ui_style", style.name).apply()
    }

    // Global password can be removed or kept, let's keep it to not break anything unless needed, but add per election"""

text = text.replace(old_init, new_init)

with open('app/src/main/java/com/example/ui/MainViewModel.kt', 'w') as f:
    f.write(text)

