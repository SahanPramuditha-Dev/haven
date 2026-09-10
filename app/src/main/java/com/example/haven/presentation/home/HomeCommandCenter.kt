package com.example.haven.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.haven.core.ui.HavenCard
import com.example.haven.core.ui.MemberAvatarPill
import com.example.haven.core.ui.UrgentBanner
import com.example.haven.data.model.FamilyMemberUiModel
import com.example.haven.data.model.TodayTaskItem

@Composable
fun HomeCommandCenter(
    members: List<FamilyMemberUiModel>,
    tasks: List<TodayTaskItem>,
    filterChips: List<String>,
    selectedChipIndex: Int,
    onSelectChip: (Int) -> Unit,
    onToggleTask: (TodayTaskItem, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 72.dp)
    ) {
        // 1. Presence & Safety Section
        item(key = "section_presence") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Family Presence",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = {}) {
                        Text("Live Map", style = MaterialTheme.typography.labelLarge)
                    }
                }

                if (members.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(items = members, key = { it.id }) { member ->
                            MemberAvatarPill(
                                name = member.displayName,
                                statusText = member.statusText,
                                isLive = member.isOnline
                            )
                        }
                    }
                } else {
                    Text(
                        text = "No family members connected yet. Invite your family to see live presence.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 3. Today's Domestic Coordination Header
        item(key = "section_tasks_header") {
            HavenCard {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Today's Agenda & Chores",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${tasks.count { it.isCompleted }}/${tasks.size} completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Filter Chips inside card
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(count = filterChips.size, key = { filterChips[it] }) { index ->
                            FilterChip(
                                selected = selectedChipIndex == index,
                                onClick = { onSelectChip(index) },
                                label = { Text(filterChips[index]) },
                                leadingIcon = if (selectedChipIndex == index) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        if (tasks.isEmpty()) {
            item {
                Text(
                    text = "No tasks or chores scheduled for today. Tap the '+' button to add an agenda item.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        // 4. Tasks Rendered as Lazy Items with Stable Keys for 60fps rendering
        items(
            items = tasks,
            key = { it.id }
        ) { task ->
            HavenCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Assigned to ${task.assignedTo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { isChecked ->
                            onToggleTask(task, isChecked)
                        }
                    )
                }
            }
        }
    }
}
