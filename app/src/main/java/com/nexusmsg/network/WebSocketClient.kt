package com.nexusmsg.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.nexusmsg.models.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persistent WebSocket (wss://) connection to the relay server.
 *
 * Protocol:
 * - Client connects with auth token over HTTPS WebSocket (wss://).
 * - Server sends JSON messages (delivery only — never plaintext).
 * - Client sends messages, acks, typing indicators, group messages, WebRTC signals.
 * - Auto-reconnect with exponential backoff on connection loss.
 */
@Singleton
class WebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    private var webSocket: WebSocket? = null
    private var isConnected = false
    private var reconnectJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val incomingMessages = Channel<MessagePayload>(Channel.BUFFERED)
    val incomingGroupMessages = Channel<GroupMessagePayload>(Channel.BUFFERED)
    val typingUpdates = Channel<TypingIndicator>(Channel.BUFFERED)
    val webRtcSignals = Channel<WebRtcSignal>(Channel.BUFFERED)
    val connectionState = Channel<Boolean>(Channel.CONFLATED)

    private var serverUrl: String = ""
    private var token: String = ""
    private var userId: String = ""

    fun connect(serverUrl: String, token: String, userId: String) {
        if (isConnected) return

        // Store for reconnect
        this.serverUrl = serverUrl
        this.token = token
        this.userId = userId

        // Build wss:// URL
        val wsUrl = serverUrl
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .trimEnd('/') + "/ws?token=$token&user_id=$userId"

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                isConnected = true
                connectionState.trySend(true)
                reconnectJob?.cancel()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = gson.fromJson<Map<String, Any>>(
                        text,
                        object : TypeToken<Map<String, Any>>() {}.type
                    )
                    when (json["type"]) {
                        "message" -> {
                            val payload = gson.fromJson<MessagePayload>(
                                gson.toJson(json["payload"]),
                                object : TypeToken<MessagePayload>() {}.type
                            )
                            incomingMessages.trySend(payload)
                        }
                        "group_message" -> {
                            val payload = gson.fromJson<GroupMessagePayload>(
                                gson.toJson(json["payload"]),
                                object : TypeToken<GroupMessagePayload>() {}.type
                            )
                            incomingGroupMessages.trySend(payload)
                        }
                        "typing" -> {
                            val typing = gson.fromJson<TypingIndicator>(
                                gson.toJson(json["payload"]),
                                object : TypeToken<TypingIndicator>() {}.type
                            )
                            typingUpdates.trySend(typing)
                        }
                        "webrtc_signal" -> {
                            val signal = gson.fromJson<WebRtcSignal>(
                                gson.toJson(json["payload"]),
                                object : TypeToken<WebRtcSignal>() {}.type
                            )
                            webRtcSignals.trySend(signal)
                        }
                        "group_update" -> {
                            // Group created/member joined — handled by event
                        }
                    }
                } catch (_: Exception) { }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected = false
                connectionState.trySend(false)
                scheduleReconnect()
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                isConnected = false
                connectionState.trySend(false)
                scheduleReconnect()
            }
        })
    }

    fun sendMessage(payload: MessagePayload) {
        sendJson("message", payload)
    }

    fun sendGroupMessage(payload: GroupMessagePayload) {
        sendJson("group_message", payload)
    }

    fun sendTypingIndicator(typing: TypingIndicator) {
        sendJson("typing", typing)
    }

    fun sendWebRtcSignal(signal: WebRtcSignal) {
        sendJson("webrtc_signal", signal)
    }

    fun sendAck(messageId: String, status: String) {
        sendJson("ack", mapOf(
            "message_id" to messageId,
            "status" to status
        ))
    }

    private fun sendJson(type: String, payload: Any) {
        val json = gson.toJson(mapOf("type" to type, "payload" to payload))
        webSocket?.send(json)
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        isConnected = false
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var delay = 1000L
            while (isActive && !isConnected) {
                delay(delay)
                delay = (delay * 2).coerceAtMost(30000L)
                connect(serverUrl, token, userId)
            }
        }
    }
}
