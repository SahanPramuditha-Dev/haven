package com.example.haven.core.realtime

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import okhttp3.*
import org.json.JSONObject

import com.example.haven.BuildConfig

data class PresenceUpdate(
    val userId: String,
    val isOnline: Boolean,
    val statusText: String
)

/**
 * High-performance, low-power WebSocket Realtime Client for live family presence.
 */
class HavenRealtimeClient(
    private val host: String = BuildConfig.WS_URL
) {
    private val client = OkHttpClient.Builder().build()
    private var webSocket: WebSocket? = null

    private val _presenceUpdates = MutableSharedFlow<PresenceUpdate>(extraBufferCapacity = 64)
    val presenceUpdates: SharedFlow<PresenceUpdate> = _presenceUpdates.asSharedFlow()

    fun connect(familyId: String, userId: String, authToken: String = "") {
        if (familyId.isBlank() || userId.isBlank()) return
        disconnect()

        val protocol = if (host.startsWith("https://") || (!host.startsWith("http://") && !host.contains("10.0.2.2") && !host.contains("localhost"))) "wss" else "ws"
        val cleanHost = host.removePrefix("https://").removePrefix("http://")
        val url = "$protocol://$cleanHost/ws/$familyId/$userId"

        val requestBuilder = Request.Builder().url(url)
        if (authToken.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $authToken")
        }
        val request = requestBuilder.build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("HavenRealtime", "WebSocket connected for family: $familyId, user: $userId")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    if (json.optString("type") == "PRESENCE_UPDATE") {
                        val uid = json.getString("user_id")
                        val isOnline = json.getBoolean("is_online")
                        val statusText = json.optString("status_text", if (isOnline) "Online" else "Offline")
                        _presenceUpdates.tryEmit(PresenceUpdate(uid, isOnline, statusText))
                    }
                } catch (e: Exception) {
                    Log.w("HavenRealtime", "Failed to parse websocket message", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("HavenRealtime", "WebSocket disconnected/failed: ${t.message}")
            }
        })
    }

    fun updateStatus(statusText: String) {
        val payload = JSONObject().apply {
            put("action", "UPDATE_STATUS")
            put("status_text", statusText)
        }
        webSocket?.send(payload.toString())
    }

    fun disconnect() {
        webSocket?.close(1000, "App closed/disconnected")
        webSocket = null
    }

    companion object {
        @Volatile
        private var INSTANCE: HavenRealtimeClient? = null

        fun getInstance(): HavenRealtimeClient {
            return INSTANCE ?: synchronized(this) {
                HavenRealtimeClient().also { INSTANCE = it }
            }
        }
    }
}
