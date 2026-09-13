package com.classroomassessment.scanner.core.network

import java.io.File

class BackendRepository(
    private val dataSource: BackendDataSource,
    private val tokenStore: TokenStore,
) {
    suspend fun login(username: String, password: String): Account {
        val response = dataSource.login(username, password)
        tokenStore.save(response.accessToken, response.refreshToken)
        return response.user.toDomain()
    }

    fun signOut() = tokenStore.clear()

    fun hasSession(): Boolean = !tokenStore.accessToken().isNullOrBlank()

    suspend fun organizations(): List<Organization> = dataSource.getOrganizations().map { Organization(it.id, it.name) }

    suspend fun classes(organizationId: String): List<Classroom> = dataSource.getClasses(organizationId).map {
        Classroom(it.id, it.organizationId, it.name)
    }

    suspend fun activeSessions(classId: String): List<AssessmentSession> = dataSource.getActiveSessions(classId)
        .filter { it.status.equals("ACTIVE", ignoreCase = true) }
        .map {
            AssessmentSession(
                id = it.id,
                classId = it.classId,
                name = it.name,
                status = it.status,
                questionCount = it.questionCount,
                submittedCount = it.submittedCount,
                participantCount = it.participantCount,
            )
        }

    suspend fun uploadScan(
        sessionId: String,
        pageToken: String,
        clientCaptureId: String,
        image: File,
    ): UploadResult = dataSource.uploadScan(sessionId, pageToken, clientCaptureId, image).toDomain()

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

    private fun UserDto.toDomain() = Account(id, displayName, email)

    private fun ScanUploadResponseDto.toDomain() = UploadResult(
        serverScanId = serverScanId,
        status = RecognitionStatus.fromApi(status),
        requestId = requestId,
        studentName = studentName,
        studentCode = studentCode,
    )

    private fun ScanStatusResponseDto.toDomain() = UploadResult(
        serverScanId = serverScanId,
        status = RecognitionStatus.fromApi(status),
        requestId = requestId,
        studentName = studentName,
        studentCode = studentCode,
    )
}
