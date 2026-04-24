package uk.gov.hmcts.ccd.tools;

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import lombok.extern.slf4j.Slf4j;

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

/**
 * Harness that inspects a definition spreadsheet and reports whether
 * specific fields are readable for the supplied roles on a supplied event.
 * If readable, the field will be returned.
 * <p>Required system properties (parameters):</p>
 * <ul>
 *   <li>{@code -Ddefinition.file}: absolute or relative path to the definition XLSX.</li>
 *   <li>{@code -Droles}: comma-separated list of roles ("idam:" prefix for a role is allowed).</li>
 *   <li>{@code -Dtarget.fields}: comma-separated list of case field IDs to check.</li>
 *   <li>{@code -Devent.id}: page event id.</li>
 * </ul>
 * <p/>
 * Example of command line arguments to run  from IntelliJ:
 * -ea
 * -Dtarget.fields=isFeePaymentEnabled,sponsorEmailAdminJ,sponsorMobileNumberAdminJ,sponsorAddress
 * -Ddefinition.file=/ccd-appeal-config-preview-pr3017.xlsx
 * -Droles=caseworker-ia-admofficer,hmcts-admin
 * -Devent.id=editAppealAfterSubmit
 * <p/>
 * <p>Process:</p>
 * <ol>
 *   <li>Load the {@code CaseEventToFields}, {@code AuthorisationCaseField}, and
 *       {@code RoleToAccessProfiles} sheets and detect header rows.</li>
 *   <li>Translate input roles into access profiles via {@code RoleToAccessProfiles}.</li>
 *   <li>For each target field, find matching case types for the supplied event.</li>
 *   <li>Evaluate read access for each {@code (fieldId, caseTypeId)} pair from
 *       {@code AuthorisationCaseField} CRUD.</li>
 *   <li>Emit one decision per evaluated pair and fail if any decision is not returned.</li>
 * </ol>
 * <p>Exit codes:</p>
 * <ul>
 *   <li>{@code 0}: all decisions returned.</li>
 *   <li>{@code 1}: invalid/missing input or spreadsheet structure.</li>
 *   <li>{@code 2}: at least one field decision is not returned.</li>
 * </ul>
 * Success: field -> Returned
 * Failure: field -> Not returned: No read access
 */
@Slf4j
public class DefinitionSpreadsheetHarness {

    private static final String ACCESS_PROFILE_COLUMN = "accessprofile";
    private static final String ACCESS_PROFILES_COLUMN = "accessprofiles";
    private static final String AUTHORISATION_CASE_FIELD_SHEET = "AuthorisationCaseField";
    private static final String CASE_EVENT_ID_COLUMN = "caseeventid";
    private static final String CASE_EVENT_TO_FIELDS_SHEET = "CaseEventToFields";
    private static final String CASE_FIELD_ID_COLUMN = "casefieldid";
    private static final String CASE_TYPE_ID_COLUMN = "casetypeid";
    private static final String EVENT_ID_PROPERTY = "event.id";
    private static final String IDAM_PREFIX = "idam:";
    private static final String ROLE_NAME_COLUMN = "rolename";
    private static final String ROLES_PROPERTY = "roles";
    private static final String ROLES_TO_ACCESS_PROFILE_SHEET = "RoleToAccessProfiles";
    private static final String SPREADSHEET_PATH_PROPERTY = "definition.file";
    private static final String TARGET_FIELDS_PROPERTY = "target.fields";

    public static void main(String[] args) {
        try {
            List<FieldDecision> failures = runFromSystemProperties();
            if (failures.isEmpty()) {
                log.info("All target fields are returned for the supplied roles.");
            } else {
                log.error("Some fields are not returned for the supplied roles (count={}): {}",
                    failures.size(),
                    failures.stream().map(FieldDecision::fieldId).distinct().collect(Collectors.joining(", ")));
                failures.forEach(failure -> log.error(failure.format()));
                System.exit(2);
            }
        } catch (IllegalArgumentException error) {
            log.error(error.getMessage());
            System.exit(1);
        } catch (Exception error) {
            log.error("Unexpected failure running definition spreadsheet harness", error);
            System.exit(1);
        }
    }

