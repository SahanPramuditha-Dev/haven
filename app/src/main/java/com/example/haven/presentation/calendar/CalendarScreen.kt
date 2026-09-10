package com.example.haven.presentation.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.haven.core.network.CalendarEventDto
import com.example.haven.core.network.HavenApiClient
import com.example.haven.core.ui.HavenCard
import com.example.haven.data.local.HavenLocalDatabase
import com.example.haven.data.repository.FamilyRepository
import com.example.haven.data.sync.HavenSyncManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    repository: FamilyRepository = FamilyRepository.instance
) {
    val familyState by repository.currentFamily.collectAsState()
    val familyId = familyState?.id ?: ""
    val context = androidx.compose.ui.platform.LocalContext.current
    val localDb = remember { HavenLocalDatabase(context) }
    val syncManager = remember { HavenSyncManager.getInstance(context) }
    val apiClient = remember { HavenApiClient() }
    val scope = rememberCoroutineScope()

    val eventList = remember { mutableStateListOf<CalendarEventDto>() }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(familyId) {
        if (familyId.isNotBlank()) {
            val cached = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                localDb.getAllCalendarEvents(familyId)
            }
            if (cached.isNotEmpty()) {
                eventList.clear()
                eventList.addAll(cached)
            } else {
                eventList.addAll(
                    listOf(
                        CalendarEventDto("1", familyId, "Charlie Soccer Practice", "4:30 PM", "6:00 PM", "Sunday, Sep 6", "Community Sports Complex", "Charlie, Bob", "Sports", "Just now"),
                        CalendarEventDto("2", familyId, "Family Dinner", "7:00 PM", "8:30 PM", "Sunday, Sep 6", "Dining Room", "All Family", "Routine", "Just now"),
                        CalendarEventDto("3", familyId, "Dentist Appointment", "10:00 AM", "11:00 AM", "Monday, Sep 7", "Central Dental Clinic", "Alice", "Health", "Just now")
                    )
                )
            }
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                syncManager.syncAll(familyId)
                apiClient.getCalendarEvents(familyId).onSuccess { remoteEvents ->
                    if (remoteEvents.isNotEmpty()) {
                        remoteEvents.forEach { localDb.upsertCalendarEvent(it, isSynced = true) }
                    }
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Sunday, Sep 6",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${eventList.size} events scheduled",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledIconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Event")
                    }
                }
            }
        }

        items(eventList, key = { it.id }) { event ->
            HavenCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = when (event.category) {
                                "Sports" -> MaterialTheme.colorScheme.tertiaryContainer
                                "Health" -> MaterialTheme.colorScheme.errorContainer
                                "School" -> MaterialTheme.colorScheme.primaryContainer
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (event.category) {
                                        "Sports" -> Icons.Outlined.FitnessCenter
                                        "Health" -> Icons.Outlined.LocalHospital
                                        "School" -> Icons.Outlined.School
                                        else -> Icons.Outlined.Event
                                    },
                                    contentDescription = null,
                                    tint = when (event.category) {
                                        "Sports" -> MaterialTheme.colorScheme.onTertiaryContainer
                                        "Health" -> MaterialTheme.colorScheme.onErrorContainer
                                        "School" -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val timeText = if (event.end_time != null) "${event.start_time} - ${event.end_time}" else event.start_time
                            Text(
                                text = "$timeText • ${event.location.ifBlank { "Home" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(event.attendee, style = MaterialTheme.typography.labelSmall) }
                                )
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(event.date, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("5:00 PM") }
        var location by remember { mutableStateOf("") }
        var attendee by remember { mutableStateOf("All Family") }
        var category by remember { mutableStateOf("Routine") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Schedule Family Event") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Event Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        label = { Text("Time (e.g. 5:00 PM)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = attendee,
                        onValueChange = { attendee = it },
                        label = { Text("Attendee / Assignee") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank() && familyId.isNotBlank()) {
                            val tempId = "event_${System.currentTimeMillis()}"
                            val newEvent = CalendarEventDto(
                                id = tempId,
                                family_id = familyId,
                                title = title,
                                start_time = time,
                                end_time = null,
                                date = "Today",
                                location = location,
                                attendee = attendee,
                                category = category,
                                created_at = "Just now"
                            )
                            // 1. Instant local render
                            eventList.add(newEvent)
                            scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                // 2. Local SQLite ACID storage off main thread
                                localDb.upsertCalendarEvent(newEvent, isSynced = false)
                                // 3. Queue in Outbox & trigger sync
                                val payload = org.json.JSONObject().apply {
                                    put("title", title)
                                    put("date", "Today")
                                    put("start_time", time)
                                    put("location", location)
                                    put("attendee", attendee)
                                    put("category", category)
                                }.toString()
                                localDb.enqueueOutboxAction("CREATE_EVENT", payload)
                                syncManager.syncAll(familyId)
                            }
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save Event")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

