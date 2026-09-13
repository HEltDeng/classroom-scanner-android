package com.classroomassessment.scanner.core.network

import org.junit.Assert.assertEquals
import org.junit.Test

class RecognitionStatusTest {
    @Test
    fun `unknown backend status fails closed`() {
        assertEquals(RecognitionStatus.FAILED, RecognitionStatus.fromApi("future_status"))
    }

    @Test
    fun `backend status names are case insensitive`() {
        assertEquals(RecognitionStatus.REVIEW_REQUIRED, RecognitionStatus.fromApi("review_required"))
    }
}
