# ISSUE-094 Task: Organisation Boundary Enforcement in `ccd-data-store-api`

## Summary
`ccd-data-store-api` currently has two organisation-boundary gaps:

1. `GET /cases/{caseId}` does not explicitly enforce that a restricted professional user can only access cases for their own organisation.
2. `POST /case-users` trusts request `organisation_id` too far and only validates that it is non-empty when present.

The service must use server-side organisation context as authority and enforce org boundaries at the service/authorization layer, not only at request parsing.

## Scope
Endpoints:
- `GET /cases/{caseId}`
- `POST /case-users`
- Review `POST /searchCases` if the current search path can bypass the same org boundary rule

Primary code areas:
- `src/main/java/uk/gov/hmcts/ccd/v2/external/controller/CaseController.java`
- `src/main/java/uk/gov/hmcts/ccd/v2/external/controller/CaseAssignedUserRolesController.java`
- `src/main/java/uk/gov/hmcts/ccd/domain/service/caseaccess/CaseAccessOperation.java`
- `src/main/java/uk/gov/hmcts/ccd/domain/service/getcase/*`

## Required Behaviour

### A. Case-user role requests
- Resolve caller organisation from a server-side source of truth.
- Do not trust request `organisation_id` as authority.
- If `organisation_id` is present and blank, reject the request.
- If `organisation_id` does not match caller organisation, reject the request.
- Enforce this below controller level where practical so the rule is not bypassable.

### B. Direct case access
- For restricted professional users such as solicitor-role users, and similar org-boundary roles already handled
  by the same access path, enforce organisation boundary on case reads.
- A user must not gain access to another organisation's case solely because they have a matching role pattern or a case grant created across organisations.
- If the case is outside the caller's organisation boundary, deny access.

### C. Search path review
- Confirm whether `searchCases` already applies org-aware filtering.
- If not, extend the same boundary rule there.

## Design Decisions Used
1. Caller organisation source of truth:
- Professional Reference Data external organisations-users endpoint
- Implemented in Data Store directly, not via AAC
- Response contract relied on by Data Store is intentionally narrow:
  - `GET /refdata/external/v1/organisations/users`
  - success-for-our-purpose requires a non-blank top-level `organisationIdentifier`
  - `null` body, missing field, blank field, or request failure are all treated as caller organisation unavailable

2. Case organisation source of truth:
- `CaseAccessGroups` on the case
- Derived from organisation policy case data using `OrgPolicyCaseAssignedRole` plus `Organisation.OrganisationID`

3. HTTP contract:
- `POST /case-users` request/caller organisation mismatch returns `400`
- `GET /cases/{caseId}` outside-boundary reads are denied by the existing access-control path and surface as not authorised / not found through the normal get-case flow

## Acceptance Criteria
- A restricted professional user cannot read another organisation's case through `GET /cases/{caseId}`.
- A caller cannot submit `organisation_id` for another organisation in `POST /case-users`.
- Same-organisation valid flows still work.
- Authorization is enforced in service/operation flow, not only in controller validation.

## Implemented
- Added PRD-backed caller-organisation lookup in Data Store.
- `POST /case-users` no longer trusts request `organisation_id`.
- If request `organisation_id` is present and differs from the caller's PRD organisation, the request is rejected.
- If request `organisation_id` is omitted, the server-side PRD organisation is used when available.
- If request `organisation_id` is omitted and the caller has an org-boundary-restricted role but no caller
  organisation can be resolved from PRD, the request is rejected with the same mismatch error.
- If request `organisation_id` is omitted and the caller is not org-boundary restricted, the request can still
  proceed when PRD organisation is unavailable.
- Existing blank-when-present validation for `organisation_id` remains in place.
- Restricted direct case reads now use the existing org-boundary-aware restricted access-profile path.
- The org-boundary intersection is applied only to org-boundary restricted roles, not to all explicit-grant-only
  users such as citizens or letter-holders.
- Event-history endpoints keep their existing explicit authorization contract.
- For that reason, history view and audit-event loading must not reuse the creator/restricted case lookup chain used
  by direct case reads, or restricted external users can be hidden too early as generic case-not-found before the
  history-specific authorization path returns the intended forbidden response.
- No search-path change was made as part of this fix.

## Role Classification Note
- Org-boundary restriction is determined by the application role-pattern logic, not by scenario naming.
- In current code, BEFTA Jurisdiction 2 roles such as `caseworker-befta_jurisdiction_2-solicitor_2` are treated as
  unrestricted because they do not match the restricted-role pattern.
- For that reason, the restricted omitted-organisation AAT branch is implemented using a `BEFTA_MASTER` solicitor
  user whose role does match the restricted-role pattern.

## Tests
Add:
- Unit tests for request org mismatch rejection
- Unit tests for same-org success
- Unit tests for restricted user denied direct case access outside their organisation
- Unit tests for restricted user allowed direct case access within their organisation
- Extend AAT/functional org-context coverage
- Add search regression coverage if search path is in scope

Implemented coverage:
- Repository tests cover:
  - `organisationIdentifier` present
  - PRD lookup failure
  - `null` response body
  - missing `organisationIdentifier`
  - blank `organisationIdentifier`
- Service tests cover:
  - explicit request org mismatch rejection
  - omitted `organisation_id` with PRD-present caller organisation
  - omitted `organisation_id` with PRD-unavailable restricted caller rejection
  - omitted `organisation_id` with PRD-unavailable unrestricted caller success
- Controller/integration tests cover:
  - restricted caller rejected when PRD returns `200` without `organisationIdentifier`
  - unrestricted caller allowed when PRD returns `200` without `organisationIdentifier`
- `CaseAccessServiceTest` documents the current role-classification behaviour for BEFTA Jurisdiction 2 solicitor
  suffix roles, proving they are treated as unrestricted by the current code.
- `F-105` AAT coverage now maps to the implemented behaviour as follows:
  - `S-105.16`: omitted `organisation_id`, PRD available, success using caller organisation from PRD
  - `S-105.17`: blank/invalid `organisation_id`, rejected
  - `S-105.18`: explicit valid `organisation_id`, existing explicit-org success path
  - `S-105.19`: omitted `organisation_id`, unrestricted caller, PRD unavailable, success
  - `S-105.20`: omitted `organisation_id`, restricted caller, PRD unavailable, rejection
- `S-105.20` uses a `BEFTA_MASTER` restricted solicitor fixture rather than the earlier BEFTA Jurisdiction 2
  solicitor fixture, because the latter is not classified as restricted by the current application logic.

## Non-goals
- Do not redesign all case access rules.
- Do not broaden scope beyond org-boundary enforcement for solicitor-style professional users and similar
  org-boundary roles already covered by the same access path.
