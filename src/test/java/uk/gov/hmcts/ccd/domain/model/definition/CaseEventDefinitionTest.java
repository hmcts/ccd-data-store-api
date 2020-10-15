package uk.gov.hmcts.ccd.domain.model.definition;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaseEventDefinitionTest {

    @Nested
    @DisplayName("build json node from case fields with a defaultValue test")
    class CaseEventDefinitionBuildJsonNodeFromCaseFieldsWithDefaultValueTest {

        @Test
        void buildsJsonRepresentationFromEventCaseFields() {

            final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
            caseEventDefinition.setCaseFields(Arrays.asList(
                TestBuildersUtil.CaseEventFieldDefinitionBuilder.newCaseEventField()
                    .withCaseFieldId("ChangeOrganisationRequestField")
                    .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                             .reference("Reason")
                                                             .defaultValue("SomeReasonX")
                                                             .build())
                    .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                             .reference("CaseRoleId")
                                                             .defaultValue(null)
                                                             .build())
                    .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                             .reference("OrganisationToAdd.OrganisationID")
                                                             .defaultValue("Solicitor firm 1")
                                                             .build())
                    .build(),
                TestBuildersUtil.CaseEventFieldDefinitionBuilder.newCaseEventField()
                    .withCaseFieldId("OrganisationPolicyField")
                    .addCaseEventFieldComplexDefinitions(CaseEventFieldComplexDefinition.builder()
                                                             .reference("OrgPolicyCaseAssignedRole")
                                                             .defaultValue("[Claimant]")
                                                             .build())
                    .build()
            ));

            Map<String, JsonNode> result = caseEventDefinition.buildJsonNodeFromCaseFieldsWithDefaultValue();

            assertAll(
                () -> assertThat(result.size(), is(2)),

                () -> assertTrue(result.containsKey("ChangeOrganisationRequestField")),
                () -> assertNotNull(result.get("ChangeOrganisationRequestField").get("Reason")),
                () -> assertNull(result.get("ChangeOrganisationRequestField").get("CaseRoleId")),
                () -> assertNotNull(result.get("ChangeOrganisationRequestField").get("OrganisationToAdd").get("OrganisationID")),
                () -> assertThat(result.get("ChangeOrganisationRequestField").get("Reason").asText(), is("SomeReasonX")),
                () -> assertThat(result.get("ChangeOrganisationRequestField").get("OrganisationToAdd").get("OrganisationID").asText(), is("Solicitor firm 1")),

                () -> assertTrue(result.containsKey("OrganisationPolicyField")),
                () -> assertNotNull(result.get("OrganisationPolicyField").get("OrgPolicyCaseAssignedRole")),
                () -> assertThat(result.get("OrganisationPolicyField").get("OrgPolicyCaseAssignedRole").asText(), is("[Claimant]"))
            );
        }
    }

}
