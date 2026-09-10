package com.example.haven.presentation.messages

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
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
import com.example.haven.core.network.ChannelDto
import com.example.haven.core.network.HavenApiClient
import com.example.haven.core.network.MessageDto
import com.example.haven.core.ui.HavenCard
import com.example.haven.data.repository.FamilyRepository
import kotlinx.coroutines.launch

@Composable
fun MessagesScreen(
    modifier: Modifier = Modifier,
    repository: FamilyRepository = FamilyRepository.instance
) {
    val familyState by repository.currentFamily.collectAsState()
    val familyId = familyState?.id ?: ""
    val apiClient = remember { HavenApiClient() }
    val scope = rememberCoroutineScope()

    var activeChannelId by remember { mutableStateOf<String?>(null) }
    var activeChannelTitle by remember { mutableStateOf("") }

    var channels by remember {
        mutableStateOf(
            listOf(
                ChannelDto("1", familyId, "Family General", "general", true, "Mom: Dinner is at 7:00 PM tonight!", "10m ago", 2),
                ChannelDto("2", familyId, "Domestic & Chores", "chores", true, "Bob: Completed the grocery run", "1h ago", 0),
                ChannelDto("3", familyId, "Emergency & Alerts", "emergency", true, "System: All members checked in safe", "Yesterday", 0),
                ChannelDto("4", familyId, "Parents Only", "parents", true, "Alice: Discussing summer camp", "2d ago", 0)
            )
        )
    }

    val channelMessages = remember {
        mutableStateMapOf<String, MutableList<MessageDto>>(
            "1" to mutableListOf(
                MessageDto("m1", "1", "u1", "Mom", "Has anyone fed Luna yet?", "12:30 PM"),
                MessageDto("m2", "1", "u2", "Charlie", "I did it before school!", "12:35 PM"),
                MessageDto("m3", "1", "u1", "Mom", "Dinner is at 7:00 PM tonight!", "12:40 PM")
            ),
            "2" to mutableListOf(
                MessageDto("m4", "2", "u3", "Bob", "Completed the grocery run", "11:15 AM")
            )
        )
    }

    val context = androidx.compose.ui.platform.LocalContext.current
    val localDb = remember { com.example.haven.data.local.HavenLocalDatabase(context) }
    val syncManager = remember { com.example.haven.data.sync.HavenSyncManager.getInstance(context) }

    LaunchedEffect(familyId) {
        if (familyId.isNotBlank()) {
            val cached = localDb.getAllChannels(familyId)
            if (cached.isNotEmpty()) {
                channels = cached
            }
            syncManager.syncAll(familyId)
        }
    }

    AnimatedContent(
        targetState = activeChannelId,
        label = "MessagesNavAnimation"
    ) { currentChannelId ->
        if (currentChannelId != null) {
            val msgs = remember(currentChannelId) {
                channelMessages.getOrPut(currentChannelId) {
                    mutableStateListOf(
                        MessageDto("seed_1", currentChannelId, "u1", "Mom", "Welcome to the $activeChannelTitle channel! 🛡️", "12:00 PM"),
                        MessageDto("seed_2", currentChannelId, "u2", "Charlie", "Messages here are encrypted.", "12:05 PM")
                    )
                }
            }

            LaunchedEffect(currentChannelId) {
                // 1. Instant load from local SQLite
                val cachedMsgs = localDb.getMessagesForChannel(currentChannelId)
                if (cachedMsgs.isNotEmpty()) {
                    msgs.clear()
                    msgs.addAll(cachedMsgs)
                }
                // 2. Fetch fresh
                if (familyId.isNotBlank()) {
                    val res = apiClient.getMessages(familyId, currentChannelId)
                    res.onSuccess { remoteMsgs ->
                        if (remoteMsgs.isNotEmpty()) {
                            msgs.clear()
                            msgs.addAll(remoteMsgs)
                            remoteMsgs.forEach { localDb.insertMessage(it, isSynced = true) }
                        }
                    }
                }
            }

            ChannelDetailScreen(
                channelId = currentChannelId,
                channelName = activeChannelTitle,
                onBack = { activeChannelId = null },
                messages = msgs,
                onSendMessage = { text ->
                    val newMsg = MessageDto(
                        id = "local_${System.currentTimeMillis()}",
                        channel_id = currentChannelId,
                        sender_id = "me",
                        sender_name = "Alice",
                        content = text,
                        created_at = "Just now"
                    )
                    // 1. Instant UI update (0ms lag)
                    msgs.add(newMsg)
                    // 2. Immediate local SQLite persistence
                    localDb.insertMessage(newMsg, isSynced = false)
                    // 3. Queue in offline Outbox & Trigger sync
                    if (familyId.isNotBlank()) {
                        val payload = org.json.JSONObject().apply {
                            put("channel_id", currentChannelId)
                            put("sender_id", "me")
                            put("sender_name", "Alice")
                            put("content", text)
                        }.toString()
                        localDb.enqueueOutboxAction("SEND_MESSAGE", payload)
                        syncManager.syncAll(familyId)
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "End-to-End Encrypted Channels",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Encrypted",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                items(channels, key = { it.id }) { channel ->
                    HavenCard(
                        modifier = Modifier.clickable {
                            activeChannelId = channel.id
                            activeChannelTitle = channel.name
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.ChatBubble,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = channel.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = channel.last_message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                if (channel.last_message_time.isNotBlank()) {
                                    Text(
                                        text = "Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                                if (channel.unread_count > 0) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("${channel.unread_count}")
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
        }
    }
}
