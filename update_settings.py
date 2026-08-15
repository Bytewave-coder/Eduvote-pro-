import re

with open('app/src/main/java/com/example/ui/screens/admin/AdminDashboardScreen.kt', 'r') as f:
    content = f.read()

settings_start = content.find('} else if (targetTab == "settings") {')
if settings_start != -1:
    settings_end = content.find('} else if (targetTab == "logout") {', settings_start)
    if settings_end == -1:
        # try to find the end of the settings block, which is before the end of the Scaffold or Box
        settings_end = content.find('}\n        }\n    }\n}', settings_start)
    
    if settings_end != -1:
        prefix = content[:settings_start]
        suffix = content[settings_end:]
        
        body = """} else if (targetTab == "settings") {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                com.example.ui.Translator.tr("App Settings"),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            )
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            // App Icon Upload Section
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text("Custom App Icon", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(customAppIconUri ?: "https://api.dicebear.com/9.x/avataaars/png?seed=$avatarSeed")
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Custom App Icon",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(72.dp)
                                                .clip(CircleShape)
                                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(20.dp))
                                        Column {
                                            Button(
                                                onClick = { imagePickerLauncher.launch("image/*") },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Upload Picture")
                                            }
                                            if (customAppIconUri != null) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                TextButton(onClick = { viewModel.setCustomAppIconUri(null) }) {
                                                    Text("Reset to Avatar", color = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    // Notifications
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(com.example.ui.Translator.tr("Enable Notifications"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Get alerts for election results", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(
                                            checked = notificationsEnabled,
                                            onCheckedChange = { viewModel.toggleNotifications() }
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    // Avatar Seed
                                    Text(com.example.ui.Translator.tr("Avatar Image Seed"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = avatarSeed,
                                        onValueChange = { viewModel.setAvatarSeed(it) },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text(com.example.ui.Translator.tr("Enter a name for your avatar")) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                        )
                                    )
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    // Language
                                    Text(com.example.ui.Translator.tr("Language"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    var expanded by remember { mutableStateOf(false) }
                                    Box(modifier = Modifier.fillMaxWidth()) {
                                        OutlinedTextField(
                                            value = appLanguage,
                                            onValueChange = {},
                                            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                                            enabled = false,
                                            shape = RoundedCornerShape(12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                                disabledBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                            )
                                        )
                                        DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(com.example.ui.Translator.tr("English")) },
                                                onClick = { viewModel.setAppLanguage("English"); expanded = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(com.example.ui.Translator.tr("Hindi")) },
                                                onClick = { viewModel.setAppLanguage("Hindi"); expanded = false }
                                            )
                                        }
                                        Spacer(modifier = Modifier.matchParentSize().clickable { expanded = true })
                                    }
                                }
                            }
                            
                            // App Specs Card
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(com.example.ui.Translator.tr("App Specifications"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(com.example.ui.Translator.tr("Version: 1.0.0 Pro\\nBuild: 2024\\nDeveloped in Kotlin & Jetpack Compose"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                """
        with open('app/src/main/java/com/example/ui/screens/admin/AdminDashboardScreen.kt', 'w') as f:
            f.write(prefix + body + suffix)
        print("Updated settings.")
    else:
        print("Failed to find end.")
else:
    print("Failed to find start.")

