# JWT issuer validation

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

Coverage is layered between validator-only behavior in `SecurityConfigurationTest` and full integration wiring in `JwtIssuerValidationIT`.

## Configuration and deployment note

This is not only a code change. Runtime configuration must still be correct:

- `spring.security.oauth2.client.provider.oidc.issuer-uri` is used for OIDC discovery and JWKS lookup.
- `oidc.issuer` is the issuer value enforced during JWT validation.
- In this repo those map to `IDAM_OIDC_URL` and `OIDC_ISSUER` in Helm values.

Before rollout, confirm:

- each environment supplies the intended `OIDC_ISSUER`
- the `iss` claim in real caller tokens matches `OIDC_ISSUER`
- no pipeline or release-time override is supplying an older issuer value

If external services still send tokens with a different issuer, this change will reject them with `401` until configuration or token issuance is aligned.

## Optional future variant

Only switch to multi-issuer validation if production tokens genuinely need both values during migration. In that case, use an explicit allow-list for issuer values rather than dropping issuer validation.
