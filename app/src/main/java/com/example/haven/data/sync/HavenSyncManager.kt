package com.example.haven.data.sync

import android.content.Context
import android.util.Log
import com.example.haven.core.network.HavenApiClient
import com.example.haven.core.network.MessageDto
import com.example.haven.core.network.TaskDto
import com.example.haven.data.local.HavenLocalDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Robust Sync Manager processing the offline outbox and syncing data bidirectionally.
 */
class HavenSyncManager private constructor(
    private val localDb: HavenLocalDatabase,
    private val apiClient: HavenApiClient = HavenApiClient()
) {
    private val syncScope = CoroutineScope(Dispatchers.IO)

    fun syncAll(familyId: String, context: Context? = null) {
        if (familyId.isBlank()) return
        // 1. Immediately trigger asynchronous sync in scope
        syncScope.launch {
            drainAndSyncDirect(familyId)
        }
        // 2. Also enqueue with WorkManager for guaranteed background delivery & connectivity retries
        if (context != null) {
            HavenSyncWorker.enqueue(context, familyId)
        }
    }

    suspend fun drainAndSyncDirect(familyId: String) {
        if (familyId.isBlank()) return
        // 1. Drain offline outbox first
        drainOutbox(familyId)


            // 2. Fetch fresh channels & messages from backend
            apiClient.getChannels(familyId).onSuccess { remoteChannels ->
                remoteChannels.forEach { ch ->
                    localDb.upsertChannel(ch)
                    // Sync messages for channel
                    apiClient.getMessages(familyId, ch.id).onSuccess { remoteMsgs ->
                        remoteMsgs.forEach { msg ->
                            localDb.insertMessage(msg, isSynced = true)
                        }
                    }
                }
            }

            // 3. Fetch fresh tasks
            apiClient.getTasks(familyId).onSuccess { remoteTasks ->
                remoteTasks.forEach { task ->
                    localDb.upsertTask(task, isSynced = true)
                }
            }

            // 4. Fetch fresh calendar events
            apiClient.getCalendarEvents(familyId).onSuccess { remoteEvents ->
                remoteEvents.forEach { event ->
                    localDb.upsertCalendarEvent(event, isSynced = true)
                }
            }

            // 5. Fetch fresh safety zones
            apiClient.getSafetyZones(familyId).onSuccess { remoteZones ->
                remoteZones.forEach { zone ->
                    localDb.upsertSafetyZone(zone)
                }
            }

            // 6. Fetch fresh expenses
            apiClient.getExpenses(familyId).onSuccess { remoteExpenses ->
                remoteExpenses.forEach { exp ->
                    localDb.upsertExpense(exp)
                }
            }

            // 7. Fetch fresh encrypted vault documents
            apiClient.getVaultDocuments(familyId).onSuccess { remoteDocs ->
                remoteDocs.forEach { doc ->
                    localDb.upsertVaultDocument(doc)
                }
            }
    }


    private suspend fun drainOutbox(familyId: String) {
        val pending = localDb.getPendingOutboxActions()
        for ((id, action) in pending) {
            val (type, payload) = action
            try {
                when (type) {
                    "CREATE_TASK" -> {
                        val json = JSONObject(payload)
                        val title = json.getString("title")
                        val assignedTo = json.getString("assigned_to")
                        val res = apiClient.createTask(familyId, title, assignedTo)
                        if (res.isSuccess) {
                            localDb.removeOutboxAction(id)
                            res.getOrNull()?.let { localDb.upsertTask(it, isSynced = true) }
                        }
                    }
                    "TOGGLE_TASK" -> {
                        val json = JSONObject(payload)
                        val taskId = json.getString("task_id")
                        val res = apiClient.toggleTask(familyId, taskId)
                        if (res.isSuccess) {
                            localDb.removeOutboxAction(id)
                        }
                    }
                    "SEND_MESSAGE" -> {
                        val json = JSONObject(payload)
                        val channelId = json.getString("channel_id")
                        val senderId = json.getString("sender_id")
                        val senderName = json.getString("sender_name")
                        val content = json.getString("content")
                        val res = apiClient.sendMessage(familyId, channelId, senderId, senderName, content)
                        if (res.isSuccess) {
                            localDb.removeOutboxAction(id)
                        }
                    }
                    "CREATE_EVENT" -> {
                        val json = JSONObject(payload)
                        val title = json.getString("title")
                        val date = json.getString("date")
                        val startTime = json.getString("start_time")
                        val endTime = if (json.isNull("end_time")) null else json.getString("end_time")
                        val location = json.getString("location")
                        val attendee = json.getString("attendee")
                        val category = json.getString("category")
                        val res = apiClient.createCalendarEvent(
                            familyId, title, date, startTime, endTime, location, attendee, category
                        )
                        if (res.isSuccess) {
                            localDb.removeOutboxAction(id)
                            res.getOrNull()?.let { localDb.upsertCalendarEvent(it, isSynced = true) }
                        }
                    }
                    "CREATE_EXPENSE" -> {
                        val json = JSONObject(payload)
                        val title = json.getString("title")
                        val amount = json.getDouble("amount")
                        val category = json.getString("category")
                        val paidBy = json.getString("paid_by")
                        val splitType = json.optString("split_type", "Equal")
                        val date = json.optString("date", "Today")
                        val res = apiClient.createExpense(familyId, title, amount, category, paidBy, splitType, date)
                        if (res.isSuccess) {
                            localDb.removeOutboxAction(id)
                            res.getOrNull()?.let { localDb.upsertExpense(it) }
                        }
                    }
                    "TOGGLE_SAFETY_ZONE" -> {
                        val json = JSONObject(payload)
                        val zoneId = json.getString("zone_id")
                        val res = apiClient.toggleSafetyZone(familyId, zoneId)
                        if (res.isSuccess) {
                            localDb.removeOutboxAction(id)
                            res.getOrNull()?.let { localDb.upsertSafetyZone(it) }
                        }
                    }
                    "STORE_VAULT_DOC" -> {
                        val json = JSONObject(payload)
                        val title = json.getString("title")
                        val docType = json.getString("doc_type")
                        val encryptedBlob = json.getString("encrypted_blob")
                        val uploadedBy = json.optString("uploaded_by", "Alice")
                        val res = apiClient.storeVaultDocument(familyId, title, docType, encryptedBlob, uploadedBy)
                        if (res.isSuccess) {
                            localDb.removeOutboxAction(id)
                            res.getOrNull()?.let { localDb.upsertVaultDocument(it) }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("HavenSyncManager", "Failed to process outbox action: $type", e)
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: HavenSyncManager? = null

        fun getInstance(context: Context): HavenSyncManager {
            return INSTANCE ?: synchronized(this) {
                val db = HavenLocalDatabase(context.applicationContext)
                HavenSyncManager(db).also { INSTANCE = it }
            }
        }
    }
}
