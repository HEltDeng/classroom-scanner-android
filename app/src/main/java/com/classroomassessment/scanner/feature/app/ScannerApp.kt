package com.classroomassessment.scanner.feature.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classroomassessment.scanner.core.AppContainer
import com.classroomassessment.scanner.feature.auth.LoginScreen
import com.classroomassessment.scanner.feature.history.ScanHistoryScreen
import com.classroomassessment.scanner.feature.history.ScanQueueScreen
import com.classroomassessment.scanner.feature.scanner.ScannerScreen
import com.classroomassessment.scanner.feature.selection.ClassSelectScreen
import com.classroomassessment.scanner.feature.selection.OrganizationSelectScreen
import com.classroomassessment.scanner.feature.selection.SessionSelectScreen
import com.classroomassessment.scanner.feature.settings.SettingsScreen
import com.classroomassessment.scanner.ui.ScannerTheme

@Composable
fun ClassroomScannerApp(container: AppContainer) {
    val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(container.backend))
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) { viewModel.start() }

    ScannerTheme {
        when (state.screen) {
            AppScreen.SPLASH -> SplashScreen()
            AppScreen.LOGIN -> LoginScreen(
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                isMock = com.classroomassessment.scanner.BuildConfig.MOCK_BACKEND_ENABLED,
                onLogin = viewModel::login,
                onClearError = viewModel::clearError,
            )
            AppScreen.ORGANIZATION_SELECT -> OrganizationSelectScreen(
                organizations = state.organizations,
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                onSelect = viewModel::selectOrganization,
                onClearError = viewModel::clearError,
            )
            AppScreen.CLASS_SELECT -> ClassSelectScreen(
                organization = state.selectedOrganization,
                classes = state.classes,
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                onBack = if (state.organizations.size > 1) viewModel::backToOrganizations else null,
                onSelect = viewModel::selectClass,
                onClearError = viewModel::clearError,
            )
            AppScreen.SESSION_SELECT -> SessionSelectScreen(
                classroom = state.selectedClass,
                sessions = state.sessions,
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                onBack = viewModel::backToClass,
                onSelect = viewModel::selectSession,
                onOpenSettings = viewModel::openSettings,
                onClearError = viewModel::clearError,
            )
            AppScreen.SCANNER -> state.selectedSession?.let { session ->
                ScannerScreen(
                    session = session,
                    classroomName = state.selectedClass?.name.orEmpty(),
                    queueRepository = container.scanQueue,
                    onBack = viewModel::backToSessions,
                    onOpenQueue = viewModel::openQueue,
                    onOpenHistory = viewModel::openHistory,
                    onOpenSettings = viewModel::openSettings,
                )
            } ?: SessionSelectScreen(
                classroom = state.selectedClass,
                sessions = state.sessions,
                isLoading = state.isLoading,
                errorMessage = "请选择一个 ACTIVE 课堂",
                onBack = viewModel::backToClass,
                onSelect = viewModel::selectSession,
                onOpenSettings = viewModel::openSettings,
                onClearError = viewModel::clearError,
            )
            AppScreen.QUEUE -> state.selectedSession?.let { session ->
                ScanQueueScreen(session, container.scanQueue, viewModel::backToScanner)
            }
            AppScreen.HISTORY -> state.selectedSession?.let { session ->
                ScanHistoryScreen(session, container.scanQueue, viewModel::backToScanner)
            }
            AppScreen.SETTINGS -> SettingsScreen(
                isMock = com.classroomassessment.scanner.BuildConfig.MOCK_BACKEND_ENABLED,
                versionInfo = state.versionInfo,
                isCheckingVersion = state.isCheckingVersion,
                versionError = state.versionError,
                onCheckForUpdate = viewModel::checkForUpdate,
                onSignOut = viewModel::signOut,
                onBack = if (state.selectedSession != null) viewModel::backToScanner else viewModel::backToSessions,
            )
        }
    }
}

@Composable
private fun SplashScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(72.dp))
            Spacer(Modifier.height(18.dp))
            Text("课堂扫描器", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Scan once. Keep the class moving.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
