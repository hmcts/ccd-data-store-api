#!/usr/bin/env bash
# This script runs the definition spreadsheet harness with the provided arguments.
#
# ./scripts/run-definition-spreadsheet-harness.sh   /Users/<user>/Downloads/ccd-appeal-config-preview-pr3017.xlsx   caseworker-ia-admofficer   isFeePaymentEnabled,sponsorEmailAdminJ,sponsorMobileNumberAdminJ,sponsorAddress   editAppealAfterSubmit;
#
set -euo pipefail

if [ "$#" -ne 4 ]; then
  echo "Usage: $0 <definition-xlsx> <roles> <target-fields> <event-id>" >&2
  exit 1
fi

definition_file="$1"
roles="$2"
target_fields="$3"
event_id="$4"

./gradlew definitionSpreadsheetHarness \
  -Pdefinition.file="$definition_file" \
  -Proles="$roles" \
  -Ptarget.fields="$target_fields" \
  -Pevent.id="$event_id"
