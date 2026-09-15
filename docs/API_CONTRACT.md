# Android Scanner API Contract

This contract is implemented by the Main Backend in the companion Web/Backend repository. Android calls these APIs only; it never reads the Web database or the Recognition Service database.

The base URL is supplied at build time with `CLASSROOM_SCANNER_API_BASE_URL`. The source fallback is deliberately non-routable and must not be used for a real installation.

## Authentication and tenant context

`POST /api/v1/auth/login`

Request:

```json
{"organization_name":"机构名称","teacher_login_name":"teacher-login","password":"..."}
```

Response:

```json
{
  "access_token":"...",
  "token_type":"bearer",
  "user":{"id":"user-id","display_name":"王老师","email":"internal-account@example.org"},
  "organization_id":"org-id",
  "organization_name":"机构名称",
  "teacher_id":"membership-id",
  "teacher_name":"王老师",
  "login_name":"teacher-login",
  "role":"TEACHER"
}
```

The account is the same organization membership used by Web. The signed token fixes `organization_id` and `teacher_id`; Android stores the token and returned identity in an Android Keystore-backed AES/GCM session store. Tokens, passwords, and full QR values must not be logged. A 401 is presented as “登录已失效，请重新登录”.

`GET /api/v1/me` returns the current user, organization, membership `teacher_id`, `login_name`, and `role`. Android uses it to restore a session after process restart; it never invents a local account.

## Class and test discovery

`GET /api/v1/classes`

```json
[
  {"id":"class-id","organization_id":"org-id","name":"初一 A 班","status":"ACTIVE"}
]
```

The response is restricted by the signed organization and the teacher's permitted classes. Android displays active classes only.

`GET /api/v1/classes/{class_id}/sessions?status=ACTIVE`

```json
[
  {
    "id":"session-id",
    "organization_id":"org-id",
    "class_id":"class-id",
    "class_name":"初一 A 班",
    "name":"进门测",
    "status":"ACTIVE",
    "question_count":20,
    "submitted_count":18,
    "participant_count":25
  }
]
```

The test name and counts come from the Web-created `AssessmentSession`. Android filters the status defensively and never creates a test locally.

## Scanner session

`POST /api/v1/sessions/{session_id}/scan-session`

The Backend creates or returns the scanner session for the signed teacher and selected Web test. It is an operational record, not another test model.

```json
{
  "id":"scanner-session-id",
  "organization_id":"org-id",
  "assessment_session_id":"session-id",
  "opened_by":"user-id",
  "opened_by_member_id":"membership-id",
  "status":"OPEN"
}
```

## Scan upload

`POST /api/v1/scans` with `multipart/form-data`:

| Part/header | Required | Meaning |
| --- | --- | --- |
| `session_id` | yes | Selected active Web assessment session |
| `scan_session_id` | yes | Scanner session opened by the current teacher |
| `page_token` | yes | Opaque QR page token; not a student name or phone number |
| `client_capture_id` | yes | UUID generated once when the image is captured |
| `image` | yes | High-quality `image/jpeg` from CameraX `ImageCapture` |
| `Idempotency-Key` | yes | Exactly the same value as `client_capture_id` |
| `X-Request-ID` | optional | Client request correlation ID; Backend returns it in the receipt |

The Backend deduplicates by `(organization_id, client_capture_id)` and returns the original receipt for retries. A network retry must never generate a second server scan.

Initial response:

```json
{
  "server_scan_id":"scan-id",
  "organization_id":"org-id",
  "session_id":"session-id",
  "scan_session_id":"scanner-session-id",
  "class_id":"class-id",
  "client_capture_id":"capture-uuid",
  "status":"PROCESSING",
  "request_id":"request-id",
  "student_id":null,
  "student_name":null,
  "student_code":null,
  "page_type":"ANSWER_SHEET",
  "image_sha256":"...",
  "image_size":123456,
  "uploaded_by":"user-id",
  "uploaded_by_member_id":"membership-id"
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

Android only renders and persists these states. OMR, OCR, answer scoring, and identity trust decisions remain server responsibilities. The Recognition Service updates the scan through the Backend contract; Android never calls a Recognition SDK.

## Version check

`GET /api/v1/app/version?platform=android&channel=internal`

```json
{
  "latest_version_code":2,
  "latest_version_name":"0.2.0",
  "apk_url":"https://download.example.org/classroom-scanner.apk",
  "force_update":false,
  "release_notes":"...",
  "sha256":"..."
}
```

V1 only displays update information and opens the APK URL. In-app download, SHA-256 verification, and Package Installer integration remain later work.

## Client state and local queue

Room persists `local_capture_id`, `session_id`, `scan_session_id`, opaque `page_token`, `image_path`, `captured_at`, `upload_status`, `retry_count`, `server_scan_id`, and request/display metadata. The state sequence is:

```text
LOCAL → QUEUED → UPLOADING → UPLOADED → PROCESSING → SUCCESS
                                             └──────→ REVIEW_REQUIRED
                                             └──────→ FAILED
```

`FAILED` is retryable from the local queue. Missing local images are never silently discarded, and unuploaded captures are not automatically deleted.
