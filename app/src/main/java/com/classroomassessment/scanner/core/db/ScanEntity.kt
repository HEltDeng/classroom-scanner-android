package com.classroomassessment.scanner.core.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scan_queue",
    indices = [
        Index(value = ["session_id", "captured_at"]),
        Index(value = ["upload_status"]),
        Index(value = ["server_scan_id"]),
    ],
)
data class ScanEntity(
    @PrimaryKey
    @ColumnInfo(name = "local_capture_id")
    val localCaptureId: String,
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "page_token")
    val pageToken: String,
    @ColumnInfo(name = "image_path")
    val imagePath: String,
    @ColumnInfo(name = "captured_at")
    val capturedAt: Long,
    @ColumnInfo(name = "upload_status")
    val uploadStatus: String,
    @ColumnInfo(name = "retry_count")
    val retryCount: Int,
    @ColumnInfo(name = "server_scan_id")
    val serverScanId: String? = null,
    @ColumnInfo(name = "student_name")
    val studentName: String? = null,
    @ColumnInfo(name = "student_code")
    val studentCode: String? = null,
    @ColumnInfo(name = "request_id")
    val requestId: String? = null,
    @ColumnInfo(name = "last_error")
    val lastError: String? = null,
)
