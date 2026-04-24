package uk.gov.hmcts.ccd.tools;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DefinitionSpreadsheetHarnessTest {

    private static final String SUCCESS_ROLE = "caseworker-ia-admofficer";
    private static final String FAILURE_ROLE = "caseworker-ia-admofficer2";
    private static final String EVENT = "editAppealAfterSubmit";
    private static final String ASYLUM = "Asylum";
    private static final List<String> TARGET_FIELDS = List.of(
        "isFeePaymentEnabled",
        "sponsorEmailAdminJ",
        "sponsorMobileNumberAdminJ",
        "sponsorAddress"
    );
    private static final String FIRST_ROW_HEADERS_DEFINITION_SPREADSHEET = "first-row-headers.xlsx";
    private static final String INVALID_DEFINITION_SPREADSHEET = "invalid-definition.xlsx";
    private static final String VALID_DEFINITION_SPREADSHEET = "ccd-appeal-config-preview-pr3017.xlsx";

    private static final Path KNOWN_SPREADSHEET = testResourcePath(VALID_DEFINITION_SPREADSHEET);

    private static Path testResourcePath(String name) {
        try {
            var resource = DefinitionSpreadsheetHarnessTest.class.getResource("/" + name);
            if (resource == null) {
                throw new IllegalStateException("Missing test resource: " + name);
            }
            return Path.of(resource.toURI());
        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve test resource: " + name, e);
        }
    }

    @Test
    void shouldReturnNoFailuresForKnownAppealSpreadsheetInputs() throws Exception {
        assumeTrue(Files.isRegularFile(KNOWN_SPREADSHEET),
            "Skipping: expected spreadsheet not found at " + KNOWN_SPREADSHEET);

        List<DefinitionSpreadsheetHarness.FieldDecision> decisions =
            DefinitionSpreadsheetHarness.run(KNOWN_SPREADSHEET, Set.of(SUCCESS_ROLE), EVENT, TARGET_FIELDS);

        assertThat(decisions)
            .extracting(DefinitionSpreadsheetHarness.FieldDecision::fieldId)
            .containsExactlyElementsOf(TARGET_FIELDS);

        assertThat(decisions)
            .allSatisfy(decision -> {
                assertThat(decision.isReturned()).isTrue();
                assertThat(decision.details()).contains("case type: " + ASYLUM);
            });
    }

    @Test
    void shouldReturnFailuresForKnownAppealSpreadsheetInputs() throws Exception {
        assumeTrue(Files.isRegularFile(KNOWN_SPREADSHEET),
            "Skipping: expected spreadsheet not found at " + KNOWN_SPREADSHEET);

        List<DefinitionSpreadsheetHarness.FieldDecision> decisions =
            DefinitionSpreadsheetHarness.run(KNOWN_SPREADSHEET, Set.of(FAILURE_ROLE), EVENT, TARGET_FIELDS);

        assertThat(decisions).hasSize(TARGET_FIELDS.size());
        assertThat(decisions)
            .extracting(DefinitionSpreadsheetHarness.FieldDecision::fieldId)
            .containsExactlyElementsOf(TARGET_FIELDS);
        assertThat(decisions)
            .allSatisfy(decision -> {
                assertThat(decision.isReturned()).isFalse();
                assertThat(decision.details())
                    .contains("No read access in AuthorisationCaseField for supplied roles")
                    .contains("case type: " + ASYLUM);
            });
    }

    @Test
    void shouldFailWhenCaseEventToFieldsHeaderRowCannotBeFound() throws Exception {
        Path invalidSpreadsheet = testResourcePath(INVALID_DEFINITION_SPREADSHEET);

        assertThatThrownBy(() -> DefinitionSpreadsheetHarness.run(
            invalidSpreadsheet,
            Set.of(SUCCESS_ROLE),
            EVENT,
            TARGET_FIELDS
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Header row not found for sheet: CaseEventToFields");
    }

    @Test
    void shouldReadHeadersWhenTheyAreOnFirstRow() throws Exception {
        Path spreadsheet = testResourcePath(FIRST_ROW_HEADERS_DEFINITION_SPREADSHEET);
        List<DefinitionSpreadsheetHarness.FieldDecision> decisions =
            DefinitionSpreadsheetHarness.run(
                spreadsheet,
                Set.of(SUCCESS_ROLE),
                EVENT,
                List.of("isFeePaymentEnabled")
            );

        assertThat(decisions).hasSize(1);
        assertThat(decisions.get(0).isReturned()).isTrue();
        assertThat(decisions.get(0).details()).contains("case type: " + ASYLUM);
    }

    @Test
    void shouldFailWhenNoValidRolesProvided() {
        assertThatThrownBy(() -> DefinitionSpreadsheetHarness.run(
            KNOWN_SPREADSHEET,
            Set.of(),
            EVENT,
            TARGET_FIELDS
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No valid roles provided in -Droles");
    }

    @Test
    void shouldFailWhenNoValidTargetFieldsProvided() {
        assertThatThrownBy(() -> DefinitionSpreadsheetHarness.run(
            KNOWN_SPREADSHEET,
            Set.of(SUCCESS_ROLE),
            EVENT,
            List.of()
        ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("No valid target fields provided in -Dtarget.fields");
    }
}
