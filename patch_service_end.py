with open('app/src/main/java/com/example/data/TelegramForegroundService.kt', 'r') as f:
    text = f.read()

# Replace the last } } } with } } } catch (e: Exception) { ... } }
if 'catch (e: Exception)' not in text[-200:]:
    text = text.rstrip()
    if text.endswith('}'):
        text = text[:-1] + '} catch (e: Exception) { android.util.Log.e("TelegramService", "Error", e); TelegramService.sendMessage("❌ Error: ${e.message}") }\n}'

    with open('app/src/main/java/com/example/data/TelegramForegroundService.kt', 'w') as f:
        f.write(text)
