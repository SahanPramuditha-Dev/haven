package com.example.haven.presentation.safety

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
import androidx.compose.ui.platform.LocalContext
import com.example.haven.core.network.HavenApiClient
import com.example.haven.core.network.SafetyZoneDto
import com.example.haven.core.ui.HavenCard
import com.example.haven.data.local.HavenLocalDatabase
import com.example.haven.data.repository.FamilyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@Composable
fun SafetyScreen(
    modifier: Modifier = Modifier,
    repository: FamilyRepository = FamilyRepository.instance
) {
    val context = LocalContext.current
    val localDb = remember { HavenLocalDatabase(context) }
    val familyState by repository.currentFamily.collectAsState()
    val familyId = familyState?.id ?: ""
    val apiClient = remember { HavenApiClient() }
    val scope = rememberCoroutineScope()

    var zones by remember { mutableStateOf<List<SafetyZoneDto>>(emptyList()) }
    var sosBroadcastSent by remember { mutableStateOf(false) }

    // Load instantly from SQLite on IO Dispatcher
    LaunchedEffect(familyId) {
        if (familyId.isNotBlank()) {
            withContext(Dispatchers.IO) {
                val cached = localDb.getAllSafetyZones(familyId)
                withContext(Dispatchers.Main) {
                    zones = cached
                }

                // Sync fresh from backend
                val res = apiClient.getSafetyZones(familyId)
                res.onSuccess { remoteZones ->
                    if (remoteZones.isNotEmpty()) {
                        remoteZones.forEach { localDb.upsertSafetyZone(it) }
                        withContext(Dispatchers.Main) {
                            zones = remoteZones
                        }
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
            // SOS Emergency SOS Trigger Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (sosBroadcastSent) "🚨 SOS ALERT BROADCASTED!" else "Family Emergency SOS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = if (sosBroadcastSent)
                            "Live coordinates (37.7749° N, 122.4194° W) dispatched to all family members."
                        else
                            "Press to broadcast instant SOS alert with live coordinates to all family members.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (familyId.isNotBlank()) {
                                com.example.haven.core.notification.HavenNotificationManager.showEmergencyAlert(
                                    context, "Alice", 37.7749, -122.4194
                                )
                                scope.launch {
                                    apiClient.broadcastSos(familyId, "Alice", 37.7749, -122.4194)
                                    sosBroadcastSent = true
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (sosBroadcastSent) "SOS Active • Tap to Resend" else "Send Family SOS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "Active Geo-Fences & Places",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (zones.isEmpty()) {
            item {
                Text(
                    text = "No safety zones configured yet. Add your home, school, or work locations to receive arrival and departure notifications.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        items(zones, key = { it.id }) { zone ->
            HavenCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = when (zone.zone_type) {
                                        "home" -> Icons.Outlined.Home
                                        "school" -> Icons.Outlined.School
                                        else -> Icons.Outlined.Work
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Column {
                            Text(zone.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(zone.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(
                        checked = zone.is_active,
                        onCheckedChange = { isChecked ->
                            val updatedZone = zone.copy(is_active = isChecked)
                            zones = zones.map { if (it.id == zone.id) updatedZone else it }
                            scope.launch(Dispatchers.IO) {
                                localDb.upsertSafetyZone(updatedZone)
                                if (familyId.isNotBlank()) {
                                    val res = apiClient.toggleSafetyZone(familyId, zone.id)
                                    if (res.isFailure) {
                                        val payload = JSONObject().apply {
                                            put("zone_id", zone.id)
                                        }
                                        localDb.enqueueOutboxAction("TOGGLE_SAFETY_ZONE", payload.toString())
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(70.dp))
        }
    }
}

