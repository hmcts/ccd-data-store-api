package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.search.CaseDetailsSearchResult;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDataFilter;
import uk.gov.hmcts.ccd.domain.service.security.AuthorisedCaseDefinitionDataService;

class AuthorisedCaseDetailsSearchOperationTest {

    private static final String CASE_TYPE = "caseType";
    private static final String QUERY = "{}";

    @Mock
    private CaseDetailsSearchOperation caseDetailsSearchOperation;
    @Mock
    private AuthorisedCaseDefinitionDataService authorisedCaseDefinitionDataService;
    @Mock
    private AuthorisedCaseDataFilter authorisedCaseDataFilter;

    @InjectMocks
    private AuthorisedCaseDetailsSearchOperation authorisedCaseDetailsSearchOperation;

    private final CaseType caseType = new CaseType();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        caseType.setId(CASE_TYPE);
    }

    @Test
    @DisplayName("should filter fields and return search results for valid query")
    void shouldFilterFieldsReturnSearchResults() {
        CaseDetails caseDetails = new CaseDetails();
        CaseDetailsSearchResult searchResult = new CaseDetailsSearchResult(singletonList(caseDetails), 1L);
        when(authorisedCaseDefinitionDataService.getAuthorisedCaseType(CASE_TYPE, CAN_READ)).thenReturn(Optional.of(caseType));
        when(caseDetailsSearchOperation.execute(CASE_TYPE, QUERY)).thenReturn(searchResult);

        CaseDetailsSearchResult result = authorisedCaseDetailsSearchOperation.execute(CASE_TYPE, QUERY);

        assertAll(
            () -> assertThat(result, is(searchResult)),
            () -> assertThat(result.getTotal(), is(1L)),
            () -> verify(authorisedCaseDefinitionDataService).getAuthorisedCaseType(CASE_TYPE, CAN_READ),
            () -> verify(caseDetailsSearchOperation).execute(CASE_TYPE, QUERY),
            () -> verify(authorisedCaseDataFilter).filterFields(caseType, caseDetails)
        );
    }

    @Test
    @DisplayName("should return empty list of cases when user is not authorised to access case type")
    void shouldReturnEmptyCaseList() {
        when(authorisedCaseDefinitionDataService.getAuthorisedCaseType(CASE_TYPE, CAN_READ)).thenReturn(Optional.empty());

        CaseDetailsSearchResult result = authorisedCaseDetailsSearchOperation.execute(CASE_TYPE, QUERY);

        assertAll(
            () -> assertThat(result.getCases(), hasSize(0)),
            () -> assertThat(result.getTotal(), is(0L)),
            () -> verify(authorisedCaseDefinitionDataService).getAuthorisedCaseType(CASE_TYPE, CAN_READ),
            () -> verifyZeroInteractions(caseDetailsSearchOperation),
            () -> verifyZeroInteractions(authorisedCaseDataFilter)
        );
    }
}
