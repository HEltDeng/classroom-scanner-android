# Classroom Scanner Android

课堂答题卡扫描器是课堂答题与学情自动化系统的独立 Android 子项目。它只负责相机、二维码、连续拍摄、本地队列、可靠上传和扫描状态展示；不包含 OMR、OCR、成绩计算、Web Dashboard 或 Recognition SDK。

## Current baseline

- Project: `D:\Codex\classroom-scanner-android`
- Application ID: `com.classroomassessment.scanner`
- Version: `0.1.0` / versionCode `1`
- UI: Kotlin + Jetpack Compose
- Camera: CameraX Preview + ImageAnalysis + ImageCapture
- QR: bundled ML Kit Barcode Scanning, restricted to `QR_CODE`
- Persistence: Room
- Upload scheduling: WorkManager with network constraint and exponential retry
- Network: Retrofit + OkHttp + Kotlin Coroutines
- Development: debug uses deterministic Mock Backend; release uses the API adapter

The application ID is frozen once the first signed release APK is produced. The actual Backend URL is injected at build time with `CLASSROOM_SCANNER_API_BASE_URL`; the source fallback is intentionally non-routable until the Backend contract is ready.

## Build

Use the shared JDK 17 and Android SDK documented in the workspace. From this directory:

```powershell
$env:JAVA_HOME = "D:\Codex\90_Shared-公共资源\toolchains\jdk-17\jdk-17.0.19+10"
$env:ANDROID_HOME = "D:\Codex\90_Shared-公共资源\toolchains\android-sdk"
.\gradlew.bat testDebugUnitTest assembleDebug
```

The Debug APK is `app\build\outputs\apk\debug\app-debug.apk`. It uses the `.debug` application ID suffix and Mock mode.

## Signing

The project has a dedicated JKS outside the repository. The one-time identity creation script generates a new key and records only DPAPI-protected SecureString values for the current Windows user:

```powershell
.\scripts\New-ClassroomScannerSigningIdentity.ps1
```

For an already-created identity, use `Register-ClassroomScannerSigning.ps1` instead. The release build never accepts a missing signing configuration as a valid signed release:

```powershell
.\scripts\Invoke-ClassroomScannerReleaseBuild.ps1
.\scripts\Verify-ClassroomScannerRelease.ps1 -ApkPath ".\release\classroom-scanner-android-v0.1.0-code1-release.apk"
```

Do not commit the JKS, passwords, the DPAPI XML, temporary signing properties, or local scan images. A secondary protected copy on another fixed volume is not the same as a disconnected offline backup; the latter must be made and verified before broader distribution.

## Mock flow

Debug login accepts any non-empty credentials. It provides one organization, two classes, active sessions, deterministic scan results, an idempotent upload provider, and a development-only “加入演示扫描（仅 Mock）” action for queue/retry/restart verification. A token containing `REVIEW` returns `REVIEW_REQUIRED`.

## Handoff

The provisional mobile API, request/response schemas, idempotency rule, local state machine, and version endpoint are in [`docs/API_CONTRACT.md`](docs/API_CONTRACT.md). The evidence boundary and real-device cases are in [`docs/TEST_PLAN.md`](docs/TEST_PLAN.md).
