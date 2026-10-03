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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.coroutines.executeAsync
import ua.alerts.shared.logging.AppLog
import ua.alerts.shared.model.NeptunAlertsResponse
import ua.alerts.shared.model.NeptunWsEnvelope
import java.time.Duration

enum class ConnectionStatus {
    STOPPED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}

class NeptunClient(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val httpClient: OkHttpClient = defaultOkHttpClient(),
    private val wsUrl: String = WS_URL,
    private val restAlertsUrl: String = REST_ALERTS_URL
) {
    companion object {
        const val TAG = "NeptunWs"
        const val WS_URL = "wss://neptun.in.ua/api/v1/stream"
        const val REST_ALERTS_URL = "https://neptun.in.ua/api/v1/alerts"
        const val INITIAL_RECONNECT_DELAY_MS = 2000L
        const val MAX_RECONNECT_DELAY_MS = 30000L

        fun defaultOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .pingInterval(Duration.ofSeconds(20))
            .connectTimeout(Duration.ofSeconds(15))
            .readTimeout(Duration.ofSeconds(15))
            .callTimeout(Duration.ofSeconds(20))
            .build()

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
            .url(wsUrl)
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (!isStarted) {
                    webSocket.close(1000, "Client stopped")
                    return
                }
                AppLog.i(TAG) { "WebSocket connected successfully to $wsUrl" }
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
                AppLog.d(TAG) { "Incoming WS frame: $text" }
                try {
                    val envelope = json.decodeFromString<NeptunWsEnvelope>(text)
                    AppLog.d(TAG) { "Parsed WS envelope: type='${envelope.type}', ts=${envelope.ts}" }
                    when (envelope.type) {
                        "alerts" -> {
                            envelope.data?.let { element ->
                                AppLog.i(TAG) { "Processing WS 'alerts' payload: $element" }
                                val alerts = json.decodeFromJsonElement<NeptunAlertsResponse>(element)
                                scope.launch {
                                    _alertsFlow.emit(alerts)
                                }
                            } ?: AppLog.w(TAG, "WS 'alerts' envelope received with null data")
                        }
                        "snapshot" -> {
                            AppLog.i(TAG) { "Received WS 'snapshot' event (threat tracks state): ${envelope.data}" }
                        }
                        "upsert" -> {
                            AppLog.d(TAG) { "Received WS 'upsert' event (threat track updated): ${envelope.data}" }
                        }
                        "remove" -> {
                            AppLog.d(TAG) { "Received WS 'remove' event (threat track removed): ${envelope.data}" }
                        }
                        "heartbeat" -> {
                            AppLog.d(TAG, "Received WS 'heartbeat'")
                        }
                        else -> {
                            AppLog.w(TAG) { "Unrecognized WS envelope type '${envelope.type}': ${envelope.data}" }
                        }
                    }
                } catch (e: Exception) {
                    AppLog.e(TAG, "Error decoding or handling WS frame: ${e.message}", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (!isStarted) return
                if (this@NeptunClient.webSocket === webSocket) {
                    this@NeptunClient.webSocket = null
                }
                AppLog.e(TAG, "WebSocket failure: ${t.localizedMessage}", t)
                _lastError.value = t.localizedMessage ?: "Connection failure"
                _connectionStatus.value = ConnectionStatus.ERROR
                scheduleReconnect()
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!isStarted) return
                if (this@NeptunClient.webSocket === webSocket) {
                    this@NeptunClient.webSocket = null
                }
                AppLog.i(TAG) { "WebSocket closed (code=$code, reason='$reason')" }
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
        AppLog.i(TAG) { "Scheduling reconnect attempt in ${delayMs}ms (attempt=$reconnectAttempts)" }
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
            AppLog.d(TAG) { "Fetching alerts snapshot from REST: $restAlertsUrl" }
            val request = Request.Builder()
                .url(restAlertsUrl)
                .build()

            httpClient.newCall(request).executeAsync().use { response ->
                if (!response.isSuccessful) {
                    AppLog.e(TAG, "REST alerts snapshot request failed: HTTP ${response.code}")
                    return@withContext Result.failure(Exception("HTTP error: ${response.code}"))
                }
                val body = response.body.string()
                if (body.isEmpty()) {
                    AppLog.e(TAG, "REST alerts snapshot response body is empty")
                    return@withContext Result.failure(Exception("Empty body"))
                }
                AppLog.d(TAG) { "REST alerts snapshot received: $body" }
                val alerts = json.decodeFromString<NeptunAlertsResponse>(body)
                _alertsFlow.emit(alerts)
                Result.success(alerts)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.e(TAG, "REST alerts snapshot exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
