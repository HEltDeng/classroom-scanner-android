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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PhotoCamera
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.classroomassessment.scanner.core.network.AssessmentSession
import com.classroomassessment.scanner.core.network.ScannerSession
import com.classroomassessment.scanner.core.scan.ScanQueueRepository
import com.classroomassessment.scanner.core.scan.ScanStatus
import com.classroomassessment.scanner.core.scan.isPendingUpload
import com.classroomassessment.scanner.ui.displayToken
import kotlinx.coroutines.launch

@Composable
fun ScannerScreen(
    session: AssessmentSession,
    scannerSession: ScannerSession,
    classroomName: String,
    queueRepository: ScanQueueRepository,
    onBack: () -> Unit,
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
    var frameQualityMessage by remember { mutableStateOf<String?>(null) }
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
    val failedCount = parsedStatuses.count { it == ScanStatus.FAILED }
    val frameColor = if (stableFrames >= 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
                onFrameQuality = { frameQualityMessage = it },
                onControllerReady = { cameraController = it },
            )
        } else {
            PermissionPanel(onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) })
        }

        if (hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .systemBarsPadding()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.93f))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(classroomName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text(session.name, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 16.dp, top = 78.dp, bottom = 194.dp)
                    .border(3.dp, frameColor, RoundedCornerShape(22.dp)),
            ) {
                Text(
                    text = if (detectedToken == null) "将答题卡放入画面" else "页面已识别，画面稳定后拍摄",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.97f))
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = when {
                                cameraError != null -> cameraError!!
                                isCapturing -> "正在保存高质量照片…"
                                frameQualityMessage != null -> frameQualityMessage!!
                                detectedToken == null -> "将答题卡放入画面"
                                stableFrames < 2 -> "页面正在稳定…"
                                else -> "可以拍摄"
                            },
                            color = if (cameraError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (detectedToken != null) {
                            Text(
                                "页面 ${displayToken(detectedToken!!)}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
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
                                        runCatching {
                                            queueRepository.enqueueCapture(session.id, scannerSession.id, pageToken, savedPlan)
                                        }.onSuccess {
                                            feedback = "已加入上传队列，准备下一张"
                                            detectedToken = null
                                            stableFrames = 0
                                        }.onFailure {
                                            feedback = "本地队列保存失败，请重试"
                                        }
                                        isCapturing = false
                                    }
                                },
                                onError = {
                                    plan.file.delete()
                                    feedback = "拍摄失败，请保持设备稳定后重试"
                                    isCapturing = false
                                },
                            )
                        },
                        enabled = cameraReady && stableFrames >= 2 && !isCapturing,
                        modifier = Modifier.size(72.dp),
                        shape = RoundedCornerShape(36.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(26.dp),
                                strokeWidth = 3.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(Icons.Rounded.PhotoCamera, contentDescription = "拍摄", modifier = Modifier.size(30.dp))
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryItem("已扫描", queue.size.toString(), Modifier.weight(1f))
                    SummaryItem("待上传", pendingCount.toString(), Modifier.weight(1f))
                    SummaryItem("处理中", processingCount.toString(), Modifier.weight(1f))
                    SummaryItem("异常", reviewCount.toString(), Modifier.weight(1f))
                }
                if (failedCount > 0) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                queue.filter { it.uploadStatus == ScanStatus.FAILED.name }
                                    .forEach { queueRepository.retry(it.localCaptureId) }
                                feedback = "已重新加入上传队列"
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text("上传失败 $failedCount 张，点击重试")
                    }
                }
                if (feedback != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(feedback!!, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun CameraSurface(
    lifecycleOwner: LifecycleOwner,
    onQrDetected: (String) -> Unit,
    onFrameQuality: (String?) -> Unit,
    onCameraState: (Boolean, String?) -> Unit,
    onControllerReady: (ScannerCameraController) -> Unit,
) {
    val context = LocalContext.current
    val latestQr by rememberUpdatedState(onQrDetected)
    val latestFrameQuality by rememberUpdatedState(onFrameQuality)
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
            onFrameQuality = { latestFrameQuality(it) },
            onCameraReady = { ready, error -> latestCameraState(ready, error) },
        )
    }
    DisposableEffect(controller) {
        controller.bind(previewView)
        onControllerReady(controller)
        onDispose { controller.close() }
    }
    AndroidView(
        factory = { previewView },
        modifier = Modifier.fillMaxSize().padding(top = 76.dp, bottom = 190.dp),
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
            Text("扫描器只使用相机识别页面并拍摄答题卡，不申请其他无关权限。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onRequest) { Text("开启相机") }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
    }
}
