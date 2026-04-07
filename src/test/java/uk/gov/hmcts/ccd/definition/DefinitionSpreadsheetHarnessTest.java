package uk.gov.hmcts.ccd.definition;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Harness-style test that inspects a definition spreadsheet and reports whether
 * specific fields are readable for the supplied roles on a fixed event.
 *
 * <p>Required system properties (parameters):</p>
 * <ul>
 *   <li>{@code -Ddefinition.file}: absolute or relative path to the definition XLSX.</li>
 *   <li>{@code -Droles}: comma-separated list of roles (idam: prefix allowed).</li>
 *   <li>{@code -Dtarget.fields}: comma-separated list of case field IDs to check.</li>
 * </ul>
 *
 * <p>Process:</p>
 * <ol>
 *   <li>Load the {@code CaseEventToFields}, {@code AuthorisationCaseField}, and
 *       {@code RoleToAccessProfiles} sheets.</li>
 *   <li>Translate input roles into access profiles via {@code RoleToAccessProfiles}.</li>
 *   <li>For each target field, find matching event/field rows for the fixed event ID.</li>
 *   <li>For each matching case type, derive CaseViewField visibility from {@code AuthorisationCaseField} CRUD.</li>
 *   <li>Report decisions and fail the test if any field is not returned.</li>
 * </ol>
 */
class DefinitionSpreadsheetHarnessTest {

    private static final Logger LOG = LogManager.getLogger(DefinitionSpreadsheetHarnessTest.class);
    private static final String SPREADSHEET_PATH_PROPERTY = "definition.file";
    private static final String ROLES_PROPERTY = "roles";
    private static final String TARGET_FIELDS_PROPERTY = "target.fields";
    private static final String EVENT_ID = "editAppealAfterSubmit";
    private static final String ROLES_TO_ACCESS_PROFILE_SHEET = "RoleToAccessProfiles";

    @Test
    void shouldReportIfFieldsReturnedForRoles() throws Exception {
        String spreadsheetPath = System.getProperty(SPREADSHEET_PATH_PROPERTY);
        String rolesCsv = System.getProperty(ROLES_PROPERTY);
        String fieldsCsv = System.getProperty(TARGET_FIELDS_PROPERTY);

        assertThat("Missing -D" + SPREADSHEET_PATH_PROPERTY + "=<path-to-xlsx>",
            spreadsheetPath, is(notNullValue()));
        assertThat("Missing -D" + ROLES_PROPERTY + "=role1,role2",
            rolesCsv, is(notNullValue()));
        assertThat("Missing -D" + TARGET_FIELDS_PROPERTY + "=field1,field2",
            fieldsCsv, is(notNullValue()));
        assertThat("Blank -D" + SPREADSHEET_PATH_PROPERTY + " value",
            spreadsheetPath.isBlank(), is(false));
        assertThat("Blank -D" + ROLES_PROPERTY + " value",
            rolesCsv.isBlank(), is(false));
        assertThat("Blank -D" + TARGET_FIELDS_PROPERTY + " value",
            fieldsCsv.isBlank(), is(false));

        Set<String> roles = parseRoles(rolesCsv);
        List<String> targetFields = parseFields(fieldsCsv);

        List<FieldDecision> decisions = evaluateSpreadsheet(
            Path.of(spreadsheetPath),
            roles,
            EVENT_ID,
            targetFields
        );

        decisions.forEach(decision -> LOG.info(decision.format()));

        List<FieldDecision> failures = decisions.stream()
            .filter(decision -> !decision.isReturned())
            .collect(Collectors.toList());

        assertThat("Some fields are not returned for the supplied roles: "
                + failures.stream().map(FieldDecision::fieldId).collect(Collectors.joining(", ")),
            failures.isEmpty(), is(true));
    }

    private static Set<String> parseRoles(String rolesCsv) {
        Set<String> roles = new HashSet<>();
        for (String role : rolesCsv.split(",")) {
            String trimmed = normalizeRole(role);
            if (!trimmed.isEmpty()) {
                roles.add(trimmed);
            }
        }
        return roles;
    }