    static List<FieldDecision> run(Path spreadsheetPath,
                                   Set<String> roles,
                                   String eventId,
                                   List<String> fieldIds) throws Exception {
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("No valid roles provided in -D" + ROLES_PROPERTY);
        }
        if (fieldIds == null || fieldIds.isEmpty()) {
            throw new IllegalArgumentException("No valid target fields provided in -D" + TARGET_FIELDS_PROPERTY);
        }
        return evaluateSpreadsheet(spreadsheetPath, roles, eventId, fieldIds);
    }

    private static List<FieldDecision> runFromSystemProperties() throws Exception {
        String spreadsheetPath = requireProperty(SPREADSHEET_PATH_PROPERTY,
            "Missing -D" + SPREADSHEET_PATH_PROPERTY + "=path-to-xlsx");
        String rolesCsv = requireProperty(ROLES_PROPERTY,
            "Missing -D" + ROLES_PROPERTY + "=role1,role2");
        String fieldsCsv = requireProperty(TARGET_FIELDS_PROPERTY,
            "Missing -D" + TARGET_FIELDS_PROPERTY + "=field1,field2");
        String eventId = requireProperty(EVENT_ID_PROPERTY,
            "Missing -D" + EVENT_ID_PROPERTY + "=eventId");

        Set<String> roles = parseRoles(rolesCsv);
        List<String> targetFields = parseFields(fieldsCsv);
        if (roles.isEmpty()) {
            throw new IllegalArgumentException("No valid roles provided in -D" + ROLES_PROPERTY);
        }
        if (targetFields.isEmpty()) {
            throw new IllegalArgumentException("No valid target fields provided in -D" + TARGET_FIELDS_PROPERTY);
        }

        List<FieldDecision> decisions = run(Path.of(spreadsheetPath), roles, eventId, targetFields);

        decisions.forEach(decision -> log.info(decision.format()));

        return decisions.stream()
            .filter(decision -> !decision.isReturned())
            .collect(Collectors.toList());
    }

    private static String requireProperty(String key, String missingMessage) {
        String value = System.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException(missingMessage);
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("Blank -D" + key + " value");
        }
        return value;
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
        log.info("Evaluating spreadsheet: {}", spreadsheetPath);
        log.info("Event ID: {}", eventId);
        log.info("Roles: {}", String.join(", ", roles));
        log.info("Target fields: {}", String.join(", ", fieldIds));
        try (InputStream inputStream = Files.newInputStream(spreadsheetPath);
             Workbook workbook = WorkbookFactory.create(inputStream)) {

            List<Map<String, String>> eventToFields = tryReadSheet(workbook, CASE_EVENT_TO_FIELDS_SHEET);
            List<Map<String, String>> authCaseFields = tryReadSheet(workbook, AUTHORISATION_CASE_FIELD_SHEET);
            List<Map<String, String>> rolesToAccessProfiles = tryReadSheet(workbook, ROLES_TO_ACCESS_PROFILE_SHEET);
            Set<String> accessProfiles = translateRolesToAccessProfiles(roles, rolesToAccessProfiles);
            log.info("Rows loaded: CaseEventToFields={}, AuthorisationCaseField={}",
                eventToFields.size(), authCaseFields.size());
            log.info("Rows loaded: RolesToAccessProfile={}, accessProfiles={}",
                rolesToAccessProfiles.size(), String.join(", ", accessProfiles));
            if (!rolesToAccessProfiles.isEmpty()) {
                log.info("RoleToAccessProfiles headers: {}",
                    String.join(", ", rolesToAccessProfiles.getFirst().keySet()));
            }
            if (!eventToFields.isEmpty()) {
                log.info("CaseEventToFields headers: {}",
                    String.join(", ", eventToFields.getFirst().keySet()));
                log.info("CaseEventToFields sample {} values: {}",
                    CASE_EVENT_ID_COLUMN,
                    eventToFields.stream()
                        .map(row -> row.get(CASE_EVENT_ID_COLUMN))
                        .filter(value -> value != null && !value.isBlank())
                        .distinct()
                        .limit(5)
                        .collect(Collectors.joining(", ")));
            }
            if (!authCaseFields.isEmpty()) {
                log.info("AuthorisationCaseField headers: {}",
                    String.join(", ", authCaseFields.getFirst().keySet()));
            }

            List<FieldDecision> decisions = new ArrayList<>();
            for (String fieldId : fieldIds) {
                List<String> caseTypesForEventField = findCaseTypesForEventField(fieldId, eventId, eventToFields);
                log.info("field {} - caseTypesForEventField: {}", fieldId, caseTypesForEventField);
                if (caseTypesForEventField.isEmpty()) {
                    decisions.add(FieldDecision.notReturned(fieldId,
                        "No CaseEventToFields row found for event/field"));
                    continue;
                }
                for (String caseTypeId : caseTypesForEventField) {
                    decisions.add(evaluateField(fieldId, caseTypeId, accessProfiles, authCaseFields));
                }
            }
            log.info("Decisions evaluated: {}", decisions.size());
            return decisions;
        }
    }

    /**
     * Determines whether a field is viewable for one specific case type.
     *
     * @param fieldId case field ID to check
     * @param caseTypeId case type ID
     * @param accessProfiles translated access profiles for the input roles
     * @param authCaseFields rows from {@code AuthorisationCaseField}
     */
    private static FieldDecision evaluateField(String fieldId,
                                               String caseTypeId,
                                               Set<String> accessProfiles,
                                               List<Map<String, String>> authCaseFields) {
        log.info("Checking case type: {}", caseTypeId);
        List<Map<String, String>> caseTypeFieldRows = authCaseFields.stream()
            .filter(row -> equalsIgnoreCase(rowValue(row, CASE_TYPE_ID_COLUMN), caseTypeId))
            .filter(row -> equalsIgnoreCase(rowValue(row, CASE_FIELD_ID_COLUMN), fieldId))
            .collect(Collectors.toList());
        log.info("AuthorisationCaseField rows for caseType/field: {}", caseTypeFieldRows.size());
        if (caseTypeFieldRows.isEmpty()) {
            log.info("No matching AuthorisationCaseField rows for caseType={} field={}", caseTypeId, fieldId);
            return FieldDecision.notReturned(fieldId,
                "No matching AuthorisationCaseField rows for case type: " + caseTypeId);
        }

        boolean viewable = canAccessCaseViewFieldWithCriteria(caseTypeFieldRows, accessProfiles);
        log.info("Derived CaseViewField visibility from CRUD: {}", viewable);
        if (viewable) {
            log.info("Viewable for accessProfiles {} in case type {}",
                String.join(", ", accessProfiles), caseTypeId);
            return FieldDecision.returned(fieldId, caseTypeId);
        }
        log.info("No read access for accessProfiles {} in case type {}", String.join(", ", accessProfiles),
            caseTypeId);
        return FieldDecision.notReturned(fieldId,
            "No read access in AuthorisationCaseField for supplied roles in case type: " + caseTypeId);
    }

    /**
     * Finds case types linked to the supplied field and event.
     *
     * @param fieldId case field ID to check
     * @param eventId event ID to match in {@code CaseEventToFields}
     * @param eventToFields rows from {@code CaseEventToFields}
     */
    private static List<String> findCaseTypesForEventField(String fieldId,
                                                           String eventId,
                                                           List<Map<String, String>> eventToFields) {
        log.info("Evaluating field: {} (eventId={})", fieldId, eventId);
        long totalEventToFields = eventToFields.size();
        List<Map<String, String>> eventIdMatches = eventToFields.stream()
            .filter(row -> equalsIgnoreCase(row.get(CASE_EVENT_ID_COLUMN), eventId))
            .toList();
        List<Map<String, String>> eventAndFieldMatches = eventIdMatches.stream()
            .filter(row -> equalsIgnoreCase(row.get(CASE_FIELD_ID_COLUMN), fieldId))
            .toList();
        List<String> caseTypesForEventField = eventAndFieldMatches.stream()
            .map(row -> nullSafeTrim(row.get(CASE_TYPE_ID_COLUMN)))
            .filter(value -> !value.isEmpty())
            .distinct()
            .collect(Collectors.toList());
        log.info("CaseEventToFields rows: total={}, eventIdMatch={}, eventId+fieldMatch={}, caseTypes={}",
            totalEventToFields, eventIdMatches.size(), eventAndFieldMatches.size(),
            String.join(", ", caseTypesForEventField));
        return caseTypesForEventField;
    }

    private static boolean canAccessCaseViewFieldWithCriteria(List<Map<String, String>> accessControlRows,
                                                              Set<String> accessProfiles) {
        log.info("Evaluating ACLs for accessProfiles: {}", String.join(", ", accessProfiles));
        log.info("AccessControl rows count: {}", accessControlRows.size());
        List<AccessControlList> accessControlLists = accessControlRows.stream()
            .map(DefinitionSpreadsheetHarness::toAccessControlList)
            .collect(Collectors.toList());
        for (AccessControlList acl : accessControlLists) {
            log.info("ACL: accessProfile={}, create={}, read={}, update={}, delete={}",
                acl.getAccessProfile(), acl.isCreate(), acl.isRead(), acl.isUpdate(), acl.isDelete());
        }
        Set<AccessProfile> profileSet = accessProfiles.stream()
            .map(AccessProfile::new)
            .collect(Collectors.toSet());
        boolean hasAccess = AccessControlService.hasAccessControlList(profileSet,
            accessControlLists,
            AccessControlService.CAN_READ);
        log.info("ACL read access result: {}", hasAccess);
        return hasAccess;
    }

    private static AccessControlList toAccessControlList(Map<String, String> row) {
        AccessControlList accessControlList = new AccessControlList();
        accessControlList.setAccessProfile(nullSafeTrim(rowValue(row, ACCESS_PROFILE_COLUMN)));
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

    private static List<Map<String, String>> tryReadSheet(Workbook workbook, String sheetName) {
        log.info("Reading sheet: {}", sheetName);
        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new IllegalArgumentException("Missing sheet: " + sheetName);
        }

        DataFormatter formatter = new DataFormatter();
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (headerRow == null) {
            throw new IllegalArgumentException("Missing header row in sheet: " + sheetName);
        } else {
            return readSheet(sheet, sheetName, headerRow, formatter);
        }
    }

    private static List<Map<String, String>> readSheet(Sheet sheet, String sheetName, Row headerRow,
                                                       DataFormatter formatter) {

        Map<Integer, String> headersByIndex = readHeaders(headerRow, formatter);
        log.info("Initial header row index for {}: {}", sheetName, headerRow.getRowNum());
        if (ROLES_TO_ACCESS_PROFILE_SHEET.equals(sheetName)) {
            if (isRoleToAccessProfilesHeader(headersByIndex)) {
                log.info("Using initial RoleToAccessProfiles header row at index {}", headerRow.getRowNum());
            } else {
                Row foundHeaderRow = findHeaderRow(sheet, headerRow.getRowNum() + 1, formatter,
                    DefinitionSpreadsheetHarness::isRoleToAccessProfilesHeader);
                if (foundHeaderRow != null) {
                    headerRow = foundHeaderRow;
                    headersByIndex = readHeaders(headerRow, formatter);
                    log.info("Adjusted RoleToAccessProfiles header row to index {}", headerRow.getRowNum());
                }
            }
        }
        if (CASE_EVENT_TO_FIELDS_SHEET.equals(sheetName)) {
            if (isCaseEventToFieldsHeader(headersByIndex)) {
                log.info("Using initial CaseEventToFields header row at index {}", headerRow.getRowNum());
            } else {
                Row foundHeaderRow = findHeaderRow(sheet, headerRow.getRowNum() + 1, formatter,
                    DefinitionSpreadsheetHarness::isCaseEventToFieldsHeader);
                if (foundHeaderRow != null) {
                    headerRow = foundHeaderRow;
                    headersByIndex = readHeaders(headerRow, formatter);
                    log.info("Adjusted CaseEventToFields header row to index {}", headerRow.getRowNum());
                }
            }
        }
        if (AUTHORISATION_CASE_FIELD_SHEET.equals(sheetName)) {
            if (isAuthorisationCaseFieldHeader(headersByIndex)) {
                log.info("Using initial AuthorisationCaseField header row at index {}", headerRow.getRowNum());
            } else {
                Row foundHeaderRow = findHeaderRow(sheet, headerRow.getRowNum() + 1, formatter,
                    DefinitionSpreadsheetHarness::isAuthorisationCaseFieldHeader);
                if (foundHeaderRow != null) {
                    headerRow = foundHeaderRow;
                    headersByIndex = readHeaders(headerRow, formatter);
                    log.info("Adjusted AuthorisationCaseField header row to index {}", headerRow.getRowNum());
                }
            }
        }
        if (ROLES_TO_ACCESS_PROFILE_SHEET.equals(sheetName) && !isRoleToAccessProfilesHeader(headersByIndex)) {
            throw new IllegalArgumentException("Header row not found for sheet: " + sheetName);
        }
        if (CASE_EVENT_TO_FIELDS_SHEET.equals(sheetName) && !isCaseEventToFieldsHeader(headersByIndex)) {
            throw new IllegalArgumentException("Header row not found for sheet: " + sheetName);
        }
        if (AUTHORISATION_CASE_FIELD_SHEET.equals(sheetName) && !isAuthorisationCaseFieldHeader(headersByIndex)) {
            throw new IllegalArgumentException("Header row not found for sheet: " + sheetName);
        }
        log.info("Using header row index for {}: {}", sheetName, headerRow.getRowNum());

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
        log.info("Loaded {} data rows from sheet {}", rows.size(), sheetName);
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
        return headersByIndex.containsValue(ROLE_NAME_COLUMN)
            && headersByIndex.containsValue(ACCESS_PROFILES_COLUMN);
    }

    private static boolean isCaseEventToFieldsHeader(Map<Integer, String> headersByIndex) {
        return headersByIndex.containsValue(CASE_EVENT_ID_COLUMN)
            && headersByIndex.containsValue(CASE_FIELD_ID_COLUMN)
            && headersByIndex.containsValue(CASE_TYPE_ID_COLUMN);
    }

    private static boolean isAuthorisationCaseFieldHeader(Map<Integer, String> headersByIndex) {
        boolean hasCaseType = headersByIndex.containsValue(CASE_TYPE_ID_COLUMN);
        boolean hasCaseField = headersByIndex.containsValue(CASE_FIELD_ID_COLUMN);
        boolean hasAccessProfile = headersByIndex.containsValue(ACCESS_PROFILE_COLUMN);
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
        Set<String> accessProfiles = new HashSet<>(roles);
        log.info("Translating roles to access profiles from sheet: {}",
            ROLES_TO_ACCESS_PROFILE_SHEET);
        log.info("Seeded direct access profiles from supplied roles: {}", String.join(", ", accessProfiles));
        for (Map<String, String> row : rolesToAccessProfiles) {
            String rawRoleName = rowValue(row, ROLE_NAME_COLUMN);
            String roleName = normalizeRole(rawRoleName);
            if (roles.contains(roleName)) {
                log.info("Matched role: raw='{}', normalized='{}'", rawRoleName, roleName);
                String profilesValue = nullSafeTrim(rowValue(row, ACCESS_PROFILES_COLUMN));
                if (!profilesValue.isEmpty()) {
                    for (String profile : profilesValue.split("[,;]")) {
                        String trimmed = profile.trim();
                        if (!trimmed.isEmpty()) {
                            accessProfiles.add(trimmed);
                            log.info("Added access profile: {}", trimmed);
                        }
                    }
                } else {
                    log.info("Matched role '{}' but accessProfiles is empty", roleName);
                }
            }
        }
        log.info("Resolved access profiles for roles {} -> {}", String.join(", ", roles),
            String.join(", ", accessProfiles));
        return accessProfiles;
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String trimmed = role.trim();
        if (trimmed.toLowerCase(Locale.ROOT).startsWith(IDAM_PREFIX)) {
            return trimmed.substring(IDAM_PREFIX.length()).trim();
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

    record FieldDecision(String fieldId, boolean returned, String details) {
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
