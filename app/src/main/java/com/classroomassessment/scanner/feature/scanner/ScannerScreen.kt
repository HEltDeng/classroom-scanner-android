package com.classroomassessment.scanner.feature.scanner

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import com.classroomassessment.scanner.BuildConfig
import com.classroomassessment.scanner.core.network.AssessmentSession
import com.classroomassessment.scanner.core.scan.CapturePlan
import com.classroomassessment.scanner.core.scan.ScanQueueRepository
import com.classroomassessment.scanner.core.scan.ScanStatus
import com.classroomassessment.scanner.core.scan.isPendingUpload
import com.classroomassessment.scanner.ui.displayToken
import kotlinx.coroutines.launch

@Composable
fun ScannerScreen(
    session: AssessmentSession,
    classroomName: String,
    queueRepository: ScanQueueRepository,
    onBack: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val queue by queueRepository.observeForSession(session.id).collectAsState(initial = emptyList())
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasCameraPermission = granted
    }
    var cameraReady by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var cameraController by remember { mutableStateOf<ScannerCameraController?>(null) }
    var detectedToken by remember { mutableStateOf<String?>(null) }
    var stableFrames by remember { mutableIntStateOf(0) }
    var isCapturing by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(hasCameraPermission) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        }
    }

    val parsedStatuses = queue.mapNotNull { runCatching { ScanStatus.valueOf(it.uploadStatus) }.getOrNull() }
    val pendingCount = parsedStatuses.count { it.isPendingUpload() }
    val processingCount = parsedStatuses.count { it == ScanStatus.UPLOADED || it == ScanStatus.PROCESSING }
    val reviewCount = parsedStatuses.count { it == ScanStatus.REVIEW_REQUIRED || it == ScanStatus.FAILED }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraSurface(
                lifecycleOwner = lifecycleOwner,
                onQrDetected = { token ->
                    if (token == detectedToken) {
                        stableFrames = (stableFrames + 1).coerceAtMost(3)
                    } else {
                        detectedToken = token
                        stableFrames = 1
                    }
                },
                onCameraState = { ready, error ->
                    cameraReady = ready
                    cameraError = error
                },
                onControllerReady = { cameraController = it },
            )
        } else {
            PermissionPanel(onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) })
        }

        if (hasCameraPermission) {
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .systemBarsPadding()
                    .background(Color(0xCC080E1B))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(classroomName, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(session.name, color = Color(0xFFB8C3D6), style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onOpenQueue) {
                        Icon(Icons.Rounded.Queue, contentDescription = "上传队列", tint = Color.White)
                    }
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.Rounded.History, contentDescription = "扫描历史", tint = Color.White)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "设置", tint = Color.White)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 290.dp, height = 390.dp)
                    .border(2.dp, if (stableFrames >= 2) Color(0xFF54D6C7) else Color(0xFFFFB454), RoundedCornerShape(20.dp)),
            ) {
                Text(
                    text = if (detectedToken == null) "将答题卡放入扫描区域" else "二维码已识别，确认画面稳定",
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xEE080E1B))
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = when {
                                cameraError != null -> cameraError!!
                                isCapturing -> "正在保存高质量照片…"
                                detectedToken == null -> "请先让二维码进入框内"
                                stableFrames < 2 -> "页面正在稳定…"
                                else -> "可以拍摄"
                            },
                            color = if (cameraError != null) MaterialTheme.colorScheme.error else Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (detectedToken != null) {
                            Text("Token ${displayToken(detectedToken!!)}", color = Color(0xFFB8C3D6), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(
                        onClick = {
                            val pageToken = detectedToken ?: return@Button
                            val controller = cameraController ?: return@Button
                            val plan = queueRepository.newCapturePlan()
                            isCapturing = true
                            feedback = null
                            controller.capture(
                                plan = plan,
                                onSaved = { savedPlan ->
                                    scope.launch {
                                        queueRepository.enqueueCapture(session.id, pageToken, savedPlan)
                                        feedback = "已加入上传队列，准备下一张"
                                        detectedToken = null
                                        stableFrames = 0
                                        isCapturing = false
                                    }
                                },
                                onError = { error ->
                                    plan.file.delete()
                                    feedback = "拍摄失败，请保持设备稳定后重试"
                                    isCapturing = false
                                },
                            )
                        },
                        enabled = cameraReady && stableFrames >= 2 && !isCapturing,
                        modifier = Modifier.size(76.dp),
                        shape = RoundedCornerShape(38.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Rounded.PhotoCamera, contentDescription = "拍摄", modifier = Modifier.size(30.dp))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryItem("已扫描", queue.size.toString(), Modifier.weight(1f))
                    SummaryItem("待上传", pendingCount.toString(), Modifier.weight(1f))
                    SummaryItem("处理中", processingCount.toString(), Modifier.weight(1f))
                    SummaryItem("异常", reviewCount.toString(), Modifier.weight(1f))
                }
                if (feedback != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(feedback!!, color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodySmall)
                }
                if (BuildConfig.MOCK_BACKEND_ENABLED) {
                    TextButton(
                        onClick = {
                            val plan = queueRepository.newCapturePlan()
                            val index = queue.size + 1
                            val pageToken = if (index % 4 == 0) "PAGE-REVIEW" else "PAGE-${index.toString().padStart(3, '0')}"
                            scope.launch {
                                runCatching {
                                    // A tiny local JPEG marker pair is enough for the deterministic Mock provider.
                                    plan.file.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
                                    queueRepository.enqueueCapture(session.id, pageToken, plan)
                                }.onSuccess {
                                    feedback = "Mock 扫描已加入队列：$pageToken"
                                }.onFailure {
                                    plan.file.delete()
                                    feedback = "Mock 扫描加入失败"
                                }
                            }
                        },
                    ) {
                        Text("加入演示扫描（仅 Mock）")
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraSurface(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    onQrDetected: (String) -> Unit,
    onCameraState: (Boolean, String?) -> Unit,
    onControllerReady: (ScannerCameraController) -> Unit,
) {
    val context = LocalContext.current
    val latestQr by rememberUpdatedState(onQrDetected)
    val latestCameraState by rememberUpdatedState(onCameraState)
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val controller = remember {
        ScannerCameraController(
            context = context,
            lifecycleOwner = lifecycleOwner,
            onQrDetected = { latestQr(it) },
            onCameraReady = { ready, error -> latestCameraState(ready, error) },
        )
    }
    DisposableEffect(controller) {
        controller.bind(previewView)
        onControllerReady(controller)
        onDispose { controller.close() }
    }
    // Keep the platform PreviewView out of the top and bottom touch zones. A full-screen
    // AndroidView can otherwise intercept taps intended for the Compose controls layered above it.
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize().padding(top = 80.dp, bottom = 250.dp),
    )
}

@Composable
private fun PermissionPanel(onRequest: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(16.dp))
            Text("需要相机权限", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("扫描器只使用相机识别二维码并拍摄答题卡，不申请其他无关权限。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRequest) { Text("开启相机") }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFFB8C3D6), style = MaterialTheme.typography.labelSmall)
    }
}
