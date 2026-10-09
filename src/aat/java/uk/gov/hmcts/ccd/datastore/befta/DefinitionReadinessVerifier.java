package uk.gov.hmcts.ccd.datastore.befta;

import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DefinitionReadinessVerifier {

    private DefinitionReadinessVerifier() {
    }

    static void verify(Response response, DefinitionReadinessSpec spec) {
        if (response.getStatusCode() != 200) {
            throw new IllegalStateException("Could not verify " + spec.caseTypeId()
                + " after data setup. Definition Store returned HTTP " + response.getStatusCode()
                + ": " + response.getBody().asString());
        }

        verify(response.jsonPath(), spec);
    }

    static void verify(JsonPath jsonPath, DefinitionReadinessSpec spec) {
        verifyVisibleFields(jsonPath, spec);
        spec.requiredFields().forEach(field -> verifyCrudAccess(jsonPath, spec, field));
        spec.definitionEventIds().forEach(eventId -> verifyRequiredEvent(jsonPath, spec, eventId));
    }

    static void verifyVisibleFields(JsonPath jsonPath, DefinitionReadinessSpec spec) {
        spec.requiredFields().forEach(field -> verifyRequiredField(jsonPath, spec, field));
    }

    private static void verifyRequiredField(JsonPath jsonPath,
                                            DefinitionReadinessSpec spec,
                                            DefinitionReadinessSpec.RequiredField field) {
        String fieldPath = String.format("case_fields.find { it.id == '%s' }", field.id());
        Object caseField = jsonPath.get(fieldPath);
        if (!(caseField instanceof Map)) {
            throw new IllegalStateException(spec.caseTypeId() + " is missing required " + field.type()
                + " field " + field.id() + " after data setup.");
        }

        String fieldType = jsonPath.getString(fieldPath + ".field_type.type");
        if (!field.type().equals(fieldType)) {
            throw new IllegalStateException(spec.caseTypeId() + " field " + field.id()
                + " must be type " + field.type() + " after data setup but was " + fieldType + ".");
        }
    }

    private static void verifyCrudAccess(JsonPath jsonPath,
                                         DefinitionReadinessSpec spec,
                                         DefinitionReadinessSpec.RequiredField field) {
        String aclPath = String.format(
            "case_fields.find { it.id == '%s' }.acls.find { it.role == '%s' }",
            field.id(),
            spec.accessRole()
        );
        Object acl = jsonPath.get(aclPath);
        if (!(acl instanceof Map)) {
            throw new IllegalStateException(spec.caseTypeId() + " field " + field.id()
                + " is missing " + spec.accessRole() + " ACL after data setup.");
        }

        if (!Boolean.TRUE.equals(jsonPath.getBoolean(aclPath + ".create"))
            || !Boolean.TRUE.equals(jsonPath.getBoolean(aclPath + ".read"))
            || !Boolean.TRUE.equals(jsonPath.getBoolean(aclPath + ".update"))
            || !Boolean.TRUE.equals(jsonPath.getBoolean(aclPath + ".delete"))) {
            throw new IllegalStateException(spec.caseTypeId() + " field " + field.id()
                + " must grant CRUD to " + spec.accessRole() + " after data setup.");
        }
    }

    private static void verifyRequiredEvent(JsonPath jsonPath, DefinitionReadinessSpec spec, String eventId) {
        String eventPath = String.format("events.find { it.id == '%s' }", eventId);
        Object event = jsonPath.get(eventPath);
        if (!(event instanceof Map)) {
            throw new IllegalStateException(spec.caseTypeId() + " is missing required event "
                + eventId + " after data setup.");
        }

        Set<String> activeEventFields = jsonStringSet(jsonPath, eventPath + ".case_fields.case_field_id");
        spec.requiredFields().forEach(field -> {
            if (!activeEventFields.contains(field.id())) {
                throw new IllegalStateException(spec.caseTypeId() + " event " + eventId
                    + " is missing required " + field.type() + " field " + field.id() + " after data setup.");
            }
        });
    }

    private static Set<String> jsonStringSet(JsonPath jsonPath, String path) {
        List<String> values = jsonPath.getList(path, String.class);
        return values == null ? Collections.emptySet() : Set.copyOf(values);
    }
}
