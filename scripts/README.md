# AAT Remote Run Scripts

These scripts support local `ccd-data-store-api` runs against AAT dependencies.

## Remote App Environment

`runRemoteAAT` and `runRemoteDemo` load local runtime settings from files named `.<env>-remote-env`.

```bash
az login
./gradlew reloadEnvSecrets -Penv=aat
./gradlew reloadEnvSecrets -Penv=demo
```

| Environment | File created | Run task |
|-------------|--------------|----------|
| `aat` | `.aat-remote-env` | `./gradlew runRemoteAAT` |
| `demo` | `.demo-remote-env` | `./gradlew runRemoteDemo` |

The `env` value maps to the Key Vault `ccd-<env>` and the secret `data-store-remote-env`.
The files are ignored by Git because they contain local runtime settings and secrets.

Source `aat-env.sh` before running AAT-backed smoke tests. It exports the BEFTA/IDAM/S2S values used by the test
runner from the same AAT Key Vault secrets used by Jenkins. The default profile fetches only the required smoke/base
secrets, then prints a short summary including `GROUP_ACCESS_ENABLED`.

```bash
source ./scripts/aat-env.sh
```

Use `AAT_ENV_MODE=full` when running the full functional suite. It fetches the additional optional private,
restricted, and cross-case-type BEFTA users.

```bash
AAT_ENV_MODE=full source ./scripts/aat-env.sh
```

If the current shell already has AAT secrets exported, the script reuses them. Force fresh Key Vault reads with:

```bash
AAT_ENV_REFRESH_SECRETS=true source ./scripts/aat-env.sh
```

Show the sourceable script help without fetching secrets:

```bash
source ./scripts/aat-env.sh help
```

## Definition Store Target

By default, leave `AAT_DEFINITION_STORE_HOST` unset. Data-store then uses the AAT definition-store URL from
`.aat-remote-env`.

```bash
unset AAT_DEFINITION_STORE_HOST
source ./scripts/aat-env.sh
./scripts/check-remote-env.sh aat
./gradlew runRemoteAAT
```

### Local Definition Store

Use `AAT_DEFINITION_STORE_HOST` when data-store should use a locally running `ccd-definition-store-api` instead of the
AAT definition-store service. The local definition-store must itself be running against AAT, otherwise data-store will
mix AAT data-store dependencies with local definition-store dependencies.

In `ccd-definition-store-api`, start the local definition-store app against AAT first:

```bash
cd ../ccd-definition-store-api
az login
./gradlew reloadEnvSecrets -Penv=aat
grep '^ENABLE_CASE_GROUP_ACCESS=' .aat-remote-env
./scripts/check-remote-env.sh aat
./gradlew runRemoteAat
```

Expected:

```text
ENABLE_CASE_GROUP_ACCESS=true
```

Then, in `ccd-data-store-api`, point both the app and BEFTA setup at that local definition-store. The override is
applied to both `DEFINITION_STORE_HOST` and `DEFINITION_STORE_URL_BASE`.

```bash
cd ../ccd-data-store-api
export AAT_DEFINITION_STORE_HOST=http://localhost:4451
source ./scripts/aat-env.sh
./scripts/check-remote-env.sh aat
./gradlew runRemoteAAT
```

With `AAT_DEFINITION_STORE_HOST` set, `check-remote-env.sh` checks `http://localhost:4451/health` as well as the
remaining AAT dependencies. The AAT backing for definition-store comes from starting it with `runRemoteAat` and an
`.aat-remote-env` that contains `ENABLE_CASE_GROUP_ACCESS=true`.

This override only changes definition-store. Data-store and definition-store still need AAT internal services such as
S2S, role-assignment, Elasticsearch, and the AAT databases.

For a one-off Gradle app run without exporting the variable:

```bash
./gradlew runRemoteAAT -PdefinitionStoreHost=http://localhost:4451
```

That one-off form also defaults `ENABLE_CASE_GROUP_ACCESS_FILTERING=true`. Override it explicitly only when needed:

```bash
./gradlew runRemoteAAT \
  -PdefinitionStoreHost=http://localhost:4451 \
  -PenableCaseGroupAccessFiltering=false
```

### Group Access Flags

When running group-access functional tests, these values must be set in the process that uses them. `aat-env.sh`
defaults `GROUP_ACCESS_ENABLED` and `ENABLE_CASE_GROUP_ACCESS_FILTERING` to `true` for this AAT flow.

| Variable | Process | Purpose | Expected value |
|----------|---------|---------|----------------|
| `GROUP_ACCESS_ENABLED` | BEFTA functional test terminal | Allows `@groupaccess` scenarios to run. | `true` |
| `ENABLE_CASE_GROUP_ACCESS_FILTERING` | `ccd-data-store-api` app terminal | Makes data-store add and maintain `CaseAccessGroups` in case data and data classification. | `true` |
| `ENABLE_CASE_GROUP_ACCESS` | `ccd-definition-store-api` app terminal | Makes definition-store expose the group-access definition behaviour needed by these tests. | `true` |

