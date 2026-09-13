package com.classroomassessment.scanner.core.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(scan: ScanEntity)

    @Update
    suspend fun update(scan: ScanEntity)

    @Query("SELECT * FROM scan_queue WHERE local_capture_id = :localCaptureId LIMIT 1")
    suspend fun findById(localCaptureId: String): ScanEntity?

    @Query("SELECT * FROM scan_queue WHERE session_id = :sessionId ORDER BY captured_at DESC")
    fun observeForSession(sessionId: String): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scan_queue ORDER BY captured_at DESC")
    fun observeAll(): Flow<List<ScanEntity>>

    @Query(
        """
        SELECT local_capture_id FROM scan_queue
        WHERE upload_status IN ('LOCAL', 'QUEUED', 'UPLOADING')
        ORDER BY captured_at ASC
        """,
    )
    suspend fun findPendingUploadIds(): List<String>

    @Query("SELECT * FROM scan_queue WHERE upload_status = 'PROCESSING' ORDER BY captured_at ASC")
    suspend fun findProcessingScans(): List<ScanEntity>

    @Query("DELETE FROM scan_queue WHERE local_capture_id = :localCaptureId")
    suspend fun deleteById(localCaptureId: String)
}
