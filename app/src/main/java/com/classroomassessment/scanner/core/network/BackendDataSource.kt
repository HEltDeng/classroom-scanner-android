package com.classroomassessment.scanner.core.network

import java.io.File

interface AuthApi {
    suspend fun login(organizationName: String, teacherLoginName: String, password: String): LoginResponseDto
    suspend fun getMe(): MeResponseDto
}

interface ClassApi {
    suspend fun getClasses(): List<ClassroomDto>
}

interface SessionApi {
    suspend fun getActiveSessions(classId: String): List<AssessmentSessionDto>
    suspend fun createOrGetScannerSession(sessionId: String): ScannerSessionDto
}

interface ScanApi {
    suspend fun uploadScan(
        sessionId: String,
        scanSessionId: String,
        pageToken: String,
        clientCaptureId: String,
        image: File,
    ): ScanUploadResponseDto

    suspend fun getScanStatus(serverScanId: String): ScanStatusResponseDto
}

interface VersionApi {
    suspend fun getLatestVersion(): VersionResponseDto
}

interface BackendDataSource : AuthApi, ClassApi, SessionApi, ScanApi, VersionApi