Values are read when each process starts. Restart the relevant app after changing a value.

```bash
echo "GROUP_ACCESS_ENABLED=${GROUP_ACCESS_ENABLED}"
echo "ENABLE_CASE_GROUP_ACCESS_FILTERING=${ENABLE_CASE_GROUP_ACCESS_FILTERING}"
./gradlew runRemoteAAT -PdefinitionStoreHost=http://localhost:4451 -PenableCaseGroupAccessFiltering=true
```

### Verify Group Access Definitions

For F-1026, the generated test definitions must be loaded into the running definition-store, not only present under
`build/tmp`. The expected extracted files are:

| File | Expected content |
|------|------------------|
| `build/tmp/BEFTA_MASTER_GROUPACCESS/common/CaseType.json` | `FT_CaseProfessionalGroupAccess` |
| `build/tmp/BEFTA_MASTER_GROUPACCESS/FT_CaseProfessionalGroupAccess/CaseField.json` | `CaseAccessGroups` and `OrganisationPolicyField` |
| `build/tmp/BEFTA_MASTER_GROUPACCESS/common/AccessTypeRole.json` | `CaseAssignedRoleField=CaseProfessionalGroupAccess_GA_Role` and `CaseAccessGroupIDTemplate=BEFTA_MASTER:FT_CaseProfessionalGroupAccess:CaseProfessionalGroupAccess_GA_Role:$ORGID$` |

If a F-1026 create-case response returns `201` and removes the submitted `CCD:all-cases-access` entry, then
`ENABLE_CASE_GROUP_ACCESS_FILTERING=true` is active in data-store. If data-store does not add the replacement
`CaseAccessGroups` value, the full case type returned by definition-store is missing the matching `AccessTypeRole`
metadata, or data-store has cached an older case type definition.

Load the group-access definitions into the running local definition-store before starting data-store:

```bash
cd ../ccd-definition-store-api
ENABLE_CASE_GROUP_ACCESS=true GROUP_ACCESS_ENABLED=true ./gradlew runRemoteAat
```

In a second terminal, run the data setup from this repo so BEFTA imports this repo's test definitions into the
definition-store instance on `localhost:4451`:

```bash
cd ../ccd-data-store-api
export AAT_DEFINITION_STORE_HOST=http://localhost:4451
source ./scripts/aat-env.sh
./gradlew highLevelDataSetup --args='aat'
```

Running a targeted functional scenario also performs the same BEFTA definition import before the scenario runs:

```bash
GROUP_ACCESS_ENABLED=true ./gradlew functional -Ptags='@S-1026.1'
```

| Check | Expected result | Action if it fails |
|-------|-----------------|--------------------|
| `ccd-definition-store-api` started with `ENABLE_CASE_GROUP_ACCESS=true` | Definition-store exposes group-access metadata in case type responses. | Restart definition-store with the flag set before running data-store. |
| `ccd-data-store-api` started after definition-store is ready | Data-store reads the group-access case type after metadata is available. | Restart data-store to clear the cached case type definition. |
| Data-store startup uses `-PdefinitionStoreHost=http://localhost:4451 -PenableCaseGroupAccessFiltering=true` | `DEFINITION_STORE_HOST`, `DEFINITION_STORE_URL_BASE`, and `ENABLE_CASE_GROUP_ACCESS_FILTERING` are set for the app process. | Restart data-store with those properties, or export the equivalent env vars before startup. |
| Local definition-store response for `FT_CaseProfessionalGroupAccess` contains `accessTypeRoles` for `CaseProfessionalGroupAccess_GA_Role` | Data-store has the metadata needed to generate `BEFTA_MASTER:FT_CaseProfessionalGroupAccess:CaseProfessionalGroupAccess_GA_Role:orgID1`. | Re-run `./gradlew highLevelDataSetup --args='aat'` from this repo, or run the targeted functional scenario so BEFTA imports `BEFTA_MASTER_GROUPACCESS`. |

### AAT S2S URL

For this repo's normal AAT flow, `runRemoteAAT` runs directly on the host machine, not inside Docker. In that setup,
`host.docker.internal` is the wrong S2S host.

| Runtime setup | S2S URL value | Valid for this AAT flow |
|---------------|---------------|-------------------------|
| Gradle app running on the host with AAT private DNS/VPN | `http://rpe-service-auth-provider-aat.service.core-compute-aat.internal` | Yes |
| Gradle app running on the host with a local S2S tunnel | `http://localhost:<port>` | Yes, only when the tunnel is running |
| App running inside Docker and calling a tunnel on the host machine | `http://host.docker.internal:<port>` | No for `runRemoteAAT`; only valid for a Dockerised app process |

`./scripts/check-remote-env.sh aat` accepts the AAT internal S2S hostname, or `localhost`/`127.0.0.1` for an intentional
host-machine S2S tunnel. It fails fast if `IDAM_S2S_URL` or `S2S_URL_BASE` points at `host.docker.internal`, because
that usually means a Docker-only tunnel value has leaked into the host-based AAT run.

### AAT Elasticsearch URLs

