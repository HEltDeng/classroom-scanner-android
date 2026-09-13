package com.classroomassessment.scanner.core.network

import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class MockBackendDataSourceTest {
    @Test
    fun `retrying the same client capture is idempotent`() = runBlocking {
        val source = MockBackendDataSource()
        val image = File.createTempFile("scanner-test-", ".jpg")
        image.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
        try {
            val first = source.uploadScan("session-1", "PAGE-001", "capture-123", image)
            val retry = source.uploadScan("session-1", "PAGE-001", "capture-123", image)

            assertEquals(first.serverScanId, retry.serverScanId)
            assertEquals(first.requestId, retry.requestId)
            assertEquals(RecognitionStatus.PROCESSING.name, first.status)
            assertEquals(RecognitionStatus.SUCCESS.name, source.getScanStatus(first.serverScanId).status)
        } finally {
            image.delete()
        }
    }

    @Test
    fun `review response is surfaced without local recognition logic`() = runBlocking {
        val source = MockBackendDataSource()
        val image = File.createTempFile("scanner-test-", ".jpg")
        image.writeBytes(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte()))
        try {
            val upload = source.uploadScan("session-1", "PAGE-REVIEW", "capture-review", image)
            val status = source.getScanStatus(upload.serverScanId)

            assertEquals(RecognitionStatus.REVIEW_REQUIRED.name, status.status)
            assertEquals("赵六", status.studentName)
            assertNotEquals(RecognitionStatus.SUCCESS.name, status.status)
        } finally {
            image.delete()
        }
    }
}
