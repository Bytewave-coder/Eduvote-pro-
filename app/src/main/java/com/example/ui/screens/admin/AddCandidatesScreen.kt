package com.example.ui.screens.admin

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Flag
import com.example.data.ElectionEvent
import com.example.ui.MainViewModel
import com.example.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCandidatesScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val allElections by viewModel.allElections.collectAsState()
    
    var selectedElection by remember { mutableStateOf<ElectionEvent?>(null) }
    var electionExpanded by remember { mutableStateOf(false) }
    
    var numCandidates by remember { mutableStateOf("2") }
    var numCandidatesExpanded by remember { mutableStateOf(false) }
    val numOptions = remember { (2..15).map { it.toString() } }
    
    var isConfigured by remember { mutableStateOf(false) }
    var candidatesAdded by remember { mutableStateOf(0) }
    
    // States for current candidate being entered
    var currentName by remember { mutableStateOf("") }
    var currentPartyName by remember { mutableStateOf("") }
    var candidateRole by remember { mutableStateOf("Male Head Candidate") }
    var currentPartyLogo by remember { mutableStateOf<Uri?>(null) }
    var currentRealPhoto by remember { mutableStateOf<Uri?>(null) }
    
    val context = LocalContext.current
    
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        currentPartyLogo = uri
    }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            try {
                val file = java.io.File(context.cacheDir, "camera_image_${System.currentTimeMillis()}.png")
                val outputStream = java.io.FileOutputStream(file)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, outputStream)
                outputStream.flush()
                outputStream.close()
                currentRealPhoto = Uri.fromFile(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            cameraLauncher.launch(null)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(com.example.ui.Translator.tr("Add Candidates"), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (!isConfigured) {
                // Phase 1: Configuration
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(com.example.ui.Translator.tr("Session Details"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(modifier = Modifier.height(16.dp))
                        ExposedDropdownMenuBox(
                            expanded = electionExpanded,
                            onExpandedChange = { electionExpanded = !electionExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = selectedElection?.title ?: com.example.ui.Translator.tr("Select Session"),
                                onValueChange = { },
                                readOnly = true,
                                label = { Text(com.example.ui.Translator.tr("Vote Session"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = electionExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = electionExpanded,
                                onDismissRequest = { electionExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                allElections.forEach { election ->
                                    DropdownMenuItem(
                                        text = { Text(election.title, color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            selectedElection = election
                                            electionExpanded = false
                                            if (election.candidateLimit in 2..15) {
                                                numCandidates = election.candidateLimit.toString()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        ExposedDropdownMenuBox(
                            expanded = numCandidatesExpanded,
                            onExpandedChange = { numCandidatesExpanded = !numCandidatesExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = numCandidates,
                                onValueChange = { },
                                readOnly = true,
                                label = { Text(com.example.ui.Translator.tr("Number of Candidates (2-15)"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null, tint = PrimaryBlue) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = numCandidatesExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = numCandidatesExpanded,
                                onDismissRequest = { numCandidatesExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                            ) {
                                numOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt, color = MaterialTheme.colorScheme.onSurface) },
                                        onClick = {
                                            numCandidates = opt
                                            numCandidatesExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                if (selectedElection != null) {
                                    isConfigured = true
                                }
                            },
                            enabled = selectedElection != null,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text(
                                com.example.ui.Translator.tr("Continue to Add Candidates"),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            } else if (candidatesAdded < numCandidates.toInt()) {
                // Phase 2: Input Candidate Info
                Column(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(com.example.ui.Translator.tr("Candidate Details"), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                                if (selectedElection != null) {
                                    Text(selectedElection!!.title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Surface(
                                color = PrimaryBlue.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    com.example.ui.Translator.tr("${candidatesAdded + 1} / $numCandidates"),
                                    color = PrimaryBlue,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(24.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp)) {
                                OutlinedTextField(
                                    value = currentName,
                                    onValueChange = { currentName = it },
                                    label = { Text(com.example.ui.Translator.tr("Candidate Name"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedBorderColor = PrimaryBlue,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                OutlinedTextField(
                                    value = currentPartyName,
                                    onValueChange = { currentPartyName = it },
                                    label = { Text(com.example.ui.Translator.tr("Name your Party"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null, tint = PrimaryBlue) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                        focusedBorderColor = PrimaryBlue,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    ),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(com.example.ui.Translator.tr("Candidate Category"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    val roles = listOf("Male Head Candidate", "Female Head Candidate")
                                    roles.forEach { role ->
                                        val selected = candidateRole == role
                                        FilterChip(
                                            selected = selected,
                                            onClick = { candidateRole = role },
                                            label = { Text(com.example.ui.Translator.tr(if (role.contains("Male") && !role.contains("Female")) "Male Head" else "Female Head"), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 13.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = PrimaryBlue,
                                                selectedLabelColor = Color.White
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(com.example.ui.Translator.tr("Photos"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Button(
                                        onClick = { galleryLauncher.launch("image/*") },
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue.copy(alpha = 0.1f), contentColor = PrimaryBlue)
                                    ) {
                                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(com.example.ui.Translator.tr("Party Logo"), fontWeight = FontWeight.Bold)
                                    }
                                    
                                    Button(
                                        onClick = { 
                                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                                cameraLauncher.launch(null)
                                            } else {
                                                permissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        },
                                        modifier = Modifier.weight(1f).height(56.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue.copy(alpha = 0.1f), contentColor = PrimaryBlue)
                                    ) {
                                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(com.example.ui.Translator.tr("Real Photo"), fontWeight = FontWeight.Bold)
                                    }
                                }
                                
                                if (currentPartyLogo != null || currentRealPhoto != null) {
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                                        if (currentPartyLogo != null) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                AsyncImage(
                                                    model = currentPartyLogo,
                                                    contentDescription = "Logo",
                                                    modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(com.example.ui.Translator.tr("Logo"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        if (currentRealPhoto != null) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                AsyncImage(
                                                    model = currentRealPhoto,
                                                    contentDescription = "Photo",
                                                    modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(com.example.ui.Translator.tr("Photo"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    Button(
                        onClick = {
                            if (currentName.isNotBlank() && selectedElection != null) {
                                val logoUri = currentPartyLogo?.toString() ?: "https://api.dicebear.com/9.x/initials/png?seed=$currentName"
                                val realPhotoUri = currentRealPhoto?.toString() ?: "https://api.dicebear.com/9.x/avataaars/png?seed=$currentName"
                                
                                viewModel.addCandidateWithNewStudent(
                                    electionId = selectedElection!!.id,
                                    studentName = currentName,
                                    partyName = currentPartyName,
                                    candidateRole = candidateRole,
                                    partyLogoUri = logoUri,
                                    realPhotoUri = realPhotoUri,
                                    classNum = selectedElection!!.classTarget,
                                    section = selectedElection!!.sectionTarget
                                )
                                
                                candidatesAdded++
                                currentName = ""
                                currentPartyName = ""
                                currentPartyLogo = null
                                currentRealPhoto = null
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text(if (candidatesAdded == numCandidates.toInt() - 1) "Save Candidates" else "Next Candidate", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = com.example.ui.theme.SuccessGreen.copy(alpha = 0.15f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = com.example.ui.theme.SuccessGreen, modifier = Modifier.padding(16.dp))
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(com.example.ui.Translator.tr("All candidates added!"), color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text(com.example.ui.Translator.tr("Finish"), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
