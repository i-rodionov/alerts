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
    STOPPED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

class NeptunClient(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    companion object {
        const val WS_URL = "wss://neptun.in.ua/api/v1/stream"
        const val REST_ALERTS_URL = "https://neptun.in.ua/api/v1/alerts"
        const val INITIAL_RECONNECT_DELAY_MS = 2000L
        const val MAX_RECONNECT_DELAY_MS = 30000L

        fun calculateBackoffDelayMs(attempt: Int): Long {
            val shift = attempt.coerceIn(0, 4)
            val delay = INITIAL_RECONNECT_DELAY_MS * (1L shl shift)
            return delay.coerceAtMost(MAX_RECONNECT_DELAY_MS)
        }
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectAttempts = 0

    @Volatile
    private var isStarted = false

    private val _alertsFlow = MutableSharedFlow<NeptunAlertsResponse>(replay = 1)
    val alertsFlow: SharedFlow<NeptunAlertsResponse> = _alertsFlow.asSharedFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.STOPPED)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    val isRunning: Boolean
        get() = isStarted

    fun start() {
        if (isStarted) {
            if (_connectionStatus.value == ConnectionStatus.ERROR) {
                reconnectAttempts = 0
                reconnectJob?.cancel()
                reconnectJob = null
                connect()
            }
            return
        }
        isStarted = true
        reconnectAttempts = 0
        connect()
    }

    fun stop() {
        isStarted = false
        reconnectJob?.cancel()
        reconnectJob = null
        reconnectAttempts = 0
        val ws = webSocket
        webSocket = null
        ws?.close(1000, "Client stopped")
        _connectionStatus.value = ConnectionStatus.STOPPED
        _lastError.value = null
    }

    private fun connect() {
        if (!isStarted) return
        _connectionStatus.value = ConnectionStatus.CONNECTING

        val oldWs = webSocket
        webSocket = null
        oldWs?.cancel()

        val request = Request.Builder()
            .url(WS_URL)
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!isStarted) {
                    webSocket.close(1000, "Client stopped")
                    return
                }
                reconnectAttempts = 0
                reconnectJob?.cancel()
                reconnectJob = null
                _lastError.value = null
                _connectionStatus.value = ConnectionStatus.CONNECTED
                scope.launch {
                    fetchAlertsSnapshot()
                }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                if (!isStarted) return
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
                if (!isStarted) return
                if (this@NeptunClient.webSocket === webSocket) {
                    this@NeptunClient.webSocket = null
                }
                _lastError.value = t.localizedMessage ?: "Connection failure"
                _connectionStatus.value = ConnectionStatus.ERROR
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!isStarted) return
                if (this@NeptunClient.webSocket === webSocket) {
                    this@NeptunClient.webSocket = null
                }
                if (code != 1000) {
                    _connectionStatus.value = ConnectionStatus.RECONNECTING
                    scheduleReconnect()
                }
            }
        })
    }

    private fun scheduleReconnect() {
        if (!isStarted) return
        if (reconnectJob?.isActive == true) return

        val delayMs = calculateBackoffDelayMs(reconnectAttempts++)
        _connectionStatus.value = ConnectionStatus.RECONNECTING

        reconnectJob = scope.launch {
            // Attempt REST snapshot fallback while disconnected
            fetchAlertsSnapshot()

            delay(delayMs)
            if (!isStarted || !isActive) return@launch

            reconnectJob = null
            connect()
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
