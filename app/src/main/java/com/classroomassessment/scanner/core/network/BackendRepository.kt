package com.classroomassessment.scanner.core.network

import java.io.File

class BackendRepository(
    private val dataSource: BackendDataSource,
    private val tokenStore: TokenStore,
) {
    suspend fun login(organizationName: String, teacherLoginName: String, password: String): Account {
        val response = dataSource.login(organizationName, teacherLoginName, password)
        val account = response.toDomain()
        tokenStore.save(response.accessToken, response.refreshToken, account)
        return account
    }

    suspend fun currentAccount(): Account {
        val account = dataSource.getMe().toDomain()
        tokenStore.saveIdentity(account)
        return account
    }

    fun signOut() = tokenStore.clear()

    fun hasSession(): Boolean = !tokenStore.accessToken().isNullOrBlank()

    suspend fun classes(): List<Classroom> = dataSource.getClasses().map {
        Classroom(it.id, it.organizationId, it.name, it.status)
    }

    suspend fun activeSessions(classId: String): List<AssessmentSession> = dataSource.getActiveSessions(classId)
        .filter { it.status.equals("ACTIVE", ignoreCase = true) }
        .map {
            AssessmentSession(
                id = it.id,
                classId = it.classId,
                name = it.name?.takeIf(String::isNotBlank) ?: "未命名测试",
                status = it.status,
                questionCount = it.questionCount,
                submittedCount = it.submittedCount,
                participantCount = it.participantCount,
            )
        }

    suspend fun createOrGetScannerSession(sessionId: String): ScannerSession =
        dataSource.createOrGetScannerSession(sessionId).toDomain()

    suspend fun uploadScan(
        sessionId: String,
        scanSessionId: String,
        pageToken: String,
        clientCaptureId: String,
        image: File,
    ): UploadResult = dataSource.uploadScan(sessionId, scanSessionId, pageToken, clientCaptureId, image).toDomain()

    suspend fun scanStatus(serverScanId: String): UploadResult = dataSource.getScanStatus(serverScanId).toDomain()

    suspend fun latestVersion(): VersionInfo = dataSource.getLatestVersion().let {
        VersionInfo(
            latestVersionCode = it.latestVersionCode,
            latestVersionName = it.latestVersionName,
            apkUrl = it.apkUrl,
            forceUpdate = it.forceUpdate,
            releaseNotes = it.releaseNotes,
            sha256 = it.sha256,
        )
    }

    private fun LoginResponseDto.toDomain(): Account = Account(
        id = user.id,
        displayName = user.displayName,
        email = user.email,
        organizationId = requireNotNull(organizationId) { "登录响应缺少机构信息" },
        organizationName = requireNotNull(organizationName) { "登录响应缺少机构名称" },
        teacherId = requireNotNull(teacherId) { "登录响应缺少教师信息" },
        teacherName = teacherName ?: user.displayName,
        loginName = requireNotNull(loginName) { "登录响应缺少登录名称" },
        role = role ?: "TEACHER",
    )

    private fun MeResponseDto.toDomain() = Account(
        id = id,
        displayName = displayName,
        email = email,
        organizationId = organizationId,
        organizationName = organization.name,
        teacherId = teacherId,
        teacherName = displayName,
        loginName = loginName,
        role = role,
    )

    private fun ScannerSessionDto.toDomain() = ScannerSession(
        id = id,
        organizationId = organizationId,
        assessmentSessionId = assessmentSessionId,
        status = status,
    )

    private fun ScanUploadResponseDto.toDomain() = UploadResult(
        serverScanId = serverScanId,
        sessionId = sessionId,
        scanSessionId = scanSessionId,
        classId = classId,
        clientCaptureId = clientCaptureId,
        status = RecognitionStatus.fromApi(status),
        requestId = requestId,
        studentName = studentName,
        studentCode = studentCode,
        pageType = pageType,
    )

    private fun ScanStatusResponseDto.toDomain() = UploadResult(
        serverScanId = serverScanId,
        sessionId = sessionId,
        scanSessionId = scanSessionId,
        classId = classId,
        clientCaptureId = clientCaptureId,
        status = RecognitionStatus.fromApi(status),
        requestId = requestId,
        studentName = studentName,
        studentCode = studentCode,
        pageType = pageType,
    )
}
