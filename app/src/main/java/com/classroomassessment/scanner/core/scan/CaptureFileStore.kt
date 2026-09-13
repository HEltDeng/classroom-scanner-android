package com.classroomassessment.scanner.core.scan

import android.content.Context
import java.io.File
import java.util.UUID

data class CapturePlan(
    val localCaptureId: String,
    val file: File,
)

class CaptureFileStore(context: Context) {
    private val imageDirectory = File(context.filesDir, "scan-images").apply { mkdirs() }

    fun newPlan(): CapturePlan {
        val id = UUID.randomUUID().toString()
        return CapturePlan(id, File(imageDirectory, "$id.jpg"))
    }
}
