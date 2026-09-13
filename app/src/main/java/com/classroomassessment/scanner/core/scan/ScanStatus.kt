package com.classroomassessment.scanner.core.scan

enum class ScanStatus {
    LOCAL,
    QUEUED,
    UPLOADING,
    UPLOADED,
    PROCESSING,
    SUCCESS,
    REVIEW_REQUIRED,
    FAILED,
}

fun ScanStatus.isTerminal(): Boolean = this == ScanStatus.SUCCESS || this == ScanStatus.REVIEW_REQUIRED

fun ScanStatus.isPendingUpload(): Boolean = this == ScanStatus.LOCAL || this == ScanStatus.QUEUED || this == ScanStatus.UPLOADING
