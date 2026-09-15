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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classroomassessment.scanner.core.AppContainer
import com.classroomassessment.scanner.feature.auth.LoginScreen
import com.classroomassessment.scanner.feature.scanner.ScannerScreen
import com.classroomassessment.scanner.feature.selection.ClassSelectScreen
import com.classroomassessment.scanner.feature.selection.SessionSelectScreen
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
                onLogin = viewModel::login,
                onClearError = viewModel::clearError,
            )
            AppScreen.CLASS_SELECT -> ClassSelectScreen(
                teacherName = state.account?.teacherName,
                organization = state.selectedOrganization,
                classes = state.classes,
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                onBack = null,
                onSelect = viewModel::selectClass,
                onRetry = viewModel::reloadClasses,
                onClearError = viewModel::clearError,
            )
            AppScreen.SESSION_SELECT -> SessionSelectScreen(
                classroom = state.selectedClass,
                sessions = state.sessions,
                isLoading = state.isLoading,
                errorMessage = state.errorMessage,
                onBack = viewModel::backToClass,
                onSelect = viewModel::selectSession,
                onClearError = viewModel::clearError,
            )
            AppScreen.SCANNER -> {
                val session = state.selectedSession
                val scannerSession = state.scannerSession
                if (session != null && scannerSession != null) {
                    ScannerScreen(
                        session = session,
                        scannerSession = scannerSession,
                        classroomName = state.selectedClass?.name.orEmpty(),
                        queueRepository = container.scanQueue,
                        onBack = viewModel::backToSessions,
                    )
                } else {
                    SessionSelectScreen(
                        classroom = state.selectedClass,
                        sessions = state.sessions,
                        isLoading = false,
                        errorMessage = "扫描会话不可用，请重新选择测试",
                        onBack = viewModel::backToClass,
                        onSelect = viewModel::selectSession,
                        onClearError = viewModel::clearError,
                    )
                }
            }
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
            Icon(
                Icons.Rounded.QrCodeScanner,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(72.dp),
            )
            Spacer(Modifier.height(18.dp))
            Text("智慧课堂系统", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("课堂扫描工作台", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
