# CCD-7882 error contract

Related requirements: CCD-7108 and CME-984.

Prevent public errors from revealing exception classes, stack traces, internal URLs, file paths
or software details. Preserve response schemas, HTTP status mappings, approved business messages
and public `message` fields where present, while retaining useful private diagnostics.

## Responses

- Disable automatic exception messages, binding errors and stack traces in fallback responses. Hide
  health details and native Tomcat reports/version information.
- Keep the `HttpError` fields, with `exception: "Exception"` and `path: "/"`. Technical failures use
  controlled messages; approved validation, callback errors and callback warnings remain available.
  Throwable objects are excluded from `details`.
- Sanitise bearer challenges without exposing decoder descriptions or internal URLs. Retain handler
  status mappings and required response headers.
- Add no public error-code field.

## Diagnostics

`ErrorCode` labels are internal log categories; `failureCode` is the structured telemetry code.
Neither introduces a public API error-code field.

Advice logs retain original exceptions at WARN/ERROR with private `failureCode` and `httpStatus`
dimensions. Correlation context is preserved. Explicit SDK exception export defaults to off
(`CCD_ERRORS_EXPLICIT_TELEMETRY_ENABLED=false`) to avoid duplicating agent-collected exceptions.

Bearer authentication and access-denied handler logs carry private `failureCode` and `httpStatus`
dimensions; the diagnostic scope restores the caller's context after logging.

## Verification

Focused response, validation, authentication and telemetry tests cover sanitisation and contracts.
Docker checks cover native malformed requests, correlated failure telemetry and DEBUG ingestion with
application/agent DEBUG enabled.

## Support

1. Record the service/environment, UTC failure time, HTTP status and any existing request/trace ID.
   No new correlation field is added to public responses; do not collect credentials or tokens.
2. Open **Application Insights → Logs** for the affected environment/resource. Leave the filters
   empty to discover codes, or set both strings to narrow the search.

```kusto
let wantedCode = "";   // e.g. "DEPENDENCY_REQUEST_FAILED"
let wantedStatus = ""; // e.g. "502"
union exceptions, traces
| where timestamp > ago(24h)
| extend failureCode = tostring(customDimensions["failureCode"]),
         httpStatus = tostring(customDimensions["httpStatus"])
| where isnotempty(failureCode)
| where isempty(wantedCode) or failureCode == wantedCode
| where isempty(wantedStatus) or httpStatus == wantedStatus
| project timestamp, cloud_RoleName, failureCode, httpStatus, operation_Id,
          diagnostic = coalesce(outerMessage, message)
| order by timestamp desc
```

These private dimensions appear on instrumented failure events, not every log or public response.
Search both tables: a failure log without an exception can be recorded as a trace. Use normal
WARN/ERROR diagnostics first; DEBUG is not required to search these fields.

3. Copy a non-empty `operation_Id` from a matching event to find related telemetry and exception
   details. Adjust the time window and include related resources if stored separately.

```kusto
union requests, dependencies, exceptions, traces
| where timestamp > ago(24h)
| where operation_Id == "PASTE_OPERATION_ID"
| order by timestamp asc
```

`operation_Id` is the standard Insights correlation column, not a new public error code. Related
records must be instrumented, collected and retained; sampling can leave an incomplete transaction.
If nothing matches, clear filters and check resource/time selection, collection and restricted
server logs rather than assuming no failure occurred.

These queries use Application Insights resource table names. Workspace queries use
`AppExceptions`/`AppTraces`/`AppRequests`/`AppDependencies`, with `TimeGenerated`, `Properties`,
`OperationId` and `AppRoleName` instead of `timestamp`, `customDimensions`, `operation_Id` and
`cloud_RoleName`. Message columns also differ; see the
[Microsoft telemetry schema](https://learn.microsoft.com/en-us/azure/azure-monitor/app/data-model-complete).

Local unit checks cover structured dimensions and context preservation. A synthetic Docker
collector received representative controlled-failure dimensions and matching request operation IDs
at INFO. Java DEBUG ingestion was also verified with application and agent DEBUG enabled; this
does not confirm deployed settings or every authentication/failure path.

**Verification limit:** these KQL examples were checked against documented schema and local
telemetry field names, but have not been executed against Azure and remain unverified. Production
collection, dependency joins, resource scope, access controls, sampling and retention need checks
in the target environment. Diagnostic details are for authorised support personnel only.

## Deployment checks

- Verify effective error/logging settings and sanitised responses in each environment. Confirm
  `server.error.include-message=never` at runtime.
- Confirm diagnostic logs and Application Insights are restricted to authorised personnel, with
  suitable sampling and retention.
- Verify failure codes and correlation IDs are searchable at normal production logging levels;
  run deployed consumer/Pact checks.
