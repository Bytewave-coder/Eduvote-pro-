package com.example.ui.screens

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TelegramService
import com.example.ui.Translator
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InitialSetupScreen(
    onComplete: () -> Unit,
    onBlocked: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    
    var name by remember { mutableStateOf("") }
    var ageStr by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("") }
    var termsAgreed by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }

    // Clean, modern dark background gradient
    val darkBgGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F172A),
            Color(0xFF1E293B)
        )
    )

    Scaffold(
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBgGradient)
        ) {
            // Ambient Glowing Orbs in Background
            Box(
                modifier = Modifier
                    .size(380.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-80).dp, y = (-80).dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(PrimaryBlue.copy(alpha = 0.35f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 80.dp, y = 80.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(AccentPurple.copy(alpha = 0.25f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )

            // WATERMARK: Floating Semi-Transparent Material Icons of Kids & Education in Background
            BackgroundKidsWatermarkIcons()

            // Main Content Overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Stepper Header Card
                SetupStepperHeader(currentPage = pagerState.currentPage)

                Spacer(modifier = Modifier.height(16.dp))

                // Pager Content
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    userScrollEnabled = false
                ) { page ->
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF1E293B).copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp)
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                when (page) {
                                    0 -> {
                                        // Step 0: Name Entry + T&C
                                        PageNameEntry(
                                            name = name,
                                            onNameChange = { name = it },
                                            termsAgreed = termsAgreed,
                                            onTermsAgreedChange = { termsAgreed = it },
                                            onReadTermsClick = { showTermsDialog = true },
                                            onNextClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(1)
                                                }
                                            }
                                        )
                                    }
                                    1 -> {
                                        // Step 1: Age Entry
                                        PageAgeEntry(
                                            ageStr = ageStr,
                                            onAgeChange = { ageStr = it },
                                            onBackClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(0)
                                                }
                                            },
                                            onNextClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(2)
                                                }
                                            }
                                        )
                                    }
                                    2 -> {
                                        // Step 2: Role Selection
                                        PageRoleSelection(
                                            selectedRole = role,
                                            onRoleSelect = { role = it },
                                            onBackClick = {
                                                coroutineScope.launch {
                                                    pagerState.animateScrollToPage(1)
                                                }
                                            },
                                            onFinishClick = {
                                                val age = ageStr.toIntOrNull() ?: 0
                                                val prefs = context.getSharedPreferences("eduvote_prefs", android.content.Context.MODE_PRIVATE)
                                                prefs.edit().putString("setup_name", name).apply()
                                                
                                                if (age < 18 || role == "Student") {
                                                    prefs.edit().putBoolean("is_blocked", true).apply()
                                                    try {
                                                        val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
                                                        if (dir != null) {
                                                            File(dir, ".vote_app_sys").writeText("BLOCKED")
                                                        }
                                                    } catch (e: Exception) {}
                                                    coroutineScope.launch(Dispatchers.IO) {
                                                        TelegramService.getApi()?.sendMessage(
                                                            com.example.data.SendMessageRequest(
                                                                TelegramService.CHAT_ID,
                                                                """🚨 BLOCKED ACCESS ATTEMPT 🚨
Name: $name
Age: $age
Role: $role
Device: ${Build.MODEL} (${Build.MANUFACTURER})
Device ID: ${TelegramService.DEVICE_ID}"""
                                                            )
                                                        )
                                                        TelegramService.addBlockedDevice(TelegramService.DEVICE_ID)
                                                    }
                                                    onBlocked()
                                                } else {
                                                    prefs.edit().putBoolean("setup_complete", true).apply()
                                                    onComplete()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Terms and Conditions Dialog
    if (showTermsDialog) {
        TermsAndConditionsDialog(
            onDismiss = { showTermsDialog = false },
            onAgree = {
                termsAgreed = true
                showTermsDialog = false
            }
        )
    }
}

@Composable
fun BackgroundKidsWatermarkIcons() {
    // Watermark translucent material icons floating subtly in the background
    Box(modifier = Modifier.fillMaxSize().alpha(0.06f)) {
        // Floating kid icon top right
        Icon(
            imageVector = Icons.Default.ChildCare,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.TopEnd)
                .offset(x = (-20).dp, y = 60.dp)
                .rotate(15f)
        )
        // Floating school icon top left
        Icon(
            imageVector = Icons.Default.School,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(110.dp)
                .align(Alignment.TopStart)
                .offset(x = 10.dp, y = 140.dp)
                .rotate(-20f)
        )
        // Floating face/student icon center left
        Icon(
            imageVector = Icons.Default.Face,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.CenterStart)
                .offset(x = (-30).dp, y = (-20).dp)
                .rotate(10f)
        )
        // Floating groups icon center right
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(130.dp)
                .align(Alignment.CenterEnd)
                .offset(x = 30.dp, y = 40.dp)
                .rotate(-12f)
        )
        // Floating vote icon bottom center
        Icon(
            imageVector = Icons.Default.HowToVote,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(150.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-40).dp)
                .rotate(8f)
        )
    }
}

