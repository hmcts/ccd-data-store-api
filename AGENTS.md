# Agents

This file is the index for repo-local workflow skills in this repository. Skill files live under `docs/skills/`.

## CCD Callback SSRF Hardening

Use the `ccd-callback-ssrf-hardening` agent for any callback security change in this repository, especially around event callbacks, webhook URL ingestion, or auth header handling.

### Trigger Phrases

- "Use ccd-callback-ssrf-hardening"
- "Run callback SSRF hardening"
- "Audit callback token leakage"

### Recommended Prompt Template

```text
Use ccd-callback-ssrf-hardening on hmcts/ccd-data-store-api.
Scope:
- src/main/java/uk/gov/hmcts/ccd/domain/service/callbacks/CallbackService.java
- src/main/java/uk/gov/hmcts/ccd/data/SecurityUtils.java
- callback URL ingestion/parsing paths
Tasks:
1. Detect SSRF and credential leakage patterns.
2. Enforce callback URL validation (allowlist, HTTPS, private/internal target blocking).
3. Remove sensitive header forwarding (Authorization, ServiceAuthorization, user-id, user-roles).
4. Add/update regression tests.
5. Summarize risk reduction and residual risk.
```

### Quick Scanner

```bash
bash docs/skills/ccd-callback-ssrf-hardening/scripts/scan_callback_risks.sh
```

## CCD SonarQube Remediation

Use the `ccd-sonarqube-remediation` agent for SonarQube-driven cleanup and quality fixes in this repository, especially maintainability/code smell issues that require safe refactors and test wiring updates.

### Trigger Phrases

- "Use ccd-sonarqube-remediation"
- "Fix Sonar issues"
- "Address code smells from SonarQube"

### Recommended Prompt Template

```text
Use ccd-sonarqube-remediation on hmcts/ccd-data-store-api.
Scope:
- files flagged by current SonarQube findings
Tasks:
1. Reproduce and identify root cause for each finding.
2. Patch with minimal behavior change and clear naming/structure.
3. Update affected tests and fixtures if constructor/bean wiring changes.
4. Add/update tests so coverage for new/changed code is at least 80%.
5. Run targeted Gradle compile/tests plus `checkstyleMain` and `checkstyleTest` for touched areas.
6. Verify SonarQube quality gate status and that blocker/critical issues introduced by the change are zero.
7. Summarize risks, behavior impact, and follow-up actions.
```

### Testing Workflow Note

- WireMock-backed integration tests now use class-level Spring context teardown via `@DirtiesContext(AFTER_CLASS)` in `WireMockBaseTest` to reduce intermittent `WireMockServer` port-bind failures.
- This improves stability but can increase test runtime because affected test classes do not reuse the same Spring context across classes.
- Build verification is split into `testUnit` (parallel unit tests) and `testIt` (serialized `*IT`/`*ITest` tests for stability).
- The default `test` task is disabled to avoid duplicate execution; `check`/`build` run `testUnit` and `testIt`.
- Smoke test execution now includes `preSmokeDiagnostics`, which logs `TEST_URL`, key auth/callback env values, and probes `${TEST_URL}/actuator/health` before BEFTA starts.
- Jenkins archives stage-specific BEFTA outputs as `target/cucumber-smoke.json` and `target/cucumber-functional.json` (plus corresponding JUnit XML), so smoke/functional failures can be triaged independently.
