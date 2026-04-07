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
- Existing blank-when-present validation for `organisation_id` remains in place.
- Restricted direct case reads now use the existing org-boundary-aware restricted access-profile path.
- The org-boundary intersection is applied only to org-boundary restricted roles, not to all explicit-grant-only
  users such as citizens or letter-holders.
- No search-path change was made as part of this fix.

## Tests
Add:
- Unit tests for request org mismatch rejection
- Unit tests for same-org success
- Unit tests for restricted user denied direct case access outside their organisation
- Unit tests for restricted user allowed direct case access within their organisation
- Extend AAT/functional org-context coverage
- Add search regression coverage if search path is in scope

## Non-goals
- Do not redesign all case access rules.
- Do not broaden scope beyond org-boundary enforcement for solicitor-style professional users and similar
  org-boundary roles already covered by the same access path.
