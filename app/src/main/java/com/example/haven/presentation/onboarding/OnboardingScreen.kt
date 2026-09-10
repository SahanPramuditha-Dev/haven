package com.example.haven.presentation.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.haven.data.repository.FamilyRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onFamilyReady: () -> Unit,
    modifier: Modifier = Modifier,
    repository: FamilyRepository = FamilyRepository.instance
) {
    val coroutineScope = rememberCoroutineScope()
    var isCreating by remember { mutableStateOf(true) }

    // Form states
    var familyName by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var householdName by remember { mutableStateOf("Main Home") }
    var inviteCode by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Welcome to HAVEN", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Your Private Family Operating System",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // High-performance M3 Tab Row with 0 touch slop delay
            PrimaryTabRow(
                selectedTabIndex = if (isCreating) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = isCreating,
                    onClick = {
                        isCreating = true
                        errorMessage = null
                    },
                    text = { Text("Create Family", fontWeight = if (isCreating) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) }
                )
                Tab(
                    selected = !isCreating,
                    onClick = {
                        isCreating = false
                        errorMessage = null
                    },
                    text = { Text("Join with Code", fontWeight = if (!isCreating) FontWeight.Bold else FontWeight.Normal) },
                    icon = { Icon(Icons.Default.GroupAdd, contentDescription = null) }
                )
            }

            // Keep all fields mounted to completely eliminate layout inflation latency on mobile CPU
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCreating) {
                    OutlinedTextField(
                        value = familyName,
                        onValueChange = { familyName = it },
                        label = { Text("Family Name (e.g. The Smiths)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                } else {
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = { inviteCode = it.uppercase().trim() },
                        label = { Text("8-Character Invite Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                }

                // Shared field between both modes: 0 reallocation!
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text(if (isCreating) "Your Display Name (e.g. Alice)" else "Your Display Name (e.g. Bob)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                if (isCreating) {
                    OutlinedTextField(
                        value = householdName,
                        onValueChange = { householdName = it },
                        label = { Text("Primary Residence (e.g. Main Home)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (displayName.isBlank() || (isCreating && familyName.isBlank()) || (!isCreating && inviteCode.isBlank())) {
                        errorMessage = "Please fill in all required fields."
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    coroutineScope.launch {
                        if (isCreating) {
                            val result = repository.createFamily(familyName, displayName, householdName)
                            isLoading = false
                            if (result.isSuccess) {
                                onFamilyReady()
                            } else {
                                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Failed to create family."
                            }
                        } else {
                            val result = repository.joinFamily(inviteCode, displayName)
                            isLoading = false
                            if (result.isSuccess) {
                                onFamilyReady()
                            } else {
                                errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Invalid invitation code."
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(
                        text = if (isCreating) "Start My Family" else "Join Family",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
