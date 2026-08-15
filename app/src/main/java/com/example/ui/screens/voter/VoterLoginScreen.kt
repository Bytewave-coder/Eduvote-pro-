package com.example.ui.screens.voter

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoterLoginScreen(
    viewModel: MainViewModel,
    onLoginSuccess: (String) -> Unit, // returns the electionId to navigate to
    onBack: () -> Unit
) {
    val ongoingElections by viewModel.ongoingElections.collectAsState()
    var selectedElectionId by remember { mutableStateOf<String?>(null) }
    var voterName by remember { mutableStateOf("") }
    var rollNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    // Removed selectedElectionId for login
    
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text(com.example.ui.Translator.tr("Voting Complete"), color = MaterialTheme.colorScheme.onSurface) },
            text = { Text(errorMessage!!, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            confirmButton = {
                Button(
                    onClick = { errorMessage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(com.example.ui.Translator.tr("OK"), color = Color.White)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(

        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(com.example.ui.Translator.tr("Select Session"), color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Glowing Background Orbs
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(AccentCyan.copy(alpha = 0.1f), Color.Transparent),
                            center = Offset(0f, 300f),
                            radius = 1000f
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(AccentPurple.copy(alpha = 0.1f), Color.Transparent),
                            center = Offset(1000f, 1500f),
                            radius = 1200f
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { -20 }
                ) {
                    Text(
                        com.example.ui.Translator.tr("Which session/class are you voting in?"),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                if (ongoingElections.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(com.example.ui.Translator.tr("No active sessions available."), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(ongoingElections) { index, election ->
                            var itemVisible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                delay(100L + (index * 100L))
                                itemVisible = true
                            }
                            
                            AnimatedVisibility(
                                visible = itemVisible,
                                enter = fadeIn(tween(400)) + slideInVertically(tween(400)) { 50 }
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            viewModel.checkCanVote(election) { canVote ->
                                                if (canVote) {
                                                    onLoginSuccess(election.id)
                                                } else {
                                                    errorMessage = "All voters have already cast their votes for this session."
                                                }
                                            }
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .background(
                                                    brush = Brush.linearGradient(
                                                        colors = listOf(PrimaryBlue, AccentCyan)
                                                    ),
                                                    shape = RoundedCornerShape(16.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.HowToVote, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                                        }
                                        Spacer(modifier = Modifier.width(20.dp))
                                        Column {
                                            Text(election.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(com.example.ui.Translator.tr("Class: ${election.classTarget} (${election.sectionTarget})"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
