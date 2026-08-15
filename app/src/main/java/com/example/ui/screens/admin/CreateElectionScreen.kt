package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateElectionScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var classTarget by remember { mutableStateOf("10") }
    var classExpanded by remember { mutableStateOf(false) }
    val classOptions = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "All")
    var sectionTarget by remember { mutableStateOf("A") }
    var sectionExpanded by remember { mutableStateOf(false) }
    val sectionOptions = listOf("A", "B", "C", "D", "E")
    val isSectionDisabled = classTarget == "All"
    var candidateLimit by remember { mutableStateOf("3") }
    var electionType by remember { mutableStateOf("Class Representative") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(com.example.ui.Translator.tr("Create New Election"), color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(com.example.ui.Translator.tr("Election Title"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.Title, contentDescription = "Title", tint = PrimaryBlue) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ExposedDropdownMenuBox(
                    expanded = classExpanded,
                    onExpandedChange = { classExpanded = !classExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = classTarget,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text(com.example.ui.Translator.tr("Class"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Default.Class, contentDescription = "Class", tint = PrimaryBlue) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = classExpanded,
                        onDismissRequest = { classExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.background)
                    ) {
                        classOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption, color = MaterialTheme.colorScheme.onBackground) },
                                onClick = {
                                    classTarget = selectionOption
                                    if (selectionOption == "All") {
                                        sectionTarget = "All"
                                    } else if (sectionTarget == "All") {
                                        sectionTarget = "A"
                                    }
                                    classExpanded = false
                                },
                                modifier = Modifier.background(MaterialTheme.colorScheme.background)
                            )
                        }
                    }
                }
                
                ExposedDropdownMenuBox(
                    expanded = sectionExpanded && !isSectionDisabled,
                    onExpandedChange = { if (!isSectionDisabled) sectionExpanded = !sectionExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = sectionTarget,
                        onValueChange = { },
                        readOnly = true,
                        enabled = !isSectionDisabled,
                        label = { Text(if (isSectionDisabled) com.example.ui.Translator.tr("Section (Disabled)") else com.example.ui.Translator.tr("Section"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = { Icon(Icons.Default.MeetingRoom, contentDescription = "Section", tint = if (isSectionDisabled) Color.Gray else PrimaryBlue) },
                        trailingIcon = { if (!isSectionDisabled) ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onBackground,
                            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                            disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedBorderColor = PrimaryBlue,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = sectionExpanded,
                        onDismissRequest = { sectionExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.background)
                    ) {
                        sectionOptions.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption, color = MaterialTheme.colorScheme.onBackground) },
                                onClick = {
                                    sectionTarget = selectionOption
                                    sectionExpanded = false
                                },
                                modifier = Modifier.background(MaterialTheme.colorScheme.background)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = electionType,
                onValueChange = { electionType = it },
                label = { Text(com.example.ui.Translator.tr("Election Type"), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                leadingIcon = { Icon(Icons.Default.HowToVote, contentDescription = "Election Type", tint = PrimaryBlue) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedBorderColor = PrimaryBlue,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        viewModel.createElection(
                            title = title,
                            classTarget = classTarget,
                            sectionTarget = sectionTarget,
                            candidateLimit = candidateLimit.toIntOrNull() ?: 3,
                            type = electionType
                        )
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create", modifier = Modifier.size(24.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(com.example.ui.Translator.tr("Create Election"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
