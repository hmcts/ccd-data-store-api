# JWT issuer validation

## Current contract

`ccd-data-store-api` uses the same allow-list contract as master:

| Setting | Purpose |
|---|---|
| `IDAM_OIDC_URL` | OIDC discovery and signing-key retrieval only |
| `OIDC_ALLOWED_ISSUERS` | Complete comma-separated list of trusted token `iss` values |
| `idam.security.allowed-issuers` | Spring property consumed by the decoder; direct overrides remain supported |
| `VERIFY_OIDC_ISSUER` | Enables the functional-test issuer pre-check in Jenkins |

`OIDC_ISSUER` is neither required nor consumed by this repository. There is no implicit
primary issuer. Include every accepted issuer explicitly in the allow-list.

Main application configuration preserves master's local defaults when the environment variable
is absent: `http://idam-web-public:9002/o,http://fr-am:8080/openam/oauth2/hmcts`.
Deployed environments must supply their intended list explicitly. An explicitly blank list
fails startup. Test configurations provide isolated local values.

Discovery and trust are separate. Do not derive trusted issuer values from `IDAM_OIDC_URL`,
the discovery response, or token input. Confirm configuration against real caller tokens
without storing or logging complete bearer tokens.

## Implementation

`SecurityConfiguration.jwtDecoder()` discovers signing keys using `issuer-uri`, then applies
`jwtValidator(configuredAllowedIssuers)`. The shared `OidcIssuerConfiguration` parser trims
entries, ignores empty comma-separated entries, removes duplicates, and rejects an empty list.

The validator combines `JwtTimestampValidator` with `JwtClaimValidator` for exact issuer
membership. Signature checks remain with `NimbusJwtDecoder`. Missing and unexpected issuer
claims are rejected. No discovery issuer is accepted implicitly.

The old `IdamSecurityConfig` and `MultiIssuerValidator` classes are replaced by this path;
the existing `idam.security.allowed-issuers` property and `OIDC_ALLOWED_ISSUERS` environment
variable remain compatible with master. The intermediate branch-only `oidc.issuer` and
`oidc.allowed-issuers` settings are no longer used.

## Deployment and pipeline alignment

Base, AAT and preview chart values already define `OIDC_ALLOWED_ISSUERS`. Their existing
lists are preserved. CNP and nightly verification use the same three-entry AAT list:

```text
https://idam-web-public.aat.platform.hmcts.net/o
https://hmcts-access.aat.platform.hmcts.net/o
https://forgerock-am.service.core-compute-idam-aat2.internal:8443/openam/oauth2/realms/root/realms/hmcts
```

Jenkins variables configure the test process; Helm configures the deployed application.
They must both include issuers used by the tests and application callers.

`verifyFunctionalTestJwtIssuer` obtains a test token and checks its `iss` against the complete
`OIDC_ALLOWED_ISSUERS` list before high-level data setup, smoke or functional tests. This
pre-check inspects the token payload; the application's decoder separately verifies signatures.
Local invocation is opt-in through `VERIFY_OIDC_ISSUER=true` and requires IDAM and test credentials.
`verifyOidcIssuerPolicy` checks that trusted issuers are not derived from discovery settings.

For other CCD-7109 repositories, assess their master branches independently before changing
their configuration contract. This update is limited to Data Store and does not modify them.

## Tests

- `JwtIssuerValidatorTest`: accepts listed issuers and rejects unlisted, missing and expired issuers/tokens.
- `OidcIssuerConfigurationTest`: parses a complete allow-list, trims and deduplicates entries,
  rejects null/blank/empty lists and returns an immutable set.
- `SecurityConfigurationJwtDecoderIssuerValidationIT`: uses the configured decoder and signed JWTs;
  accepts both configured issuers and rejects unexpected or missing issuers, without a primary-issuer setting.

Run the focused tests with `OIDC_ISSUER` unset. The integration tests require Docker for PostgreSQL.
Runtime tests do not prove live issuer values or all external callers are aligned.

Local verification on 30 October 2026: 22 focused tests passed with `OIDC_ISSUER` and
`OIDC_ALLOWED_ISSUERS` unset in the test process, including four full-stack decoder tests
using an explicit test allow-list. AAT and contract-test compilation, main/test Checkstyle,
and `verifyOidcIssuerPolicy` passed. The verifier now calls the current `getIdamOidcToken()`
helper API. CNP and nightly lists were checked against the preview list. Live IDAM/AAT
execution was not repeated; the earlier local AAT attempt was blocked by unavailable IDAM.

## History and review against master

Historically, Data Store configured `OIDC_ISSUER` while issuer enforcement was disabled.
Master commit `7f0fcdace` (CCD-7454, 28 October 2026) replaced this with `OIDC_ALLOWED_ISSUERS`
and enabled `MultiIssuerValidator`. Cached master `891ba8a7d` already validates timestamps
and issuer membership.

An intermediate CCD-7109 implementation reintroduced a mandatory primary issuer. That created
a deployment gap because base/AAT charts supplied only the allow-list. This implementation
removes that requirement and preserves master's public configuration contract.

The constructor initialization error, duplicate injection, duplicate preview keys and obsolete
issuer path have also been addressed. Current review uses cached master; GitHub SSH fetch
failed, so remote freshness and live environment values remain unverified.

## HMCTS guidance

[JWT issuer validation guidance](https://tools.hmcts.net/confluence/spaces/SISM/pages/1958056812/JWT+iss+Claim+Validation+for+OIDC+and+OAuth+2+Tokens#JWTissClaimValidationforOIDCandOAuth2Tokens-Configurationrecommendation)
remains the external reference for service-level issuer decisions. Validate deployed values
against the agreed environment policy before rollout.
