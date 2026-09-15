# Android Scanner MVP Test Plan

## Technical checks only

- `ScanStatusTest`: pending/terminal state classification.
- `RecognitionStatusTest`: case-insensitive mapping and fail-closed unknown status.
- Kotlin compilation and Room schema migration check for the official Backend adapter.
- `:app:assembleDebug`: debug APK and bundled QR model packaging.
- `:app:testDebugUnitTest`: JVM unit suite.

## Real-device checks

The following are for the human operator's real-device acceptance; an emulator is not evidence for camera behavior.

1. Grant camera permission, open CameraX preview, and confirm the preview remains the main visual surface.
2. Present a QR containing an opaque page token. Confirm the QR is restricted to `QR_CODE`, stabilizes before enabling capture, and never displays a full token.
3. Tap capture. Confirm `ImageCapture` writes a private JPEG first, inserts a Room row, queues WorkManager, and immediately permits the next capture.
4. Log in with the same organization/teacher account as Web, create five captures while offline, and confirm all remain in the local queue. Re-enable network and confirm automatic upload and status refresh.
5. Force-stop and relaunch with three pending captures. Confirm Room rows and private image files remain and WorkManager recovery continues.
6. Submit the same `client_capture_id` twice through the real Backend test environment and confirm one `server_scan_id`.
7. Have Recognition/Backend return `REVIEW_REQUIRED` and confirm the scanner state is rendered as “待复核”.

## Evidence classification

`PASS` is reserved for an executed check with captured evidence. Real Backend account checks are `PENDING_REAL_ACCOUNT` until the versioned contract and credentials are supplied. Physical QR framing and camera capture are `PENDING_REAL_DEVICE` until executed on a device with a visible QR target.
