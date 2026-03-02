---
name: ccd-sonarqube-remediation
description: Triage and fix SonarQube findings in hmcts/ccd-data-store-api with minimal-risk refactors, explicit behavior checks, and targeted regression validation. Use for maintainability/code smell fixes, naming/regex compliance, logging/performance improvements, and Spring wiring/qualifier updates.
---

# CCD SonarQube Remediation

## Overview

Use this skill to handle SonarQube issues with safe, reviewable patches.
Prioritize behavior-preserving changes, then verify with focused compile/tests.

## Workflow

1. Identify the exact finding and location (rule key, file, line, message).
2. Confirm whether the issue is real or intentional.
3. Apply the smallest safe code change that satisfies the rule.
4. Update dependent wiring/tests/fixtures when constructor or bean names change.
5. Add or update tests so new/changed code paths meet at least 80% coverage.
6. Run targeted Gradle compile/tests and checkstyle for changed areas.
7. Report behavior impact, risk, and any residual items.

## Typical Findings In This Repository

- Naming compliance (`^[a-z][a-zA-Z0-9]*$`) for Spring bean names.
- Invoke methods conditionally to avoid unnecessary work in logging paths.
- Remove unused fields/constants/imports.
- Replace ambiguous names with intent-revealing identifiers.
- Keep callback/security hardening comments explicit where rules or literals may look suspicious.

## Hotspots In This Repository

- `src/main/java/uk/gov/hmcts/ccd/config/JacksonObjectMapperConfig.java`
- `src/main/java/uk/gov/hmcts/ccd/domain/service/callbacks/CallbackService.java`
- `src/main/java/uk/gov/hmcts/ccd/domain/service/callbacks/CallbackUrlValidator.java`
- `src/test/java/uk/gov/hmcts/ccd/` (bean qualifier and integration test wiring)

## Implementation Guidance

### Keep Behavior Stable

- Default to refactors that do not change runtime outcomes.
- If behavior changes are necessary, call them out explicitly before patching.

### Spring Bean/Wiring Changes

- When renaming bean names, update every `@Qualifier` reference in main and test code.
- Re-run `compileJava` and `compileTestJava` after qualifier changes.

### Logging/Performance Changes

- Guard expensive argument construction with log-level checks where appropriate.
- Prefer one computed guard boolean reused in nearby log statements.

### Tests and Fixtures

- If validation shifts earlier (ingestion-time vs runtime), ensure test fixtures contain valid placeholder replacements.
- Fix tests by aligning setup with real production wiring, not by weakening assertions.
- Coverage gate: ensure coverage for touched/new logic is >=80% (line/branch as available in project reports).

## Quick Commands

```bash
rg -n "sonar|@Qualifier\\(|@Bean\\(name|unused|WILDCARD|printCallbackDetails" src/main/java src/test/java
./gradlew compileJava compileTestJava
./gradlew test --tests <affected.test.ClassName>
./gradlew checkstyleMain checkstyleTest
```

## Deliverable Checklist

- Each finding has a clear root cause and patch rationale.
- No accidental behavior regressions introduced.
- Main and test wiring updated for any renamed beans/constructors.
- Coverage for new/changed code is >=80%.
- Checkstyle warnings/errors are resolved for touched files.
- Targeted compile/tests pass for modified areas.
- Residual risks and deferred items are documented.
