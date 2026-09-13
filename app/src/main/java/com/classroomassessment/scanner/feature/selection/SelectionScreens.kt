package com.classroomassessment.scanner.feature.selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Class
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classroomassessment.scanner.core.network.AssessmentSession
import com.classroomassessment.scanner.core.network.Classroom
import com.classroomassessment.scanner.core.network.Organization
import com.classroomassessment.scanner.ui.InfoCard
import com.classroomassessment.scanner.ui.InlineError
import com.classroomassessment.scanner.ui.PrimaryAction
import com.classroomassessment.scanner.ui.ScreenHeader

@Composable
fun OrganizationSelectScreen(
    organizations: List<Organization>,
    isLoading: Boolean,
    errorMessage: String?,
    onSelect: (Organization) -> Unit,
    onClearError: () -> Unit,
) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader("选择机构", "当前账号可访问的机构")
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            if (errorMessage != null) {
                InlineError(errorMessage, onClearError)
                Spacer(Modifier.height(12.dp))
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(organizations, key = { it.id }) { organization ->
                    SelectableRow(
                        title = organization.name,
                        subtitle = "机构",
                        icon = Icons.Rounded.Business,
                        onClick = { onSelect(organization) },
                    )
                }
            }
        }
    }
}

@Composable
fun ClassSelectScreen(
    organization: Organization?,
    classes: List<Classroom>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: (() -> Unit)?,
    onSelect: (Classroom) -> Unit,
    onClearError: () -> Unit,
) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(
                title = "选择班级",
                subtitle = organization?.name,
                onBack = onBack,
            )
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            if (errorMessage != null) {
                InlineError(errorMessage, onClearError)
                Spacer(Modifier.height(12.dp))
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(classes, key = { it.id }) { classroom ->
                    SelectableRow(
                        title = classroom.name,
                        subtitle = "进入后选择 ACTIVE 课堂",
                        icon = Icons.Rounded.Class,
                        onClick = { onSelect(classroom) },
                    )
                }
            }
        }
    }
}

@Composable
fun SessionSelectScreen(
    classroom: Classroom?,
    sessions: List<AssessmentSession>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSelect: (AssessmentSession) -> Unit,
    onOpenSettings: () -> Unit,
    onClearError: () -> Unit,
) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(
                title = "选择课堂",
                subtitle = classroom?.name,
                onBack = onBack,
                trailing = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "设置")
                    }
                },
            )
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            if (errorMessage != null) {
                InlineError(errorMessage, onClearError)
                Spacer(Modifier.height(12.dp))
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(sessions, key = { it.id }) { session ->
                    InfoCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(session.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text("ACTIVE · ${session.questionCount} 题", color = MaterialTheme.colorScheme.secondary)
                            }
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        Text("已提交 ${session.submittedCount} / ${session.participantCount}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(12.dp))
                        PrimaryAction("开始扫描", onClick = { onSelect(session) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    InfoCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
