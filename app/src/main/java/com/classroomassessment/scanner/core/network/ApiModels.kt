package com.classroomassessment.scanner.core.network

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
)

data class LoginResponseDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String? = null,
    @SerializedName("user") val user: UserDto,
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

data class ClassroomDto(
    @SerializedName("id") val id: String,
    @SerializedName("organization_id") val organizationId: String,
    @SerializedName("name") val name: String,
)

data class AssessmentSessionDto(
    @SerializedName("id") val id: String,
    @SerializedName("class_id") val classId: String,
    @SerializedName("name") val name: String,
    @SerializedName("status") val status: String,
    @SerializedName("question_count") val questionCount: Int,
    @SerializedName("submitted_count") val submittedCount: Int,
    @SerializedName("participant_count") val participantCount: Int,
)

data class ScanUploadResponseDto(
    @SerializedName("server_scan_id") val serverScanId: String,
    @SerializedName("status") val status: String,
    @SerializedName("request_id") val requestId: String? = null,
    @SerializedName("student_name") val studentName: String? = null,
    @SerializedName("student_code") val studentCode: String? = null,
)

data class ScanStatusResponseDto(
    @SerializedName("server_scan_id") val serverScanId: String,
    @SerializedName("status") val status: String,
    @SerializedName("request_id") val requestId: String? = null,
    @SerializedName("student_name") val studentName: String? = null,
    @SerializedName("student_code") val studentCode: String? = null,
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
)

data class Organization(
    val id: String,
    val name: String,
)

data class Classroom(
    val id: String,
    val organizationId: String,
    val name: String,
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
    val status: RecognitionStatus,
    val requestId: String?,
    val studentName: String?,
    val studentCode: String?,
)

data class VersionInfo(
    val latestVersionCode: Int,
    val latestVersionName: String,
    val apkUrl: String,
    val forceUpdate: Boolean,
    val releaseNotes: String,
    val sha256: String,
)