    private static List<String> parseFields(String fieldsCsv) {
        List<String> fields = new ArrayList<>();
        for (String field : fieldsCsv.split(",")) {
            String trimmed = field.trim();
            if (!trimmed.isEmpty()) {
                fields.add(trimmed);
            }
        }
        return fields;
    }

    /**
     * Evaluates the spreadsheet against the provided inputs.
     *
     * @param spreadsheetPath path to the XLSX definition file
     * @param roles role names to translate into access profiles
     * @param eventId event ID to match in {@code CaseEventToFields}
     * @param fieldIds case field IDs to evaluate
     */
    private static List<FieldDecision> evaluateSpreadsheet(Path spreadsheetPath,
                                                           Set<String> roles,
                                                           String eventId,
                                                           List<String> fieldIds) throws Exception {
        LOG.info("Evaluating spreadsheet: {}", spreadsheetPath);
        LOG.info("Event ID: {}", eventId);
        LOG.info("Roles: {}", String.join(", ", roles));
        LOG.info("Target fields: {}", String.join(", ", fieldIds));
        try (InputStream inputStream = Files.newInputStream(spreadsheetPath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            List<Map<String, String>> eventToFields = readSheet(workbook, "CaseEventToFields");
            List<Map<String, String>> authCaseFields = readSheet(workbook, "AuthorisationCaseField");
            List<Map<String, String>> rolesToAccessProfiles = readSheet(workbook, ROLES_TO_ACCESS_PROFILE_SHEET);
            Set<String> accessProfiles = translateRolesToAccessProfiles(roles, rolesToAccessProfiles);
            LOG.info("Rows loaded: CaseEventToFields={}, AuthorisationCaseField={}",
                eventToFields.size(), authCaseFields.size());
            LOG.info("Rows loaded: RolesToAccessProfile={}, accessProfiles={}",
                rolesToAccessProfiles.size(), String.join(", ", accessProfiles));
            if (!rolesToAccessProfiles.isEmpty()) {
                LOG.info("RoleToAccessProfiles headers: {}",
                    String.join(", ", rolesToAccessProfiles.get(0).keySet()));
            }
            if (!eventToFields.isEmpty()) {
                LOG.info("CaseEventToFields headers: {}",
                    String.join(", ", eventToFields.get(0).keySet()));
                LOG.info("CaseEventToFields sample caseeventid values: {}",
                    eventToFields.stream()
                        .map(row -> row.get("caseeventid"))
                        .filter(value -> value != null && !value.isBlank())
                        .distinct()
                        .limit(5)
                        .collect(Collectors.joining(", ")));
            }
            if (!authCaseFields.isEmpty()) {
                LOG.info("AuthorisationCaseField headers: {}",
                    String.join(", ", authCaseFields.get(0).keySet()));
            }

            List<FieldDecision> decisions = new ArrayList<>();
            for (String fieldId : fieldIds) {
                decisions.add(evaluateField(fieldId, accessProfiles, eventId, eventToFields, authCaseFields));
            }
            LOG.info("Decisions evaluated: {}", decisions.size());
            return decisions;
        }
    }

    /**
     * Determines whether a field is viewable for any case type linked to the event.
     *
     * @param fieldId case field ID to check
     * @param accessProfiles translated access profiles for the input roles
     * @param eventId event ID to match in {@code CaseEventToFields}
     * @param eventToFields rows from {@code CaseEventToFields}
     * @param authCaseFields rows from {@code AuthorisationCaseField}
     */
    private static FieldDecision evaluateField(String fieldId,
                                               Set<String> accessProfiles,
                                               String eventId,
                                               List<Map<String, String>> eventToFields,
                                               List<Map<String, String>> authCaseFields) {
        LOG.info("Evaluating field: {} (eventId={})", fieldId, eventId);
        long totalEventToFields = eventToFields.size();
        List<Map<String, String>> eventIdMatches = eventToFields.stream()
            .filter(row -> equalsIgnoreCase(row.get("caseeventid"), eventId))
            .collect(Collectors.toList());
        List<Map<String, String>> eventAndFieldMatches = eventIdMatches.stream()
            .filter(row -> equalsIgnoreCase(row.get("casefieldid"), fieldId))
            .collect(Collectors.toList());
        List<String> caseTypesForEventField = eventAndFieldMatches.stream()
            .map(row -> nullSafeTrim(row.get("casetypeid")))
            .filter(value -> !value.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        LOG.info("CaseEventToFields rows: total={}, eventIdMatch={}, eventId+fieldMatch={}, caseTypes={}",
            totalEventToFields, eventIdMatches.size(), eventAndFieldMatches.size(),
            String.join(", ", caseTypesForEventField));

        if (caseTypesForEventField.isEmpty()) {
            LOG.info("No CaseEventToFields row found for event/field");
            return FieldDecision.notReturned(fieldId, "No CaseEventToFields row found for event/field");
        }

        for (String caseTypeId : caseTypesForEventField) {
            LOG.info("Checking case type: {}", caseTypeId);
            List<Map<String, String>> caseTypeFieldRows = authCaseFields.stream()
                .filter(row -> equalsIgnoreCase(rowValue(row, "casetypeid", "case type id", "case type"), caseTypeId))
                .filter(row -> equalsIgnoreCase(rowValue(row, "casefieldid", "case field id", "case field"), fieldId))
                .collect(Collectors.toList());
            LOG.info("AuthorisationCaseField rows for caseType/field: {}", caseTypeFieldRows.size());
            if (caseTypeFieldRows.isEmpty()) {
                LOG.info("No matching AuthorisationCaseField rows for caseType={} field={}", caseTypeId, fieldId);
            }

            boolean viewable = canAccessCaseViewFieldWithCriteria(caseTypeFieldRows, accessProfiles);

            LOG.info("Derived CaseViewField visibility from CRUD: {}", viewable);
            if (viewable) {
                LOG.info("Viewable for accessProfiles {} in case type {}",
                    String.join(", ", accessProfiles), caseTypeId);
                return FieldDecision.returned(fieldId, caseTypeId);
            }
        }


        LOG.info("No read access for accessProfiles {} in case types {}", String.join(", ", accessProfiles),
            String.join(", ", caseTypesForEventField));
        return FieldDecision.notReturned(fieldId,
            "No read access in AuthorisationCaseField for supplied roles and matching case types: "
                + String.join(", ", caseTypesForEventField));
    }

    private static boolean canAccessCaseViewFieldWithCriteria(List<Map<String, String>> accessControlRows,
                                                              Set<String> accessProfiles) {
        Set<AccessProfile> profileSet = accessProfiles.stream()
            .map(AccessProfile::new)
            .collect(Collectors.toSet());
        LOG.info("Evaluating ACLs for accessProfiles: {}", String.join(", ", accessProfiles));
        LOG.info("AccessControl rows count: {}", accessControlRows.size());
        List<AccessControlList> accessControlLists = accessControlRows.stream()
            .map(DefinitionSpreadsheetHarnessTest::toAccessControlList)
            .collect(Collectors.toList());
        for (AccessControlList acl : accessControlLists) {
            LOG.info("ACL: accessProfile={}, create={}, read={}, update={}, delete={}",
                acl.getAccessProfile(), acl.isCreate(), acl.isRead(), acl.isUpdate(), acl.isDelete());
        }
        boolean hasAccess = AccessControlService.hasAccessControlList(profileSet,
            accessControlLists,
            AccessControlService.CAN_READ);
        LOG.info("ACL read access result: {}", hasAccess);
        return hasAccess;
    }

    private static AccessControlList toAccessControlList(Map<String, String> row) {
        AccessControlList accessControlList = new AccessControlList();
        accessControlList.setAccessProfile(nullSafeTrim(rowValue(row, "accessprofile", "access profile")));
        String crud = rowValue(row, "crud");
        if (crud != null) {
            String upper = crud.toUpperCase(Locale.ROOT);
            accessControlList.setCreate(upper.contains("C"));
            accessControlList.setRead(upper.contains("R"));
            accessControlList.setUpdate(upper.contains("U"));
            accessControlList.setDelete(upper.contains("D"));
        }
        return accessControlList;
    }

    private static List<Map<String, String>> readSheet(Workbook workbook, String sheetName) {
        LOG.info("Reading sheet: {}", sheetName);
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException("Missing sheet: " + sheetName);
        }

        DataFormatter formatter = new DataFormatter();
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            throw new IllegalArgumentException("Missing header row in sheet: " + sheetName);
        }

        Map<Integer, String> headersByIndex = readHeaders(headerRow, formatter);
        LOG.info("Initial header row index for {}: {}", sheetName, headerRow.getRowNum());
        if (ROLES_TO_ACCESS_PROFILE_SHEET.equals(sheetName)
            && !isRoleToAccessProfilesHeader(headersByIndex)) {
            headerRow = findHeaderRow(sheet, headerRow.getRowNum() + 1, formatter, thisRowHeaders ->
                isRoleToAccessProfilesHeader(thisRowHeaders));
            if (headerRow != null) {
                headersByIndex = readHeaders(headerRow, formatter);
                LOG.info("Adjusted RoleToAccessProfiles header row to index {}", headerRow.getRowNum());
            }
        }
        if ("CaseEventToFields".equals(sheetName)
            && !isCaseEventToFieldsHeader(headersByIndex)) {
            headerRow = findHeaderRow(sheet, headerRow.getRowNum() + 1, formatter, thisRowHeaders ->
                isCaseEventToFieldsHeader(thisRowHeaders));
            if (headerRow != null) {
                headersByIndex = readHeaders(headerRow, formatter);
                LOG.info("Adjusted CaseEventToFields header row to index {}", headerRow.getRowNum());
            }
        }
        if ("AuthorisationCaseField".equals(sheetName)
            && !isAuthorisationCaseFieldHeader(headersByIndex)) {
            headerRow = findHeaderRow(sheet, headerRow.getRowNum() + 1, formatter, thisRowHeaders ->
                isAuthorisationCaseFieldHeader(thisRowHeaders));
            if (headerRow != null) {
                headersByIndex = readHeaders(headerRow, formatter);
                LOG.info("Adjusted AuthorisationCaseField header row to index {}", headerRow.getRowNum());
            }
        }
        LOG.info("Using header row index for {}: {}", sheetName, headerRow.getRowNum());

        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = headerRow.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }
            Map<String, String> rowData = new HashMap<>();
            boolean hasValue = false;
            for (Map.Entry<Integer, String> entry : headersByIndex.entrySet()) {
                String value = formatter.formatCellValue(row.getCell(entry.getKey())).trim();
                if (!value.isEmpty()) {
                    hasValue = true;
                }
                rowData.put(entry.getValue(), value);
            }
            if (hasValue) {
                rows.add(rowData);
            }
        }
        LOG.info("Loaded {} data rows from sheet {}", rows.size(), sheetName);
        return rows;
    }

    private static boolean equalsIgnoreCase(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private static String nullSafeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private static Map<Integer, String> readHeaders(Row headerRow, DataFormatter formatter) {
        Map<Integer, String> headersByIndex = new HashMap<>();
        headerRow.forEach(cell -> {
            String header = formatter.formatCellValue(cell).trim();
            if (!header.isEmpty()) {
                headersByIndex.put(cell.getColumnIndex(), header.toLowerCase(Locale.ROOT));
            }
        });
        return headersByIndex;
    }

    private static boolean isRoleToAccessProfilesHeader(Map<Integer, String> headersByIndex) {
        return headersByIndex.containsValue("rolename")
            && headersByIndex.containsValue("accessprofiles");
    }

    private static boolean isCaseEventToFieldsHeader(Map<Integer, String> headersByIndex) {
        return headersByIndex.containsValue("caseeventid")
            && headersByIndex.containsValue("casefieldid")
            && headersByIndex.containsValue("casetypeid");
    }

    private static boolean isAuthorisationCaseFieldHeader(Map<Integer, String> headersByIndex) {
        boolean hasCaseType = headersByIndex.containsValue("casetypeid")
            || headersByIndex.containsValue("case type id")
            || headersByIndex.containsValue("case type");
        boolean hasCaseField = headersByIndex.containsValue("casefieldid")
            || headersByIndex.containsValue("case field id")
            || headersByIndex.containsValue("case field");
        boolean hasAccessProfile = headersByIndex.containsValue("accessprofile")
            || headersByIndex.containsValue("access profile");
        boolean hasCrud = headersByIndex.containsValue("crud");
        return hasCaseType && hasCaseField && hasAccessProfile && hasCrud;
    }

    private static Row findHeaderRow(Sheet sheet,
                                     int startRow,
                                     DataFormatter formatter,
                                     java.util.function.Predicate<Map<Integer, String>> matcher) {
        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row candidateRow = sheet.getRow(i);
            if (candidateRow == null) {
                continue;
            }
            Map<Integer, String> candidateHeaders = readHeaders(candidateRow, formatter);
            if (matcher.test(candidateHeaders)) {
                return candidateRow;
            }
        }
        return null;
    }

    /**
     * Maps role names to access profile names using the {@code RoleToAccessProfiles} sheet.
     *
     * @param roles input role names (normalized)
     * @param rolesToAccessProfiles sheet rows with role/profile mappings
     */
    private static Set<String> translateRolesToAccessProfiles(Set<String> roles,
                                                              List<Map<String, String>> rolesToAccessProfiles) {
        Set<String> accessProfiles = new HashSet<>();
        LOG.info("Translating roles to access profiles from sheet: {}",
            ROLES_TO_ACCESS_PROFILE_SHEET);
        for (Map<String, String> row : rolesToAccessProfiles) {
            String rawRoleName = rowValue(row, "rolename", "role name", "role");
            String roleName = normalizeRole(rawRoleName);
            if (!roleName.isEmpty() && roles.contains(roleName)) {
                LOG.info("Matched role: raw='{}', normalized='{}'", rawRoleName, roleName);
                String profilesValue = nullSafeTrim(rowValue(row, "accessprofiles", "accessprofile"));
                if (!profilesValue.isEmpty()) {
                    for (String profile : profilesValue.split("[,;]")) {
                        String trimmed = profile.trim();
                        if (!trimmed.isEmpty()) {
                            accessProfiles.add(trimmed);
                            LOG.info("Added access profile: {}", trimmed);
                        }
                    }
                } else {
                    LOG.info("Matched role '{}' but accessProfiles is empty", roleName);
                }
            }
        }
        if (accessProfiles.isEmpty()) {
            LOG.info("No access profiles found for roles {}", String.join(", ", roles));
        }
        return accessProfiles;
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String trimmed = role.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith("idam:")) {
            return trimmed.substring("idam:".length()).trim();
        }
        return trimmed;
    }

    private static String rowValue(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(key);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private record FieldDecision(String fieldId, boolean returned, String details) {
        static FieldDecision returned(String fieldId, String caseTypeId) {
            return new FieldDecision(fieldId, true, "Returned (case type: " + caseTypeId + ")");
        }

        static FieldDecision notReturned(String fieldId, String reason) {
            return new FieldDecision(fieldId, false, "Not returned: " + reason);
        }

        boolean isReturned() {
            return returned;
        }

        String format() {
            return fieldId + " -> " + details;
        }
    }
}
