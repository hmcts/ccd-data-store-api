package uk.gov.hmcts.ccd.datastore.befta;

import java.util.List;

final class DefinitionReadinessSpec {

    private final String caseTypeId;
    private final String dataStoreReadinessEventId;
    private final String accessRole;
    private final String userEmail;
    private final String userPasswordEnvironmentVariable;
    private final List<RequiredField> requiredFields;
    private final List<String> definitionEventIds;

    DefinitionReadinessSpec(String caseTypeId,
                            String dataStoreReadinessEventId,
                            String accessRole,
                            String userEmail,
                            String userPasswordEnvironmentVariable,
                            List<RequiredField> requiredFields,
                            List<String> definitionEventIds) {
        this.caseTypeId = caseTypeId;
        this.dataStoreReadinessEventId = dataStoreReadinessEventId;
        this.accessRole = accessRole;
        this.userEmail = userEmail;
        this.userPasswordEnvironmentVariable = userPasswordEnvironmentVariable;
        this.requiredFields = List.copyOf(requiredFields);
        this.definitionEventIds = List.copyOf(definitionEventIds);
    }

    String caseTypeId() {
        return caseTypeId;
    }

    String dataStoreReadinessEventId() {
        return dataStoreReadinessEventId;
    }

    String accessRole() {
        return accessRole;
    }

    String userEmail() {
        return userEmail;
    }

    String userPasswordEnvironmentVariable() {
        return userPasswordEnvironmentVariable;
    }

    List<RequiredField> requiredFields() {
        return requiredFields;
    }

    List<String> definitionEventIds() {
        return definitionEventIds;
    }

    static final class RequiredField {

        private final String id;
        private final String type;

        RequiredField(String id, String type) {
            this.id = id;
            this.type = type;
        }

        String id() {
            return id;
        }

        String type() {
            return type;
        }
    }
}
