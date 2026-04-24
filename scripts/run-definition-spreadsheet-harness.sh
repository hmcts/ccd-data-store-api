#!/usr/bin/env bash
# Runs the definition spreadsheet harness with the provided arguments.
#
# Usage:
# run-definition-spreadsheet-harness.sh <definition-xlsx> <roles> <target-fields> <event-id>
#
# <roles> and <target-fields> can be comma separated lists.
#
# Example:
# ./scripts/run-definition-spreadsheet-harness.sh /ccd-appeal-config-preview-pr3017.xlsx caseworker-ia-admofficer isFeePaymentEnabled,sponsorEmailAdminJ,sponsorMobileNumberAdminJ,sponsorAddress editAppealAfterSubmit;
#
# Process summary:
# 1) Reads CaseEventToFields, AuthorisationCaseField, RoleToAccessProfiles from the XLSX.
# 2) Resolves roles to access profiles.
# 3) Evaluates read access for each field and matching case type for the event.
#
# Exit code:
# Non-zero on Gradle task failure (invalid input/configuration or non-returned decisions).
#
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

if [ "$#" -ne 4 ]; then
  exit 1
fi

definition_file="$1"
roles="$2"
target_fields="$3"
event_id="$4"

cd "${repo_root}"

./gradlew definitionSpreadsheetHarness \
  -Pdefinition.file="$definition_file" \
  -Proles="$roles" \
  -Ptarget.fields="$target_fields" \
  -Pevent.id="$event_id"
