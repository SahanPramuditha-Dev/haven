package com.example.haven.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.haven.core.network.*

/**
 * Enterprise-grade Android SQLite OpenHelper providing ACID local persistence
 * with zero annotation processor bottlenecks on ARM targets.
 */
class HavenLocalDatabase(context: Context) : SQLiteOpenHelper(context, "haven_local.db", null, 4) {

    override fun onCreate(db: SQLiteDatabase) {
        // 1. Tasks Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_tasks (
                id TEXT PRIMARY KEY,
                family_id TEXT NOT NULL,
                title TEXT NOT NULL,
                assigned_to TEXT NOT NULL,
                is_completed INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                sync_status INTEGER NOT NULL DEFAULT 1 -- 0: Pending Sync, 1: Synced
            )
        """.trimIndent())

        // 2. Channels Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_channels (
                id TEXT PRIMARY KEY,
                family_id TEXT NOT NULL,
                name TEXT NOT NULL,
                channel_type TEXT NOT NULL,
                is_encrypted INTEGER NOT NULL DEFAULT 1,
                last_message TEXT,
                last_message_time TEXT,
                unread_count INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        // 3. Messages Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_messages (
                id TEXT PRIMARY KEY,
                channel_id TEXT NOT NULL,
                sender_id TEXT NOT NULL,
                sender_name TEXT NOT NULL,
                content TEXT NOT NULL,
                created_at TEXT NOT NULL,
                sync_status INTEGER NOT NULL DEFAULT 1 -- 0: Pending Sync, 1: Synced
            )
        """.trimIndent())

        // 4. Offline Outbox Queue Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS sync_outbox (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                action_type TEXT NOT NULL, -- CREATE_TASK, TOGGLE_TASK, SEND_MESSAGE, CREATE_EVENT
                payload_json TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
        """.trimIndent())

        // 5. Calendar Events Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_calendar_events (
                id TEXT PRIMARY KEY,
                family_id TEXT NOT NULL,
                title TEXT NOT NULL,
                start_time TEXT NOT NULL,
                end_time TEXT,
                date TEXT NOT NULL,
                location TEXT NOT NULL,
                attendee TEXT NOT NULL,
                category TEXT NOT NULL,
                created_at TEXT NOT NULL,
                sync_status INTEGER NOT NULL DEFAULT 1
            )
        """.trimIndent())

        // 6. Safety Zones Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_safety_zones (
                id TEXT PRIMARY KEY,
                family_id TEXT NOT NULL,
                name TEXT NOT NULL,
                subtitle TEXT NOT NULL,
                zone_type TEXT NOT NULL,
                is_active INTEGER NOT NULL DEFAULT 1,
                created_at TEXT NOT NULL
            )
        """.trimIndent())

        // 7. Finance Expenses Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_expenses (
                id TEXT PRIMARY KEY,
                family_id TEXT NOT NULL,
                title TEXT NOT NULL,
                amount REAL NOT NULL,
                category TEXT NOT NULL,
                paid_by TEXT NOT NULL,
                split_type TEXT NOT NULL,
                date TEXT NOT NULL,
                created_at TEXT NOT NULL
            )
        """.trimIndent())

        // 8. Vault Documents Table
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS local_vault_documents (
                id TEXT PRIMARY KEY,
                family_id TEXT NOT NULL,
                title TEXT NOT NULL,
                doc_type TEXT NOT NULL,
                encrypted_blob TEXT NOT NULL,
                uploaded_by TEXT NOT NULL,
                created_at TEXT NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS local_safety_zones (
                    id TEXT PRIMARY KEY,
                    family_id TEXT NOT NULL,
                    name TEXT NOT NULL,
                    subtitle TEXT NOT NULL,
                    zone_type TEXT NOT NULL,
                    is_active INTEGER NOT NULL DEFAULT 1,
                    created_at TEXT NOT NULL
                )
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS local_expenses (
                    id TEXT PRIMARY KEY,
                    family_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    amount REAL NOT NULL,
                    category TEXT NOT NULL,
                    paid_by TEXT NOT NULL,
                    split_type TEXT NOT NULL,
                    date TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
            """.trimIndent())
        }
        if (oldVersion < 4) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS local_vault_documents (
                    id TEXT PRIMARY KEY,
                    family_id TEXT NOT NULL,
                    title TEXT NOT NULL,
                    doc_type TEXT NOT NULL,
                    encrypted_blob TEXT NOT NULL,
                    uploaded_by TEXT NOT NULL,
                    created_at TEXT NOT NULL
                )
            """.trimIndent())
        }
    }

    // --- Task Operations ---
    fun getAllTasks(familyId: String): List<TaskDto> {
        val list = mutableListOf<TaskDto>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, family_id, title, assigned_to, is_completed, created_at FROM local_tasks WHERE family_id = ? ORDER BY created_at ASC", arrayOf(familyId))
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    TaskDto(
                        id = it.getString(0),
                        family_id = it.getString(1),
                        title = it.getString(2),
                        assigned_to = it.getString(3),
                        is_completed = it.getInt(4) == 1,
                        created_at = it.getString(5)
                    )
                )
            }
        }
        return list
    }

    fun upsertTask(task: TaskDto, isSynced: Boolean = true) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", task.id)
            put("family_id", task.family_id)
            put("title", task.title)
            put("assigned_to", task.assigned_to)
            put("is_completed", if (task.is_completed) 1 else 0)
            put("created_at", task.created_at)
            put("sync_status", if (isSynced) 1 else 0)
        }
        db.insertWithOnConflict("local_tasks", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun toggleTaskLocally(taskId: String): Boolean {
        val db = writableDatabase
        var newState = false
        val cursor = db.rawQuery("SELECT is_completed FROM local_tasks WHERE id = ?", arrayOf(taskId))
        cursor.use {
            if (it.moveToNext()) {
                newState = it.getInt(0) == 0
            }
        }
        val values = ContentValues().apply {
            put("is_completed", if (newState) 1 else 0)
            put("sync_status", 0) // Mark pending sync
        }
        db.update("local_tasks", values, "id = ?", arrayOf(taskId))
        return newState
    }

    // --- Channel Operations ---
    fun getAllChannels(familyId: String): List<ChannelDto> {
        val list = mutableListOf<ChannelDto>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, family_id, name, channel_type, is_encrypted, last_message, last_message_time, unread_count FROM local_channels WHERE family_id = ?", arrayOf(familyId))
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    ChannelDto(
                        id = it.getString(0),
                        family_id = it.getString(1),
                        name = it.getString(2),
                        channel_type = it.getString(3),
                        is_encrypted = it.getInt(4) == 1,
                        last_message = if (it.isNull(5)) "" else it.getString(5),
                        last_message_time = if (it.isNull(6)) "" else it.getString(6),
                        unread_count = it.getInt(7)
                    )
                )
            }
        }
        return list
    }

    fun upsertChannel(channel: ChannelDto) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", channel.id)
            put("family_id", channel.family_id)
            put("name", channel.name)
            put("channel_type", channel.channel_type)
            put("is_encrypted", if (channel.is_encrypted) 1 else 0)
            put("last_message", channel.last_message)
            put("last_message_time", channel.last_message_time)
            put("unread_count", channel.unread_count)
        }
        db.insertWithOnConflict("local_channels", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- Message Operations ---
    fun getMessagesForChannel(channelId: String): List<MessageDto> {
        val list = mutableListOf<MessageDto>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, channel_id, sender_id, sender_name, content, created_at FROM local_messages WHERE channel_id = ? ORDER BY id ASC", arrayOf(channelId))
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    MessageDto(
                        id = it.getString(0),
                        channel_id = it.getString(1),
                        sender_id = it.getString(2),
                        sender_name = it.getString(3),
                        content = it.getString(4),
                        created_at = it.getString(5)
                    )
                )
            }
        }
        return list
    }

    fun insertMessage(message: MessageDto, isSynced: Boolean = true) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", message.id)
            put("channel_id", message.channel_id)
            put("sender_id", message.sender_id)
            put("sender_name", message.sender_name)
            put("content", message.content)
            put("created_at", message.created_at)
            put("sync_status", if (isSynced) 1 else 0)
        }
        db.insertWithOnConflict("local_messages", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- Outbox Queue Operations ---
    fun enqueueOutboxAction(actionType: String, payloadJson: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("action_type", actionType)
            put("payload_json", payloadJson)
            put("created_at", System.currentTimeMillis())
        }
        db.insert("sync_outbox", null, values)
    }

    fun getPendingOutboxActions(): List<Pair<Long, Pair<String, String>>> {
        val list = mutableListOf<Pair<Long, Pair<String, String>>>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, action_type, payload_json FROM sync_outbox ORDER BY id ASC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(Pair(it.getLong(0), Pair(it.getString(1), it.getString(2))))
            }
        }
        return list
    }

    fun removeOutboxAction(id: Long) {
        writableDatabase.delete("sync_outbox", "id = ?", arrayOf(id.toString()))
    }

    // --- Calendar Operations ---
    fun getAllCalendarEvents(familyId: String): List<CalendarEventDto> {
        val list = mutableListOf<CalendarEventDto>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT id, family_id, title, start_time, end_time, date, location, attendee, category, created_at FROM local_calendar_events WHERE family_id = ? ORDER BY created_at ASC",
            arrayOf(familyId)
        )
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    CalendarEventDto(
                        id = it.getString(0),
                        family_id = it.getString(1),
                        title = it.getString(2),
                        start_time = it.getString(3),
                        end_time = if (it.isNull(4)) null else it.getString(4),
                        date = it.getString(5),
                        location = it.getString(6),
                        attendee = it.getString(7),
                        category = it.getString(8),
                        created_at = it.getString(9)
                    )
                )
            }
        }
        return list
    }

    fun upsertCalendarEvent(event: CalendarEventDto, isSynced: Boolean = true) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", event.id)
            put("family_id", event.family_id)
            put("title", event.title)
            put("start_time", event.start_time)
            put("end_time", event.end_time)
            put("date", event.date)
            put("location", event.location)
            put("attendee", event.attendee)
            put("category", event.category)
            put("created_at", event.created_at)
            put("sync_status", if (isSynced) 1 else 0)
        }
        db.insertWithOnConflict("local_calendar_events", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- Safety Zones Operations ---
    fun getAllSafetyZones(familyId: String): List<SafetyZoneDto> {
        val list = mutableListOf<SafetyZoneDto>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, family_id, name, subtitle, zone_type, is_active, created_at FROM local_safety_zones WHERE family_id = ?", arrayOf(familyId))
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    SafetyZoneDto(
                        id = it.getString(0),
                        family_id = it.getString(1),
                        name = it.getString(2),
                        subtitle = it.getString(3),
                        zone_type = it.getString(4),
                        is_active = it.getInt(5) == 1,
                        created_at = it.getString(6)
                    )
                )
            }
        }
        return list
    }

    fun upsertSafetyZone(zone: SafetyZoneDto) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", zone.id)
            put("family_id", zone.family_id)
            put("name", zone.name)
            put("subtitle", zone.subtitle)
            put("zone_type", zone.zone_type)
            put("is_active", if (zone.is_active) 1 else 0)
            put("created_at", zone.created_at)
        }
        db.insertWithOnConflict("local_safety_zones", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- Finance Operations ---
    fun getAllExpenses(familyId: String): List<FamilyExpenseDto> {
        val list = mutableListOf<FamilyExpenseDto>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, family_id, title, amount, category, paid_by, split_type, date, created_at FROM local_expenses WHERE family_id = ? ORDER BY created_at DESC", arrayOf(familyId))
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    FamilyExpenseDto(
                        id = it.getString(0),
                        family_id = it.getString(1),
                        title = it.getString(2),
                        amount = it.getDouble(3),
                        category = it.getString(4),
                        paid_by = it.getString(5),
                        split_type = it.getString(6),
                        date = it.getString(7),
                        created_at = it.getString(8)
                    )
                )
            }
        }
        return list
    }

    fun upsertExpense(expense: FamilyExpenseDto) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", expense.id)
            put("family_id", expense.family_id)
            put("title", expense.title)
            put("amount", expense.amount)
            put("category", expense.category)
            put("paid_by", expense.paid_by)
            put("split_type", expense.split_type)
            put("date", expense.date)
            put("created_at", expense.created_at)
        }
        db.insertWithOnConflict("local_expenses", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // --- Vault Documents Operations ---
    fun getAllVaultDocuments(familyId: String): List<VaultDocumentDto> {
        val list = mutableListOf<VaultDocumentDto>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, family_id, title, doc_type, encrypted_blob, uploaded_by, created_at FROM local_vault_documents WHERE family_id = ? ORDER BY created_at DESC", arrayOf(familyId))
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    VaultDocumentDto(
                        id = it.getString(0),
                        family_id = it.getString(1),
                        title = it.getString(2),
                        doc_type = it.getString(3),
                        encrypted_blob = it.getString(4),
                        uploaded_by = it.getString(5),
                        created_at = it.getString(6)
                    )
                )
            }
        }
        return list
    }

    fun upsertVaultDocument(doc: VaultDocumentDto) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("id", doc.id)
            put("family_id", doc.family_id)
            put("title", doc.title)
            put("doc_type", doc.doc_type)
            put("encrypted_blob", doc.encrypted_blob)
            put("uploaded_by", doc.uploaded_by)
            put("created_at", doc.created_at)
        }
        db.insertWithOnConflict("local_vault_documents", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
