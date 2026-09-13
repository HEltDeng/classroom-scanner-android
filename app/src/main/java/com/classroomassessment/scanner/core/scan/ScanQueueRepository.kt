package com.classroomassessment.scanner.core.scan

import com.classroomassessment.scanner.core.db.ScanDao
import com.classroomassessment.scanner.core.db.ScanEntity
import kotlinx.coroutines.flow.Flow

class ScanQueueRepository(
    private val dao: ScanDao,
    private val scheduler: UploadScheduler,
    private val fileStore: CaptureFileStore,
) {
    fun newCapturePlan(): CapturePlan = fileStore.newPlan()

    fun observeForSession(sessionId: String): Flow<List<ScanEntity>> = dao.observeForSession(sessionId)

    fun observeAll(): Flow<List<ScanEntity>> = dao.observeAll()

    suspend fun enqueueCapture(sessionId: String, pageToken: String, plan: CapturePlan): ScanEntity {
        val local = ScanEntity(
            localCaptureId = plan.localCaptureId,
            sessionId = sessionId,
            pageToken = pageToken,
            imagePath = plan.file.absolutePath,
            capturedAt = System.currentTimeMillis(),
            uploadStatus = ScanStatus.LOCAL.name,
            retryCount = 0,
        )
        dao.insert(local)
        val queued = local.copy(uploadStatus = ScanStatus.QUEUED.name)
        dao.update(queued)
        scheduler.enqueueUpload(local.localCaptureId)
        return queued
    }

    suspend fun retry(localCaptureId: String) {
        val current = dao.findById(localCaptureId) ?: return
        dao.update(current.copy(uploadStatus = ScanStatus.QUEUED.name, lastError = null))
        scheduler.enqueueUpload(localCaptureId)
    }
}
