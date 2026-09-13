package com.classroomassessment.scanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import com.classroomassessment.scanner.core.scan.ScanStatus

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
            }
            Spacer(Modifier.width(4.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (subtitle != null) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun PrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
    ) {
        Text(text)
    }
}

@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            content()
        }
    }
}

@Composable
fun ModeChip(isMock: Boolean) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(if (isMock) "本地 Mock 模式" else "Backend 模式") },
    )
}

@Composable
fun ScanStatusChip(status: ScanStatus) {
    val (label, color, icon) = when (status) {
        ScanStatus.LOCAL -> Triple("本地保存", MaterialTheme.colorScheme.secondary, Icons.Rounded.CloudOff)
        ScanStatus.QUEUED -> Triple("待上传", MaterialTheme.colorScheme.primary, Icons.Rounded.HourglassTop)
        ScanStatus.UPLOADING -> Triple("上传中", MaterialTheme.colorScheme.primary, Icons.Rounded.Sync)
        ScanStatus.UPLOADED -> Triple("已上传", MaterialTheme.colorScheme.secondary, Icons.Rounded.Sync)
        ScanStatus.PROCESSING -> Triple("处理中", MaterialTheme.colorScheme.tertiary, Icons.Rounded.HourglassTop)
        ScanStatus.SUCCESS -> Triple("已完成", MaterialTheme.colorScheme.secondary, Icons.Rounded.CheckCircle)
        ScanStatus.REVIEW_REQUIRED -> Triple("待复核", MaterialTheme.colorScheme.primary, Icons.Rounded.WarningAmber)
        ScanStatus.FAILED -> Triple("上传失败", MaterialTheme.colorScheme.error, Icons.Rounded.ErrorOutline)
    }
    AssistChip(
        onClick = {},
        enabled = false,
        leadingIcon = { Icon(icon, contentDescription = null, tint = color) },
        label = { Text(label, color = color, fontWeight = FontWeight.SemiBold) },
    )
}

@Composable
fun EmptyState(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun InlineError(message: String, onDismiss: (() -> Unit)? = null) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.16f)),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(10.dp))
            Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground)
            if (onDismiss != null) TextButton(onClick = onDismiss) { Text("知道了") }
        }
    }
}

fun displayToken(token: String): String = when {
    token.length <= 14 -> token
    else -> "${token.take(6)}…${token.takeLast(4)}"
}
