# Scanner 0.3.0 / code 6

Session discovery no longer sends `status=ACTIVE`. The backend returns scannable ACTIVE and CLOSED assessments, and the repository retains both. DRAFT and ARCHIVED remain unavailable. The scan-session and upload APIs are unchanged, so the signed opaque A5 page token is passed through without Android interpreting student identity or recognition templates.

The scanner remains a capture/upload client. OMR, OCR, page merging, scoring and review remain server responsibilities.

Validation: debug unit tests and debug/release builds, production API base URL, original scanner signing certificate. This new build still requires real-device acceptance with printed A5 fronts/backs; earlier 0.2.3 phone acceptance does not establish 0.3.0 acceptance. No official GitHub Release is implied by the local signed package.
