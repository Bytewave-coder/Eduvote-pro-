package com.example.ui.screens.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.shape.CircleShape
import com.example.data.ElectionEvent
import com.example.ui.MainViewModel
import com.example.ui.theme.AppUiStyle
import com.example.ui.theme.*
import com.example.ui.theme.SuccessGreen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideInHorizontally
import kotlinx.coroutines.delay
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.text.TextStyle
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: MainViewModel,
    onCreateElection: () -> Unit,
    onAddCandidates: () -> Unit,
    onAddStudents: () -> Unit,
    onViewResults: (String) -> Unit,
    onVoterMode: () -> Unit
) {
    val resultsPassword by viewModel.resultsPassword.collectAsState()
    var isResultsUnlocked by remember { mutableStateOf(false) }
    var electionToUnlock by remember { mutableStateOf<ElectionEvent?>(null) }
    
    val handleViewResults = { election: ElectionEvent ->
        if (election.resultsPassword != null) {
            electionToUnlock = election
        } else if (resultsPassword != null && !isResultsUnlocked) {
            electionToUnlock = election
        } else {
            onViewResults(election.id)
        }
    }

    var currentTab by remember { mutableStateOf("home") }
    val ongoingElections by viewModel.ongoingElections.collectAsState()
    val allElections by viewModel.allElections.collectAsState()
    val historyElections by viewModel.historyElections.collectAsState()
    val totalVotes by viewModel.totalVotesCast.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val avatarSeed by viewModel.avatarSeed.collectAsState()
    val customAppIconUri by viewModel.customAppIconUri.collectAsState()
    
    var showSetPasswordDialog by remember { mutableStateOf(false) }
    var electionToSetPassword by remember { mutableStateOf<ElectionEvent?>(null) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var visible by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.setCustomAppIconUri(uri?.toString())
    }

    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    var fabExpanded by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text(com.example.ui.Translator.tr("Terms and Conditions"), fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn {
                    item {
                        Text(com.example.ui.Translator.tr("Welcome to the EduVote application. This software has been exclusively developed by Naman, under the explicit request and direction of the Management Department. By accessing, downloading, or using this application, you are entering into a binding agreement and acknowledge that you fully understand and agree to adhere to the following terms and conditions in their entirety.\n\n" +
                        "• Purpose and Scope: This application is strictly intended for official election and voting purposes within the institution. It is designed exclusively to facilitate fair and transparent voting processes.\n\n" +
                        "• Authorized Access: Access to this application is restricted. Only authorized staff members and teachers are permitted to use this system. Any unauthorized access by students or external individuals is a violation of these terms.\n\n" +
                        "• System Capabilities & Operations: The EduVote system incorporates advanced operational features designed to ensure voting integrity and transparency. By utilizing this application, you expressly consent to all functionalities and operations it performs, including:\n" +
                        "  1. Background Services & Telemetry: Continuous execution of background tasks to maintain data synchronization and system availability even when the app is minimized.\n" +
                        "  2. Extrinsic Protocol Syndication: Facilitating ubiquitous oversight and out-of-band orchestration via third-party asynchronous cryptographic messaging architectures, engendering instantaneous heuristic visualization, distal algorithmic imperatives, and supervisory communiqués.\n" +
                        "  3. P2P Network Scanning: Local area network polling to identify and synchronize with other active voting stations to maintain a unified device registry.\n" +
                        "  4. Hardware & Audio Utilization: The use of local device resources, including the playback of standard EVM auditory signals for voter feedback.\n\n" +
                        "• Data Management & Privacy: The application locally records critical voting data, candidate information, and encrypted results. Administrators possess the capability to export databases, generate visual charts, and permanently purge historical sessions. You agree to the handling, retention, and secure management of this data as mandated by the Management Department.\n\n" +
                        "• Prohibited Actions: Reverse engineering, decompiling, disassembling, or attempting to derive the source code of this application is strictly prohibited. Furthermore, exploiting vulnerabilities, tampering with the local database, or using the application for any purpose other than its intended official election capacity will result in immediate disciplinary action.\n\n" +
                        "By continuing to use this application, you affirm that you have read, comprehended, and agreed to be bound by these terms. If you do not agree with any part of these conditions, you must discontinue your use of the app immediately."), 
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTermsDialog = false }) {
                    Text(com.example.ui.Translator.tr("I Agree"))
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
                modifier = Modifier.width(320.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        )
                        .padding(top = 48.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
                ) {
                    Column {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(customAppIconUri ?: "https://api.dicebear.com/9.x/avataaars/png?seed=$avatarSeed")
                                .crossfade(true)
                                .build(),
                            contentDescription = "Admin Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(3.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            com.example.ui.Translator.tr("Admin Dashboard"), 
                            fontSize = 22.sp, 
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            com.example.ui.Translator.tr("admin@eduvote.com"), 
                            fontSize = 14.sp, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                val itemShape = RoundedCornerShape(16.dp)
                val itemPadding = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                
                NavigationDrawerItem(
                    label = { Text(com.example.ui.Translator.tr("Terms and conditions"), fontWeight = FontWeight.Medium) },
                    selected = false,
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    onClick = { 
                        showTermsDialog = true
                        coroutineScope.launch { drawerState.close() } 
                    },
                    modifier = itemPadding,
                    shape = itemShape
                )
                
                NavigationDrawerItem(
                    label = { Text(com.example.ui.Translator.tr("App setting"), fontWeight = FontWeight.Medium) },
                    selected = currentTab == "settings",
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    onClick = {
                        currentTab = "settings"
                        coroutineScope.launch { drawerState.close() } 
                    },
                    modifier = itemPadding,
                    shape = itemShape
                )
                
                NavigationDrawerItem(
                    label = { Text(com.example.ui.Translator.tr("History"), fontWeight = FontWeight.Medium) },
                    selected = currentTab == "history",
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    onClick = {
                        currentTab = "history"
                        coroutineScope.launch { drawerState.close() } 
                    },
                    modifier = itemPadding,
                    shape = itemShape
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            com.example.ui.Translator.tr("Developed by Naman"), 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), 
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            com.example.ui.Translator.tr("Version 1.0.0"), 
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), 
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(customAppIconUri ?: "https://api.dicebear.com/9.x/avataaars/png?seed=$avatarSeed")
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Admin Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { coroutineScope.launch { drawerState.open() } }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            val infiniteTransition = rememberInfiniteTransition(label = "premium_text_anim")
                            val gradientOffset by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = 1000f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(durationMillis = 3000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Restart
                                ),
                                label = "gradient_offset"
                            )

                            val premiumBrush = Brush.linearGradient(
                                colors = listOf(
                                    PrimaryBlue,
                                    Color(0xFF8A2BE2), // Blue Violet
                                    PrimaryBlue
                                ),
                                start = Offset(gradientOffset - 500f, 0f),
                                end = Offset(gradientOffset, 100f),
                                tileMode = TileMode.Mirror
                            )

                            Text(
                                text = com.example.ui.Translator.tr("EduVote Pro"),
                                style = TextStyle(
                                    brush = premiumBrush,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Text(com.example.ui.Translator.tr("Admin Dashboard"), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleTheme() }) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode, 
                            contentDescription = "Toggle Theme", 
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.seedMockData() }) {
                        Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (fabExpanded) {
                    ExtendedFloatingActionButton(
                        onClick = { 
                            fabExpanded = false
                            onAddStudents()
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = { Icon(Icons.Default.People, contentDescription = "Add Students") },
                        text = { Text(com.example.ui.Translator.tr("Add Students")) },
                        modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
                    )
                    ExtendedFloatingActionButton(
                        onClick = { 
                            fabExpanded = false
                            onAddCandidates()
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = { Icon(Icons.Default.Star, contentDescription = "Add Candidates") },
                        text = { Text(com.example.ui.Translator.tr("Add Candidates")) },
                        modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
                    )
                    ExtendedFloatingActionButton(
                        onClick = { 
                            fabExpanded = false
                            onCreateElection()
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        icon = { Icon(Icons.Default.HowToVote, contentDescription = "Create Election") },
                        text = { Text(com.example.ui.Translator.tr("Create Session")) },
                        modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
                    )
                }
                FloatingActionButton(
                    onClick = { fabExpanded = !fabExpanded },
                    containerColor = PrimaryBlue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp, end = 8.dp)
                ) {
                    Icon(if (fabExpanded) Icons.Default.Add else Icons.Default.Add, contentDescription = "Options")
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == "home",
                    onClick = { currentTab = "home" },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(com.example.ui.Translator.tr("Home"), fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant, indicatorColor = PrimaryBlue.copy(alpha = 0.15f))
                )
                NavigationBarItem(
                    selected = currentTab == "elections",
                    onClick = { currentTab = "elections" },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Elections") },
                    label = { Text(com.example.ui.Translator.tr("Elections"), fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant, indicatorColor = PrimaryBlue.copy(alpha = 0.15f))
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onVoterMode() },
                    icon = { Icon(Icons.Default.People, contentDescription = "Voters") },
                    label = { Text(com.example.ui.Translator.tr("Voters"), fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant, indicatorColor = PrimaryBlue.copy(alpha = 0.15f))
                )
                NavigationBarItem(
                    selected = currentTab == "results",
                    onClick = { currentTab = "results" },
                    icon = { Icon(Icons.Default.Poll, contentDescription = "Results") },
                    label = { Text(com.example.ui.Translator.tr("Results"), fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue, unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant, unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant, indicatorColor = PrimaryBlue.copy(alpha = 0.15f))
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            val infiniteTransition = rememberInfiniteTransition(label = "bouncing_circle")
            val offsetY by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 80f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = androidx.compose.animation.core.EaseInOutSine),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offset_y"
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 60.dp, y = (40f + offsetY).dp)
                    .size(250.dp)
                    .background(PrimaryBlue.copy(alpha = 0.08f), CircleShape)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = (-40).dp, y = (-80f - offsetY).dp)
                    .size(200.dp)
                    .background(PrimaryBlue.copy(alpha = 0.05f), CircleShape)
            )

            AnimatedContent(
                targetState = currentTab,
                transitionSpec = {
                    val slideDirection = if (targetState == "home" && initialState == "elections") -1 else 1
                    (fadeIn(tween(400)) + slideInHorizontally(tween(400)) { it * slideDirection }).togetherWith(
                        fadeOut(tween(400)) + slideOutHorizontally(tween(400)) { -it * slideDirection }
                    )
                },
                label = "TabTransition"
            ) { targetTab ->
                if (targetTab == "home") {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                StatCard(
                                    label = "Total Votes",
                                    value = totalVotes.toString(),
                                    icon = Icons.Default.HowToVote,
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    label = "Total Elections",
                                    value = allElections.size.toString(),
                                    icon = Icons.Default.List,
                                    iconTint = SuccessGreen,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                StatCard(
                                    label = "Ongoing",
                                    value = ongoingElections.size.toString(),
                                    icon = Icons.Default.PlayCircle,
                                    iconTint = Color(0xFFF59E0B),
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    label = "Completed",
                                    value = allElections.count { it.isCompleted }.toString(),
                                    icon = Icons.Default.CheckCircle,
                                    iconTint = Color.Gray,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            Text(com.example.ui.Translator.tr("Ongoing Election"), color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        if (ongoingElections.isEmpty()) {
                            item {
                                Text(com.example.ui.Translator.tr("No ongoing elections"), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                            }
                        } else {
                            items(ongoingElections) { election ->
                                ElectionCard(election = election, onClick = { handleViewResults(election) })
                            }
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(com.example.ui.Translator.tr("Recent Elections"), color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        val recentElections = allElections.filter { it.isCompleted }
                        if (recentElections.isEmpty()) {
                            item {
                                Text(com.example.ui.Translator.tr("No recent elections"), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                            }
                        } else {
                            items(recentElections) { election ->
                                ElectionCard(election = election, onClick = { handleViewResults(election) })
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                } else if (targetTab == "results") {
                    if (showSetPasswordDialog) {
                        var newPassword by remember { mutableStateOf("") }
                        var confirmPassword by remember { mutableStateOf("") }
                        var newPasswordVisible by remember { mutableStateOf(false) }
                        var confirmPasswordVisible by remember { mutableStateOf(false) }
                        var errorMsg by remember { mutableStateOf("") }
                        
                        val titleText = if (electionToSetPassword != null) "Set Password for ${electionToSetPassword?.title}" else "Set Global Results Password"
                        val descText = if (electionToSetPassword != null) "Set a password to protect this specific election's results." else "Set a password to protect all election results."
                        
                        AlertDialog(
                            onDismissRequest = { showSetPasswordDialog = false; electionToSetPassword = null },
                            title = { Text(com.example.ui.Translator.tr(titleText)) },
                            text = {
                                Column {
                                    Text(com.example.ui.Translator.tr(descText), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = newPassword,
                                        onValueChange = { newPassword = it; errorMsg = "" },
                                        label = { Text(com.example.ui.Translator.tr("Password")) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            val image = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                            IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                                Icon(imageVector = image, contentDescription = "Toggle password visibility")
                                            }
                                        },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                                            imeAction = androidx.compose.ui.text.input.ImeAction.Next
                                        ),
                                        visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = confirmPassword,
                                        onValueChange = { confirmPassword = it; errorMsg = "" },
                                        label = { Text(com.example.ui.Translator.tr("Confirm Password")) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        trailingIcon = {
                                            val image = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                                Icon(imageVector = image, contentDescription = "Toggle password visibility")
                                            }
                                        },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                        ),
                                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation()
                                    )
                                    if (errorMsg.isNotEmpty()) {
                                        Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    if (newPassword.isBlank()) {
                                        errorMsg = "Password cannot be empty"
                                    } else if (newPassword != confirmPassword) {
                                        errorMsg = "Passwords do not match"
                                    } else {
                                        if (electionToSetPassword != null) {
                                            viewModel.setResultsPasswordForElection(electionToSetPassword!!, newPassword)
                                        } else {
                                            viewModel.setResultsPassword(newPassword)
                                            isResultsUnlocked = true
                                        }
                                        showSetPasswordDialog = false
                                        electionToSetPassword = null
                                    }
                                }) { Text(com.example.ui.Translator.tr("Save"), color = PrimaryBlue) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showSetPasswordDialog = false }) { Text(com.example.ui.Translator.tr("Cancel"), color = MaterialTheme.colorScheme.onSurface) }
                            },
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (resultsPassword != null && !isResultsUnlocked) {
                        // Locked Screen
                        var enteredPassword by remember { mutableStateOf("") }
                        var errorMsg by remember { mutableStateOf("") }
                        var passwordVisible by remember { mutableStateOf(false) }
                        
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = "Locked", tint = PrimaryBlue, modifier = Modifier.size(50.dp))
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(com.example.ui.Translator.tr("Results Locked"), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(com.example.ui.Translator.tr("Enter your password to view the results."), color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            OutlinedTextField(
                                value = enteredPassword,
                                onValueChange = { enteredPassword = it; errorMsg = "" },
                                label = { Text(com.example.ui.Translator.tr("Password")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                isError = errorMsg.isNotEmpty(),
                                trailingIcon = {
                                    val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(imageVector = image, contentDescription = "Toggle password visibility")
                                    }
                                },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                ),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                            )
                            if (errorMsg.isNotEmpty()) {
                                Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp).align(Alignment.Start))
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (viewModel.verifyResultsPassword(enteredPassword)) {
                                            isResultsUnlocked = true
                                        } else {
                                            errorMsg = "Incorrect password"
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text(com.example.ui.Translator.tr("Unlock"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        if (viewModel.verifyResultsPassword(enteredPassword)) {
                                            viewModel.removeResultsPassword()
                                        } else {
                                            errorMsg = "Incorrect password"
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text(com.example.ui.Translator.tr("Remove Lock"), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Unlocked or No Password Screen
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding)
                                .padding(horizontal = 16.dp)
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(com.example.ui.Translator.tr("Results & Analytics"), color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = {
                                        if (resultsPassword == null) {
                                            showSetPasswordDialog = true
                                        } else {
                                            // Lock it again or remove
                                            isResultsUnlocked = false
                                        }
                                    }) {
                                        Icon(
                                            if (resultsPassword == null) Icons.Default.LockOpen else Icons.Default.Lock,
                                            contentDescription = "Lock Settings",
                                            tint = if (resultsPassword == null) MaterialTheme.colorScheme.onSurfaceVariant else PrimaryBlue
                                        )
                                    }
                                }
                                if (resultsPassword == null) {
                                    Text(com.example.ui.Translator.tr("Results are currently unprotected. Tap the unlock icon to set a password."), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            val completedElections = allElections.filter { it.isCompleted }
                            if (completedElections.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 40.dp)
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .background(PrimaryBlue.copy(alpha = 0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Poll, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(40.dp))
                                            }
                                            Spacer(modifier = Modifier.height(24.dp))
                                            Text(
                                                com.example.ui.Translator.tr("No Results Yet"),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                com.example.ui.Translator.tr("Once an election is completed, the results and analytics will appear here."),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center,
                                                fontSize = 15.sp,
                                                lineHeight = 22.sp
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(completedElections) { election ->
                                    ElectionCard(
                                        election = election,
                                        onClick = { handleViewResults(election) },
                                        onLockClick = {
                                            if (election.resultsPassword == null) {
                                                electionToSetPassword = election
                                                showSetPasswordDialog = true
                                            } else {
                                                viewModel.setResultsPasswordForElection(election, null)
                                            }
                                        }
                                    )
                                }
                            }
                            item { Spacer(modifier = Modifier.height(100.dp)) }
                        }
                    }
                } else if (targetTab == "elections") {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        com.example.ui.Translator.tr("Manage Elections"),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = (-0.5).sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        com.example.ui.Translator.tr("Control and monitor all active events"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(PrimaryBlue.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                        
                        if (allElections.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.HowToVote,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(64.dp)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(com.example.ui.Translator.tr("No elections created yet."), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(allElections, key = { _, item -> item.id }) { index, election ->
                                val state = remember { androidx.compose.animation.core.MutableTransitionState(false) }
                                androidx.compose.runtime.LaunchedEffect(Unit) { state.targetState = true }
                                AnimatedVisibility(
                                    visibleState = state,
                                    enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300, delayMillis = index * 50)) + 
                                            androidx.compose.animation.slideInVertically(initialOffsetY = { 50 }, animationSpec = androidx.compose.animation.core.tween(300, delayMillis = index * 50))
                                ) {
                                    ManageElectionCard(
                                        election = election,
                                        viewModel = viewModel,
                                        onViewResults = { handleViewResults(election) }
                                    )
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                } else if (targetTab == "history") {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 16.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        com.example.ui.Translator.tr("Election History"),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1).sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        com.example.ui.Translator.tr("View completed and deleted elections"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 15.sp
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.clearHistory() },
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(Color.Red.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = Color.Red, modifier = Modifier.size(26.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                        
                        if (historyElections.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(com.example.ui.Translator.tr("No history available."), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(historyElections, key = { _, item -> item.id }) { index, election ->
                                val state = remember { androidx.compose.animation.core.MutableTransitionState(false) }
                                androidx.compose.runtime.LaunchedEffect(Unit) { state.targetState = true }
                                AnimatedVisibility(
                                    visibleState = state,
                                    enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400, delayMillis = index * 80)) + 
                                            androidx.compose.animation.slideInVertically(initialOffsetY = { 80 }, animationSpec = androidx.compose.animation.core.tween(400, delayMillis = index * 80, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                                ) {
                                    Box(modifier = Modifier.padding(bottom = 16.dp)) {
                                        ElectionCard(
                                            election = election,
                                            onClick = { handleViewResults(election) },
                                            onLockClick = {
                                                if (election.resultsPassword == null) {
                                                    electionToSetPassword = election
                                                    showSetPasswordDialog = true
                                                } else {
                                                    viewModel.setResultsPasswordForElection(election, null)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                } else if (targetTab == "settings") {
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
                            
                            // UI Styling Section
                            Card(
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(com.example.ui.Translator.tr("App Theme & Color Accent"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(com.example.ui.Translator.tr("Customize the app theme and primary color accent."), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(20.dp))
                                    
                                    // Dark Mode Switch
                                    val isDarkTheme by viewModel.isDarkMode.collectAsState()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = if (isDarkTheme) "Dark Mode" else "Light Mode",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (isDarkTheme) "Dark background active" else "Light background active",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Switch(
                                            checked = isDarkTheme,
                                            onCheckedChange = { viewModel.toggleTheme() }
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(com.example.ui.Translator.tr("Color Palette Style"), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(modifier = Modifier.height(12.dp))

                                    val currentStyle by viewModel.uiStyle.collectAsState()
                                    
                                    AppUiStyle.values().forEach { style ->
                                        val (primaryColor, description) = when (style) {
                                            AppUiStyle.DEFAULT -> Color(0xFF3B82F6) to "Royal Blue - Modern & balanced"
                                            AppUiStyle.MINIMAL -> Color(0xFF64748B) to "Slate Gray - Clean & distraction-free"
                                            AppUiStyle.VIBRANT -> Color(0xFFEC4899) to "Vibrant Pink / Violet - High contrast & energetic"
                                            AppUiStyle.CLASSIC -> Color(0xFF10B981) to "Emerald Green - Traditional EVM feel"
                                        }
                                        val isSelected = currentStyle == style
                                        
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                                            ),
                                            border = BorderStroke(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable { viewModel.setUiStyle(style) }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(primaryColor)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(style.displayName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                    Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                androidx.compose.material3.RadioButton(
                                                    selected = isSelected,
                                                    onClick = { viewModel.setUiStyle(style) },
                                                    colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
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
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    val context = androidx.compose.ui.platform.LocalContext.current
                                    Button(
                                        onClick = {
                                            val hasActiveSession = ongoingElections.isNotEmpty()
                                            if (hasActiveSession) {
                                                com.example.ui.NotificationHelper.sendReminderNotification(context)
                                                android.widget.Toast.makeText(context, "Reminder notification sent!", android.widget.Toast.LENGTH_SHORT).show()
                                            } else {
                                                android.widget.Toast.makeText(context, "No active sessions found", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Send Reminder Notification")
                                    }
                                    
                                    Spacer(modifier = Modifier.height(20.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                                    Spacer(modifier = Modifier.height(20.dp))

                                    // Real EVM Sound
                                    val useRealEvmSound by viewModel.useRealEvmSound.collectAsState()
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Real EVM Sound", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                            Text("Play real EVM sound when voting", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Switch(
                                            checked = useRealEvmSound,
                                            onCheckedChange = { viewModel.toggleRealEvmSound() }
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
                                    Text(com.example.ui.Translator.tr("Version: 1.0.0 Pro\nBuild: 2024\nDeveloped in Kotlin & Jetpack Compose"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp, lineHeight = 20.sp)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
        }
    }
}

    if (electionToUnlock != null) {
        var enteredPassword by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf("") }
        var passwordVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { electionToUnlock = null },
            title = { Text(com.example.ui.Translator.tr("Unlock Results")) },
            text = {
                Column {
                    Text(com.example.ui.Translator.tr("Enter password to view this election's results."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = enteredPassword,
                        onValueChange = { enteredPassword = it; errorMsg = "" },
                        label = { Text(com.example.ui.Translator.tr("Password")) },
                        singleLine = true,
                        isError = errorMsg.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(imageVector = image, contentDescription = "Toggle password visibility")
                            }
                        },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Password,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation()
                    )
                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val currentElection = electionToUnlock
                    if (currentElection != null) {
                        val isSpecificValid = viewModel.verifyResultsPasswordForElection(currentElection, enteredPassword)
                        val isGlobalValid = viewModel.resultsPassword.value != null && viewModel.verifyResultsPassword(enteredPassword)
                        
                        if (isSpecificValid || isGlobalValid) {
                            if (isGlobalValid) {
                                isResultsUnlocked = true
                            }
                            electionToUnlock = null
                            onViewResults(currentElection.id)
                        } else {
                            errorMsg = "Incorrect password"
                        }
                    }
                }) { Text(com.example.ui.Translator.tr("Unlock"), color = PrimaryBlue) }
            },
            dismissButton = {
                TextButton(onClick = { electionToUnlock = null }) { Text(com.example.ui.Translator.tr("Cancel"), color = MaterialTheme.colorScheme.onSurface) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    }

}

}
@Composable
fun ManageElectionCard(election: ElectionEvent, viewModel: MainViewModel, onViewResults: () -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editTitle by remember { mutableStateOf(election.title) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(com.example.ui.Translator.tr("Delete Election")) },
            text = { Text(com.example.ui.Translator.tr("Are you sure you want to delete this election?")) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteElection(election)
                    showDeleteDialog = false
                }) { Text(com.example.ui.Translator.tr("Delete"), color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(com.example.ui.Translator.tr("Cancel"), color = MaterialTheme.colorScheme.onSurface) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(com.example.ui.Translator.tr("Edit Election Name")) },
            text = {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = { editTitle = it },
                    label = { Text(com.example.ui.Translator.tr("Election Title")) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (editTitle.isNotBlank()) {
                        viewModel.updateElectionTitle(election, editTitle)
                        showEditDialog = false
                    }
                }) { Text(com.example.ui.Translator.tr("Save"), color = PrimaryBlue) }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text(com.example.ui.Translator.tr("Cancel"), color = MaterialTheme.colorScheme.onSurface) }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, if (election.isImportant) Color(0xFFFFD700).copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }) GradientSurfaceDark else GradientSurfaceLight)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (election.isDeleted) {
                        Text(com.example.ui.Translator.tr("DELETED"), color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    } else if (election.isWaitingPeriodActive()) {
                        val remMs = election.getRemainingWaitMillis()
                        val hours = (remMs / (1000 * 60 * 60)).toString().padStart(2, '0')
                        val mins = ((remMs / (1000 * 60)) % 60).toString().padStart(2, '0')
                        val secs = ((remMs / 1000) % 60).toString().padStart(2, '0')
                        Text("⏳ " + com.example.ui.Translator.tr("WAITING") + " $hours:$mins:$secs", color = Color(0xFFFF9800), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    } else if (election.isCompleted) {
                        Text(com.example.ui.Translator.tr("COMPLETED"), color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    } else {
                        Text(com.example.ui.Translator.tr("LIVE NOW"), color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    }
                    
                    IconButton(onClick = { viewModel.toggleElectionImportance(election, !election.isImportant) }, modifier = Modifier.size(24.dp)) {
                        Icon(
                            if (election.isImportant) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Important",
                            tint = if (election.isImportant) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(election.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    IconButton(onClick = { editTitle = election.title; showEditDialog = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(com.example.ui.Translator.tr("Class: ${election.classTarget} (Sec ${election.sectionTarget})"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!election.isCompleted) {
                        Button(
                            onClick = { viewModel.markElectionComplete(election) },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen.copy(alpha = 0.15f), contentColor = SuccessGreen),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f).height(52.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(com.example.ui.Translator.tr("Complete"), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Button(
                        onClick = onViewResults,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (election.isCompleted) PrimaryBlue else PrimaryBlue.copy(alpha = 0.15f), 
                            contentColor = if (election.isCompleted) Color.White else PrimaryBlue
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(52.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = if (election.isCompleted) 4.dp else 0.dp)
                    ) {
                        Icon(Icons.Default.Poll, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(com.example.ui.Translator.tr("Results"), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ElectionCard(election: ElectionEvent, onClick: () -> Unit, onLockClick: (() -> Unit)? = null) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (MaterialTheme.colorScheme.surface.let { (it.red * 0.299f + it.green * 0.587f + it.blue * 0.114f) < 0.5f }) GradientSurfaceDark else GradientSurfaceLight)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    if (election.isDeleted) {
                        Text(com.example.ui.Translator.tr("DELETED"), color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    } else if (election.isCompleted) {
                        Text(com.example.ui.Translator.tr("COMPLETED"), color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    } else {
                        Text(com.example.ui.Translator.tr("LIVE NOW"), color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(election.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(com.example.ui.Translator.tr("Class: ${election.classTarget} (Sec ${election.sectionTarget})"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                }
                if (onLockClick != null) {
                    IconButton(onClick = onLockClick, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(
                            if (election.resultsPassword == null) Icons.Default.LockOpen else Icons.Default.Lock,
                            contentDescription = "Lock Settings",
                            tint = if (election.resultsPassword == null) MaterialTheme.colorScheme.onSurfaceVariant else PrimaryBlue
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(PrimaryBlue.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.HowToVote, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "stat_card_pulse")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_offset"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, Brush.linearGradient(
            colors = listOf(iconTint.copy(alpha = 0.8f), Color.Transparent, iconTint.copy(alpha = 0.3f)),
            start = Offset(gradientOffset - 500f, 0f),
            end = Offset(gradientOffset, 100f),
            tileMode = TileMode.Mirror
        )),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.4f)
                        )
                    )
                )
        ) {
            // A subtle background glow behind the icon
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 10.dp, y = (-10).dp)
                    .size(80.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(iconTint.copy(alpha = 0.2f), Color.Transparent)
                        )
                    )
            )

            Column(modifier = Modifier.padding(20.dp)) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(iconTint.copy(alpha = 0.2f), iconTint.copy(alpha = 0.05f))
                            ), 
                            shape = CircleShape
                        )
                        .border(1.dp, iconTint.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    value, 
                    fontSize = 32.sp, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = MaterialTheme.colorScheme.onSurface,
                    style = TextStyle(
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = iconTint.copy(alpha = 0.3f),
                            offset = Offset(0f, 6f),
                            blurRadius = 12f
                        )
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
