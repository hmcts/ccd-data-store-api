package uk.gov.hmcts.ccd.data.casedetails.query;

import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;

class CaseStateAuthorisationSecurityTest {

    @Mock
    private AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;
    @Mock
    private CaseDetailsQueryBuilder<Long> builder;

    @InjectMocks
    private CaseStateAuthorisationSecurity caseStateAuthorisationSecurity;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Nested
    @DisplayName("should secure with authorised case states")
    class Secure {

        @Test
        @DisplayName("should secure the builder query with user authorised list of case states")
        void shouldSecureWithAuthorisedCaseStates() {
            List<String> caseStates = asList("state1", "state2");
            MetaData metaData = new MetaData("CaseType", "Jurisdiction");
            when(authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds(metaData.getJurisdiction(),
                metaData.getCaseTypeId(), CAN_READ)).thenReturn(caseStates);

            caseStateAuthorisationSecurity.secure(builder, metaData);

            assertAll(
                () -> verify(authorisedCaseDefinitionDataService).getUserAuthorisedCaseStateIds(
                    metaData.getJurisdiction(), metaData.getCaseTypeId(), CAN_READ),
                () -> verify(builder).whereStates(caseStates));
        }
    }
}