For AAT, data-store reads Elasticsearch nodes from `ELASTIC_SEARCH_HOSTS` or `ELASTIC_SEARCH_DATA_NODES_HOSTS`.
`check-remote-env.sh` checks every configured node.

| Runtime setup | Elasticsearch URL value | Valid for this AAT flow |
|---------------|-------------------------|-------------------------|
| Gradle app running on the host with AAT private DNS/VPN | `http://ccd-data-<n>.service.core-compute-aat.internal:9200` | Yes |
| Gradle app running on the host with a local Elasticsearch tunnel | `http://localhost:<port>` | Yes, only when the tunnel is running |
| App running inside Docker and calling a tunnel on the host machine | `http://host.docker.internal:<port>` | No for `runRemoteAAT`; only valid for a Dockerised app process |

`./scripts/check-remote-env.sh aat` accepts AAT `ccd-data-*` nodes, or `localhost`/`127.0.0.1` for an intentional
host-machine Elasticsearch tunnel. It fails fast if Elasticsearch is configured with `host.docker.internal`.

### Troubleshooting

| Symptom | Meaning | Action |
|---------|---------|--------|
| `UnknownHostException: rpe-service-auth-provider-aat.service.core-compute-aat.internal` | The local shell or app cannot resolve private AAT DNS. | Connect to the required HMCTS VPN/bastion/private DNS route, then rerun `./scripts/check-remote-env.sh aat` in both repos. |
| `FAIL s2s: AAT IDAM_S2S_URL/S2S_URL_BASE uses http://host.docker.internal:<port>` | A Docker-only host alias is configured for the host-based AAT run. | Use the AAT internal S2S hostname, or use `http://localhost:<port>` if a local S2S tunnel is intentionally running on the host. |
| `FAIL elastic search: AAT ELASTIC_SEARCH_HOSTS/ELASTIC_SEARCH_DATA_NODES_HOSTS uses http://host.docker.internal:<port>` | A Docker-only host alias is configured for the host-based AAT run. | Use the AAT `ccd-data-*` Elasticsearch hosts, or use `http://localhost:<port>` if a local Elasticsearch tunnel is intentionally running on the host. |
| `401` from `http://localhost:4451/api/data/case-type/<case-type>/version` | The request reached local definition-store, but definition-store rejected data-store's forwarded AAT `Authorization` or `ServiceAuthorization` headers. | Check definition-store logs at the same timestamp. Confirm it was started with `runRemoteAat`, can reach AAT S2S, and allows the `ccd_data` S2S service. |
| `Timeout connecting to ccd-data-<n>.service.core-compute-aat.internal:9200` or `Exception executing Elasticsearch search: Connection reset` from `/internal/searchCases` | DNS resolved, but data-store could not open or keep a TCP connection to an AAT Elasticsearch data node. | Run `./scripts/check-remote-env.sh aat`. It checks every value in `ELASTIC_SEARCH_HOSTS` or `ELASTIC_SEARCH_DATA_NODES_HOSTS`; all configured nodes should be reachable on port `9200`. If only one node fails, refresh `.aat-remote-env` and check whether that AAT node is down or unreachable from the VPN/bastion route. |
| `actualResponse.body.case_data.CaseAccessGroups is unavailable` or `actualResponse.body.data_classification.CaseAccessGroups is unavailable` in F-1023/F-1026 | BEFTA ran the scenario. If the submitted `CCD:all-cases-access` entry was removed, data-store filtering is active but no matching `AccessTypeRole` metadata was available. | Use the group-access definition checks above. Restart definition-store first with `ENABLE_CASE_GROUP_ACCESS=true`, then restart data-store with `-PdefinitionStoreHost=http://localhost:4451 -PenableCaseGroupAccessFiltering=true` so it does not use a stale cached case type. |

If `DATA_STORE_DB_HOST` is missing from the Key Vault value, the Gradle helper adds the standard host for the selected
environment:

```text
ccd-data-store-api-postgres-db-v15-<env>.postgres.database.azure.com
```

`DEFINITION_STORE_HOST` should continue to point at `ccd-definition-store-api`; data-store depends on definition-store
for case definitions.

## Connectivity Check

Before starting the app, check DNS and TCP connectivity to the remote dependencies:

```bash
./scripts/check-remote-env.sh aat
```

The check reads `.aat-remote-env` and tests data-store dependencies including Postgres, S2S, IDAM, definition-store,
user-profile, case-document, role-assignment, location reference, and Elasticsearch.

Failures usually mean the local shell is not connected to the required VPN/bastion route, or the remote env file is
missing a required setting.

Start the application in one terminal:

```bash
source ./scripts/aat-env.sh
./gradlew runRemoteAAT
```

Run tests in a second terminal:

```bash
az login
source ./scripts/aat-env.sh
./gradlew --stop
./gradlew --no-daemon smoke

AAT_ENV_MODE=full source ./scripts/aat-env.sh
./gradlew --no-daemon functional
```

```text
AAT environment exported:
  GROUP_ACCESS_ENABLED=true
```
