# Jenkins pipeline optimisations

Investigation recorded on 7 October 2026; review corrections added on 15 October
2026. Original implementation commit: `a1c3b52c1`
(`Optimize Jenkins test execution and container build context`).

The investigation began with reported durations of **27 minutes 36 seconds** for
**Functional Test - aat** and **11 minutes 29 seconds** for
**Static checks / Container build**. These are reported Jenkins timings, not
measurements of the changes described below.

The changes address repeated IDAM authentication, duplicate integration tests,
and an oversized Docker build context. Runtime savings require a Jenkins run
and comparison against those timings.

## 1. Reuse authentication during functional tests

### Previous behaviour

Jenkins configured `BEFTA_USER_TOKEN_CACHE_TTL_SECONDS=1`. Requests separated by
more than one second could trigger another IDAM login even when the previously
issued token remained valid.

Simply increasing this setting would not safely bound token age. BEFTA uses
**expire-after-access**, meaning each cache access extends the entry's lifetime.
A frequently used token could remain cached after the JWT itself expired,
causing authentication failures during a long test suite.

### Change

`FunctionalTestUserTokenCache` now handles token reuse through
`DataStoreTestAutomationAdapter`. It:

- Reuses authentication for at most **five minutes from insertion**, without
  extending that lifetime on subsequent accesses.
- Checks JWT expiry before every reuse and stops reusing tokens with
  **30 seconds or less remaining**.
- Stores a copy of the authenticated user so scenario changes do not mutate
  the cached entry.
- Separates entries by username, password and client ID.
- Does not cache missing, malformed, expired or expiry-less tokens, or failed
  authentication attempts.
- Bounds the cache to 100 entries.

When an entry cannot be reused, the adapter delegates authentication to BEFTA.
Refresh happens on the next authentication request; there is no background
refresh process. JWT decoding determines cache lifetime only. The receiving API
still validates the token.

BEFTA's underlying cache remains configured with a one-second timeout so an
adapter refresh can obtain a new token. The Gradle `functional` and `smoke`
tasks enforce this setting for local runs as well as Jenkins. Data setup does
not use the adapter cache and retains the developer's environment setting.

The isolated six cache tests run through `tokenCacheTest`, which is a dependency
of `check`. This task selects only `FunctionalTestUserTokenCacheTest` from the
`aat` source set: the existing adapter tests need environment variables, and
other AAT tests need deployed services. The full `aatTest` task remains separate.

BEFTA keys its cache by username, whereas the adapter includes password and
client ID. A change of credentials or client within BEFTA's one-second window
can therefore still return the previous token. The adapter's key isolation does
not eliminate this underlying limitation.

### Expected benefit

Repeated requests using the same credentials can avoid unnecessary IDAM logins,
reducing network time and authentication-service load while retaining bounded
token reuse during long test runs.

## 2. Run the dedicated integration tests once

### Previous behaviour

The main Gradle `test` task had no exclusions and selected classes in
`uk/gov/hmcts/ccd/integrations/`. The separate `integration` task selected the
same classes.

Jenkins explicitly invoked `integration` in `afterAlways('test')`, after
`check` had already run the main test task. Active tests in this package therefore
ran twice. The package contained five classes, two of which were already disabled.

The dedicated integration task also ran after coverage reporting, so its new
execution data was not available when that report was generated.

### Change

- The main `test` task excludes `uk/gov/hmcts/ccd/integrations/**`.
- The `integration` task remains responsible for that package.
- `integration.mustRunAfter test` runs unit tests first when both are scheduled.
- Both tasks share JVM module access, Mockito agent, locale, instrumentation-key
  fixture and the 30-minute timeout.
- `jacocoTestReport` explicitly depends on both `test` and `integration`.
- Jenkins publishes integration-test results without invoking the tests again.
- The unused custom `GradleBuilder` instance and import were removed from
  `Jenkinsfile_CNP`.

The resulting dependency structure is:

```text
check
  ├─ tokenCacheTest
  └─ jacocoTestReport
       ├─ test
       └─ integration (after test)
```

Both suites finish before the combined coverage report is generated. Coverage
thresholds remain unchanged. The exclusion applies only to the dedicated
`integrations` package; other integration-style tests elsewhere under `src/test`
still run through the main test task.

### Expected benefit

This removes duplicate execution of the active tests in that package, including
their Spring context setup and cache-expiry waits. It also ensures the combined
coverage report is generated after both suites complete.

## 3. Restrict the container build context

### Previous behaviour

The runtime Dockerfile uses an already compiled application JAR and needs only:

```text
build/libs/core-case-data.jar
lib/applicationinsights.json
```

There was no project `.dockerignore`. The inspected local Jenkins-library
checkout's default ignore file excluded only one unrelated script. Source files,
compiled classes, reports, Gradle state and other workspace files could therefore
be included in the remote build context.

### Change

An allowlist-based `.dockerignore` retains:

- `Dockerfile`
- `acb.tpl.yaml` and the generated `acb.yaml`
- `build/libs/core-case-data.jar`
- `lib/applicationinsights.json`

Only file exceptions are used. Re-including `build/`, `build/libs/` or `lib/`
also re-includes their other contents. Excluding those contents keeps reports
and compiled classes out of the context while the parallel test stage writes
to them. The JAR must still be built before context packaging begins.

The Dockerfile also copies the stable `applicationinsights.json` before the
application JAR. Where Docker layer caching is available, a JAR change can
preserve the earlier configuration layer.

### Expected benefit

