package uk.gov.hmcts.ccd.data.casedetails;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

class JurisdictionDefinitionMapperTest {
    uk.gov.hmcts.ccd.data.casedetails.JurisdictionMapper jurisdictionMapper =
            new uk.gov.hmcts.ccd.data.casedetails.JurisdictionMapper();

    CaseStateDefinition caseStateDefinition1 = CaseStateDefinition.builder()
        .id("ST1")
        .build();
    CaseStateDefinition caseStateDefinition2 = CaseStateDefinition.builder()
        .id("ST2")
        .build();

    CaseTypeDefinition caseTypeDefinition1 = CaseTypeDefinition.builder()
        .id("CT1")
        .states(List.of(caseStateDefinition1))
        .build();
    CaseTypeDefinition caseTypeDefinition2 = CaseTypeDefinition.builder()
        .id("CT2")
        .states(List.of(caseStateDefinition2))
        .build();

    JurisdictionDefinition jurisdictionDefinition = JurisdictionDefinition.builder()
        .id("jid")
        .name("name")
        .description("description")
        .caseTypeDefinitions(List.of(caseTypeDefinition1, caseTypeDefinition2))
        .build();

    @Test
    void toResponse() {

        final JurisdictionDisplayProperties response = jurisdictionMapper.toResponse(jurisdictionDefinition);
        assertAll(
            () -> assertThat(response.getId(), is(equalTo(jurisdictionDefinition.getId()))),
            () -> assertThat(response.getName(), is(equalTo(jurisdictionDefinition.getName()))),
            () -> assertThat(response.getDescription(), is(equalTo(jurisdictionDefinition.getDescription()))),
            () -> assertThat(response.getCaseTypeDefinitions().size(), is(2)),
            () -> assertThat(response.getCaseTypeDefinitions().get(0).getStates().size(), is(1)),
            () -> assertThat(response.getCaseTypeDefinitions().get(1).getStates().get(0).getId(),
                    is(caseStateDefinition2.getId()))
        );
    }
}
