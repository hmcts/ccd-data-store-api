# CCD-4311: token-claim validation in preview

`TOKEN_CLAIM_VALIDATION_ENABLED` validates that an event token belongs to the user,
jurisdiction, case type, event and case used by the request.

The flag remains enabled in AAT. It is temporarily disabled for preview because legacy
BEFTA fixtures submit event tokens whose claims do not match their create requests:

- F-038 uses a token created for a different user.
- F-1007 uses a token created for the base case type and submits to a suffixed case type.
- F-108 uses a token created for a different user, jurisdiction and case type.

With the flag enabled, these are correctly rejected with `403 EventTokenException` and
`Token properties do not match the expected values`.

Re-enable the preview flag after those fixtures create and submit each event using matching
token claims. This work is separate from CCD-7841 Logstash queue processing.
