package com.classroomassessment.scanner.core.scan

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.classroomassessment.scanner.core.db.ScanDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class UploadScheduler(context: Context) {
    private val workManager = WorkManager.getInstance(context)
    private val recoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun enqueueUpload(localCaptureId: String) {
        val request = OneTimeWorkRequestBuilder<UploadWorker>()
            .setInputData(workDataOf(UploadWorker.KEY_LOCAL_CAPTURE_ID to localCaptureId))
            .setConstraints(connectedConstraint())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag(UploadWorker.TAG)
            .build()
        workManager.enqueueUniqueWork(uploadName(localCaptureId), ExistingWorkPolicy.KEEP, request)
    }

    fun enqueueStatusRefresh(localCaptureId: String, serverScanId: String, delayMillis: Long = 1_000L) {
        val request = OneTimeWorkRequestBuilder<StatusRefreshWorker>()
            .setInputData(
                workDataOf(
                    StatusRefreshWorker.KEY_LOCAL_CAPTURE_ID to localCaptureId,
                    StatusRefreshWorker.KEY_SERVER_SCAN_ID to serverScanId,
                ),
            )
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setConstraints(connectedConstraint())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag(StatusRefreshWorker.TAG)
            .build()
        workManager.enqueueUniqueWork(statusName(localCaptureId), ExistingWorkPolicy.KEEP, request)
    }

    fun recoverPendingWork(dao: ScanDao) {
        recoveryScope.launch {
            dao.findPendingUploadIds().forEach(::enqueueUpload)
            dao.findProcessingScans().forEach { scan ->
                scan.serverScanId?.let { enqueueStatusRefresh(scan.localCaptureId, it, 500L) }
            }
        }
    }

    private fun connectedConstraint() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    private fun uploadName(id: String) = "scan-upload-$id"

    private fun statusName(id: String) = "scan-status-$id"
}
