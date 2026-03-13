# CCD-7110 Server side request forgery urls

## What changed
- Strengthened callback allowlist preflight checks in `build.gradle`:
  - Validate required callback hosts across all three env vars:
    - `CCD_CALLBACK_ALLOWED_HOSTS`
    - `CCD_CALLBACK_ALLOWED_HTTP_HOSTS`
    - `CCD_CALLBACK_ALLOW_PRIVATE_HOSTS`
  - Required hosts now include:
    - `ccd-test-stubs-service-aat.service.core-compute-aat.internal`
    - `aac-manage-case-assignment-aat.service.core-compute-aat.internal`
  - Improved failure output to show exact missing hosts per env var and remediation guidance.
- Added AAC host in callback allowlists for pipeline and helm values:
  - `Jenkinsfile_CNP`
  - `Jenkinsfile_nightly`
  - `charts/ccd-data-store-api/values.yaml`
  - `charts/ccd-data-store-api/values.preview.template.yaml`
  - `charts/ccd-data-store-api/values.aat.template.yaml`
- Callback service hardening/cleanup in `CallbackService.java`:
  - Null-safe `Client-Context` header rewrite (prevents edge-case NPE).
  - Generic typing cleanup for `HttpEntity`/`ResponseEntity` (Sonar/type-safety).
- Docs alignment:
  - `README.md`
  - `docs/api/security.md`

## Why
- F-051 failures were traced to callback allowlist drift (`aac-manage-case-assignment-aat...` not allowlisted), not auth failure.
- Deterministic fail-fast diagnostics and consistent env wiring are needed to prevent pipeline/runtime drift.
- `CallbackService` fix removes a defensive gap and addresses maintainability warnings without behavior change.

## Impact
- No intended behavior change for successful paths.
- Better preflight error messages and earlier failure when required hosts are missing.
- Prevents potential callback-path NPE under misconfiguration.

## Testing
- `./gradlew -q verifyBefTaStubHostConfigConsistency` passed.
- `./gradlew testUnit --tests '*CallbackServiceTest' --tests '*CallbackUrlValidatorTest'` passed.
- Manual preflight checks verified:
  - unset allowlists -> skip strict check
  - missing AAC host -> clear drift failure message with exact missing vars/hosts

## Risk / rollback
- Low risk (config + validation + defensive null check).
- Rollback: revert this PR; previous behavior restored.
