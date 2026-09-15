package com.classroomassessment.scanner.core.scan

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.classroomassessment.scanner.ScannerApplication
import com.classroomassessment.scanner.core.db.ScanEntity
import com.classroomassessment.scanner.core.network.RecognitionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.File
import java.io.IOException

class UploadWorker(
    appContext: android.content.Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    private val container get() = (applicationContext as ScannerApplication).container

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val localCaptureId = inputData.getString(KEY_LOCAL_CAPTURE_ID) ?: return@withContext Result.failure()
        val current = container.database.scanDao().findById(localCaptureId) ?: return@withContext Result.failure()
        val currentStatus = statusOf(current)
        if (currentStatus.isTerminal()) return@withContext Result.success()
        if (currentStatus == ScanStatus.PROCESSING && !current.serverScanId.isNullOrBlank()) {
            container.uploadScheduler.enqueueStatusRefresh(localCaptureId, current.serverScanId, 300L)
            return@withContext Result.success()
        }

        val image = File(current.imagePath)
        if (!image.isFile) {
            markFailed(current, "本地照片不存在，请重新扫描")
            return@withContext Result.failure(workDataOf(KEY_ERROR to "image_missing"))
        }
        val scanSessionId = current.scanSessionId
        if (scanSessionId.isNullOrBlank()) {
            markFailed(current, "扫描会话已失效，请重新进入测试后重试")
            return@withContext Result.failure(workDataOf(KEY_ERROR to "scan_session_missing"))
        }

        container.database.scanDao().update(current.copy(uploadStatus = ScanStatus.UPLOADING.name, lastError = null))
        return@withContext try {
            val result = container.backend.uploadScan(
                sessionId = current.sessionId,
                scanSessionId = scanSessionId,
                pageToken = current.pageToken,
                clientCaptureId = current.localCaptureId,
                image = image,
            )
            val accepted = current.copy(
                uploadStatus = ScanStatus.UPLOADED.name,
                serverScanId = result.serverScanId,
                requestId = result.requestId,
                studentName = result.studentName,
                studentCode = result.studentCode,
                lastError = null,
            )
            container.database.scanDao().update(accepted)
            when (result.status) {
                RecognitionStatus.PROCESSING -> {
                    container.database.scanDao().update(accepted.copy(uploadStatus = ScanStatus.PROCESSING.name))
                    container.uploadScheduler.enqueueStatusRefresh(localCaptureId, result.serverScanId)
                }
                RecognitionStatus.SUCCESS -> container.database.scanDao().update(accepted.copy(uploadStatus = ScanStatus.SUCCESS.name))
                RecognitionStatus.REVIEW_REQUIRED -> container.database.scanDao().update(accepted.copy(uploadStatus = ScanStatus.REVIEW_REQUIRED.name))
                RecognitionStatus.FAILED -> markFailed(accepted, "服务器未能处理这张答题卡")
            }
            Result.success()
        } catch (error: Throwable) {
            val attempt = current.retryCount + 1
            val message = friendlyMessage(error)
            val retryable = error is IOException || error is HttpException && isRetryable(error.code())
            if (retryable && runAttemptCount < MAX_ATTEMPTS) {
                container.database.scanDao().update(
                    current.copy(
                        uploadStatus = ScanStatus.QUEUED.name,
                        retryCount = attempt,
                        lastError = message,
                    ),
                )
                Result.retry()
            } else {
                markFailed(current.copy(retryCount = attempt), message)
                Result.failure(workDataOf(KEY_ERROR to message))
            }
        }
    }

    private suspend fun markFailed(scan: ScanEntity, message: String) {
        container.database.scanDao().update(scan.copy(uploadStatus = ScanStatus.FAILED.name, lastError = message))
    }

    private fun statusOf(scan: ScanEntity): ScanStatus = runCatching { ScanStatus.valueOf(scan.uploadStatus) }
        .getOrDefault(ScanStatus.FAILED)

    private fun friendlyMessage(error: Throwable): String = when (error) {
        is HttpException -> when {
            error.code() == 401 -> "登录已失效，请重新登录"
            error.code() == 403 -> "当前账号没有提交权限"
            error.code() in 408..499 -> "服务器未接受这张答题卡"
            else -> "服务器暂不可用，稍后会自动重试"
        }
        is IOException -> "网络不可用，已保留在本地队列"
        else -> "上传失败，已保留在本地队列"
    }

    private fun isRetryable(code: Int): Boolean = code == 408 || code == 429 || code >= 500

    companion object {
        const val KEY_LOCAL_CAPTURE_ID = "local_capture_id"
        const val KEY_ERROR = "error"
        const val TAG = "scan-upload"
        private const val MAX_ATTEMPTS = 5
    }
}
