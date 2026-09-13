package com.classroomassessment.scanner.feature.scanner

import android.content.Context
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
    private val onCameraReady: (Boolean, String?) -> Unit,
) {
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val processingFrame = AtomicBoolean(false)
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var barcodeScanner: BarcodeScanner? = null

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
}
