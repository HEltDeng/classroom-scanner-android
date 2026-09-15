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
import retrofit2.HttpException
import java.io.IOException

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
                        it.copy(
                            isLoading = false,
                            account = null,
                            classes = emptyList(),
                            sessions = emptyList(),
                            selectedClass = null,
                            selectedSession = null,
                            scannerSession = null,
                            errorMessage = userFacingError(
                                error = error,
                                fallback = "登录失败，请稍后重试",
                                unauthorizedMessage = "机构名称、老师或密码不正确",
                            ),
                        )
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
                    if (handleUnauthorized(error)) return@onFailure
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            screen = AppScreen.CLASS_SELECT,
                            sessions = emptyList(),
                            errorMessage = userFacingError(error, "测试加载失败"),
                        )
                    }
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
                    if (handleUnauthorized(error)) return@onFailure
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            screen = AppScreen.SESSION_SELECT,
                            errorMessage = userFacingError(error, "无法打开扫描会话"),
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

    fun reloadClasses() {
        if (_uiState.value.isLoading) return
        _uiState.value.account?.let(::loadWorkspace)
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
                    screen = AppScreen.CLASS_SELECT,
                    isLoading = true,
                    account = account,
                    selectedOrganization = organization,
                    classes = emptyList(),
                    sessions = emptyList(),
                    selectedClass = null,
                    selectedSession = null,
                    scannerSession = null,
                    errorMessage = null,
                )
            }
            runCatching { backend.classes() }
                .onSuccess { classes ->
                    val activeClasses = classes.filter {
                        it.organizationId == account.organizationId && it.status.equals("ACTIVE", ignoreCase = true)
                    }
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
                    if (handleUnauthorized(error)) return@onFailure
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            screen = AppScreen.CLASS_SELECT,
                            classes = emptyList(),
                            sessions = emptyList(),
                            selectedClass = null,
                            selectedSession = null,
                            scannerSession = null,
                            errorMessage = userFacingError(error, "班级加载失败"),
                        )
                    }
                }
        }
    }

    private fun userFacingError(
        error: Throwable,
        fallback: String,
        unauthorizedMessage: String = "登录已失效，请重新登录",
    ): String = when (error) {
        is HttpException -> when (error.code()) {
            401 -> unauthorizedMessage
            403 -> "当前账号没有访问权限"
            404 -> "加载失败，请重试"
            in 500..599 -> "服务暂不可用，请稍后重试"
            else -> fallback
        }
        is IOException -> "网络无法连接，请检查网络后重试"
        else -> fallback
    }

    private fun handleUnauthorized(error: Throwable): Boolean {
        if (error !is HttpException || error.code() != 401) return false
        backend.signOut()
        _uiState.value = AppUiState(
            screen = AppScreen.LOGIN,
            errorMessage = "登录已失效，请重新登录",
        )
        return true
    }

    class Factory(private val backend: BackendRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(AppViewModel::class.java))
            return AppViewModel(backend) as T
        }
    }
}
