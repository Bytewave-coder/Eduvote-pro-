import re

with open('app/src/main/java/com/example/MainActivity.kt', 'r') as f:
    text = f.read()

old_content = """        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            
            EduVoteTheme(darkTheme = isDarkMode) {"""

new_content = """        setContent {
            val isDarkMode by viewModel.isDarkMode.collectAsState()
            val uiStyle by viewModel.uiStyle.collectAsState()
            
            EduVoteTheme(darkTheme = isDarkMode, uiStyle = uiStyle) {"""

text = text.replace(old_content, new_content)

with open('app/src/main/java/com/example/MainActivity.kt', 'w') as f:
    f.write(text)

