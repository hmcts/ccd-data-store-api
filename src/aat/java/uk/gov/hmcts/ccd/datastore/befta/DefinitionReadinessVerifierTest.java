package uk.gov.hmcts.ccd.datastore.befta;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefinitionReadinessVerifierTest {

    private static final DefinitionReadinessSpec SPEC = new DefinitionReadinessSpec(
        "FT_MasterCaseType",
        "createCase",
        "caseworker-befta_master",
        "master.caseworker@gmail.com",
        "CCD_CASEWORKER_AUTOTEST_PASSWORD",
        List.of(
            new DefinitionReadinessSpec.RequiredField("RichTextAreaField", "RichTextArea"),
            new DefinitionReadinessSpec.RequiredField("RichTextAreaMinField", "RichTextArea")
        ),
        List.of("createCase", "updateCase")
    );

    @Test
    void shouldAcceptDefinitionWithRequiredConfiguration() {
        assertThatCode(() -> DefinitionReadinessVerifier.verify(JsonPath.from(validDefinition()), SPEC))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptVisibleFieldsWithoutDefinitionAclOrEventMappings() {
        assertThatCode(() -> DefinitionReadinessVerifier.verifyVisibleFields(JsonPath.from(visibleFields()), SPEC))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectDefinitionMissingRequiredField() {
        String definition = validDefinition().replace(
            "\"id\": \"RichTextAreaMinField\"",
            "\"id\": \"OtherField\""
        );

        assertThatThrownBy(() -> DefinitionReadinessVerifier.verify(JsonPath.from(definition), SPEC))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("missing required RichTextArea field RichTextAreaMinField");
    }

    @Test
    void shouldRejectDefinitionWhenFieldBaseTypeIsWrong() {
        String definition = validDefinition().replace(
            "\"type\": \"RichTextArea\"",
            "\"type\": \"Text\""
        );

        assertThatThrownBy(() -> DefinitionReadinessVerifier.verify(JsonPath.from(definition), SPEC))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must be type RichTextArea after data setup but was Text");
    }

    @Test
    void shouldRejectDefinitionMissingCaseworkerCrudAccess() {
        String definition = validDefinition().replace("\"update\": true", "\"update\": false");

        assertThatThrownBy(() -> DefinitionReadinessVerifier.verify(JsonPath.from(definition), SPEC))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must grant CRUD to caseworker-befta_master");
    }

    @Test
    void shouldRejectDefinitionMissingEventFieldMapping() {
        String definition = validDefinition().replace(
            "\"case_field_id\": \"RichTextAreaMinField\"",
            "\"case_field_id\": \"OtherField\""
        );

        assertThatThrownBy(() -> DefinitionReadinessVerifier.verify(JsonPath.from(definition), SPEC))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("is missing required RichTextArea field RichTextAreaMinField");
    }

    private String visibleFields() {
        return """
            {
              "case_fields": [
                {
                  "id": "RichTextAreaField",
                  "field_type": {
                    "id": "RichTextArea",
                    "type": "RichTextArea"
                  }
                },
                {
                  "id": "RichTextAreaMinField",
                  "field_type": {
                    "id": "RichTextAreaMinField-3c359e9e-3b68-43a7-9948-2001ac9b4daf",
                    "type": "RichTextArea",
                    "min": 10
                  }
                }
              ]
            }
            """;
    }

    private String validDefinition() {
        return """
            {
              "case_fields": [
                {
                  "id": "RichTextAreaField",
                  "field_type": {
                    "id": "RichTextArea",
                    "type": "RichTextArea"
                  },
                  "acls": [
                    {
                      "role": "caseworker-befta_master",
                      "create": true,
                      "read": true,
                      "update": true,
                      "delete": true
                    }
                  ]
                },
                {
                  "id": "RichTextAreaMinField",
                  "field_type": {
                    "id": "RichTextAreaMinField-3c359e9e-3b68-43a7-9948-2001ac9b4daf",
                    "type": "RichTextArea",
                    "min": 10
                  },
                  "acls": [
                    {
                      "role": "caseworker-befta_master",
                      "create": true,
                      "read": true,
                      "update": true,
                      "delete": true
                    }
                  ]
                }
              ],
              "events": [
                {
                  "id": "createCase",
                  "case_fields": [
                    {
                      "case_field_id": "RichTextAreaField"
                    },
                    {
                      "case_field_id": "RichTextAreaMinField"
                    }
                  ]
                },
                {
                  "id": "updateCase",
                  "case_fields": [
                    {
                      "case_field_id": "RichTextAreaField"
                    },
                    {
                      "case_field_id": "RichTextAreaMinField"
                    }
                  ]
                }
              ]
            }
            """;
    }
}
