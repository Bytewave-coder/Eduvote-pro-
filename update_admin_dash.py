import re

with open('app/src/main/java/com/example/ui/screens/admin/AdminDashboardScreen.kt', 'r') as f:
    text = f.read()

import_str = "import com.example.ui.theme.AppUiStyle"
if import_str not in text:
    text = text.replace("import com.example.ui.MainViewModel", "import com.example.ui.MainViewModel\\n" + import_str)

old_settings_block = """                            // App Icon Upload Section"""

new_settings_block = """                            // UI Styling Section
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(com.example.ui.Translator.tr("App UI Design & Theme"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(com.example.ui.Translator.tr("Select a custom layout theme. This will instantly change the appearance of the dashboard and voting screens."), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    val currentStyle by viewModel.uiStyle.collectAsState()
                                    
                                    AppUiStyle.values().forEach { style ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            androidx.compose.material3.RadioButton(
                                                selected = currentStyle == style,
                                                onClick = { viewModel.setUiStyle(style) },
                                                colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(style.displayName, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                            
                            // App Icon Upload Section"""

text = text.replace(old_settings_block, new_settings_block)

with open('app/src/main/java/com/example/ui/screens/admin/AdminDashboardScreen.kt', 'w') as f:
    f.write(text)

