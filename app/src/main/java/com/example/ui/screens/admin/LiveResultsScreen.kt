package com.example.ui.screens.admin

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.CandidateWithStudent
import com.example.data.ElectionEvent
import com.example.ui.MainViewModel
import com.example.ui.SessionWaitingManager
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.delay
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveResultsScreen(
    electionId: String,
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val candidates by viewModel.candidatesForActiveElection.collectAsState()
    val election by viewModel.getElectionById(electionId).collectAsState(initial = null)
    
    val maleHeadCandidates = remember(candidates) {
        candidates.filter { 
            val role = it.candidate.candidateRole.lowercase()
            role.contains("male") && !role.contains("female")
        }.sortedByDescending { it.candidate.voteCount }
    }
    val femaleHeadCandidates = remember(candidates) {
        candidates.filter { 
            val role = it.candidate.candidateRole.lowercase()
            role.contains("female")
        }.sortedByDescending { it.candidate.voteCount }
    }
    val hasSplitRoles = maleHeadCandidates.isNotEmpty() && femaleHeadCandidates.isNotEmpty()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Male Head, 1 = Female Head

    val activeDisplayCandidates = remember(candidates, selectedTab, hasSplitRoles) {
        if (hasSplitRoles) {
            if (selectedTab == 0) maleHeadCandidates else femaleHeadCandidates
        } else {
            candidates.sortedByDescending { it.candidate.voteCount }
        }
    }

    val activeCategoryTitle = remember(election, selectedTab, hasSplitRoles) {
        val baseTitle = election?.title ?: ""
        if (hasSplitRoles) {
            if (selectedTab == 0) "$baseTitle (Head Boy)" else "$baseTitle (Head Girl)"
        } else {
            baseTitle
        }
    }

    LaunchedEffect(electionId) {
        viewModel.loadCandidatesForElection(electionId)
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
            MaterialTheme.colorScheme.background
        )
    )

    val isWaiting = election?.isWaitingPeriodActive() == true
    var forceUnlocked by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(if (isWaiting && !forceUnlocked) "Waiting Period" else if (election?.isCompleted == true) "Final Results" else "Live Results", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(election?.title ?: "Loading...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        modifier = Modifier.background(backgroundGradient)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isWaiting && !forceUnlocked && election != null) {
                WaitingCountdownView(
                    election = election!!,
                    onFinish = { forceUnlocked = true }
                )
            } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 64.dp)
            ) {
                if (hasSplitRoles) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilterChip(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                label = { 
                                    Text(
                                        com.example.ui.Translator.tr("👦 Head Boy Results"), 
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 15.sp
                                    ) 
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = com.example.ui.theme.PrimaryBlue,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                label = { 
                                    Text(
                                        com.example.ui.Translator.tr("👧 Head Girl Results"), 
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 15.sp
                                    ) 
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = com.example.ui.theme.PrimaryBlue,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (activeDisplayCandidates.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text(com.example.ui.Translator.tr("No candidates found in this category."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    val maxVotes = activeDisplayCandidates.maxOfOrNull { it.candidate.voteCount }?.coerceAtLeast(1) ?: 1
                    val winner = if (election?.isCompleted == true) activeDisplayCandidates.firstOrNull() else null

                    if (winner != null && winner.candidate.voteCount > 0) {
                        item {
                            WinnerCard(winner = winner, electionTitle = activeCategoryTitle)
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(com.example.ui.Translator.tr("Voting Statistics & Map"), color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    } else if (activeDisplayCandidates.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(com.example.ui.Translator.tr("Live Vote Distribution"), color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Visual Pie Chart
                            val totalVotes = activeDisplayCandidates.sumOf { it.candidate.voteCount }.coerceAtLeast(1)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.size(200.dp)) {
                                    var startAngle = -90f
                                    activeDisplayCandidates.forEachIndexed { index, candidate ->
                                        val sweepAngle = (candidate.candidate.voteCount.toFloat() / totalVotes) * 360f
                                        val color = when (index % 4) {
                                            0 -> Color(0xFF6366F1)
                                            1 -> Color(0xFFFACC15)
                                            2 -> Color(0xFF22C55E)
                                            else -> Color(0xFFEC4899)
                                        }
                                        drawArc(
                                            color = color,
                                            startAngle = startAngle,
                                            sweepAngle = sweepAngle,
                                            useCenter = true
                                        )
                                        startAngle += sweepAngle
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(32.dp))

                            Text(com.example.ui.Translator.tr("Current Standings"), color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(24.dp))
                        }
                    }

                    itemsIndexed(activeDisplayCandidates, key = { _, item -> item.candidate.id }) { index, item ->
                        val color = when (index % 4) {
                            0 -> Color(0xFF6366F1) // Indigo
                            1 -> Color(0xFFFACC15) // Yellow
                            2 -> Color(0xFF22C55E) // Green
                            else -> Color(0xFFEC4899) // Pink
                        }
                        CandidateChartBar(
                            candidate = item,
                            maxVotes = maxVotes,
                            color = color,
                            isWinner = election?.isCompleted == true && index == 0 && item.candidate.voteCount > 0,
                            index = index
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }

            // Confetti effect over everything if election is completed and there is a winner
            if (election?.isCompleted == true && candidates.firstOrNull()?.candidate?.voteCount ?: 0 > 0) {
                ConfettiEffect()
            }
            }
        }
    }
}

@Composable
fun WinnerCard(winner: CandidateWithStudent, electionTitle: String) {
    var isVisible by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        delay(300)
        isVisible = true
        com.example.ui.SoundPlayer.playCrowdCheering(context)
    }

    val goldGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFFDF00), Color(0xFFD4AF37), Color(0xFFFFF8DC), Color(0xFFFFDF00))
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { 100 }, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(animationSpec = tween(800))
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(32.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .shadow(24.dp, RoundedCornerShape(32.dp), spotColor = Color(0xFFFFD700).copy(alpha = 0.4f))
                .border(2.dp, goldGradient, RoundedCornerShape(32.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFFFFD700).copy(alpha = 0.1f), Color.Transparent)
                        )
                    )
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Icon(
                    Icons.Default.EmojiEvents, 
                    contentDescription = "Winner", 
                    tint = Color(0xFFFFD700), 
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .background(goldGradient, CircleShape)
                        .padding(4.dp)
                ) {
                    if (winner.student.photoUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(winner.student.photoUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Winner Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(winner.student.name.take(1), color = MaterialTheme.colorScheme.onBackground, fontSize = 56.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = com.example.ui.Translator.tr("CONGRATULATIONS"), 
                    style = TextStyle(
                        brush = goldGradient,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(winner.student.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 32.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(com.example.ui.Translator.tr("Winner of $electionTitle"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(24.dp))
                
                Surface(
                    color = SuccessGreen.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(com.example.ui.Translator.tr("${winner.candidate.voteCount} Votes Received"), color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CandidateChartBar(candidate: CandidateWithStudent, maxVotes: Int, color: Color, isWinner: Boolean, index: Int) {
    val fraction = if (maxVotes > 0) candidate.candidate.voteCount.toFloat() / maxVotes.toFloat() else 0f
    var animationPlayed by remember { mutableStateOf(false) }
    
    val animatedFraction by animateFloatAsState(
        targetValue = if (animationPlayed) fraction else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "bar_anim"
    )

    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(500L + (index * 150L)) // Staggered appearance after winner card
        isVisible = true
        delay(100)
        animationPlayed = true
    }

    val barGradient = Brush.horizontalGradient(
        colors = listOf(color.copy(alpha = 0.6f), color)
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInHorizontally(initialOffsetX = { it / 2 }) + fadeIn(animationSpec = tween(500))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(if (isWinner) 16.dp else 4.dp, RoundedCornerShape(20.dp), spotColor = color),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, color.copy(alpha = if (isWinner) 0.8f else 0.2f))
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(color.copy(alpha = 0.2f), CircleShape)
                            .border(1.dp, color, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(com.example.ui.Translator.tr("${index + 1}"), color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    if (candidate.student.photoUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(candidate.student.photoUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Candidate Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(2.dp, color.copy(alpha = 0.5f), CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.15f))
                                .border(2.dp, color.copy(alpha = 0.5f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(candidate.student.name.take(1), color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(candidate.student.name, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = if (isWinner) FontWeight.Bold else FontWeight.SemiBold)
                        Text(com.example.ui.Translator.tr("${candidate.candidate.voteCount} Votes"), color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Text(com.example.ui.Translator.tr("${(fraction * 100).toInt()}%"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                ) {
                    if (animatedFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedFraction)
                                .fillMaxHeight()
                                .shadow(8.dp, RoundedCornerShape(12.dp), spotColor = color, ambientColor = color)
                                .background(barGradient, RoundedCornerShape(12.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConfettiEffect() {
    val particles = remember { List(100) { Particle() } }
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        particles.forEach { particle ->
            val infiniteTransition = rememberInfiniteTransition(label = "confetti_fall")
            val yOffset by infiniteTransition.animateFloat(
                initialValue = -200f,
                targetValue = 2500f,
                animationSpec = infiniteRepeatable(
                    animation = tween(particle.durationMillis, easing = LinearEasing, delayMillis = particle.delayMillis),
                    repeatMode = RepeatMode.Restart
                ),
                label = "yOffset"
            )
            
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(particle.durationMillis, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation"
            )

            Box(
                modifier = Modifier
                    .offset(x = particle.xOffset.dp, y = yOffset.dp)
                    .graphicsLayer { 
                        rotationZ = rotation 
                        rotationX = rotation / 2 
                        rotationY = rotation / 3
                    }
                    .size(particle.size.dp)
                    .background(particle.color, if (particle.isCircle) CircleShape else RoundedCornerShape(2.dp))
            )
        }
    }
}

class Particle {
    val xOffset = Random.nextInt(-400, 400)
    val delayMillis = Random.nextInt(0, 5000)
    val durationMillis = Random.nextInt(2500, 6000)
    val size = Random.nextInt(6, 14)
    val isCircle = Random.nextBoolean()
    val color = listOf(
        Color(0xFF6366F1), // Indigo
        Color(0xFFFACC15), // Yellow
        Color(0xFF22C55E), // Green
        Color(0xFFEC4899), // Pink
        Color(0xFF3B82F6), // Blue
        Color(0xFFA855F7), // Purple
        Color(0xFF14B8A6), // Teal
        Color(0xFFF97316)  // Orange
    ).random()
}

@Composable
fun WaitingCountdownView(
    election: ElectionEvent,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    var remainingMillis by remember { mutableLongStateOf(election.getRemainingWaitMillis()) }
    var alarmPlayed by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        SessionWaitingManager.startTickingSound(context)
        onDispose {
            SessionWaitingManager.stopTickingSound()
        }
    }

    LaunchedEffect(election.completedTimeMillis) {
        while (remainingMillis > 0) {
            delay(1000L)
            remainingMillis = election.getRemainingWaitMillis()
        }
        if (!alarmPlayed) {
            alarmPlayed = true
            SessionWaitingManager.playAlarmSoundAndNotify(context)
            onFinish()
        }
    }

    val hours = (remainingMillis / (1000 * 60 * 60)).toString().padStart(2, '0')
    val minutes = ((remainingMillis / (1000 * 60)) % 60).toString().padStart(2, '0')
    val seconds = ((remainingMillis / 1000) % 60).toString().padStart(2, '0')

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            border = BorderStroke(1.5.dp, com.example.ui.theme.PrimaryBlue.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = com.example.ui.theme.PrimaryBlue.copy(alpha = 0.15f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Waiting",
                            tint = com.example.ui.theme.PrimaryBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = com.example.ui.Translator.tr("Winner Announcement Locked"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = com.example.ui.Translator.tr("Voting session has completed. The 6.5-hour waiting period is currently active."),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(28.dp))

                // Timer display box
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TimerBlock(value = hours, label = "HOURS")
                    Text(" : ", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.PrimaryBlue)
                    TimerBlock(value = minutes, label = "MINS")
                    Text(" : ", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = com.example.ui.theme.PrimaryBlue)
                    TimerBlock(value = seconds, label = "SECS")
                }

                Spacer(modifier = Modifier.height(28.dp))
                val progress = (1f - (remainingMillis.toFloat() / ElectionEvent.WAIT_DURATION_MILLIS.toFloat())).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = com.example.ui.theme.PrimaryBlue,
                    trackColor = com.example.ui.theme.PrimaryBlue.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = com.example.ui.Translator.tr("Teachers & staff will be able to reveal the winner once the countdown finishes."),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun TimerBlock(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = com.example.ui.theme.PrimaryBlue.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, com.example.ui.theme.PrimaryBlue.copy(alpha = 0.3f))
        ) {
            Text(
                text = value,
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = com.example.ui.theme.PrimaryBlue,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
