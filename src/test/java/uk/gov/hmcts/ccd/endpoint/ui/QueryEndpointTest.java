package uk.gov.hmcts.ccd.endpoint.ui;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.search.FieldMapSanitizeOperation;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.service.aggregated.*;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.ccd.domain.model.callbacks.EventTokenProperties.JURISDICTION_ID;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

class QueryEndpointTest {

    @Mock
    private AuthorisedGetCaseViewOperation getCaseViewOperation;
    @Mock
    private GetEventTriggerOperation getEventTriggerOperation;
    @Mock
    private SearchQueryOperation searchQueryOperation;
    @Mock
    private FieldMapSanitizeOperation fieldMapSanitizerOperation;
    @Mock
    private AuthorisedFindSearchInputOperation findSearchInputOperation;
    @Mock
    private DefaultFindWorkbasketInputOperation findWorkbasketInputOperation;
    @Mock
    private GetCaseTypesOperation getCaseTypesOperation;

    private QueryEndpoint queryEndpoint;


    @BeforeEach
    void setup(){
        MockitoAnnotations.initMocks(this);
        queryEndpoint = new QueryEndpoint(getCaseViewOperation,
            getEventTriggerOperation,
            searchQueryOperation,
            fieldMapSanitizerOperation,
            findSearchInputOperation,
            findWorkbasketInputOperation,
            getCaseTypesOperation);
    }

    @Test
    void shouldFailIfAccessParamInvalid() {
        assertThrows(ResourceNotFoundException.class,
                     () -> queryEndpoint.getCaseTypes(JURISDICTION_ID, "INVALID"));
    }

    @Test
    void shouldCallGetCaseViewOperation() {
        CaseView caseView = new CaseView();
        doReturn(caseView).when(getCaseViewOperation).execute(any(), any(), any());
        queryEndpoint.findCase("jurisdictionId", "caseTypeId", "caseId");
        verify(getCaseViewOperation, times(1)).execute("jurisdictionId", "caseTypeId", "caseId");
    }

    @Test
    void shouldCallFindWorkBasketOperation() {
        List<WorkbasketInput> workBasketResults = new ArrayList<>();
        when(findWorkbasketInputOperation.execute("TEST", "TEST-CASE-TYPE", CAN_READ)).thenReturn(workBasketResults);
        queryEndpoint.findWorkbasketInputDetails(22, "TEST", "TEST-CASE-TYPE");
        verify(findWorkbasketInputOperation, times(1)).execute("TEST", "TEST-CASE-TYPE", CAN_READ);
    }
}
