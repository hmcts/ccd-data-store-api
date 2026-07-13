---
name: security
description: Use when working in the HMCTS `ccd-data-store-api` repository on Spring Security configuration, auth filters, IDAM/OIDC integration, or related regression testing. This skill is for resuming in-flight security patches, checking local diffs, and running focused Gradle tests before and after code changes.
---

# Security

## Overview

Use this skill for broader security changes in `ccd-data-store-api`, especially around Spring Security wiring,
auth filters, IDAM/OIDC integration, and narrowly scoped regression tests.

For JWT issuer validation, issuer mismatch diagnosis, token `iss` checks, and pipeline issuer verification,
use `docs/skills/security-jwt-issuer/SKILL.md`.

## Workflow

1. Check current state with `git status --short` and inspect local diffs before editing.
2. Review `SecurityConfiguration` together with the auth filters and related properties before changing behavior.
3. Search for relevant security components before editing, for example `SecurityFilterChain`, `ServiceAuthFilter`,
   `ExceptionHandlingFilter`, `SecurityLoggingFilter`, and IDAM/OIDC config.
4. Keep JWT issuer-specific changes out of this path unless they are tightly coupled. For issuer work, switch to
   `docs/skills/security-jwt-issuer/SKILL.md`.
5. Start verification with the narrowest relevant tests for the touched area.
6. Preserve any in-flight local work and continue from the existing patch state instead of recreating it.
