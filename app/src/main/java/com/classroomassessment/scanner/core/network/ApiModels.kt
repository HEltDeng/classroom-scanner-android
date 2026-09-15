package com.classroomassessment.scanner.core.network

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("organization_name") val organizationName: String,
    @SerializedName("teacher_login_name") val teacherLoginName: String,
    @SerializedName("password") val password: String,
)

data class LoginResponseDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("user") val user: UserDto,
    @SerializedName("organization_id") val organizationId: String? = null,
    @SerializedName("organization_name") val organizationName: String? = null,
    @SerializedName("teacher_id") val teacherId: String? = null,
    @SerializedName("teacher_name") val teacherName: String? = null,
    @SerializedName("login_name") val loginName: String? = null,
    @SerializedName("role") val role: String? = null,
)

data class UserDto(
    @SerializedName("id") val id: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("email") val email: String,
)

data class OrganizationDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
)

data class MeResponseDto(
    @SerializedName("id") val id: String,
    @SerializedName("email") val email: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("organization_id") val organizationId: String,
    @SerializedName("teacher_id") val teacherId: String,
    @SerializedName("login_name") val loginName: String,
    @SerializedName("role") val role: String,
    @SerializedName("organization") val organization: OrganizationDto,
)

data class ClassroomDto(
    @SerializedName("id") val id: String,
    @SerializedName("organization_id") val organizationId: String,
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String = "ACTIVE",
)

data class AssessmentSessionDto(
    @SerializedName("id") val id: String,
    @SerializedName("class_id") val classId: String,
    @SerializedName("class_name") val className: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("question_count") val questionCount: Int = 0,
    @SerializedName("submitted_count") val submittedCount: Int = 0,
    @SerializedName("participant_count") val participantCount: Int = 0,
)

data class ScannerSessionDto(
    @SerializedName("id") val id: String,
    @SerializedName("organization_id") val organizationId: String,
    @SerializedName("assessment_session_id") val assessmentSessionId: String,
    @SerializedName("opened_by") val openedBy: String,
    @SerializedName("opened_by_member_id") val openedByMemberId: String,
    @SerializedName("status") val status: String,
)

data class ScanUploadResponseDto(
    @SerializedName("server_scan_id") val serverScanId: String,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("scan_session_id") val scanSessionId: String? = null,
    @SerializedName("class_id") val classId: String? = null,
    @SerializedName("client_capture_id") val clientCaptureId: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("request_id") val requestId: String? = null,
    @SerializedName("student_name") val studentName: String? = null,
    @SerializedName("student_code") val studentCode: String? = null,
    @SerializedName("page_type") val pageType: String? = null,
)

data class ScanStatusResponseDto(
    @SerializedName("server_scan_id") val serverScanId: String,
    @SerializedName("session_id") val sessionId: String? = null,
    @SerializedName("scan_session_id") val scanSessionId: String? = null,
    @SerializedName("class_id") val classId: String? = null,
    @SerializedName("client_capture_id") val clientCaptureId: String? = null,
    @SerializedName("status") val status: String,
    @SerializedName("request_id") val requestId: String? = null,
    @SerializedName("student_name") val studentName: String? = null,
    @SerializedName("student_code") val studentCode: String? = null,
    @SerializedName("page_type") val pageType: String? = null,
)

data class VersionResponseDto(
    @SerializedName("latest_version_code") val latestVersionCode: Int,
    @SerializedName("latest_version_name") val latestVersionName: String,
    @SerializedName("apk_url") val apkUrl: String,
    @SerializedName("force_update") val forceUpdate: Boolean,
    @SerializedName("release_notes") val releaseNotes: String,
    @SerializedName("sha256") val sha256: String,
)

data class Account(
    val id: String,
    val displayName: String,
    val email: String,
    val organizationId: String,
    val organizationName: String,
    val teacherId: String,
    val teacherName: String,
    val loginName: String,
    val role: String,
)

data class Organization(
    val id: String,
    val name: String,
)

data class Classroom(
    val id: String,
    val organizationId: String,
    val name: String,
    val status: String,
)

data class AssessmentSession(
    val id: String,
    val classId: String,
    val name: String,
    val status: String,
    val questionCount: Int,
    val submittedCount: Int,
    val participantCount: Int,
)

data class ScannerSession(
    val id: String,
    val organizationId: String,
    val assessmentSessionId: String,
    val status: String,
)

enum class RecognitionStatus {
    PROCESSING,
    SUCCESS,
    REVIEW_REQUIRED,
    FAILED;

    companion object {
        fun fromApi(value: String): RecognitionStatus = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
            ?: RecognitionStatus.FAILED
    }
}

data class UploadResult(
    val serverScanId: String,
    val sessionId: String? = null,
    val scanSessionId: String? = null,
    val classId: String? = null,
    val clientCaptureId: String? = null,
    val status: RecognitionStatus,
    val requestId: String?,
    val studentName: String?,
    val studentCode: String?,
    val pageType: String? = null,
)

data class VersionInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val apkUrl: String,
    val forceUpdate: Boolean,
    val releaseNotes: String,
    val sha256: String,
)
