package com.example.haven.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

import com.example.haven.BuildConfig

class HavenApiClient(
    private val baseUrl: String = BuildConfig.BACKEND_URL
) {
    private var authToken: String = ""

    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
                .addHeader("Content-Type", "application/json")
            if (authToken.isNotBlank()) {
                builder.addHeader("Authorization", "Bearer $authToken")
            }
            chain.proceed(builder.build())
        }
        .build()

    fun setAuthToken(token: String) {
        authToken = token
    }

    fun getAuthToken(): String = authToken

    suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): Result<AuthTokenDto> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("display_name", displayName)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/auth/register")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                val dto = AuthTokenDto.fromJson(JSONObject(bodyString))
                authToken = dto.access_token
                Result.success(dto)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<AuthTokenDto> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", password)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/auth/login")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                val dto = AuthTokenDto.fromJson(JSONObject(bodyString))
                authToken = dto.access_token
                Result.success(dto)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserProfile(): Result<UserProfileDto> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/auth/me")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                Result.success(UserProfileDto.fromJson(JSONObject(bodyString)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createFamily(
        familyName: String,
        displayName: String,
        householdName: String = "Main Home"
    ): Result<FamilyDto> = withContext(Dispatchers.IO) {
        try {
            val reqModel = CreateFamilyApiRequest(familyName, displayName, householdName)
            val reqBody = reqModel.toJson().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/families")
                .post(reqBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                val family = FamilyDto.fromJson(JSONObject(bodyString))
                Result.success(family)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinFamily(
        inviteCode: String,
        displayName: String
    ): Result<FamilyMemberDto> = withContext(Dispatchers.IO) {
        try {
            val reqModel = JoinFamilyApiRequest(inviteCode, displayName)
            val reqBody = reqModel.toJson().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$baseUrl/families/join")
                .post(reqBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                val member = FamilyMemberDto.fromJson(JSONObject(bodyString))
                Result.success(member)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listMyFamilies(): Result<List<FamilyDto>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/families")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                val jsonArray = JSONArray(bodyString)
                val families = mutableListOf<FamilyDto>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i)
                    if (obj != null) {
                        families.add(FamilyDto.fromJson(obj))
                    }
                }
                Result.success(families)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChannels(familyId: String): Result<List<ChannelDto>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/chat/$familyId/channels")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                val arr = JSONArray(bodyString)
                val channels = mutableListOf<ChannelDto>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) channels.add(ChannelDto.fromJson(obj))
                }
                Result.success(channels)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessages(familyId: String, channelId: String): Result<List<MessageDto>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/chat/$familyId/channels/$channelId/messages")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                val arr = JSONArray(bodyString)
                val list = mutableListOf<MessageDto>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) list.add(MessageDto.fromJson(obj))
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(
        familyId: String,
        channelId: String,
        senderId: String,
        senderName: String,
        content: String
    ): Result<MessageDto> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("sender_id", senderId)
                put("sender_name", senderName)
                put("content", content)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/chat/$familyId/channels/$channelId/messages")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                Result.success(MessageDto.fromJson(JSONObject(bodyString)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTasks(familyId: String): Result<List<TaskDto>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/tasks/$familyId")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                val arr = JSONArray(bodyString)
                val list = mutableListOf<TaskDto>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) list.add(TaskDto.fromJson(obj))
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTask(familyId: String, title: String, assignedTo: String): Result<TaskDto> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("title", title)
                put("assigned_to", assignedTo)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/tasks/$familyId")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                Result.success(TaskDto.fromJson(JSONObject(bodyString)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleTask(familyId: String, taskId: String): Result<TaskDto> = withContext(Dispatchers.IO) {
        try {
            val emptyBody = "".toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/tasks/$familyId/$taskId/toggle")
                .patch(emptyBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                Result.success(TaskDto.fromJson(JSONObject(bodyString)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCalendarEvents(familyId: String): Result<List<CalendarEventDto>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/families/$familyId/calendar")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                val arr = JSONArray(bodyString)
                val list = mutableListOf<CalendarEventDto>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) list.add(CalendarEventDto.fromJson(obj))
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createCalendarEvent(
        familyId: String,
        title: String,
        date: String,
        startTime: String,
        endTime: String?,
        location: String,
        attendee: String,
        category: String
    ): Result<CalendarEventDto> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("title", title)
                put("date", date)
                put("start_time", startTime)
                if (endTime != null) put("end_time", endTime)
                put("location", location)
                put("attendee", attendee)
                put("category", category)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/families/$familyId/calendar")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                Result.success(CalendarEventDto.fromJson(JSONObject(bodyString)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSafetyZones(familyId: String): Result<List<SafetyZoneDto>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/families/$familyId/safety/zones")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                val arr = JSONArray(bodyString)
                val list = mutableListOf<SafetyZoneDto>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) list.add(SafetyZoneDto.fromJson(obj))
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleSafetyZone(familyId: String, zoneId: String): Result<SafetyZoneDto> = withContext(Dispatchers.IO) {
        try {
            val emptyBody = "".toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/families/$familyId/safety/zones/$zoneId/toggle")
                .patch(emptyBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                Result.success(SafetyZoneDto.fromJson(JSONObject(bodyString)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun broadcastSos(familyId: String, senderName: String, lat: Double, lng: Double): Result<SosAlertDto> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("sender_name", senderName)
                put("latitude", lat)
                put("longitude", lng)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/families/$familyId/safety/sos")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                Result.success(SosAlertDto.fromJson(JSONObject(bodyString)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExpenses(familyId: String): Result<List<FamilyExpenseDto>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/families/$familyId/finance/expenses")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                val arr = JSONArray(bodyString)
                val list = mutableListOf<FamilyExpenseDto>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) list.add(FamilyExpenseDto.fromJson(obj))
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createExpense(
        familyId: String,
        title: String,
        amount: Double,
        category: String,
        paidBy: String,
        splitType: String = "Equal",
        date: String = "Today"
    ): Result<FamilyExpenseDto> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("title", title)
                put("amount", amount)
                put("category", category)
                put("paid_by", paidBy)
                put("split_type", splitType)
                put("date", date)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/families/$familyId/finance/expenses")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                Result.success(FamilyExpenseDto.fromJson(JSONObject(bodyString)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getVaultDocuments(familyId: String): Result<List<VaultDocumentDto>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$baseUrl/families/$familyId/finance/vault")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                val arr = JSONArray(bodyString)
                val list = mutableListOf<VaultDocumentDto>()
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i)
                    if (obj != null) list.add(VaultDocumentDto.fromJson(obj))
                }
                Result.success(list)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun storeVaultDocument(
        familyId: String,
        title: String,
        docType: String,
        encryptedBlob: String,
        uploadedBy: String = "Alice"
    ): Result<VaultDocumentDto> = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("title", title)
                put("doc_type", docType)
                put("encrypted_blob", encryptedBlob)
                put("uploaded_by", uploadedBy)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$baseUrl/families/$familyId/finance/vault")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException("HTTP Error ${response.code}: $bodyString"))
                }
                Result.success(VaultDocumentDto.fromJson(JSONObject(bodyString)))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

