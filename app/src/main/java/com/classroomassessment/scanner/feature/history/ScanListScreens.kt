package com.classroomassessment.scanner.feature.history

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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classroomassessment.scanner.core.db.ScanEntity
import com.classroomassessment.scanner.core.network.AssessmentSession
import com.classroomassessment.scanner.core.scan.ScanQueueRepository
import com.classroomassessment.scanner.core.scan.ScanStatus
import com.classroomassessment.scanner.core.scan.isPendingUpload
import com.classroomassessment.scanner.ui.EmptyState
import com.classroomassessment.scanner.ui.InfoCard
import com.classroomassessment.scanner.ui.PrimaryAction
import com.classroomassessment.scanner.ui.ScanStatusChip
import com.classroomassessment.scanner.ui.ScreenHeader
import com.classroomassessment.scanner.ui.displayToken
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScanQueueScreen(
    session: AssessmentSession,
    queueRepository: ScanQueueRepository,
    onBack: () -> Unit,
) {
    ScanListScreen(
        title = "上传队列",
        subtitle = session.name,
        session = session,
        queueRepository = queueRepository,
        onBack = onBack,
        predicate = { status -> status != ScanStatus.SUCCESS && status != ScanStatus.REVIEW_REQUIRED },
    )
}

@Composable
fun ScanHistoryScreen(
    session: AssessmentSession,
    queueRepository: ScanQueueRepository,
    onBack: () -> Unit,
) {
    ScanListScreen(
        title = "扫描历史",
        subtitle = session.name,
        session = session,
        queueRepository = queueRepository,
        onBack = onBack,
        predicate = { true },
    )
}

@Composable
private fun ScanListScreen(
    title: String,
    subtitle: String,
    session: AssessmentSession,
    queueRepository: ScanQueueRepository,
    onBack: () -> Unit,
    predicate: (ScanStatus) -> Boolean,
) {
    val scope = rememberCoroutineScope()
    val allScans by queueRepository.observeForSession(session.id).collectAsState(initial = emptyList())
    val scans = allScans.filter { predicate(statusOf(it)) }
    Scaffold { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScreenHeader(title, subtitle, onBack)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text("共 ${allScans.size} 张", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("待处理 ${allScans.count { statusOf(it) == ScanStatus.PROCESSING || statusOf(it).isPendingUpload() }}", color = MaterialTheme.colorScheme.primary)
                Text("异常 ${allScans.count { statusOf(it) == ScanStatus.FAILED || statusOf(it) == ScanStatus.REVIEW_REQUIRED }}", color = MaterialTheme.colorScheme.error)
            }
            if (scans.isEmpty()) {
                EmptyState(
                    title = if (title == "上传队列") "队列已清空" else "还没有扫描记录",
                    body = if (title == "上传队列") "新的答题卡会在拍摄后自动出现在这里" else "开始扫描后，记录会保存在本 Session",
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(scans, key = { it.localCaptureId }) { scan ->
                        ScanCard(scan = scan, onRetry = { scope.launch { queueRepository.retry(scan.localCaptureId) } })
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanCard(scan: ScanEntity, onRetry: () -> Unit) {
    val status = statusOf(scan)
    InfoCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(scan.studentName ?: "页面待后端确认", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!scan.studentCode.isNullOrBlank()) {
                    Text(scan.studentCode.orEmpty(), color = MaterialTheme.colorScheme.secondary)
                } else {
                    Text("Token ${displayToken(scan.pageToken)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            ScanStatusChip(status)
        }
        Spacer(Modifier.height(8.dp))
        Text(formatTime(scan.capturedAt), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        if (!scan.lastError.isNullOrBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(scan.lastError.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (status == ScanStatus.FAILED) {
            Spacer(Modifier.height(10.dp))
            PrimaryAction("点击重试", onClick = onRetry, modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun statusOf(scan: ScanEntity): ScanStatus = runCatching { ScanStatus.valueOf(scan.uploadStatus) }
    .getOrDefault(ScanStatus.FAILED)

private fun formatTime(timestamp: Long): String = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestamp))
