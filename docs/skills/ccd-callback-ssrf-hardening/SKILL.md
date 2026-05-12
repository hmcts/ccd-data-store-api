---
name: ccd-callback-ssrf-hardening
description: Detect and remediate SSRF and credential leakage in CCD callback flows for hmcts/ccd-data-store-api and related HMCTS services. Use when working on callback handlers, URL ingestion/validation, WebhookEntity parsing, RestTemplate callback POSTs, or security header forwarding (ServiceAuthorization, Authorization, user-id, user-roles) during case creation/state-change events.
---

# CCD Callback SSRF Hardening

## Overview

Use this skill to harden CCD event callbacks against SSRF and token leakage.
Apply a consistent workflow: find callback URL sources, block untrusted targets, stop forwarding sensitive headers, and enforce regression tests.

## Workflow

1. Identify callback entry points and URL sources.
2. Confirm whether untrusted callback URLs can be stored/imported.
3. Inspect callback HTTP invocation for sensitive header forwarding.
4. Implement URL validation and outbound destination restrictions.
5. Minimize callback credentials (do not pass through user JWT/context headers).
6. Add or update tests for allowlist/denylist and header behavior.

## Hotspots In This Repository

Start with these files:

- `src/main/java/uk/gov/hmcts/ccd/domain/service/callbacks/CallbackService.java`
- `src/main/java/uk/gov/hmcts/ccd/data/SecurityUtils.java`
- Definition import/parsing code that persists callback URLs (for example webhook/event definition parser classes)

Load [references/callback-hotspots.md](references/callback-hotspots.md) for targeted checks and expected secure behavior.

## Required Secure Outcomes

- Reject callback URLs that are not in approved domains/hosts.
- Reject non-HTTPS callback URLs except explicitly approved local/test environments.
- Block SSRF targets (localhost, loopback, link-local, private CIDRs, metadata endpoints).
- Remove pass-through of end-user credentials/context to callbacks by default.
- Use least-privilege callback authentication model (service-only or dedicated callback token).
- Log blocked callbacks with safe redaction and enough metadata for incident triage.

## Implementation Guidance

### URL Validation

- Parse with `URI`/`URL` defensively and fail closed on parse errors.
- Validate scheme, host, and optional port.
- Enforce configured allowlist from application configuration.
- Resolve DNS as needed and reject private/internal address ranges.

### Header Policy

- Do not call `putAll(securityHeaders)` into callback request headers.
- Explicitly construct allowed outbound headers.
- Never forward `Authorization`, `ServiceAuthorization`, `user-id`, or `user-roles` unless explicitly required and approved.

### Testing

Add tests for:

- Allowed trusted callback URL succeeds.
- Disallowed domain is rejected.
- SSRF targets are rejected.
- Callback request excludes sensitive inbound/user headers.
- Regression test for previously vulnerable callback path.

## Quick Commands

Use the bundled scanner to find risky patterns quickly:

```bash
bash skills/ccd-callback-ssrf-hardening/scripts/scan_callback_risks.sh
```

Then inspect matches and patch code accordingly.

## Deliverable Checklist

- URL validation implemented in callback URL ingestion and/or invocation path.
- Sensitive header forwarding removed or strictly allowlisted.
- Configuration introduced for trusted callback destinations.
- Unit/integration tests cover validation and header policy.
- Security notes added to PR describing threat model and mitigations.
