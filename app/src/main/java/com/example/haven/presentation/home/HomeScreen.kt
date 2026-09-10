package com.example.haven.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.example.haven.core.ui.HavenCard
import com.example.haven.core.ui.MemberAvatarPill
import com.example.haven.core.ui.UrgentBanner
import com.example.haven.data.model.FamilyMemberUiModel
import com.example.haven.data.model.TodayTaskItem
import com.example.haven.data.repository.FamilyRepository
import kotlinx.coroutines.launch

/**
 * Material 3 Home Screen - Family Command Center
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    repository: FamilyRepository = FamilyRepository.instance
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var selectedChipIndex by remember { mutableIntStateOf(0) }

    val activeFamily by repository.currentFamily.collectAsState()
    val scope = rememberCoroutineScope()
    val apiClient = remember { com.example.haven.core.network.HavenApiClient() }
    var showCreateTaskDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val localDb = remember { com.example.haven.data.local.HavenLocalDatabase(context) }
    val syncManager = remember { com.example.haven.data.sync.HavenSyncManager.getInstance(context) }

    val familyName = activeFamily?.name ?: "My Family"
    val householdName = activeFamily?.primaryHousehold ?: "Home"
    val members = activeFamily?.members ?: emptyList()

    val mockTasks = remember { mutableStateListOf<TodayTaskItem>() }
    val realtimeClient = remember { com.example.haven.core.realtime.HavenRealtimeClient.getInstance() }
    val liveMembers = remember { mutableStateListOf<FamilyMemberUiModel>() }

    LaunchedEffect(members) {
        liveMembers.clear()
        liveMembers.addAll(members)
    }

    val familyId = activeFamily?.id ?: ""
    LaunchedEffect(familyId) {
        if (familyId.isNotBlank()) {
            // Connect to WebSocket for live presence
            val myUserId = repository.currentUser.value?.userId ?: members.firstOrNull()?.id ?: ""
            val myToken = repository.currentUser.value?.token ?: ""
            if (myUserId.isNotBlank()) {
                realtimeClient.connect(familyId, myUserId, myToken)
            }

            // 1. Asynchronous load from local SQLite off main thread
            val cached = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                localDb.getAllTasks(familyId)
            }
            mockTasks.clear()
            cached.forEach { t ->
                mockTasks.add(TodayTaskItem(t.id, t.title, t.assigned_to, t.is_completed))
            }
            // 2. Trigger sync with server in background
            syncManager.syncAll(familyId)
        }
    }

    // Collect real-time presence updates
    LaunchedEffect(Unit) {
        realtimeClient.presenceUpdates.collect { update ->
            val idx = liveMembers.indexOfFirst { it.id == update.userId }
            if (idx != -1) {
                val current = liveMembers[idx]
                liveMembers[idx] = current.copy(
                    isOnline = update.isOnline,
                    statusText = update.statusText
                )
            }
        }
    }

    val filterChips = listOf("All Tasks", "Mine", "Awaiting Approval", "High Priority")

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = familyName,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "$householdName • ${members.size} members",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Outlined.QrCode,
                            contentDescription = "Family Invite QR",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            if (selectedNavIndex == 0 || selectedNavIndex == 3) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateTaskDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("New Action") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                NavigationBarItem(
                    selected = selectedNavIndex == 0,
                    onClick = { selectedNavIndex = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedNavIndex == 0) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Home"
                        )
                    },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 1,
                    onClick = { selectedNavIndex = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedNavIndex == 1) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Chat"
                        )
                    },
                    label = { Text("Chat") }
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 2,
                    onClick = { selectedNavIndex = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedNavIndex == 2) Icons.Filled.CalendarMonth else Icons.Outlined.CalendarMonth,
                            contentDescription = "Calendar"
                        )
                    },
                    label = { Text("Calendar") }
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 3,
                    onClick = { selectedNavIndex = 3 },
                    icon = {
                        Icon(
                            imageVector = if (selectedNavIndex == 3) Icons.Filled.Shield else Icons.Outlined.Shield,
                            contentDescription = "Safety"
                        )
                    },
                    label = { Text("Safety") }
                )
                NavigationBarItem(
                    selected = selectedNavIndex == 4,
                    onClick = { selectedNavIndex = 4 },
                    icon = {
                        Icon(
                            imageVector = if (selectedNavIndex == 4) Icons.Filled.HomeRepairService else Icons.Outlined.HomeRepairService,
                            contentDescription = "Hub"
                        )
                    },
                    label = { Text("Hub") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedNavIndex) {
                0 -> {
                    HomeCommandCenter(
                        members = liveMembers,
                        tasks = mockTasks,
                        filterChips = filterChips,
                        selectedChipIndex = selectedChipIndex,
                        onSelectChip = { selectedChipIndex = it },
                        onToggleTask = { task, isChecked ->
                            val index = mockTasks.indexOfFirst { it.id == task.id }
                            if (index != -1) {
                                mockTasks[index] = task.copy(isCompleted = isChecked)
                            }
                            if (familyId.isNotBlank()) {
                                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    localDb.upsertTask(
                                        com.example.haven.core.network.TaskDto(
                                            id = task.id,
                                            family_id = familyId,
                                            title = task.title,
                                            assigned_to = task.assignedTo,
                                            is_completed = isChecked,
                                            created_at = "Today"
                                        ),
                                        isSynced = false
                                    )
                                    val payload = org.json.JSONObject().apply {
                                        put("task_id", task.id)
                                        put("is_completed", isChecked)
                                    }.toString()
                                    localDb.enqueueOutboxAction("TOGGLE_TASK", payload)
                                    syncManager.syncAll(familyId)
                                }
                            }
                        }
                    )
                }
                1 -> {
                    com.example.haven.presentation.messages.MessagesScreen()
                }
                2 -> {
                    com.example.haven.presentation.calendar.CalendarScreen()
                }
                3 -> {
                    com.example.haven.presentation.safety.SafetyScreen()
                }
                4 -> {
                    com.example.haven.presentation.hub.HouseholdHubScreen(
                        members = liveMembers,
                        inviteCode = activeFamily?.invitationCode ?: "HAVEN888"
                    )
                }
            }
        }

        if (showCreateTaskDialog) {
            CreateTaskDialog(
                onDismiss = { showCreateTaskDialog = false },
                onCreateTask = { newTitle, assignedToName ->
                    val tempId = "task_${System.currentTimeMillis()}"
                    val newTask = TodayTaskItem(tempId, newTitle, assignedToName, false)
                    mockTasks.add(newTask)
                    showCreateTaskDialog = false
                    if (familyId.isNotBlank()) {
                        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                            // 1. Asynchronously write to local SQLite off main thread
                            localDb.upsertTask(
                                com.example.haven.core.network.TaskDto(
                                    id = tempId,
                                    family_id = familyId,
                                    title = newTitle,
                                    assigned_to = assignedToName,
                                    is_completed = false,
                                    created_at = "Just now"
                                ),
                                isSynced = false
                            )
                            // 2. Queue in Outbox
                            val payload = org.json.JSONObject().apply {
                                put("title", newTitle)
                                put("assigned_to", assignedToName)
                            }.toString()
                            localDb.enqueueOutboxAction("CREATE_TASK", payload)
                            // 3. Trigger sync
                            syncManager.syncAll(familyId)
                        }
                    }
                },
                memberNames = members.map { it.displayName }.ifEmpty { listOf("Alice", "Bob", "Charlie") }
            )
        }
    }
}
