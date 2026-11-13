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

In Application Insights, filter private `failureCode` and `httpStatus` dimensions, then use
`operation_Id` to find related requests and dependencies. Inspect exception details in restricted
telemetry or server logs; public error messages are sanitised. Failure diagnosis should work at
normal production logging levels without enabling DEBUG.

## Deployment checks

- Verify effective error/logging settings and sanitised responses in each environment. Confirm
  `server.error.include-message=never` at runtime.
- Confirm diagnostic logs and Application Insights are restricted to authorised personnel, with
  suitable sampling and retention.
- Verify failure codes and correlation IDs are searchable at normal production logging levels;
  run deployed consumer/Pact checks.
