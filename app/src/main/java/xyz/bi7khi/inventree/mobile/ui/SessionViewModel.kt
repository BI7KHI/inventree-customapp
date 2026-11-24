package xyz.bi7khi.inventree.mobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.bi7khi.inventree.mobile.InvenTreeApplication
import xyz.bi7khi.inventree.mobile.data.api.ApiClient
import xyz.bi7khi.inventree.mobile.data.api.ApiException
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.ServerInfo
import xyz.bi7khi.inventree.mobile.data.api.Session
import xyz.bi7khi.inventree.mobile.data.api.toUserMessage
import xyz.bi7khi.inventree.mobile.util.UrlUtils

sealed interface SessionUiState {
    data object Loading : SessionUiState

    data class SignedOut(
        val serverUrl: String,
        val showServerSetup: Boolean = false,
        val busy: Boolean = false,
        val error: String? = null,
        val notice: String? = null,
        val serverInfo: ServerInfo? = null,
    ) : SessionUiState

    data class SignedIn(
        val session: Session,
        val repository: InvenTreeRepository,
    ) : SessionUiState
}

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as InvenTreeApplication).container
    private val store = container.sessionStore
    private val _state = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    val state: StateFlow<SessionUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { restore() }
        viewModelScope.launch {
            ApiClient.unauthorizedEvents.collect {
                val current = _state.value
                if (current is SessionUiState.SignedIn) {
                    logout("登录已过期，请重新登录")
                }
            }
        }
    }

    private suspend fun restore() {
        val stored = runCatching { store.read() }.getOrNull()
        if (stored == null) {
            _state.value = SessionUiState.SignedOut(UrlUtils.defaultServer(), showServerSetup = true)
            return
        }
        if (!stored.serverConfigured) {
            _state.value = SessionUiState.SignedOut(
                serverUrl = stored.serverUrl,
                showServerSetup = true,
            )
            return
        }
        if (stored.token.isNullOrBlank() || stored.user == null) {
            _state.value = SessionUiState.SignedOut(
                serverUrl = stored.serverUrl,
                showServerSetup = false,
            )
            return
        }

        val repository = container.repository(stored.serverUrl, stored.token)
        val signedIn = SessionUiState.SignedIn(
            session = Session(stored.serverUrl, stored.token, stored.user),
            repository = repository,
        )
        _state.value = signedIn

        // Validate in the background. A network outage keeps the cached session;
        // an explicit 401 forces a fresh login.
        runCatching { repository.me() }
            .onSuccess { user ->
                store.updateUser(user)
                val current = _state.value
                if (current is SessionUiState.SignedIn) {
                    _state.value = current.copy(session = current.session.copy(user = user))
                }
            }
            .onFailure { error ->
                if (error is ApiException && error.statusCode == 401) {
                    store.clearToken(stored.serverUrl)
                    _state.value = SessionUiState.SignedOut(
                        serverUrl = stored.serverUrl,
                        notice = "登录已过期，请重新登录",
                    )
                }
            }
    }

    suspend fun testServer(input: String): Result<ServerInfo> = runCatching {
        val url = UrlUtils.normalizeServerUrl(input)
        ApiClient.probeServer(url)
    }

    fun confirmServer(input: String) {
        val current = _state.value as? SessionUiState.SignedOut ?: return
        viewModelScope.launch {
            _state.value = current.copy(busy = true, error = null)
            val result = testServer(input)
            result.onSuccess { info ->
                val url = UrlUtils.normalizeServerUrl(input)
                store.markServerConfigured(url)
                _state.value = SessionUiState.SignedOut(
                    serverUrl = url,
                    showServerSetup = false,
                    serverInfo = info,
                )
            }.onFailure { error ->
                _state.value = current.copy(
                    busy = false,
                    error = error.toUserMessage(),
                )
            }
        }
    }

    fun login(username: String, password: String) {
        val current = _state.value as? SessionUiState.SignedOut ?: return
        if (username.isBlank() || password.isBlank()) {
            _state.value = current.copy(error = "请输入用户名和密码")
            return
        }
        viewModelScope.launch {
            _state.value = current.copy(busy = true, error = null, notice = null)
            runCatching {
                val info = ApiClient.probeServer(current.serverUrl)
                val token = ApiClient.requestToken(current.serverUrl, username.trim(), password)
                val repository = container.repository(current.serverUrl, token.token)
                val user = repository.me()
                val session = Session(current.serverUrl, token.token, user)
                store.saveSession(session)
                Triple(session, repository, info)
            }.onSuccess { (session, repository, info) ->
                _state.value = SessionUiState.SignedIn(session, repository)
            }.onFailure { error ->
                _state.value = current.copy(
                    busy = false,
                    error = error.toUserMessage(),
                )
            }
        }
    }

    fun loginWithToken(tokenInput: String) {
        val current = _state.value as? SessionUiState.SignedOut ?: return
        val token = tokenInput.trim().removePrefix("Token ").removePrefix("token ").trim()
        if (token.isBlank()) {
            _state.value = current.copy(error = "请输入 API Token")
            return
        }
        viewModelScope.launch {
            _state.value = current.copy(busy = true, error = null, notice = null)
            runCatching {
                val repository = container.repository(current.serverUrl, token)
                val user = repository.me()
                val session = Session(current.serverUrl, token, user)
                store.saveSession(session)
                session to repository
            }.onSuccess { (session, repository) ->
                _state.value = SessionUiState.SignedIn(session, repository)
            }.onFailure { error ->
                _state.value = current.copy(busy = false, error = error.toUserMessage())
            }
        }
    }

    fun refreshUser() {
        val current = _state.value as? SessionUiState.SignedIn ?: return
        viewModelScope.launch {
            runCatching { current.repository.me() }.onSuccess { user ->
                store.updateUser(user)
                _state.value = current.copy(session = current.session.copy(user = user))
            }
        }
    }

    fun logout(message: String? = null) {
        val current = _state.value
        val server = when (current) {
            is SessionUiState.SignedIn -> current.session.serverUrl
            is SessionUiState.SignedOut -> current.serverUrl
            else -> UrlUtils.defaultServer()
        }
        viewModelScope.launch {
            runCatching { store.clearToken(server) }
            _state.value = SessionUiState.SignedOut(
                serverUrl = server,
                showServerSetup = false,
                notice = message,
            )
        }
    }

    fun switchServer() {
        val current = _state.value
        val server = when (current) {
            is SessionUiState.SignedIn -> current.session.serverUrl
            is SessionUiState.SignedOut -> current.serverUrl
            else -> UrlUtils.defaultServer()
        }
        viewModelScope.launch {
            runCatching {
                store.clearToken(server)
                store.resetServerConfiguration()
            }
            _state.value = SessionUiState.SignedOut(
                serverUrl = server,
                showServerSetup = true,
            )
        }
    }

    fun handleUnauthorized() {
        logout("登录已过期，请重新登录")
    }
}
