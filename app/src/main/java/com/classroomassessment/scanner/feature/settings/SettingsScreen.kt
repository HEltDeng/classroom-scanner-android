package com.classroomassessment.scanner.feature.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.classroomassessment.scanner.BuildConfig
import com.classroomassessment.scanner.core.network.VersionInfo
import com.classroomassessment.scanner.ui.InfoCard
import com.classroomassessment.scanner.ui.InlineError
import com.classroomassessment.scanner.ui.ModeChip
import com.classroomassessment.scanner.ui.PrimaryAction
import com.classroomassessment.scanner.ui.ScreenHeader

@Composable
fun SettingsScreen(
    isMock: Boolean,
    versionInfo: VersionInfo?,
    isCheckingVersion: Boolean,
    versionError: String?,
    onCheckForUpdate: () -> Unit,
    onSignOut: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            ScreenHeader("设置", "内部设备配置", onBack)
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                InfoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("当前版本", fontWeight = FontWeight.Bold)
                            Text("${BuildConfig.VERSION_NAME} · code ${BuildConfig.VERSION_CODE}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ModeChip(isMock)
                    }
                }
                InfoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("版本更新", fontWeight = FontWeight.Bold)
                            Text(
                                versionInfo?.let { "最新 ${it.latestVersionName} · code ${it.latestVersionCode}" } ?: "检查内部 APK 更新地址",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (isCheckingVersion) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), strokeWidth = 2.dp)
                    } else {
                        PrimaryAction("检查新版本", onClick = onCheckForUpdate, modifier = Modifier.fillMaxWidth())
                    }
                    if (versionInfo != null && versionInfo.latestVersionCode > BuildConfig.VERSION_CODE) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(versionInfo.apkUrl)))
                            },
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(if (versionInfo.forceUpdate) "立即下载更新" else "打开下载地址")
                        }
                    }
                    if (!versionError.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        InlineError(versionError)
                    }
                }
                InfoCard {
                    Text("安全与隐私", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "答题卡照片先保存在应用私有目录，上传确认后仍保留短周期缓存；未上传照片不会被自动清理。日志不记录密码、Token 或完整二维码内容。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onSignOut, modifier = Modifier.align(Alignment.End)) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("退出登录")
                }
            }
        }
    }
}
