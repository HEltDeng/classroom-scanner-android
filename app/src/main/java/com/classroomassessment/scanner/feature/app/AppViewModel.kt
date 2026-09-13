package com.classroomassessment.scanner.feature.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.classroomassessment.scanner.core.network.Account
import com.classroomassessment.scanner.core.network.AssessmentSession
import com.classroomassessment.scanner.core.network.Classroom
import com.classroomassessment.scanner.core.network.Organization
import com.classroomassessment.scanner.core.network.BackendRepository
import com.classroomassessment.scanner.core.network.VersionInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    SPLASH,
    LOGIN,
    ORGANIZATION_SELECT,
    CLASS_SELECT,
    SESSION_SELECT,
    SCANNER,
    QUEUE,
    HISTORY,
    SETTINGS,
}

data class AppUiState(
    val screen: AppScreen = AppScreen.SPLASH,
    val account: Account? = null,
    val organizations: List<Organization> = emptyList(),
    val classes: List<Classroom> = emptyList(),
    val sessions: List<AssessmentSession> = emptyList(),
    val selectedOrganization: Organization? = null,
    val selectedClass: Classroom? = null,
    val selectedSession: AssessmentSession? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val versionInfo: VersionInfo? = null,
    val isCheckingVersion: Boolean = false,
    val versionError: String? = null,
)

class AppViewModel(
    private val backend: BackendRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var started = false

    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            delay(550)
            if (backend.hasSession()) {
                loadOrganizations(restoredAccount())
            } else {
                _uiState.update { it.copy(screen = AppScreen.LOGIN) }
            }
        }
    }

    fun login(username: String, password: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { backend.login(username.trim(), password) }
                .onSuccess { account -> loadOrganizations(account) }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "登录失败，请稍后重试")
                    }
                }
        }
    }

    fun selectOrganization(organization: Organization) {
        loadClasses(organization)
    }

    fun selectClass(classroom: Classroom) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, selectedClass = classroom) }
            runCatching { backend.activeSessions(classroom.id) }
                .onSuccess { sessions ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sessions = sessions,
                            screen = AppScreen.SESSION_SELECT,
                            errorMessage = if (sessions.isEmpty()) "当前班级没有 ACTIVE 课堂" else null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "课堂加载失败") }
                }
        }
    }

    fun selectSession(session: AssessmentSession) {
        _uiState.update { it.copy(selectedSession = session, screen = AppScreen.SCANNER, errorMessage = null) }
    }

    fun openQueue() = _uiState.update { it.copy(screen = AppScreen.QUEUE) }

    fun openHistory() = _uiState.update { it.copy(screen = AppScreen.HISTORY) }

    fun openSettings() {
        _uiState.update { it.copy(screen = AppScreen.SETTINGS, versionError = null) }
        checkForUpdate()
    }

    fun backToScanner() = _uiState.update { it.copy(screen = AppScreen.SCANNER) }

    fun backToOrganizations() = _uiState.update { it.copy(screen = AppScreen.ORGANIZATION_SELECT) }

    fun backToClass() = _uiState.update { it.copy(screen = AppScreen.CLASS_SELECT) }

    fun backToSessions() = _uiState.update { it.copy(screen = AppScreen.SESSION_SELECT) }

    fun checkForUpdate() {
        if (_uiState.value.isCheckingVersion) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingVersion = true, versionError = null) }
            runCatching { backend.latestVersion() }
                .onSuccess { version -> _uiState.update { it.copy(versionInfo = version, isCheckingVersion = false) } }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isCheckingVersion = false, versionError = error.message ?: "版本检查失败")
                    }
                }
        }
    }

    fun signOut() {
        backend.signOut()
        _uiState.value = AppUiState(screen = AppScreen.LOGIN)
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private fun loadOrganizations(account: Account?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, account = account, errorMessage = null) }
            runCatching { backend.organizations() }
                .onSuccess { organizations ->
                    if (organizations.size == 1) {
                        loadClasses(organizations.single())
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                organizations = organizations,
                                screen = AppScreen.ORGANIZATION_SELECT,
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "机构加载失败") }
                }
        }
    }

    private fun loadClasses(organization: Organization) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    selectedOrganization = organization,
                    errorMessage = null,
                )
            }
            runCatching { backend.classes(organization.id) }
                .onSuccess { classes ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            classes = classes,
                            screen = AppScreen.CLASS_SELECT,
                            errorMessage = if (classes.isEmpty()) "当前机构没有可用班级" else null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "班级加载失败") }
                }
        }
    }

    private fun restoredAccount() = Account("restored", "已登录账号", "")

    class Factory(private val backend: BackendRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AppViewModel::class.java))
            return AppViewModel(backend) as T
        }
    }
}
