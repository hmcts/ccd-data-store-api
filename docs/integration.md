# Integrating with CCD Data Store

CCD Data Store is where case data lives.

## Technical resources

* [API Security](api/security.md)
* [Case data format](api/case-data.md)

## Callback Test Fixtures

When loading case definition fixtures in integration tests, ensure callback URL placeholders (for example
`${CALLBACK_URL}` and `${GET_CASE_CALLBACK_URL}`) are always replaced with valid absolute URLs.

Callback URL hardening validates definition callback URLs at ingestion/read-time, so unresolved placeholders will now
be rejected as invalid URLs before callback execution.

For BEFTA/AAT definitions that use environment placeholders (for example
`${TEST_STUB_SERVICE_BASE_URL:...}/callback_get_case_injectedData`), ensure placeholder resolution happens before
callback URL validation in the import path.
