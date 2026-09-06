package ua.alerts.mobile.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import ua.alerts.shared.model.NeptunAlertsResponse
import ua.alerts.shared.model.NeptunWsEnvelope
import java.util.concurrent.TimeUnit

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING
}

class NeptunClient(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    companion object {
        const val WS_URL = "wss://neptun.in.ua/api/v1/stream"
        const val REST_ALERTS_URL = "https://neptun.in.ua/api/v1/alerts"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var isStarted = false

    private val _alertsFlow = MutableSharedFlow<NeptunAlertsResponse>(replay = 1)
    val alertsFlow: SharedFlow<NeptunAlertsResponse> = _alertsFlow.asSharedFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    fun start() {
        if (isStarted) return
        isStarted = true
        connect()
    }

    fun stop() {
        isStarted = false
        reconnectJob?.cancel()
        reconnectJob = null
        webSocket?.close(1000, "Client stopped")
        webSocket = null
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
    }

    private fun connect() {
        if (!isStarted) return
        _connectionStatus.value = ConnectionStatus.CONNECTING

        val request = Request.Builder()
            .url(WS_URL)
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                // Fetch snapshot immediately on connect to ensure up-to-date state
                scope.launch {
                    fetchAlertsSnapshot()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val envelope = json.decodeFromString<NeptunWsEnvelope>(text)
                    when (envelope.type) {
                        "alerts" -> {
                            envelope.data?.let { element ->
                                val alerts = json.decodeFromJsonElement<NeptunAlertsResponse>(element)
                                scope.launch {
                                    _alertsFlow.emit(alerts)
                                }
                            }
                        }
                        "snapshot" -> {
                            // snapshot frame contains threats, but alerts may also be attached
                            scope.launch {
                                fetchAlertsSnapshot()
                            }
                        }
                        "heartbeat" -> {
                            // Heartbeat received
                        }
                    }
                } catch (_: Exception) {
                    // Ignored or unrecognized frame
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                handleDisconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                handleDisconnect()
            }
        })
    }

    private fun handleDisconnect() {
        if (!isStarted) return
        _connectionStatus.value = ConnectionStatus.RECONNECTING
        scheduleReconnect()
    }

    private fun scheduleReconnect() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            var delayMs = 3000L
            while (isActive && isStarted && _connectionStatus.value != ConnectionStatus.CONNECTED) {
                // Try REST snapshot while WebSocket is disconnected
                fetchAlertsSnapshot()

                delay(delayMs)
                if (!isStarted) break

                connect()
                delayMs = (delayMs * 1.5).toLong().coerceAtMost(30000L)
            }
        }
    }

    suspend fun fetchAlertsSnapshot(): Result<NeptunAlertsResponse> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(REST_ALERTS_URL)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error: ${response.code}"))
                }
                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty body"))
                val alerts = json.decodeFromString<NeptunAlertsResponse>(body)
                _alertsFlow.emit(alerts)
                Result.success(alerts)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
