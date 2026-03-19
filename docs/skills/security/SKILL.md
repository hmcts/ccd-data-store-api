---
name: security
description: Use when working in the HMCTS `ccd-data-store-api` repository on authentication, JWT issuer validation, Spring Security configuration, IDAM/OIDC integration, or related regression testing. This skill is for resuming in-flight security patches, checking local diffs, validating issuer and decoder behavior, and running focused Gradle tests before and after code changes.
---

# Security

## Overview

Use this skill for security changes in `ccd-data-store-api`, especially around JWT validation, IDAM issuer settings, Spring Security wiring, and narrowly scoped regression tests.

Read `docs/security/jwt-issuer-validation.md` first for the repo-specific JWT behavior, config split, and rollout notes.

## Workflow

1. Check current state with `git status --short` and inspect local diffs before editing.
2. Review `SecurityConfiguration` together with issuer-related properties and verify which issuer value is meant for validation.
3. Search for `issuer`, `JwtDecoder`, `JwtIssuerValidator`, `JwtTimestampValidator`, `oidc.issuer`, `IDAM_OIDC_URL`, and `OIDC_ISSUER` before changing behavior.
4. Treat OIDC discovery and issuer validation as separate concerns:
   - `IDAM_OIDC_URL` is for discovery and JWKS lookup.
   - `OIDC_ISSUER` is the exact enforced `iss` claim.
5. Do not infer `OIDC_ISSUER` from the public OIDC URL. Decode a real token and compare its `iss` to the configured issuer before changing preview/AAT/pipeline values.
6. For JWT coverage in this repo, prefer the existing two-layer approach:
   - validator-level checks in `SecurityConfigurationTest`
   - integration wiring checks in `JwtIssuerValidationIT`
   Do not reintroduce a Boot-based lightweight security slice test unless you are sure it will not interfere with the wider Spring test context.
7. If changing smoke or functional JWT behavior, also check the pipeline-side verifier:
   - `verifyFunctionalTestJwtIssuer` runs before smoke/functional tests
   - it runs in the Jenkins/test JVM, not in the deployed app container
   - Jenkins must therefore export `OIDC_ISSUER` as well as the Helm chart
8. Start verification with the narrowest relevant Gradle test, usually `./gradlew test --tests uk.gov.hmcts.ccd.SecurityConfigurationTest`.
9. Preserve any in-flight local work and continue from the existing patch state instead of recreating it.
