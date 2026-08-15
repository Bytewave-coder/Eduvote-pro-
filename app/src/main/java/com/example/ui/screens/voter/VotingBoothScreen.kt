package com.example.ui.screens.voter

import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.CandidateWithStudent
import com.example.ui.MainViewModel
import com.example.ui.VoteResult
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SuccessGreen

private fun isMaleHeadRole(role: String): Boolean {
    val r = role.lowercase()
    return r.contains("male") && !r.contains("female")
}

private fun isFemaleHeadRole(role: String): Boolean {
    val r = role.lowercase()
    return r.contains("female")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VotingBoothScreen(
    electionId: String,
    viewModel: MainViewModel,
    onVoteSuccess: () -> Unit
) {
    val candidates by viewModel.candidatesForActiveElection.collectAsState()
    val currentVoter by viewModel.currentVoter.collectAsState()
    val voteResult by viewModel.voteResultState.collectAsState()
    val useRealEvmSound by viewModel.useRealEvmSound.collectAsState()
    
    val context = LocalContext.current
    
    val maleHeadCandidates = remember(candidates) {
        candidates.filter { isMaleHeadRole(it.candidate.candidateRole) }
    }
    val femaleHeadCandidates = remember(candidates) {
        candidates.filter { isFemaleHeadRole(it.candidate.candidateRole) }
    }
    val hasSplitSessions = maleHeadCandidates.isNotEmpty() && femaleHeadCandidates.isNotEmpty()
    
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Male Head, 1 = Female Head
    
    var maleHeadVoted by remember { mutableStateOf(false) }
    var femaleHeadVoted by remember { mutableStateOf(false) }
    var maleVotedCandidateName by remember { mutableStateOf<String?>(null) }
    var femaleVotedCandidateName by remember { mutableStateOf<String?>(null) }
    
    var pendingVoteCategory by remember { mutableStateOf<Int?>(null) } // 0 = Male, 1 = Female
    
    LaunchedEffect(electionId) {
        viewModel.loadCandidatesForElection(electionId)
    }
    
    LaunchedEffect(voteResult) {
        if (voteResult is VoteResult.Success) {
            viewModel.clearVoteResult()
            
            if (pendingVoteCategory == 0) {
                maleHeadVoted = true
                Toast.makeText(context, com.example.ui.Translator.tr("Vote recorded for Male Head!"), Toast.LENGTH_SHORT).show()
                if (femaleHeadCandidates.isNotEmpty() && !femaleHeadVoted) {
                    selectedTab = 1
                }
            } else if (pendingVoteCategory == 1) {
                femaleHeadVoted = true
                Toast.makeText(context, com.example.ui.Translator.tr("Vote recorded for Female Head!"), Toast.LENGTH_SHORT).show()
                if (maleHeadCandidates.isNotEmpty() && !maleHeadVoted) {
                    selectedTab = 0
                }
            } else {
                maleHeadVoted = true
                femaleHeadVoted = true
            }

            pendingVoteCategory = null

            val maleDone = maleHeadCandidates.isEmpty() || maleHeadVoted
            val femaleDone = femaleHeadCandidates.isEmpty() || femaleHeadVoted

            if ((hasSplitSessions && maleDone && femaleDone) || (!hasSplitSessions && (maleHeadVoted || femaleHeadVoted))) {
                Toast.makeText(context, com.example.ui.Translator.tr("All votes submitted successfully!"), Toast.LENGTH_SHORT).show()
                onVoteSuccess()
            }
        }
    }

    val currentDisplayCandidates = remember(candidates, selectedTab, hasSplitSessions) {
        if (!hasSplitSessions) {
            candidates
        } else if (selectedTab == 0) {
            maleHeadCandidates
        } else {
            femaleHeadCandidates
        }
    }

    val isCurrentSessionVoted = if (hasSplitSessions) {
        if (selectedTab == 0) maleHeadVoted else femaleHeadVoted
    } else {
        maleHeadVoted || femaleHeadVoted
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { 
                    Text(com.example.ui.Translator.tr("Voting Booth"), color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasSplitSessions) {
                // Dual Session Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { 
                            Text(
                                com.example.ui.Translator.tr(if (maleHeadVoted) "👦 Head Boy (✓ Voted)" else "👦 Head Boy Session"), 
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            ) 
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { 
                            Text(
                                com.example.ui.Translator.tr(if (femaleHeadVoted) "👧 Head Girl (✓ Voted)" else "👧 Head Girl Session"), 
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            ) 
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue,
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (isCurrentSessionVoted) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                com.example.ui.Translator.tr("Vote Cast for this Session"),
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen,
                                fontSize = 15.sp
                            )
                            val name = if (selectedTab == 0) maleVotedCandidateName else femaleVotedCandidateName
                            if (name != null) {
                                Text(
                                    com.example.ui.Translator.tr("You voted for $name"),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            } else {
                                Text(
                                    com.example.ui.Translator.tr("This session is disabled."),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            } else if (!hasSplitSessions) {
                Text(
                    com.example.ui.Translator.tr("Select your candidate"), 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
                )
            }
            
            if (currentDisplayCandidates.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(com.example.ui.Translator.tr("No candidates found in this session..."), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(currentDisplayCandidates, key = { it.candidate.id }) { item ->
                        CandidateRow(
                            candidateItem = item,
                            enabled = !isCurrentSessionVoted,
                            onVoteClick = {
                                if (isCurrentSessionVoted) return@CandidateRow
                                
                                val isMaleCandidate = isMaleHeadRole(item.candidate.candidateRole)
                                val isFemaleCandidate = isFemaleHeadRole(item.candidate.candidateRole)
                                
                                if (isMaleCandidate) {
                                    pendingVoteCategory = 0
                                    maleVotedCandidateName = item.student.name
                                } else if (isFemaleCandidate) {
                                    pendingVoteCategory = 1
                                    femaleVotedCandidateName = item.student.name
                                } else {
                                    pendingVoteCategory = if (selectedTab == 0) 0 else 1
                                    if (selectedTab == 0) maleVotedCandidateName = item.student.name else femaleVotedCandidateName = item.student.name
                                }
                                
                                if (useRealEvmSound) {
                                    com.example.ui.SoundPlayer.playEvm(context)
                                } else {
                                    com.example.ui.SoundPlayer.playBeep(context)
                                }
                                viewModel.castVote(currentVoter?.name ?: "Unknown", item.candidate.id, electionId)
                            },
                            modifier = Modifier.animateItem()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CandidateRow(
    candidateItem: CandidateWithStudent, 
    onVoteClick: () -> Unit, 
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 6.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with Coil
            if (candidateItem.student.photoUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(candidateItem.student.photoUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Candidate Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(candidateItem.student.name.take(1), color = MaterialTheme.colorScheme.surface, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(candidateItem.student.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                
                if (candidateItem.candidate.candidateRole.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        com.example.ui.Translator.tr(candidateItem.candidate.candidateRole),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (candidateItem.candidate.partySymbolUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(candidateItem.candidate.partySymbolUri)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Party Logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(candidateItem.candidate.partyName, color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(if (enabled) PrimaryBlue else Color.Gray.copy(alpha = 0.5f), CircleShape)
                    .clip(CircleShape)
                    .clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = ripple(color = Color.White),
                        onClick = onVoteClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (enabled) Icons.Default.HowToVote else Icons.Default.Check, 
                    contentDescription = if (enabled) "Vote" else "Voted", 
                    tint = Color.White, 
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