Review measurements of the original directory exceptions found roughly **8,200
files / 252 MB** in the Docker context and **9,000 entries / 172 MB compressed**
in the Azure CLI archive. The previous claim of a reduction from 900 MB to 153 MB
was incorrect: 153 MiB described approximately the JAR size alone.

With file exceptions only, the local Azure CLI packer selects four files when
`acb.yaml` has not been generated, or five when it exists. Actual archive sizes
are recorded under validation below. Jenkins sizes depend on its JAR and the
generated ACR configuration; no Jenkins upload reduction has been measured yet.

The application JAR itself was not reduced in size. Reordering the two `COPY`
instructions is a smaller improvement than reducing the context.

## Files changed

| File | Purpose |
| --- | --- |
| `.dockerignore` | Restrict the container build context |
| `Dockerfile` | Put the stable configuration layer before the JAR |
| `Jenkinsfile_CNP` | Remove repeated integration execution and publish its results |
| `build.gradle` | Separate test selection, run cache tests in check, share test JVM settings, order coverage, and configure BEFTA's underlying cache |
| `src/aat/java/uk/gov/hmcts/ccd/datastore/befta/DataStoreTestAutomationAdapter.java` | Use the new authentication cache |
| `src/aat/java/uk/gov/hmcts/ccd/datastore/befta/FunctionalTestUserTokenCache.java` | Implement bounded, expiry-aware token reuse |
| `src/aat/java/uk/gov/hmcts/ccd/datastore/befta/FunctionalTestUserTokenCacheTest.java` | Test reuse, refresh, isolation and failure handling |
| `README.md` | Document authentication behaviour |

## Validation performed

The original investigation reported six cache tests and eight existing adapter
tests passing, with AAT Checkstyle passing. The original command required the
adapter's environment variables; it did not demonstrate that CI ran these tests.

```bash
./gradlew --offline aatTest \
  --tests uk.gov.hmcts.ccd.datastore.befta.FunctionalTestUserTokenCacheTest \
  --tests uk.gov.hmcts.ccd.datastore.befta.DataStoreTestAutomationAdapterTest
./gradlew --offline checkstyleAat
```

On 15 October 2026, the new CI task ran all **six cache tests** successfully
without AAT environment setup, and AAT Checkstyle passed:

```bash
./gradlew --offline tokenCacheTest checkstyleAat
```

The installed Azure CLI 2.87.0 `_pack_source_code` function was exercised locally
in task-context mode (no separately appended Dockerfile), without uploading or
contacting ACR. Its archive contained exactly `Dockerfile`, `acb.tpl.yaml`,
`build/libs/core-case-data.jar` and `lib/applicationinsights.json`: **160,597,557
bytes of file data (160.60 MB / 153.16 MiB)** and **143,396,295 compressed bytes
(143.40 MB / 136.75 MiB)**. The local JAR accounts for 160,593,959 bytes.
`acb.yaml` was absent in this workspace; Jenkins generates it before upload.

On 15 October 2026, the three focused `ApplicationParamsTest` tests also passed
with the shared JVM settings:

```bash
./gradlew --offline test --tests uk.gov.hmcts.ccd.ApplicationParamsTest
```

The original investigation also exercised selection through the dedicated
integration task, but both selected integration classes were already disabled:
their eight skipped tests do **not** establish that the active integration suite
passes.

```bash
./gradlew --offline integration \
  --tests uk.gov.hmcts.ccd.integrations.IdamIT \
  --tests uk.gov.hmcts.ccd.integrations.ServiceToServiceIT \
  test --tests uk.gov.hmcts.ccd.ApplicationParamsTest
```

A Gradle dry run on 15 October 2026 confirmed that `check` includes
`tokenCacheTest`, and orders `test` before `integration`, with both before coverage
reporting:

```bash
./gradlew --offline check --dry-run
```

Every Dockerfile `COPY` input was checked against the context allowlist, and
`git diff --check` passed. A full container build and Docker-dependent integration
tests were not run because Docker was unavailable locally.

## Jenkins timing interpretation and remaining work

The inspected local Jenkins-library checkout runs these branches in parallel
within **Static checks / Container build**:

- Unit tests and Sonar scan, including the quality-gate wait
- Security checks
- Tech-stack maintenance
- Docker build

The stage duration is determined by the slowest branch. These changes reduce
work, but the reported **11 minutes 29 seconds** will only improve if they speed
up the branch determining that duration. Confirm the library revision used by
the actual build and inspect Jenkins console output to identify that branch.

Next validation steps are to run Jenkins, confirm that the active integration
suite and coverage thresholds pass, verify the reduced ACR upload, and compare
branch timings and functional-test duration with the previous build. No specific
runtime saving has yet been measured.

Before merging the authentication change, attach a green Jenkins functional-test
run for this branch and compare its duration with the **27m36s** baseline. Also
obtain confirmation from the owner of **#2903 (`7b0af8251`)** that five-minute
adapter reuse is acceptable: that change deliberately set BEFTA's TTL to one
second to avoid mid-suite expiry. Neither the green run nor owner confirmation
has been obtained by this local validation. If authentication cannot be validated
with this PR, split it from the independently verifiable CI and Docker changes.

Functional tests still run on one Cucumber thread. The Jenkins setting
`MAX_NUM_PARALLEL_THREADS=6` controls Gradle test forks; it does not override the
functional runner's explicit `--threads 1` argument.

Feature files also contain 69 explicit five-second wait occurrences, totalling
**345 seconds**, including ignored scenarios. This is a static inventory, not
the measured wait time of an executed suite.

Parallel functional execution and readiness polling remain opportunities for
further optimisation. They require additional work to verify shared-data
isolation and preserve reliable checks while search indexing completes.
