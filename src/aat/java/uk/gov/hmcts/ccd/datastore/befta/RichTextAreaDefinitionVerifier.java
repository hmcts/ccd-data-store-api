package uk.gov.hmcts.ccd.datastore.befta;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class RichTextAreaDefinitionVerifier {

    static final String MASTER_CASE_TYPE = "FT_MasterCaseType";

    private static final String RICH_TEXT_AREA_TYPE = "RichTextArea";
    private static final String BEFTA_MASTER_CASEWORKER_ROLE = "caseworker-befta_master";
    private static final Set<String> REQUIRED_RICH_TEXT_AREA_FIELDS = Set.of(
        "RichTextAreaField",
        "RichTextAreaMinField"
    );
    private static final Set<String> REQUIRED_RICH_TEXT_AREA_EVENTS = Set.of(
        "createCase",
        "updateCase"
    );

    private RichTextAreaDefinitionVerifier() {
    }

    static void verify(Response response) {
        if (response.getStatusCode() != 200) {
            throw new IllegalStateException("Could not verify " + MASTER_CASE_TYPE
                + " after data setup. Definition Store returned HTTP " + response.getStatusCode()
                + ": " + response.getBody().asString());
        }

        verify(response.jsonPath());
    }

    static void verify(JsonPath jsonPath) {
        REQUIRED_RICH_TEXT_AREA_FIELDS.forEach(fieldId -> verifyRequiredRichTextAreaField(jsonPath, fieldId));
        REQUIRED_RICH_TEXT_AREA_EVENTS.forEach(eventId -> verifyRequiredRichTextAreaEvent(jsonPath, eventId));
    }

    private static void verifyRequiredRichTextAreaField(JsonPath jsonPath, String fieldId) {
        String fieldPath = String.format("case_fields.find { it.id == '%s' }", fieldId);
        Object caseField = jsonPath.get(fieldPath);
        if (!(caseField instanceof Map)) {
            throw new IllegalStateException(MASTER_CASE_TYPE + " is missing required RichTextArea field "
                + fieldId + " after data setup.");
        }

        String fieldType = jsonPath.getString(fieldPath + ".field_type.id");
        if (!RICH_TEXT_AREA_TYPE.equals(fieldType)) {
            throw new IllegalStateException(MASTER_CASE_TYPE + " field " + fieldId
                + " must be type " + RICH_TEXT_AREA_TYPE + " after data setup but was " + fieldType + ".");
        }

        verifyCaseworkerCrudAccess(jsonPath, fieldId);
    }

    private static void verifyCaseworkerCrudAccess(JsonPath jsonPath, String fieldId) {
        String aclPath = String.format(
            "case_fields.find { it.id == '%s' }.acls.find { it.role == '%s' }",
            fieldId,
            BEFTA_MASTER_CASEWORKER_ROLE
        );
        Object acl = jsonPath.get(aclPath);
        if (!(acl instanceof Map)) {
            throw new IllegalStateException(MASTER_CASE_TYPE + " field " + fieldId
                + " is missing " + BEFTA_MASTER_CASEWORKER_ROLE + " ACL after data setup.");
        }

        if (!Boolean.TRUE.equals(jsonPath.getBoolean(aclPath + ".create"))
            || !Boolean.TRUE.equals(jsonPath.getBoolean(aclPath + ".read"))
            || !Boolean.TRUE.equals(jsonPath.getBoolean(aclPath + ".update"))
            || !Boolean.TRUE.equals(jsonPath.getBoolean(aclPath + ".delete"))) {
            throw new IllegalStateException(MASTER_CASE_TYPE + " field " + fieldId
                + " must grant CRUD to " + BEFTA_MASTER_CASEWORKER_ROLE + " after data setup.");
        }
    }

    private static void verifyRequiredRichTextAreaEvent(JsonPath jsonPath, String eventId) {
        String eventPath = String.format("events.find { it.id == '%s' }", eventId);
        Object event = jsonPath.get(eventPath);
        if (!(event instanceof Map)) {
            throw new IllegalStateException(MASTER_CASE_TYPE + " is missing required RichTextArea event "
                + eventId + " after data setup.");
        }

        Set<String> activeEventFields = jsonStringSet(jsonPath, eventPath + ".case_fields.case_field_id");
        REQUIRED_RICH_TEXT_AREA_FIELDS.forEach(fieldId -> {
            if (!activeEventFields.contains(fieldId)) {
                throw new IllegalStateException(MASTER_CASE_TYPE + " event " + eventId
                    + " is missing required RichTextArea field " + fieldId + " after data setup.");
            }
        });
    }

    private static Set<String> jsonStringSet(JsonPath jsonPath, String path) {
        List<String> values = jsonPath.getList(path, String.class);
        return values == null ? Collections.emptySet() : Set.copyOf(values);
    }
}
