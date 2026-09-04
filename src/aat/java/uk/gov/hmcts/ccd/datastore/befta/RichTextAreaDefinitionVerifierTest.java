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
            .hasMessageContaining("event createCase is missing required RichTextArea field RichTextAreaMinField");
    }

    private String validDefinition() {
        return """
            {
              "case_fields": [
                {
                  "id": "RichTextAreaField",
                  "field_type": {
                    "id": "RichTextArea"
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
                    "id": "RichTextArea"
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
