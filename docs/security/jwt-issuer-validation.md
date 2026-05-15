# JWT issuer validation

## Service

`ccd-data-store-api`

## Summary

- re-enables issuer validation in `ccd-data-store-api`, so JWTs must match configured issuers as well as pass timestamp checks
- follows the single configured issuer approach by default, with optional `OIDC_ALLOWED_ISSUERS` support for migration
- any change to this repo's JWT issuer configuration should remain consistent with the HMCTS guidance in the [HMCTS Guidance](#hmcts-guidance) section and the externally agreed service issuer policy

## HMCTS Guidance

- [JWT iss Claim Validation guidance](https://tools.hmcts.net/confluence/spaces/SISM/pages/1958056812/JWT+iss+Claim+Validation+for+OIDC+and+OAuth+2+Tokens#JWTissClaimValidationforOIDCandOAuth2Tokens-Configurationrecommendation)
- Use that guidance as the reference point for service-level issuer decisions and configuration recommendations.

## Current approach

| Area | Current approach in this repo |
|---|---|
| JWT validation | Signature, timestamp, and issuer are all enforced |
| Discovery / JWKS source | `spring.security.oauth2.client.provider.oidc.issuer-uri` |
| Enforced issuer | `oidc.issuer` / `OIDC_ISSUER`, plus optional `oidc.allowed-issuers` / `OIDC_ALLOWED_ISSUERS` |
| Issuer model | `OIDC_ISSUER` remains the primary issuer; optional additional issuers may be configured during migration |

## Context

- `src/main/java/uk/gov/hmcts/ccd/SecurityConfiguration.java` builds the decoder from `spring.security.oauth2.client.provider.oidc.issuer-uri`.
- The service separately configures trusted issuers because the discovered issuer is not always the value trusted for validation.
- The previous implementation instantiated `JwtIssuerValidator(issuerOverride)` but only applied `JwtTimestampValidator`, which meant an unexpected `iss` claim could still be accepted if signature and timestamps were valid.

## Implemented fix

`SecurityConfiguration.jwtDecoder()` now uses:

```java
OAuth2TokenValidator<Jwt> withTimestamp = new JwtTimestampValidator();
OAuth2TokenValidator<Jwt> withIssuer = new JwtClaimValidator<>(
    "iss",
    OidcIssuerConfiguration.allowedIssuers(issuerOverride, allowedIssuersOverride)::contains
);
OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(withTimestamp, withIssuer);
```

## Runtime model

| Setting | Purpose | Notes |
|---|---|---|
| `spring.security.oauth2.client.provider.oidc.issuer-uri` | OIDC discovery and JWKS lookup | Built from `IDAM_OIDC_URL` |
| `oidc.issuer` | Primary enforced token `iss` value | Supplied from `OIDC_ISSUER` |
| `oidc.allowed-issuers` | Optional additional accepted token `iss` values | Supplied from comma-separated `OIDC_ALLOWED_ISSUERS`; falls back to `OIDC_ISSUER` when unset |
| `IDAM_OIDC_URL` | Discovery base URL | Not the source of truth for token `iss` |
| `OIDC_ISSUER` | Primary expected JWT issuer | Must match a real caller token `iss` exactly |
| `OIDC_ALLOWED_ISSUERS` | Optional migration issuer list | Use only for explicit additional issuer values that must be accepted |

For this repo, the FORGEROCK issuer used in deployed environments is an explicit `OIDC_ISSUER` value supplied by Helm/Jenkins configuration. It is not a runtime fallback that appears automatically when issuer settings are absent. `OIDC_ALLOWED_ISSUERS` is optional; if unset, validation falls back to the single primary `OIDC_ISSUER` behavior.

## Tests

| Test | Coverage |
|---|---|
| `src/test/java/uk/gov/hmcts/ccd/SecurityConfigurationTest.java` | Accept primary issuer, accept additional allowed issuer, reject unexpected issuer, reject expired token |
| `src/test/java/uk/gov/hmcts/ccd/security/OidcIssuerConfigurationTest.java` | Fallback to primary issuer when optional allowed issuers are unset, comma-separated parsing, blank config rejection |
| `src/test/java/uk/gov/hmcts/ccd/integrations/JwtIssuerValidationIT.java` | Full-stack rejection of a signed JWT whose `iss` does not match configured issuers |

The test fixtures use valid JWT timelines so failures reflect validator behavior rather than builder constraints.

Coverage is intentionally two-layered here: validator-only behavior in `SecurityConfigurationTest` and full integration wiring in `JwtIssuerValidationIT`. A lighter Spring web-security slice test was not kept because it introduced unwanted test-context complexity in this repo.

## Configuration and deployment notes

Before rollout, confirm:

- each environment supplies the intended `OIDC_ISSUER`
- any `OIDC_ALLOWED_ISSUERS` value contains only explicitly accepted migration issuers
- the `iss` claim in real caller tokens matches `OIDC_ISSUER` or an explicitly configured allowed issuer
- no pipeline or release-time override is supplying an older issuer value
- external callers, smoke tests, and AAT clients obtain tokens whose `iss` claim matches this service's configured OIDC issuers

### Guidance alignment

| Item | Current repo state |
|---|---|
| Service issuer model | Single configured primary issuer, with optional explicit migration allow-list |
| Issuer pattern used for this service | Canonical FORGEROCK issuer pattern, consistent with the HMCTS guidance in the [HMCTS Guidance](#hmcts-guidance) section and the external service issuer policy for `ccd-data-store-api` |
| Repo wiring status | Base Helm values remain aligned to the FORGEROCK primary issuer; PR preview uses the public AAT IDAM issuer as primary and allows the legacy FORGEROCK issuer for CCD gateway/callback tokens during migration |

Preview could not be switched to only `https://idam-web-public.aat.platform.hmcts.net/o` because the PR functional
issuer verifier gets a real token through the CCD gateway OAuth client, and that token currently has the FORGEROCK
`iss` value. During migration, configure `OIDC_ALLOWED_ISSUERS` with the additional public IDAM or ForgeRock issuer
values that must be accepted instead of disabling issuer validation.

Do not infer `OIDC_ISSUER` or `OIDC_ALLOWED_ISSUERS` from the public OIDC discovery URL. In preview/AAT for this repo,
the correct issuer values had to be taken from decoded real tokens and did not always match the public
`IDAM_OIDC_URL` base.

Smoke and functional pipeline runs now perform a pre-check that acquires a real test token and fails fast if its
`iss` claim does not match `OIDC_ISSUER` or an explicit `OIDC_ALLOWED_ISSUERS` value.
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

## Migration allow-list usage

Set `OIDC_ALLOWED_ISSUERS` only when the service genuinely needs to accept more than the primary issuer during
migration. Use a comma-separated explicit list of issuer values, for example:

```text
OIDC_ALLOWED_ISSUERS=https://idam-web-public.aat.platform.hmcts.net/o,https://forgerock-am.service.core-compute-idam-aat2.internal:8443/openam/oauth2/realms/root/realms/hmcts
```

Do not use `OIDC_ALLOWED_ISSUERS` to derive issuers from discovery configuration. Remove migration issuers once callers
have moved to the target issuer.

## Current implementation status

| Area | Current status |
|---|---|
| Decoder / validator chain | `SecurityConfiguration` enforces timestamp and configured issuer validation |
| Additional action needed | Set `OIDC_ALLOWED_ISSUERS` only where migration requires multiple accepted issuers |

## Merge checklist

Before merging JWT issuer-validation changes, confirm:

- the active `JwtDecoder` is built from `spring.security.oauth2.client.provider.oidc.issuer-uri`
- the active validator chain includes both `JwtTimestampValidator` and issuer claim validation
- there is no disabled, commented-out, or alternate runtime path that leaves issuer validation off
- `issuer-uri` is used for discovery and JWKS lookup only
- `oidc.issuer` / `OIDC_ISSUER` is used as the primary enforced token `iss` value only
- `oidc.allowed-issuers` / `OIDC_ALLOWED_ISSUERS` contains only explicit migration issuers, when set
- `OIDC_ISSUER` is explicitly configured and not guessed from the discovery URL
- app config, Helm values, preview values, and CI/Jenkins values are aligned for the target environment
- if `OIDC_ISSUER` changed, it was verified against a real token for the target environment
- there is a test that accepts a token with the expected issuer
- there is a test that rejects a token with an unexpected issuer
- there is a test that rejects an expired token
- there is decoder-level coverage using a signed token, not only validator-only coverage
- CI or build verification checks that a real token issuer matches configured OIDC issuers, or the repo documents why that does not apply
- comments and docs do not describe the old insecure behavior

Do not merge if:

- issuer validation is constructed but not applied
- only timestamp validation is active
- `OIDC_ISSUER` or `OIDC_ALLOWED_ISSUERS` was inferred rather than verified
- Helm and CI/Jenkins issuer values disagree without explanation
- only happy-path tests exist

## Configuration Policy

| Policy | Requirement |
|---|---|
| Discovery | `spring.security.oauth2.client.provider.oidc.issuer-uri` is used for OIDC discovery and JWKS lookup only |
| Enforcement | `oidc.issuer` / `OIDC_ISSUER` is the primary enforced JWT issuer; `oidc.allowed-issuers` / `OIDC_ALLOWED_ISSUERS` may add explicit migration issuers |
| Derivation | Do not derive `OIDC_ISSUER` or `OIDC_ALLOWED_ISSUERS` from `IDAM_OIDC_URL` or the discovery URL |
| Production-like environments | Must provide `OIDC_ISSUER` explicitly |
| Local / test-only fallbacks | Acceptable only when static, intentional, and clearly scoped to non-production use |
| Build guard | `verifyOidcIssuerPolicy` fails if OIDC issuer validation config is derived from discovery config |

## References

See [HMCTS Guidance](#hmcts-guidance).
