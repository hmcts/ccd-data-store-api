# CCD-7877 Hardcoded Credentials

## Objective

Remove credential-looking values from Data Store API test evidence while preserving the negative-authentication test intent.

## Acceptance criteria

- The S-284 fixture contains no real or reusable username/password.
- Authentication used by AAT is supplied through the established environment/secret mechanism.
- No live validity, deployment, reuse, or rotation status is inferred from repository contents.

## Findings and changes

- The screenshot evidence maps to `src/aat/resources/features/F-056 - Submit Event Citizen V1 External/S-284.td.json`; the same invalid test credentials were also duplicated in S-134.
- Centralised the deliberately non-routable values in `src/aat/resources/features/common/users/InvalidUser.td.json`, and made both scenarios extend that fixture. They remain negative-test data, not runtime credentials. Their historical validity, reuse, and deployment cannot be established from this checkout.
- The screenshot’s additional `pip-account-management` path was not found in this checkout and is outside this repository’s direct contents; it requires separate owner/history verification.
- Standalone Docker now uses `scripts/setup-local-secrets.sh` to create an ignored `.env`; Compose consumes it with `--env-file .env`.

## Validation and status

Run the focused AAT scenario for S-284. Repository inspection confirms the fixture location and change, but cannot establish whether the former values were valid, deployed, reused, or rotated.

## Recommendations

Confirm the corresponding test account and any historical value with the service owner, rotate if it was ever valid, and verify CI variables and test secret stores.
