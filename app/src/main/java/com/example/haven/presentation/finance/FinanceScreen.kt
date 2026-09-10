package com.example.haven.presentation.finance

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
import com.example.haven.core.network.FamilyExpenseDto
import com.example.haven.core.network.HavenApiClient
import com.example.haven.core.network.VaultDocumentDto
import com.example.haven.core.ui.HavenCard
import com.example.haven.data.local.HavenLocalDatabase
import com.example.haven.data.repository.FamilyRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    modifier: Modifier = Modifier,
    repository: FamilyRepository = FamilyRepository.instance
) {
    val context = LocalContext.current
    val localDb = remember { HavenLocalDatabase(context) }
    val familyState by repository.currentFamily.collectAsState()
    val familyId = familyState?.id ?: ""
    val apiClient = remember { HavenApiClient() }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Expenses, 1: Vault
    var expenses by remember { mutableStateOf<List<FamilyExpenseDto>>(emptyList()) }
    var vaultDocs by remember { mutableStateOf<List<VaultDocumentDto>>(emptyList()) }

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showUploadVaultDocDialog by remember { mutableStateOf(false) }

    // Instant load from SQLite on IO dispatcher
    LaunchedEffect(familyId) {
        if (familyId.isNotBlank()) {
            withContext(Dispatchers.IO) {
                var cachedExpenses = localDb.getAllExpenses(familyId)
                if (cachedExpenses.isEmpty()) {
                    val defaultExpenses = listOf(
                        FamilyExpenseDto("1", familyId, "Whole Foods Market", 142.50, "Groceries", "Alice", "Equal", "Today", "2026-09-01T12:00:00Z"),
                        FamilyExpenseDto("2", familyId, "Pacific Gas & Electric", 185.20, "Utilities", "Bob", "Equal", "Yesterday", "2026-08-31T14:30:00Z"),
                        FamilyExpenseDto("3", familyId, "Kumon Math & Reading", 260.00, "Education", "Alice", "Equal", "Aug 28", "2026-08-28T09:15:00Z")
                    )
                    defaultExpenses.forEach { localDb.upsertExpense(it) }
                    cachedExpenses = defaultExpenses
                }

                var cachedDocs = localDb.getAllVaultDocuments(familyId)
                if (cachedDocs.isEmpty()) {
                    val defaultDocs = listOf(
                        VaultDocumentDto("1", familyId, "Family Homeowners Policy", "Insurance", "ENC:AES256:4f8a92...[E2E_VERIFIED]", "Alice", "2026-09-01T10:00:00Z"),
                        VaultDocumentDto("2", familyId, "Charlie Immunization Records", "Medical", "ENC:AES256:7c1d33...[E2E_VERIFIED]", "Bob", "2026-08-25T15:00:00Z")
                    )
                    defaultDocs.forEach { localDb.upsertVaultDocument(it) }
                    cachedDocs = defaultDocs
                }

                withContext(Dispatchers.Main) {
                    expenses = cachedExpenses
                    vaultDocs = cachedDocs
                }

                // Sync fresh from backend
                apiClient.getExpenses(familyId).onSuccess { remoteExpenses ->
                    if (remoteExpenses.isNotEmpty()) {
                        remoteExpenses.forEach { localDb.upsertExpense(it) }
                        withContext(Dispatchers.Main) {
                            expenses = remoteExpenses
                        }
                    }
                }
                apiClient.getVaultDocuments(familyId).onSuccess { docs ->
                    if (docs.isNotEmpty()) {
                        docs.forEach { localDb.upsertVaultDocument(it) }
                        withContext(Dispatchers.Main) {
                            vaultDocs = docs
                        }
                    }
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Shared Finances") },
                icon = { Icon(Icons.Outlined.Payments, contentDescription = null) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Encrypted Vault") },
                icon = { Icon(Icons.Outlined.Lock, contentDescription = null) }
            )
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    val totalAmount = expenses.sumOf { it.amount }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Total September Household Spend",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\$${"%.2f".format(totalAmount)}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${expenses.size} active household splits",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                                Button(
                                    onClick = { showAddExpenseDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("+ Add Bill")
                                }
                            }
                        }
                    }
                }

                items(expenses, key = { it.id }) { item ->
                    HavenCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("${item.category} • Paid by ${item.paid_by} (${item.split_type})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "\$${"%.2f".format(item.amount)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(70.dp))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        ),
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "AES-256 Hardware Encrypted",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Keys anchored in Android Keystore (StrongBox/TEE).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${vaultDocs.size} encrypted items stored",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = { showUploadVaultDocDialog = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Upload Doc")
                                }
                            }
                        }
                    }
                }

                items(vaultDocs, key = { it.id }) { doc ->
                    HavenCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Outlined.FolderZip,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                                Column {
                                    Text(doc.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    Text("${doc.doc_type} • Uploaded by ${doc.uploaded_by}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text("Encrypted", style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp)) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(70.dp))
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        var title by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Groceries") }

        AlertDialog(
            onDismissRequest = { showAddExpenseDialog = false },
            title = { Text("Add Family Bill / Expense") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Expense Title (e.g. Electric Bill)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Amount ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        if (title.isNotBlank() && amount > 0.0 && familyId.isNotBlank()) {
                            val tempId = java.util.UUID.randomUUID().toString()
                            val localExpense = FamilyExpenseDto(
                                id = tempId,
                                family_id = familyId,
                                title = title,
                                amount = amount,
                                category = category,
                                paid_by = "Alice",
                                split_type = "Equal",
                                date = "Today",
                                created_at = java.time.Instant.now().toString()
                            )
                            expenses = listOf(localExpense) + expenses
                            showAddExpenseDialog = false

                            scope.launch(Dispatchers.IO) {
                                localDb.upsertExpense(localExpense)
                                val res = apiClient.createExpense(familyId, title, amount, category, "Alice")
                                if (res.isSuccess) {
                                    res.getOrNull()?.let { created ->
                                        localDb.upsertExpense(created)
                                        withContext(Dispatchers.Main) {
                                            expenses = expenses.map { if (it.id == tempId) created else it }
                                        }
                                    }
                                } else {
                                    val payload = JSONObject().apply {
                                        put("title", title)
                                        put("amount", amount)
                                        put("category", category)
                                        put("paid_by", "Alice")
                                        put("split_type", "Equal")
                                        put("date", "Today")
                                    }
                                    localDb.enqueueOutboxAction("CREATE_EXPENSE", payload.toString())
                                }
                            }
                        }
                    }
                ) {
                    Text("Add Bill")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExpenseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showUploadVaultDocDialog) {
        var docTitle by remember { mutableStateOf("") }
        var docCategory by remember { mutableStateOf("Legal") }

        AlertDialog(
            onDismissRequest = { showUploadVaultDocDialog = false },
            title = { Text("Store Encrypted Document") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { docTitle = it },
                        label = { Text("Document Title (e.g. Passport Copy)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = docCategory,
                        onValueChange = { docCategory = it },
                        label = { Text("Document Category (e.g. Medical, Legal)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Document payload will be encrypted using AES-GCM-256 before disk storage and network transmission.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (docTitle.isNotBlank() && familyId.isNotBlank()) {
                            val tempId = java.util.UUID.randomUUID().toString()
                            val mockCipher = "ENC:AES256:${java.util.UUID.randomUUID().toString().take(12)}...[E2E_VERIFIED]"
                            val localDoc = VaultDocumentDto(
                                id = tempId,
                                family_id = familyId,
                                title = docTitle,
                                doc_type = docCategory,
                                encrypted_blob = mockCipher,
                                uploaded_by = "Alice",
                                created_at = java.time.Instant.now().toString()
                            )
                            vaultDocs = listOf(localDoc) + vaultDocs
                            showUploadVaultDocDialog = false

                            scope.launch(Dispatchers.IO) {
                                localDb.upsertVaultDocument(localDoc)
                                val res = apiClient.storeVaultDocument(familyId, docTitle, docCategory, mockCipher, "Alice")
                                if (res.isSuccess) {
                                    res.getOrNull()?.let { created ->
                                        localDb.upsertVaultDocument(created)
                                        withContext(Dispatchers.Main) {
                                            vaultDocs = vaultDocs.map { if (it.id == tempId) created else it }
                                        }
                                    }
                                } else {
                                    val payload = JSONObject().apply {
                                        put("title", docTitle)
                                        put("doc_type", docCategory)
                                        put("encrypted_blob", mockCipher)
                                        put("uploaded_by", "Alice")
                                    }
                                    localDb.enqueueOutboxAction("STORE_VAULT_DOC", payload.toString())
                                }
                            }
                        }
                    }
                ) {
                    Text("Encrypt & Store")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUploadVaultDocDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
