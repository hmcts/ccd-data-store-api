package uk.gov.hmcts.ccd.datastore.befta;

import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RichTextAreaDefinitionVerifierTest {

    @Test
    void shouldAcceptDefinitionWithRequiredRichTextAreaConfiguration() {
        assertThatCode(() -> RichTextAreaDefinitionVerifier.verify(JsonPath.from(validDefinition())))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldAcceptVisibleFieldsWithoutDefinitionAclOrEventMappings() {
        assertThatCode(() -> RichTextAreaDefinitionVerifier.verifyVisibleFields(JsonPath.from(visibleFields())))
            .doesNotThrowAnyException();
    }

    @Test
    void shouldRejectDefinitionMissingRichTextAreaField() {
        String definition = validDefinition().replace(
            "\"id\": \"RichTextAreaMinField\"",
            "\"id\": \"OtherField\""
        );

        assertThatThrownBy(() -> RichTextAreaDefinitionVerifier.verify(JsonPath.from(definition)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("missing required RichTextArea field RichTextAreaMinField");
    }

    @Test
    void shouldRejectDefinitionWhenFieldBaseTypeIsWrong() {
        String definition = validDefinition().replace(
            "\"type\": \"RichTextArea\"",
            "\"type\": \"Text\""
        );

        assertThatThrownBy(() -> RichTextAreaDefinitionVerifier.verify(JsonPath.from(definition)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must be type RichTextArea after data setup but was Text");
    }

    @Test
    void shouldRejectDefinitionMissingCaseworkerCrudAccess() {
        String definition = validDefinition().replace("\"update\": true", "\"update\": false");

        assertThatThrownBy(() -> RichTextAreaDefinitionVerifier.verify(JsonPath.from(definition)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("must grant CRUD to caseworker-befta_master");
    }

    @Test
    void shouldRejectDefinitionMissingEventFieldMapping() {
        String definition = validDefinition().replace(
            "\"case_field_id\": \"RichTextAreaMinField\"",
            "\"case_field_id\": \"OtherField\""
        );

        assertThatThrownBy(() -> RichTextAreaDefinitionVerifier.verify(JsonPath.from(definition)))
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