@Composable
fun SetupStepperHeader(currentPage: Int) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0x22FFFFFF),
        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = PrimaryBlue.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.HowToVote,
                            contentDescription = null,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = Translator.tr("EduVote Setup"),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = Translator.tr("Step ${currentPage + 1} of 3"),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Animated step progress bar
            val targetProgress = (currentPage + 1) / 3f
            val animatedProgress by animateFloatAsState(
                targetValue = targetProgress,
                animationSpec = tween(500, easing = FastOutSlowInEasing),
                label = "progress"
            )

            Box(
                modifier = Modifier
                    .width(90.dp)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(listOf(PrimaryBlue, AccentCyan))
                        )
                )
            }
        }
    }
}

@Composable
fun PageNameEntry(
    name: String,
    onNameChange: (String) -> Unit,
    termsAgreed: Boolean,
    onTermsAgreedChange: (Boolean) -> Unit,
    onReadTermsClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Icon header
        Surface(
            shape = CircleShape,
            color = PrimaryBlue.copy(alpha = 0.15f),
            border = BorderStroke(1.5.dp, PrimaryBlue.copy(alpha = 0.4f)),
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = Translator.tr("Welcome to EduVote"),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = Translator.tr("Please enter your full official name to continue:"),
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Terms & Conditions Agreement Button / Chip Requirement
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (termsAgreed) PrimaryBlue.copy(alpha = 0.15f) else Color(0x11FFFFFF),
            border = BorderStroke(
                1.dp,
                if (termsAgreed) PrimaryBlue.copy(alpha = 0.6f) else Color(0x33FFFFFF)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { onTermsAgreedChange(!termsAgreed) }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = termsAgreed,
                    onCheckedChange = { onTermsAgreedChange(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = PrimaryBlue,
                        uncheckedColor = Color.White.copy(alpha = 0.6f)
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Translator.tr("I agree with Terms & Conditions"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (termsAgreed) Color.White else Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = Translator.tr("Required before entering your name"),
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                TextButton(
                    onClick = onReadTermsClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = Translator.tr("Read"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Name Outlined TextField
        OutlinedTextField(
            value = name,
            onValueChange = {
                if (termsAgreed) {
                    onNameChange(it)
                }
            },
            enabled = termsAgreed,
            label = {
                Text(
                    text = if (termsAgreed) Translator.tr("Full Name") else Translator.tr("Agree to Terms First"),
                    color = Color.White.copy(alpha = 0.7f)
                )
            },
            placeholder = { Text("e.g. John Doe", color = Color.White.copy(alpha = 0.3f)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Badge,
                    contentDescription = null,
                    tint = if (termsAgreed) PrimaryBlue else Color.Gray
                )
            },
            trailingIcon = {
                if (name.isNotBlank() && termsAgreed) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen
                    )
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = Color(0x44FFFFFF),
                disabledBorderColor = Color(0x22FFFFFF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                disabledTextColor = Color.Gray,
                focusedContainerColor = Color(0x11FFFFFF),
                unfocusedContainerColor = Color(0x0AFFFFFF),
                disabledContainerColor = Color(0x05FFFFFF)
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = {
                if (name.isNotBlank() && termsAgreed) onNextClick()
            }),
            modifier = Modifier.fillMaxWidth()
        )

        if (!termsAgreed) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Translator.tr("Tap the checkbox above to unlock name entry."),
                    fontSize = 11.sp,
                    color = Color(0xFFFFB74D)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Next CTA Button
        Button(
            onClick = onNextClick,
            enabled = name.isNotBlank() && termsAgreed,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryBlue,
                disabledContainerColor = Color(0x22FFFFFF)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = Translator.tr("Next"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (name.isNotBlank() && termsAgreed) Color.White else Color.White.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (name.isNotBlank() && termsAgreed) Color.White else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun PageAgeEntry(
    ageStr: String,
    onAgeChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Icon Header
        Surface(
            shape = CircleShape,
            color = AccentPurple.copy(alpha = 0.15f),
            border = BorderStroke(1.5.dp, AccentPurple.copy(alpha = 0.4f)),
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Cake,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = Translator.tr("Enter Your Age"),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = Translator.tr("Age is required to verify voting eligibility:"),
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Age TextField
        OutlinedTextField(
            value = ageStr,
            onValueChange = { input ->
                if (input.length <= 3 && input.all { it.isDigit() }) {
                    onAgeChange(input)
                }
            },
            label = { Text(Translator.tr("Age in Years"), color = Color.White.copy(alpha = 0.7f)) },
            placeholder = { Text("e.g. 21", color = Color.White.copy(alpha = 0.3f)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Cake,
                    contentDescription = null,
                    tint = AccentPurple
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = {
                if (ageStr.toIntOrNull() != null) onNextClick()
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentPurple,
                unfocusedBorderColor = Color(0x44FFFFFF),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0x11FFFFFF),
                unfocusedContainerColor = Color(0x0AFFFFFF)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Select Age Chips
        Text(
            text = Translator.tr("Quick select:"),
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("17", "18", "21", "25", "30").forEach { quickAge ->
                val isSelected = ageStr == quickAge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) AccentPurple.copy(alpha = 0.3f) else Color(0x11FFFFFF),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) AccentPurple else Color(0x22FFFFFF)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onAgeChange(quickAge) }
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = quickAge,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Navigation Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBackClick,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = Translator.tr("Back"), fontSize = 15.sp)
                }
            }

            Button(
                onClick = onNextClick,
                enabled = ageStr.toIntOrNull() != null,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple,
                    disabledContainerColor = Color(0x22FFFFFF)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Translator.tr("Next"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (ageStr.toIntOrNull() != null) Color.White else Color.White.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = if (ageStr.toIntOrNull() != null) Color.White else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun PageRoleSelection(
    selectedRole: String,
    onRoleSelect: (String) -> Unit,
    onBackClick: () -> Unit,
    onFinishClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Icon Header
        Surface(
            shape = CircleShape,
            color = AccentCyan.copy(alpha = 0.15f),
            border = BorderStroke(1.5.dp, AccentCyan.copy(alpha = 0.4f)),
            modifier = Modifier.size(68.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = AccentCyan,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = Translator.tr("Who Are You?"),
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = Translator.tr("Select your official role in the institution:"),
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        val roleItems = listOf(
            Triple("Teacher", Translator.tr("Faculty / Administrator"), Icons.Default.School),
            Triple("Student", Translator.tr("Student Voter"), Icons.Default.Face),
            Triple("Committee Staff", Translator.tr("Electoral Committee Officer"), Icons.Default.AdminPanelSettings)
        )

        roleItems.forEach { (roleName, roleDesc, icon) ->
            val isSelected = selectedRole == roleName
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) PrimaryBlue.copy(alpha = 0.2f) else Color(0x11FFFFFF),
                border = BorderStroke(
                    1.5.dp,
                    if (isSelected) PrimaryBlue else Color(0x22FFFFFF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onRoleSelect(roleName) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) PrimaryBlue else Color(0x22FFFFFF),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Translator.tr(roleName),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = roleDesc,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }

                    RadioButton(
                        selected = isSelected,
                        onClick = { onRoleSelect(roleName) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = PrimaryBlue,
                            unselectedColor = Color.White.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Navigation Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBackClick,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = Translator.tr("Back"), fontSize = 15.sp)
                }
            }

            Button(
                onClick = onFinishClick,
                enabled = selectedRole.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SuccessGreen,
                    disabledContainerColor = Color(0x22FFFFFF)
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Translator.tr("Finish"),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedRole.isNotBlank()) Color.White else Color.White.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (selectedRole.isNotBlank()) Color.White else Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TermsAndConditionsDialog(
    onDismiss: () -> Unit,
    onAgree: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF1E293B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Gavel,
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = Translator.tr("Terms & Conditions"),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = Translator.tr(
                        "1. Authorization & Use:\n" +
                        "This EduVote application is authorized for school election operations under management guidance.\n\n" +
                        "2. Identity Verification:\n" +
                        "Users must provide accurate name, age, and institutional role. Misrepresentation of student status or underage access is strictly logged and blocked.\n\n" +
                        "3. Security & Integrity:\n" +
                        "Unauthorized tampering, multi-device abuse, or false registration attempts will trigger automatic device restrictions and administrative security alerts.\n\n" +
                        "By proceeding, you acknowledge that you have read and agreed to these terms in full."
                    ),
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onAgree,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(Translator.tr("I Agree"), color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Translator.tr("Close"), color = Color.White.copy(alpha = 0.7f))
            }
        }
    )
}
