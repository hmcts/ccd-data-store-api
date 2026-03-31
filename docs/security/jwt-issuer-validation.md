# JWT issuer validation

## Service

`ccd-data-store-api`

## Reference

HMCTS guidance: [JWT iss Claim Validation guidance](https://tools.hmcts.net/confluence/spaces/SISM/pages/1958056812/JWT+iss+Claim+Validation+for+OIDC+and+OAuth+2+Tokens#JWTissClaimValidationforOIDCandOAuth2Tokens-Configurationrecommendation)

## Summary

This change re-enables issuer validation in `ccd-data-store-api` so JWTs must match `oidc.issuer` as well as pass timestamp checks.
For this repo, the current implementation follows the single configured issuer approach rather than an allow-list model.
Service-level issuer decisions should be checked against the reference above and the externally agreed service issuer policy before changing this repo's JWT issuer configuration.

## At a glance

| Area | Current approach in this repo |
|---|---|
| JWT validation | Signature, timestamp, and issuer are all enforced |
| Discovery / JWKS source | `spring.security.oauth2.client.provider.oidc.issuer-uri` |
| Enforced issuer | `oidc.issuer` / `OIDC_ISSUER` |
| Issuer model | Single configured issuer, not allow-list |
| Main runtime config | `OIDC_ISSUER` must be supplied explicitly |

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

## Runtime model

| Setting | Purpose | Notes |
|---|---|---|
| `spring.security.oauth2.client.provider.oidc.issuer-uri` | OIDC discovery and JWKS lookup | Built from `IDAM_OIDC_URL` |
| `oidc.issuer` | Enforced token `iss` value | Supplied from `OIDC_ISSUER` |
| `IDAM_OIDC_URL` | Discovery base URL | Not the source of truth for token `iss` |
| `OIDC_ISSUER` | Expected JWT issuer | Must match real caller token `iss` exactly |

## Tests

| Test | Coverage |
|---|---|
| `src/test/java/uk/gov/hmcts/ccd/SecurityConfigurationTest.java` | Accept expected issuer, reject unexpected issuer, reject expired token |
| `src/test/java/uk/gov/hmcts/ccd/integrations/JwtIssuerValidationIT.java` | Full-stack rejection of a signed JWT whose `iss` does not match configured issuer |

The test fixtures use valid JWT timelines so failures reflect validator behavior rather than builder constraints.

Coverage is intentionally two-layered here: validator-only behavior in `SecurityConfigurationTest` and full integration wiring in `JwtIssuerValidationIT`. A lighter Spring web-security slice test was not kept because it introduced unwanted test-context complexity in this repo.

## Configuration and deployment notes

Before rollout, confirm:

- each environment supplies the intended `OIDC_ISSUER`
- the `iss` claim in real caller tokens matches `OIDC_ISSUER`
- no pipeline or release-time override is supplying an older issuer value
- external callers, smoke tests, and AAT clients obtain tokens whose `iss` claim matches this service's configured `OIDC_ISSUER`

### Guidance alignment

| Item | Current repo state |
|---|---|
| Service issuer model | Single configured issuer |
| Issuer pattern used for this service | Canonical FORGEROCK issuer pattern, consistent with the HMCTS guidance in the Reference section and the external service issuer policy for `ccd-data-store-api` |
| Repo wiring status | Helm values, preview values, and Jenkins wiring are already aligned to that FORGEROCK issuer pattern |

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

## Current repo status

| Area | Current status |
|---|---|
| Decoder / validator chain | `SecurityConfiguration` enforces both timestamp and issuer validation |
| Additional action needed | No further JWT issuer config change is required in this repo unless the external service issuer policy changes |

## Merge checklist

Before merging JWT issuer-validation changes, confirm:

- the active `JwtDecoder` is built from `spring.security.oauth2.client.provider.oidc.issuer-uri`
- the active validator chain includes both `JwtTimestampValidator` and `JwtIssuerValidator(oidc.issuer)`
- there is no disabled, commented-out, or alternate runtime path that leaves issuer validation off
- `issuer-uri` is used for discovery and JWKS lookup only
- `oidc.issuer` / `OIDC_ISSUER` is used as the enforced token `iss` value only
- `OIDC_ISSUER` is explicitly configured and not guessed from the discovery URL
- app config, Helm values, preview values, and CI/Jenkins values are aligned for the target environment
- if `OIDC_ISSUER` changed, it was verified against a real token for the target environment
- there is a test that accepts a token with the expected issuer
- there is a test that rejects a token with an unexpected issuer
- there is a test that rejects an expired token
- there is decoder-level coverage using a signed token, not only validator-only coverage
- CI or build verification checks that a real token issuer matches `OIDC_ISSUER`, or the repo documents why that does not apply
- comments and docs do not describe the old insecure behavior

Do not merge if:

- issuer validation is constructed but not applied
- only timestamp validation is active
- `OIDC_ISSUER` was inferred rather than verified
- Helm and CI/Jenkins issuer values disagree without explanation
- only happy-path tests exist

## Configuration Policy

| Policy | Requirement |
|---|---|
| Discovery | `spring.security.oauth2.client.provider.oidc.issuer-uri` is used for OIDC discovery and JWKS lookup only |
| Enforcement | `oidc.issuer` / `OIDC_ISSUER` is the enforced JWT issuer and must match the token `iss` claim exactly |
| Derivation | Do not derive `OIDC_ISSUER` from `IDAM_OIDC_URL` or the discovery URL |
| Production-like environments | Must provide `OIDC_ISSUER` explicitly |
| Main runtime config | Explicit `OIDC_ISSUER` with no static fallback is the preferred pattern; this repo follows that pattern |
| Local / test-only fallbacks | Acceptable only when static, intentional, and clearly scoped to non-production use |
| Build guard | `verifyOidcIssuerPolicy` fails if `oidc.issuer` is derived from discovery config |
