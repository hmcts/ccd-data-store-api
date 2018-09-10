package uk.gov.hmcts.ccd.data.casedetails;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.Jurisdiction;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseStateBuilder.newState;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseTypeBuilder.newCaseType;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.JurisdictionBuilder.newJurisdiction;

class JurisdictionMapperTest {
    JurisdictionMapper jurisdictionMapper = new JurisdictionMapper();

    CaseState caseState1 = newState()
        .withId("ST1")
        .build();
    CaseState caseState2 = newState()
        .withId("ST2")
        .build();

    CaseType caseType1 = newCaseType()
        .withId("CT1")
        .withState(caseState1)
        .build();
    CaseType caseType2 = newCaseType()
        .withId("CT2")
        .withState(caseState2)
        .build();

    Jurisdiction jurisdiction = newJurisdiction()
        .withJurisdictionId("jid")
        .withName("name")
        .withDescription("description")
        .withCaseType(caseType1)
        .withCaseType(caseType2)
        .build();

    @Test
    void toResponse() {

        final JurisdictionDisplayProperties response = jurisdictionMapper.toResponse(jurisdiction);
        assertAll(
            () -> assertThat(response.getId(), is(equalTo(jurisdiction.getId()))),
            () -> assertThat(response.getName(), is(equalTo(jurisdiction.getName()))),
            () -> assertThat(response.getDescription(), is(equalTo(jurisdiction.getDescription()))),
            () -> assertThat(response.getCaseTypes().size(), is(2)),
            () -> assertThat(response.getCaseTypes().get(0).getStates().size(), is(1)),
            () -> assertThat(response.getCaseTypes().get(1).getStates().get(0).getId(), is(caseState2.getId()))
        );
    }
}
