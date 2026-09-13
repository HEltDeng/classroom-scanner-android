package com.classroomassessment.scanner.core.network

import kotlinx.coroutines.delay
import java.io.IOException
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Deterministic local provider for UI, queue, and WorkManager development. */
class MockBackendDataSource(
    private val canReachBackend: () -> Boolean = { true },
) : BackendDataSource {
    private val captures = ConcurrentHashMap<String, MockCapture>()

    override suspend fun login(username: String, password: String): LoginResponseDto {
        delay(120)
        if (username.isBlank() || password.isBlank()) error("请输入账号和密码")
        return LoginResponseDto(
            accessToken = "mock-access-token",
            refreshToken = "mock-refresh-token",
            user = UserDto(
                id = "mock-user-001",
                displayName = "演示助教",
                email = username,
            ),
        )
    }

    override suspend fun getOrganizations(): List<OrganizationDto> = listOf(
        OrganizationDto("mock-org-001", "星河教育中心"),
    )

    override suspend fun getClasses(organizationId: String): List<ClassroomDto> = listOf(
        ClassroomDto("mock-class-001", organizationId, "初一 A 班"),
        ClassroomDto("mock-class-002", organizationId, "周六上午班"),
    )

    override suspend fun getActiveSessions(classId: String): List<AssessmentSessionDto> = listOf(
        AssessmentSessionDto(
            id = "mock-session-001",
            classId = classId,
            name = "阅读 A 篇",
            status = "ACTIVE",
            questionCount = 20,
            submittedCount = captures.values.count { it.sessionId == "mock-session-001" && it.status == RecognitionStatus.SUCCESS },
            participantCount = 25,
        ),
        AssessmentSessionDto(
            id = "mock-session-002",
            classId = classId,
            name = "词汇小测",
            status = "ACTIVE",
            questionCount = 15,
            submittedCount = 18,
            participantCount = 25,
        ),
    )

    override suspend fun uploadScan(
        sessionId: String,
        pageToken: String,
        clientCaptureId: String,
        image: File,
    ): ScanUploadResponseDto {
        if (!canReachBackend()) throw IOException("Mock backend is offline")
        delay(220)
        check(image.exists()) { "本地照片不存在" }
        val existing = captures[clientCaptureId]
        if (existing != null) return existing.uploadResponse

        val response = ScanUploadResponseDto(
            serverScanId = "mock-scan-${clientCaptureId.take(8)}",
            status = "PROCESSING",
            requestId = "mock-request-${UUID.randomUUID().toString().take(8)}",
            studentName = mockStudent(pageToken).first,
            studentCode = mockStudent(pageToken).second,
        )
        captures[clientCaptureId] = MockCapture(sessionId, pageToken, RecognitionStatus.PROCESSING, response)
        return response
    }

    override suspend fun getScanStatus(serverScanId: String): ScanStatusResponseDto {
        if (!canReachBackend()) throw IOException("Mock backend is offline")
        delay(140)
        val capture = captures.values.firstOrNull { it.uploadResponse.serverScanId == serverScanId }
            ?: error("演示扫描不存在")
        val status = if (capture.pageToken.contains("REVIEW", ignoreCase = true)) {
            RecognitionStatus.REVIEW_REQUIRED
        } else {
            RecognitionStatus.SUCCESS
        }
        capture.status = status
        return ScanStatusResponseDto(
            serverScanId = serverScanId,
            status = status.name,
            requestId = capture.uploadResponse.requestId,
            studentName = capture.uploadResponse.studentName,
            studentCode = capture.uploadResponse.studentCode,
        )
    }

    override suspend fun getLatestVersion(): VersionResponseDto = VersionResponseDto(
        latestVersionCode = 1,
        latestVersionName = "0.1.0",
        apkUrl = "https://example.invalid/classroom-scanner-android-0.1.0.apk",
        forceUpdate = false,
        releaseNotes = "首个内部扫描器版本：CameraX、QR、本地队列和可靠上传。",
        sha256 = "DEMO_ONLY",
    )

    private fun mockStudent(pageToken: String): Pair<String, String> = when {
        pageToken.contains("002") -> "李四" to "YZ024"
        pageToken.contains("003") -> "王五" to "YZ025"
        pageToken.contains("REVIEW", ignoreCase = true) -> "赵六" to "YZ026"
        else -> "张三" to "YZ023"
    }

    private data class MockCapture(
        val sessionId: String,
        val pageToken: String,
        var status: RecognitionStatus,
        val uploadResponse: ScanUploadResponseDto,
    )
}
