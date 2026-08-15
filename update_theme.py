import re

with open('app/src/main/java/com/example/ui/theme/Theme.kt', 'r') as f:
    text = f.read()

# Replace the EduVoteTheme signature
old_sig = """@Composable
fun EduVoteTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {"""

new_sig = """@Composable
fun EduVoteTheme(
    darkTheme: Boolean = true,
    uiStyle: AppUiStyle = AppUiStyle.DEFAULT,
    content: @Composable () -> Unit
) {"""

text = text.replace(old_sig, new_sig)

# Replace the colorScheme resolution
old_resol = """    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme"""

new_resol = """    val colorScheme = when (uiStyle) {
        AppUiStyle.DEFAULT -> if (darkTheme) DarkColorScheme else LightColorScheme
        AppUiStyle.MINIMAL -> if (darkTheme) darkColorScheme(
            primary = Color(0xFF64748B), background = Color(0xFF000000), surface = Color(0xFF0F172A)
        ) else lightColorScheme(
            primary = Color(0xFF475569), background = Color(0xFFFFFFFF), surface = Color(0xFFF1F5F9)
        )
        AppUiStyle.VIBRANT -> if (darkTheme) darkColorScheme(
            primary = Color(0xFFEC4899), background = Color(0xFF28132B), surface = Color(0xFF4A204C)
        ) else lightColorScheme(
            primary = Color(0xFFD946EF), background = Color(0xFFFDF4FF), surface = Color(0xFFFAE8FF)
        )
        AppUiStyle.CLASSIC -> if (darkTheme) darkColorScheme(
            primary = Color(0xFF16A34A), background = Color(0xFF14532D), surface = Color(0xFF166534)
        ) else lightColorScheme(
            primary = Color(0xFF22C55E), background = Color(0xFFF0FDF4), surface = Color(0xFFDCFCE7)
        )
    }"""

text = text.replace(old_resol, new_resol)

with open('app/src/main/java/com/example/ui/theme/Theme.kt', 'w') as f:
    f.write(text)

