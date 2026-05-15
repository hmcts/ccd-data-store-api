---
name: security-jwt-issuer
description: Use for JWT issuer validation, issuer mismatch diagnosis, token iss checks, and pipeline verifier updates in ccd-data-store-api.
---

# Security JWT Issuer

Use this skill when working specifically on JWT issuer validation in `ccd-data-store-api`.

Read `docs/security/jwt-issuer-validation.md` first for the detailed behavior, config, and rollout guidance.

## Workflow

1. Check current diffs with `git status --short` before editing.
2. Review `SecurityConfiguration` and confirm how `IDAM_OIDC_URL` and `OIDC_ISSUER` are used.
3. For code changes, check:
   - `src/main/java/uk/gov/hmcts/ccd/SecurityConfiguration.java`
   - `src/test/java/uk/gov/hmcts/ccd/SecurityConfigurationTest.java`
   - `src/test/java/uk/gov/hmcts/ccd/integrations/JwtIssuerValidationIT.java`
4. For pipeline/test-run alignment, check:
   - `src/aat/java/uk/gov/hmcts/ccd/datastore/befta/JwtIssuerVerificationApp.java`
   - `build.gradle`
   - `Jenkinsfile_CNP`
   - `Jenkinsfile_nightly`
5. For issuer values, token `iss` diagnosis, CI verifier behavior, and Helm vs Jenkins env alignment, follow `docs/security/jwt-issuer-validation.md` rather than duplicating that guidance here.
6. Start verification with the narrowest useful test:
   - `./gradlew test --tests uk.gov.hmcts.ccd.SecurityConfigurationTest`
7. Preserve in-flight local work and continue from the existing patch state rather than recreating it.
