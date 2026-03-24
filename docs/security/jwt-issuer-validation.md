# JWT issuer validation

## Service

`ccd-data-store-api`

## Summary

This change re-enables issuer validation in `ccd-data-store-api` so JWTs must match `oidc.issuer` as well as pass timestamp checks.

## Context

- `src/main/java/uk/gov/hmcts/ccd/SecurityConfiguration.java` builds the decoder from `spring.security.oauth2.client.provider.oidc.issuer-uri`.
- The service separately configures `oidc.issuer` because the discovered issuer is not the value trusted for validation.
- The previous implementation instantiated `JwtIssuerValidator(issuerOverride)` but only applied `JwtTimestampValidator`, which meant an unexpected `iss` claim could still be accepted if signature and timestamps were valid.

## Implemented fix

`SecurityConfiguration.jwtDecoder()` now uses:

```java
OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
OAuth2TokenValidator<Jwt> withIssuer = new JwtIssuerValidator(issuerOverride);
OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withTimestamp, withIssuer);
```

## Tests

`src/test/java/uk/gov/hmcts/ccd/SecurityConfigurationTest.java` covers:

- accepted token from the configured issuer
- rejected token from an unexpected issuer
- rejected expired token from the configured issuer

The test fixtures use valid JWT timelines so failures reflect validator behavior rather than builder constraints.

`src/test/java/uk/gov/hmcts/ccd/integrations/JwtIssuerValidationIT.java` adds full-stack coverage for a signed JWT whose `iss` claim does not match the configured issuer. This test requires the normal integration-test runtime dependencies for the repo.

Coverage is intentionally two-layered here: validator-only behavior in `SecurityConfigurationTest` and full integration wiring in `JwtIssuerValidationIT`. A lighter Spring web-security slice test was not kept because it introduced unwanted test-context complexity in this repo.

## Configuration and deployment note

This is not only a code change. Runtime configuration must still be correct:

- `spring.security.oauth2.client.provider.oidc.issuer-uri` is used for OIDC discovery and JWKS lookup.
- `oidc.issuer` is the issuer value enforced during JWT validation.
- In this repo those map to `IDAM_OIDC_URL` and `OIDC_ISSUER` in Helm values.

Before rollout, confirm:

- each environment supplies the intended `OIDC_ISSUER`
- the `iss` claim in real caller tokens matches `OIDC_ISSUER`
- no pipeline or release-time override is supplying an older issuer value
- external callers, smoke tests, and AAT clients obtain tokens whose `iss` claim matches this service's configured `OIDC_ISSUER`

Do not infer `OIDC_ISSUER` from the public OIDC discovery URL. In preview/AAT for this repo, the correct
`OIDC_ISSUER` had to be taken from decoded real tokens and did not match the public `IDAM_OIDC_URL` base.

Smoke and functional pipeline runs now perform a pre-check that acquires a real test token and fails fast if its
`iss` claim does not match `OIDC_ISSUER`.
This verifier is enabled in CI via `VERIFY_OIDC_ISSUER=true` and remains opt-in for local runs.
Because the verifier runs in the build JVM before deployed app env is available, issuer changes may need updating in
both Jenkins test env and Helm app config.

If external services still send tokens with a different issuer, this change will reject them with `401` until configuration or token issuance is aligned.

For local running, `IDAM_OIDC_URL` should point to the local OIDC discovery base, usually `http://localhost:5000`, and `OIDC_ISSUER` must exactly match the `iss` claim in the local access tokens being used. Common local values are `OIDC_ISSUER=http://fr-am:8080/openam/oauth2/hmcts` or `OIDC_ISSUER=http://localhost:5000/o`, depending on how the local token source is configured.

## How to derive `OIDC_ISSUER`

- Do not guess the issuer from the public discovery URL alone.
- Decode only the JWT payload from a real access token for the target environment and inspect the `iss` claim.
- Do not store or document full bearer tokens. Record only the derived issuer value.

Example:

```bash
TOKEN='eyJ...'
PAYLOAD=$(printf '%s' "$TOKEN" | cut -d '.' -f2)
python3 - <<'PY' "$PAYLOAD"
import base64, json, sys
payload = sys.argv[1]
payload += '=' * (-len(payload) % 4)
print(json.loads(base64.urlsafe_b64decode(payload))["iss"])
PY
```

- JWTs are `header.payload.signature`.
- The second segment is base64url-encoded JSON.
- This decodes the payload only. It does not verify the signature.

## Optional future variant

Only switch to multi-issuer validation if production tokens genuinely need both values during migration. In that case, use an explicit allow-list for issuer values rather than dropping issuer validation.
