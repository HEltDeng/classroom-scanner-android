package com.classroomassessment.scanner.feature.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.classroomassessment.scanner.core.network.Account
import com.classroomassessment.scanner.core.network.AssessmentSession
import com.classroomassessment.scanner.core.network.BackendRepository
import com.classroomassessment.scanner.core.network.Classroom
import com.classroomassessment.scanner.core.network.Organization
import com.classroomassessment.scanner.core.network.ScannerSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    SPLASH,
    LOGIN,
    CLASS_SELECT,
    SESSION_SELECT,
    SCANNER,
}

data class AppUiState(
    val screen: AppScreen = AppScreen.SPLASH,
    val account: Account? = null,
    val classes: List<Classroom> = emptyList(),
    val sessions: List<AssessmentSession> = emptyList(),
    val selectedOrganization: Organization? = null,
    val selectedClass: Classroom? = null,
    val selectedSession: AssessmentSession? = null,
    val scannerSession: ScannerSession? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
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
            if (!backend.hasSession()) {
                _uiState.update { it.copy(screen = AppScreen.LOGIN) }
                return@launch
            }

            runCatching { backend.currentAccount() }
                .onSuccess(::loadWorkspace)
                .onFailure {
                    backend.signOut()
                    _uiState.update { state ->
                        state.copy(screen = AppScreen.LOGIN, account = null, errorMessage = "登录已失效，请重新登录")
                    }
                }
        }
    }

    fun login(organizationName: String, teacherLoginName: String, password: String) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching { backend.login(organizationName.trim(), teacherLoginName.trim(), password) }
                .onSuccess(::loadWorkspace)
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "登录失败，请稍后重试")
                    }
                }
        }
    }

    fun selectClass(classroom: Classroom) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    selectedClass = classroom,
                    selectedSession = null,
                    scannerSession = null,
                    errorMessage = null,
                )
            }
            runCatching { backend.activeSessions(classroom.id) }
                .onSuccess { sessions ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            sessions = sessions,
                            screen = AppScreen.SESSION_SELECT,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "测试加载失败") }
                }
        }
    }

    fun selectSession(session: AssessmentSession) {
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    selectedSession = session,
                    scannerSession = null,
                    errorMessage = null,
                )
            }
            runCatching { backend.createOrGetScannerSession(session.id) }
                .onSuccess { scannerSession ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            scannerSession = scannerSession,
                            screen = AppScreen.SCANNER,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            screen = AppScreen.SESSION_SELECT,
                            errorMessage = error.message ?: "无法打开扫描会话",
                        )
                    }
                }
        }
    }

    fun backToClass() = _uiState.update {
        it.copy(screen = AppScreen.CLASS_SELECT, selectedSession = null, scannerSession = null, errorMessage = null)
    }

    fun backToSessions() = _uiState.update {
        it.copy(screen = AppScreen.SESSION_SELECT, scannerSession = null, errorMessage = null)
    }

    fun signOut() {
        backend.signOut()
        _uiState.value = AppUiState(screen = AppScreen.LOGIN)
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private fun loadWorkspace(account: Account) {
        viewModelScope.launch {
            val organization = Organization(account.organizationId, account.organizationName)
            _uiState.update {
                it.copy(
                    isLoading = true,
                    account = account,
                    selectedOrganization = organization,
                    errorMessage = null,
                )
            }
            runCatching { backend.classes() }
                .onSuccess { classes ->
                    val activeClasses = classes.filter { it.status.equals("ACTIVE", ignoreCase = true) }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            classes = activeClasses,
                            screen = AppScreen.CLASS_SELECT,
                            errorMessage = if (activeClasses.isEmpty()) "当前账号暂无可访问班级" else null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "班级加载失败") }
                }
        }
    }

    class Factory(private val backend: BackendRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AppViewModel::class.java))
            return AppViewModel(backend) as T
        }
    }
}
