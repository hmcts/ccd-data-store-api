---
name: security
description: Use when working in the HMCTS `ccd-data-store-api` repository on authentication, JWT issuer validation, Spring Security configuration, IDAM/OIDC integration, or related regression testing. This skill is for resuming in-flight security patches, checking local diffs, validating issuer and decoder behavior, and running focused Gradle tests before and after code changes.
---

# Security

## Overview

Use this skill for security changes in `ccd-data-store-api`, especially around JWT validation, IDAM issuer settings, and narrowly scoped regression tests.

## Workflow

1. Check current state with `git status --short` and inspect local diffs before editing.
2. Review `SecurityConfiguration` together with issuer-related properties and verify which issuer value is meant for validation.
3. Search for `issuer`, `JwtDecoder`, `JwtIssuerValidator`, `JwtTimestampValidator`, and `oidc.issuer` before changing behavior.
4. Start verification with the narrowest relevant Gradle test, usually `./gradlew test --tests uk.gov.hmcts.ccd.SecurityConfigurationTest`.
5. Preserve any in-flight local work and continue from the existing patch state instead of recreating it.
