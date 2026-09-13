package com.classroomassessment.scanner.core.scan

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanStatusTest {
    @Test
    fun `upload states remain pending until backend accepts the image`() {
        assertTrue(ScanStatus.LOCAL.isPendingUpload())
        assertTrue(ScanStatus.QUEUED.isPendingUpload())
        assertTrue(ScanStatus.UPLOADING.isPendingUpload())
        assertFalse(ScanStatus.PROCESSING.isPendingUpload())
        assertFalse(ScanStatus.SUCCESS.isPendingUpload())
    }

    @Test
    fun `only final recognition states are terminal`() {
        assertTrue(ScanStatus.SUCCESS.isTerminal())
        assertTrue(ScanStatus.REVIEW_REQUIRED.isTerminal())
        assertFalse(ScanStatus.PROCESSING.isTerminal())
        assertFalse(ScanStatus.FAILED.isTerminal())
    }
}
