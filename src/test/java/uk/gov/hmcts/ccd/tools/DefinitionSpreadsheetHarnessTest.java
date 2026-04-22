package uk.gov.hmcts.ccd.tools;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
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

    private static final Path KNOWN_SPREADSHEET =
        Path.of(System.getProperty("user.dir"))
            .resolve("ccd-appeal-config-preview-pr3017.xlsx")
            .toAbsolutePath();

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
    void shouldFailWhenCaseEventToFieldsHeaderRowCannotBeFound(@TempDir Path tempDir) throws Exception {
        Path invalidSpreadsheet = tempDir.resolve("invalid-definition.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             OutputStream outputStream = Files.newOutputStream(invalidSpreadsheet)) {
            workbook.createSheet("CaseEventToFields").createRow(0).createCell(0).setCellValue("not-a-header");
            workbook.createSheet("AuthorisationCaseField").createRow(0).createCell(0).setCellValue("not-a-header");
            workbook.createSheet("RoleToAccessProfiles").createRow(0).createCell(0).setCellValue("not-a-header");
            workbook.write(outputStream);
        }

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
    void shouldReadHeadersWhenTheyAreOnFirstRow(@TempDir Path tempDir) throws Exception {
        Path spreadsheet = tempDir.resolve("first-row-headers.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             OutputStream outputStream = Files.newOutputStream(spreadsheet)) {
            var caseEventSheet = workbook.createSheet("CaseEventToFields");
            var caseEventHeader = caseEventSheet.createRow(0);
            caseEventHeader.createCell(0).setCellValue("caseeventid");
            caseEventHeader.createCell(1).setCellValue("casefieldid");
            caseEventHeader.createCell(2).setCellValue("casetypeid");
            var caseEventData = caseEventSheet.createRow(1);
            caseEventData.createCell(0).setCellValue("editAppealAfterSubmit");
            caseEventData.createCell(1).setCellValue("isFeePaymentEnabled");
            caseEventData.createCell(2).setCellValue("Asylum");

            var authSheet = workbook.createSheet("AuthorisationCaseField");
            var authHeader = authSheet.createRow(0);
            authHeader.createCell(0).setCellValue("casetypeid");
            authHeader.createCell(1).setCellValue("casefieldid");
            authHeader.createCell(2).setCellValue("accessprofile");
            authHeader.createCell(3).setCellValue("crud");
            var authData = authSheet.createRow(1);
            authData.createCell(0).setCellValue("Asylum");
            authData.createCell(1).setCellValue("isFeePaymentEnabled");
            authData.createCell(2).setCellValue("caseworker-ia-admofficer");
            authData.createCell(3).setCellValue("R");

            var roleSheet = workbook.createSheet("RoleToAccessProfiles");
            var roleHeader = roleSheet.createRow(0);
            roleHeader.createCell(0).setCellValue("rolename");
            roleHeader.createCell(1).setCellValue("accessprofiles");
            var roleData = roleSheet.createRow(1);
            roleData.createCell(0).setCellValue("caseworker-ia-admofficer");
            roleData.createCell(1).setCellValue("caseworker-ia-admofficer");

            workbook.write(outputStream);
        }

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
