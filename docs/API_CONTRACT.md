# Android Scanner API Contract

Status: provisional mobile contract. Replace endpoint details only after the Main Backend publishes its versioned OpenAPI contract. The Android app must not read the Web database or the Recognition Service database.

Base URL is supplied at build time with `CLASSROOM_SCANNER_API_BASE_URL`. The current source fallback is a deliberately non-routable placeholder, so a real Backend build must provide an explicit base URL.

## Authentication

`POST /api/v1/auth/login`

Request:

```json
{"username":"teacher-or-assistant","password":"..."}
```

Response:

```json
{
  "access_token":"...",
  "refresh_token":"...",
  "user":{"id":"user-id","display_name":"助教","email":"assistant@example.org"}
}
```

The app stores access and refresh tokens encrypted with Android Keystore-backed AES/GCM. Tokens, passwords, and full QR values must not be logged. A 401 is presented as “登录已失效，请重新登录”.

## Organization, class, and session discovery

`GET /api/v1/organizations`

```json
[{"id":"org-id","name":"机构名称"}]
```

`GET /api/v1/organizations/{organization_id}/classes`

```json
[{"id":"class-id","organization_id":"org-id","name":"初一 A 班"}]
```

`GET /api/v1/classes/{class_id}/assessment-sessions?status=ACTIVE`

```json
[
  {
    "id":"session-id",
    "class_id":"class-id",
    "name":"阅读 A 篇",
    "status":"ACTIVE",
    "question_count":20,
    "submitted_count":18,
    "participant_count":25
  }
]
```

The client filters session status defensively and only opens an `ACTIVE` session.

## Scan upload

`POST /api/v1/scans` with `multipart/form-data`:

| Part/header | Required | Meaning |
| --- | --- | --- |
| `session_id` | yes | Selected active assessment session |
| `page_token` | yes | Opaque QR page token; not a student name or phone number |
| `client_capture_id` | yes | UUID generated once when the image is captured |
| `image` | yes | High-quality `image/jpeg` from CameraX `ImageCapture` |
| `Idempotency-Key` | yes | Exactly the same value as `client_capture_id` |

The Backend must deduplicate by `(organization/account scope, client_capture_id)` and return the original result for retries. A network retry must never generate a second server scan.

Initial response:

```json
{
  "server_scan_id":"scan-id",
  "status":"PROCESSING",
  "request_id":"request-id",
  "student_name":"张三",
  "student_code":"YZ023"
}
```

`student_name` and `student_code` are optional server-confirmed display fields. The QR token itself is not treated as identity proof by Android.

## Recognition status

`GET /api/v1/scans/{server_scan_id}` returns the same response shape and one of:

```text
PROCESSING
SUCCESS
REVIEW_REQUIRED
FAILED
```

Android only renders and persists these states. OMR, OCR, answer scoring, and identity trust decisions remain server responsibilities.

## Version check

`GET /api/v1/app/version?platform=android&channel=internal`

```json
{
  "latest_version_code":1,
  "latest_version_name":"0.1.0",
  "apk_url":"https://download.example.org/classroom-scanner.apk",
  "force_update":false,
  "release_notes":"...",
  "sha256":"..."
}
```

V1 only displays update information and opens the APK URL. In-app download, SHA-256 verification, and Package Installer integration remain later work.

## Client state and local queue

Room persists `local_capture_id`, `session_id`, opaque `page_token`, `image_path`, `captured_at`, `upload_status`, `retry_count`, `server_scan_id`, and request/display metadata. The state sequence is:

```text
LOCAL → QUEUED → UPLOADING → UPLOADED → PROCESSING → SUCCESS
                                             └──────→ REVIEW_REQUIRED
                                             └──────→ FAILED
```

`FAILED` is retryable from the UI. Missing local images are never silently discarded.
