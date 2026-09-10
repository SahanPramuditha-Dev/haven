package com.example.haven.data.model

data class FamilyMemberUiModel(
    val id: String,
    val displayName: String,
    val role: String,
    val colorHex: String = "#4E6058",
    val statusText: String = "At Home",
    val isOnline: Boolean = true
)

data class UrgentAlertItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val isCritical: Boolean = false
)

data class TodayTaskItem(
    val id: String,
    val title: String,
    val assignedTo: String,
    val isCompleted: Boolean = false
)
