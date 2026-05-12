# Callback SSRF Hotspots (ccd-data-store-api)

## Primary Paths

- `src/main/java/uk/gov/hmcts/ccd/domain/service/callbacks/CallbackService.java`
  - Look for callback POST execution (`RestTemplate.exchange(...)`) and outbound header assembly.
  - Flag any `httpHeaders.putAll(securityHeaders)` or equivalent broad header copy.

- `src/main/java/uk/gov/hmcts/ccd/domain/service/callbacks/CallbackUrlValidator.java`
  - Keep URL policy centralised (allowlist/scheme/private-network/credentials checks + URL redaction).

- `src/main/java/uk/gov/hmcts/ccd/data/definition/DefaultCaseDefinitionRepository.java`
  - Confirm callback URL validation runs while retrieving/ingesting case definitions, not only at callback execution time.

- `src/main/java/uk/gov/hmcts/ccd/data/SecurityUtils.java`
  - Confirm which security headers are exposed by `authorizationHeaders()`.
  - Treat `ServiceAuthorization`, `Authorization`, `user-id`, and `user-roles` as sensitive.

- Definition parsing/persistence for callback URL fields (e.g., webhook parser/entity mapping)
  - Ensure callback URL normalization + validation exists at import time.
  - Reject blank/invalid URLs and untrusted hosts.

## Risk Pattern To Eliminate

1. Callback URL comes from case definition input and is trusted without validation.
2. Callback invocation forwards inbound auth/user headers to external URL.
3. Attacker controls URL and receives service + user credentials.

## Secure Design Pattern

1. Validate URL at import and before invocation (defense in depth).
2. Restrict callback destination via allowlist config.
3. Build outbound headers from explicit allowlist; default deny sensitive headers.
4. Prefer dedicated callback auth over user token propagation.

## Suggested Validation Rules

- Scheme: `https` by default; `http` only for explicitly approved hosts (`CCD_CALLBACK_ALLOWED_HTTP_HOSTS`).
- Host allowlist: exact domains, controlled subdomain rules, and/or regex patterns (`CCD_CALLBACK_ALLOWED_HOSTS`).
- DNS/IP checks: reject loopback, private, link-local, multicast, and metadata service ranges unless explicitly approved (`CCD_CALLBACK_ALLOW_PRIVATE_HOSTS`).

## Recommended Next Controls (Not Yet Enforced Here)

- Port restrictions: allow only expected ports.
- Redirect policy: do not follow redirects to untrusted hosts.

## Test Matrix

- Valid allowlisted HTTPS host -> accepted.
- Non-allowlisted host -> rejected.
- `http://` URL for non-approved host -> rejected.
- Localhost/127.0.0.1/::1/private CIDR host -> rejected.
- Callback request headers do not include `Authorization`, `ServiceAuthorization`, `user-id`, `user-roles` unless explicitly approved.
