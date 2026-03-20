#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-.}"

if ! command -v rg >/dev/null 2>&1; then
  echo "[ERROR] rg (ripgrep) is required." >&2
  exit 1
fi

echo "== CCD callback risk scan in: ${ROOT} =="

echo
printf '%s\n' "-- Callback HTTP invocation candidates (high signal) --"
rg -n --no-heading 'CallbackService|CallbackInvoker|CallbackUrlValidator|DefaultCaseDefinitionRepository|sendSingleRequest\(|callback.*exchange\(|exchange\(.*callback|callBackURL|callbackGetCaseUrl|validateCallbackUrl' "$ROOT/src/main/java" || true

echo
printf '%s\n' "-- Outbound HTTP client usage (broad sweep) --"
rg -n --no-heading 'restTemplate\.exchange\(|RestTemplate|WebClient|FeignClient|HttpClient|HttpMethod\.POST' "$ROOT/src/main/java" || true

echo
printf '%s\n' "-- Potential broad header forwarding --"
rg -n --no-heading 'putAll\(|authorizationHeaders\(|addPassThroughHeaders\(|ServiceAuthorization|Authorization|user-roles|user-id' "$ROOT/src/main/java" || true

echo
printf '%s\n' "-- Callback URL ingestion/model points --"
rg -n --no-heading 'callback_url_about_to_start_event|callback_url_about_to_submit_event|callback_url_submitted_event|callback_url_mid_event|callback_get_case_url|setCallBackURL|setCallbackGetCaseUrl|setUrl\(|webhook' "$ROOT/src/main/java" || true

echo
printf '%s\n' "-- Potential missing URL validation clues --"
rg -n --no-heading 'URI\(|URL\(|allowlist|whitelist|trusted|validate.*url|InvalidUrlException' "$ROOT/src/main/java" || true

echo
printf '%s\n' "-- Callback hardening config presence --"
rg -n --no-heading 'ccd\.callback\.(allowed-hosts|allowed-http-hosts|allow-private-hosts|passthru-header-contexts)' \
  "$ROOT/src/main/resources" "$ROOT/charts" "$ROOT/src/main/java/uk/gov/hmcts/ccd/ApplicationParams.java" 2>/dev/null || true
