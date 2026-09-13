package com.classroomassessment.scanner.feature.scanner

import android.content.Context
import android.os.SystemClock
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.classroomassessment.scanner.core.scan.CapturePlan
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ScannerCameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val onQrDetected: (String) -> Unit,
    private val onFrameQuality: (String?) -> Unit,
    private val onCameraReady: (Boolean, String?) -> Unit,
) {
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val processingFrame = AtomicBoolean(false)
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var barcodeScanner: BarcodeScanner? = null
    private var lastQualityReportAt = 0L

    fun bind(previewView: PreviewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                runCatching {
                    val provider = providerFuture.get()
                    cameraProvider = provider
                    val preview = Preview.Builder()
                        .setTargetRotation(previewView.display?.rotation ?: android.view.Surface.ROTATION_0)
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                        .setJpegQuality(92)
                        .setTargetRotation(previewView.display?.rotation ?: android.view.Surface.ROTATION_0)
                        .build()
                    barcodeScanner = BarcodeScanning.getClient(
                        BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build(),
                    )
                    val analysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .build()
                    analysis.setAnalyzer(analysisExecutor, ::analyze)
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture,
                        analysis,
                    )
                    onCameraReady(true, null)
                }.onFailure { error ->
                    onCameraReady(false, "无法启动相机，请检查权限或设备相机状态")
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun capture(
        plan: CapturePlan,
        onSaved: (CapturePlan) -> Unit,
        onError: (Throwable) -> Unit,
    ) {
        val capture = imageCapture
        if (capture == null) {
            onError(IllegalStateException("Camera is not ready"))
            return
        }
        val options = ImageCapture.OutputFileOptions.Builder(plan.file).build()
        capture.takePicture(
            options,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onSaved(plan)
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            },
        )
    }

    fun close() {
        cameraProvider?.unbindAll()
        barcodeScanner?.close()
        barcodeScanner = null
        analysisExecutor.shutdown()
    }

    private fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        val scanner = barcodeScanner
        if (mediaImage == null || scanner == null || !processingFrame.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val now = SystemClock.elapsedRealtime()
        if (now - lastQualityReportAt >= QUALITY_REPORT_INTERVAL_MS) {
            lastQualityReportAt = now
            onFrameQuality(assessFrameQuality(imageProxy))
        }
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull { it.format == Barcode.FORMAT_QR_CODE }
                    ?.rawValue
                    ?.takeIf { it.isNotBlank() }
                    ?.let(onQrDetected)
            }
            .addOnCompleteListener {
                processingFrame.set(false)
                imageProxy.close()
            }
    }

    private fun assessFrameQuality(imageProxy: ImageProxy): String? {
        if (minOf(imageProxy.width, imageProxy.height) < MIN_ANALYSIS_DIMENSION) {
            return "画面分辨率偏低，请靠近答题卡"
        }
        val lumaPlane = imageProxy.planes.firstOrNull() ?: return null
        val buffer = lumaPlane.buffer
        val rowStep = maxOf(1, imageProxy.height / LUMA_SAMPLE_ROWS)
        val columnStep = maxOf(1, imageProxy.width / LUMA_SAMPLE_COLUMNS)
        var total = 0L
        var count = 0
        for (row in 0 until imageProxy.height step rowStep) {
            val rowOffset = row * lumaPlane.rowStride
            for (column in 0 until imageProxy.width step columnStep) {
                val offset = rowOffset + column * lumaPlane.pixelStride
                if (offset < buffer.limit()) {
                    total += buffer.get(offset).toInt() and 0xFF
                    count++
                }
            }
        }
        if (count == 0) return null
        val averageLuma = total / count
        return when {
            averageLuma < DARK_LUMA_THRESHOLD -> "画面偏暗，请补充光线"
            averageLuma > BRIGHT_LUMA_THRESHOLD -> "画面偏亮，请避开反光"
            else -> null
        }
    }

    companion object {
        private const val QUALITY_REPORT_INTERVAL_MS = 500L
        private const val MIN_ANALYSIS_DIMENSION = 480
        private const val LUMA_SAMPLE_ROWS = 12
        private const val LUMA_SAMPLE_COLUMNS = 16
        private const val DARK_LUMA_THRESHOLD = 25L
        private const val BRIGHT_LUMA_THRESHOLD = 235L
    }
}
