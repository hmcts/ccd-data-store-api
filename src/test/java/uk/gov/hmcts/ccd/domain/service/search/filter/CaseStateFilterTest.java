package uk.gov.hmcts.ccd.domain.service.search.filter;


import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.service.common.AuthorisedCaseDefinitionDataService;

class CaseStateFilterTest {

    @Mock
    private AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;

    @InjectMocks
    private CaseStateFilter caseStateFilter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @DisplayName("should return case state ids where user has read access")
    void shouldReturnCaseStateIdsWithReadAccess() {
        String caseTypeId = "CaseTypeId";
        List<String> caseIds = Arrays.asList("case1", "case2");
        when(authorisedCaseDefinitionDataService.getUserAuthorisedCaseStateIds(caseTypeId, CAN_READ)).thenReturn(caseIds);

        List<String> result = caseStateFilter.getCaseStateIdsForUserReadAccess(caseTypeId);

        assertThat(result, is(caseIds));
        verify(authorisedCaseDefinitionDataService).getUserAuthorisedCaseStateIds(caseTypeId, CAN_READ);
    }
}
