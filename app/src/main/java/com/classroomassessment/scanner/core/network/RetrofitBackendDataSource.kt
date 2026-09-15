package com.classroomassessment.scanner.core.network

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class RetrofitBackendDataSource(
    baseUrl: String,
    tokenStore: TokenStore,
) : BackendDataSource {
    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl.ensureTrailingSlash())
        .client(
            OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(tokenStore))
                .addInterceptor(RequestIdInterceptor())
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build(),
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val authService = retrofit.create(RetrofitAuthService::class.java)
    private val organizationService = retrofit.create(RetrofitOrganizationService::class.java)
    private val classService = retrofit.create(RetrofitClassService::class.java)
    private val sessionService = retrofit.create(RetrofitSessionService::class.java)
    private val scanService = retrofit.create(RetrofitScanService::class.java)
    private val versionService = retrofit.create(RetrofitVersionService::class.java)

    override suspend fun login(organizationName: String, teacherLoginName: String, password: String): LoginResponseDto =
        authService.login(LoginRequest(organizationName, teacherLoginName, password))

    override suspend fun getMe(): MeResponseDto = authService.getMe()

    override suspend fun getOrganizations(): List<OrganizationDto> = organizationService.getOrganizations()

    override suspend fun getClasses(): List<ClassroomDto> = classService.getClasses()

    override suspend fun getActiveSessions(classId: String): List<AssessmentSessionDto> =
        sessionService.getActiveSessions(classId, "ACTIVE")

    override suspend fun createOrGetScannerSession(sessionId: String): ScannerSessionDto =
        sessionService.createOrGetScannerSession(sessionId)

    override suspend fun uploadScan(
        sessionId: String,
        scanSessionId: String,
        pageToken: String,
        clientCaptureId: String,
        image: File,
    ): ScanUploadResponseDto {
        val imagePart = MultipartBody.Part.createFormData(
            "image",
            image.name,
            image.asRequestBody("image/jpeg".toMediaType()),
        )
        return scanService.uploadScan(
            sessionId = sessionId.asTextPart(),
            scanSessionId = scanSessionId.asTextPart(),
            pageToken = pageToken.asTextPart(),
            clientCaptureId = clientCaptureId.asTextPart(),
            image = imagePart,
            idempotencyKey = clientCaptureId,
        )
    }

    override suspend fun getScanStatus(serverScanId: String): ScanStatusResponseDto = scanService.getScanStatus(serverScanId)

    override suspend fun getLatestVersion(): VersionResponseDto = versionService.getLatestVersion()

    private fun String.asTextPart() = toRequestBody("text/plain".toMediaType())

    private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"
}

private class AuthInterceptor(
    private val tokenStore: TokenStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val token = tokenStore.accessToken()
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) header("Authorization", "Bearer $token")
        }.build()
        return chain.proceed(request)
    }
}

private class RequestIdInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val request: Request = chain.request().newBuilder()
            .header("X-Request-ID", UUID.randomUUID().toString())
            .build()
        return chain.proceed(request)
    }
}

private interface RetrofitAuthService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponseDto

    @GET("me")
    suspend fun getMe(): MeResponseDto
}

private interface RetrofitOrganizationService {
    @GET("organizations")
    suspend fun getOrganizations(): List<OrganizationDto>
}

private interface RetrofitClassService {
    @GET("classes")
    suspend fun getClasses(): List<ClassroomDto>
}

private interface RetrofitSessionService {
    @GET("classes/{classId}/sessions")
    suspend fun getActiveSessions(
        @Path("classId") classId: String,
        @Query("status") status: String,
    ): List<AssessmentSessionDto>

    @POST("sessions/{sessionId}/scan-session")
    suspend fun createOrGetScannerSession(@Path("sessionId") sessionId: String): ScannerSessionDto
}

private interface RetrofitScanService {
    @Multipart
    @POST("scans")
    suspend fun uploadScan(
        @Part("session_id") sessionId: okhttp3.RequestBody,
        @Part("scan_session_id") scanSessionId: okhttp3.RequestBody,
        @Part("page_token") pageToken: okhttp3.RequestBody,
        @Part("client_capture_id") clientCaptureId: okhttp3.RequestBody,
        @Part image: MultipartBody.Part,
        @Header("Idempotency-Key") idempotencyKey: String,
    ): ScanUploadResponseDto

    @GET("scans/{serverScanId}")
    suspend fun getScanStatus(@Path("serverScanId") serverScanId: String): ScanStatusResponseDto
}

private interface RetrofitVersionService {
    @GET("app/version")
    suspend fun getLatestVersion(
        @Query("platform") platform: String = "android",
        @Query("channel") channel: String = "internal",
    ): VersionResponseDto
}
