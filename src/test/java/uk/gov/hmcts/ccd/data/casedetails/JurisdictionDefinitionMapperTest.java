package uk.gov.hmcts.ccd.data.casedetails;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.JurisdictionDefinition;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseStateBuilder.newState;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.JurisdictionBuilder.newJurisdiction;

class JurisdictionDefinitionMapperTest {
    uk.gov.hmcts.ccd.data.casedetails.JurisdictionMapper jurisdictionMapper =
            new uk.gov.hmcts.ccd.data.casedetails.JurisdictionMapper();

    CaseStateDefinition caseStateDefinition1 = newState()
        .withId("ST1")
        .build();
    CaseStateDefinition caseStateDefinition2 = newState()
        .withId("ST2")
        .build();

    CaseTypeDefinition caseTypeDefinition1 = newCaseType()
        .withId("CT1")
        .withState(caseStateDefinition1)
        .build();
    CaseTypeDefinition caseTypeDefinition2 = newCaseType()
        .withId("CT2")
        .withState(caseStateDefinition2)
        .build();

    JurisdictionDefinition jurisdictionDefinition = newJurisdiction()
        .withJurisdictionId("jid")
        .withName("name")
        .withDescription("description")
        .withCaseType(caseTypeDefinition1)
        .withCaseType(caseTypeDefinition2)
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
