package com.classroomassessment.scanner.core.scan

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.classroomassessment.scanner.ScannerApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StatusRefreshWorker(
    appContext: android.content.Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val container get() = (applicationContext as ScannerApplication).container

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val localCaptureId = inputData.getString(KEY_LOCAL_CAPTURE_ID) ?: return@withContext Result.failure()
        val serverScanId = inputData.getString(KEY_SERVER_SCAN_ID) ?: return@withContext Result.failure()
        val current = container.database.scanDao().findById(localCaptureId) ?: return@withContext Result.failure()

        try {
            val result = container.backend.scanStatus(serverScanId)
            val status = when (result.status) {
                com.classroomassessment.scanner.core.network.RecognitionStatus.PROCESSING -> ScanStatus.PROCESSING
                com.classroomassessment.scanner.core.network.RecognitionStatus.SUCCESS -> ScanStatus.SUCCESS
                com.classroomassessment.scanner.core.network.RecognitionStatus.REVIEW_REQUIRED -> ScanStatus.REVIEW_REQUIRED
                com.classroomassessment.scanner.core.network.RecognitionStatus.FAILED -> ScanStatus.FAILED
            }
            val updated = current.copy(
                uploadStatus = status.name,
                requestId = result.requestId ?: current.requestId,
                studentName = result.studentName ?: current.studentName,
                studentCode = result.studentCode ?: current.studentCode,
                lastError = if (status == ScanStatus.FAILED) "服务器未能处理这张答题卡" else null,
            )
            container.database.scanDao().update(updated)
            if (status == ScanStatus.PROCESSING) {
                if (runAttemptCount >= MAX_ATTEMPTS) {
                    container.database.scanDao().update(updated.copy(uploadStatus = ScanStatus.FAILED.name, lastError = "服务器处理超时，请稍后重试"))
                    Result.failure()
                } else {
                    Result.retry()
                }
            } else {
                Result.success()
            }
        } catch (_: java.io.IOException) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        } catch (_: retrofit2.HttpException) {
            if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_LOCAL_CAPTURE_ID = "local_capture_id"
        const val KEY_SERVER_SCAN_ID = "server_scan_id"
        const val TAG = "scan-status"
        private const val MAX_ATTEMPTS = 10
    }
}
