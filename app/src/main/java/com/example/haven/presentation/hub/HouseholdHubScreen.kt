package com.example.haven.presentation.hub

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.haven.data.model.FamilyMemberUiModel
import com.example.haven.presentation.family.FamilyScreen
import com.example.haven.presentation.finance.FinanceScreen

/**
 * Household Hub uniting Family Members & Roles, Shared Finances & Bills, and Encrypted Vault.
 * Complies with Material Design 3 guidelines for top-level IA organization.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdHubScreen(
    members: List<FamilyMemberUiModel>,
    inviteCode: String,
    modifier: Modifier = Modifier
) {
    var selectedHubTab by remember { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedHubTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Tab(
                selected = selectedHubTab == 0,
                onClick = { selectedHubTab = 0 },
                text = { Text("Members") },
                icon = { Icon(Icons.Outlined.People, contentDescription = null) }
            )
            Tab(
                selected = selectedHubTab == 1,
                onClick = { selectedHubTab = 1 },
                text = { Text("Finances & Vault") },
                icon = { Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null) }
            )
        }

        when (selectedHubTab) {
            0 -> FamilyScreen(members = members, inviteCode = inviteCode)
            1 -> FinanceScreen()
        }
    }
}
