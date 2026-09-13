package com.classroomassessment.scanner.core.network

import java.io.File

interface AuthApi {
    suspend fun login(username: String, password: String): LoginResponseDto
}

interface OrganizationApi {
    suspend fun getOrganizations(): List<OrganizationDto>
}

interface ClassApi {
    suspend fun getClasses(organizationId: String): List<ClassroomDto>
}

interface SessionApi {
    suspend fun getActiveSessions(classId: String): List<AssessmentSessionDto>
}

interface ScanApi {
    suspend fun uploadScan(
        sessionId: String,
        pageToken: String,
        clientCaptureId: String,
        image: File,
    ): ScanUploadResponseDto

    suspend fun getScanStatus(serverScanId: String): ScanStatusResponseDto
}

interface VersionApi {
    suspend fun getLatestVersion(): VersionResponseDto
}

interface BackendDataSource : AuthApi, OrganizationApi, ClassApi, SessionApi, ScanApi, VersionApi
