package com.classroomassessment.scanner.feature.selection

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.rounded.Class
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
import com.classroomassessment.scanner.ui.EmptyState
import com.classroomassessment.scanner.ui.InfoCard
import com.classroomassessment.scanner.ui.InlineError
import com.classroomassessment.scanner.ui.PrimaryAction
import com.classroomassessment.scanner.ui.SecondaryAction
import com.classroomassessment.scanner.ui.ScreenHeader

@Composable
fun ClassSelectScreen(
    teacherName: String?,
    organization: Organization?,
    classes: List<Classroom>,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: (() -> Unit)?,
    onSelect: (Classroom) -> Unit,
    onRetry: () -> Unit,
    onClearError: () -> Unit,
) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(
                title = "选择班级",
                onBack = onBack,
            )
            teacherName?.takeIf(String::isNotBlank)?.let { currentTeacher ->
                Text(
                    text = currentTeacher,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            organization?.name?.takeIf(String::isNotBlank)?.let { organizationName ->
                Text(
                    text = organizationName,
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            if (errorMessage != null) {
                InlineError(errorMessage, onClearError)
                Spacer(Modifier.height(12.dp))
                if (!isLoading && classes.isEmpty()) {
                    SecondaryAction(
                        text = "重新加载",
                        onClick = onRetry,
                        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    )
                }
            }
            if (!isLoading && classes.isEmpty() && errorMessage == null) {
                EmptyState(
                    title = "暂无可访问班级",
                    body = "当前账号还没有可进入的班级。",
                )
            } else if (classes.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(classes, key = { it.id }) { classroom ->
                        SelectableRow(
                            title = classroom.name,
                            icon = Icons.Rounded.Class,
                            onClick = { onSelect(classroom) },
                        )
                    }
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
    onClearError: () -> Unit,
) {
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(
                title = "选择测试",
                subtitle = classroom?.name,
                onBack = onBack,
            )
            Text(
                text = "可扫描测试",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            if (errorMessage != null) {
                InlineError(errorMessage, onClearError)
                Spacer(Modifier.height(12.dp))
            }
            if (!isLoading && sessions.isEmpty() && errorMessage == null) {
                EmptyState(
                    title = "暂无可扫描测试",
                    body = "请先在 Web 工作台创建并发布一场可扫描测试。",
                )
            } else if (sessions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(sessions, key = { it.id }) { session ->
                        InfoCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(session.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "${session.questionCount} 道题 · 已提交 ${session.submittedCount} / ${session.participantCount}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { onSelect(session) }) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "开始扫描", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                            Spacer(Modifier.height(12.dp))
                            PrimaryAction("开始扫描", onClick = { onSelect(session) }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    InfoCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "进入", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
