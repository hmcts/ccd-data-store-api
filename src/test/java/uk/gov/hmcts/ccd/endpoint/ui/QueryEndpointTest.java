package uk.gov.hmcts.ccd.endpoint.ui;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.callbacks.EventTokenProperties.JURISDICTION_ID;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.search.FieldMapSanitizeOperation;
import uk.gov.hmcts.ccd.domain.model.aggregated.*;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.service.aggregated.*;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

class QueryEndpointTest {

    @Mock
    private AuthorisedGetCaseViewOperation getCaseViewOperation;
    @Mock
    private AuthorisedGetCaseHistoryViewOperation getCaseHistoryViewOperation;
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
    @Mock
    private GetUserProfileOperation getUserProfileOperation;

    private QueryEndpoint queryEndpoint;


    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        queryEndpoint = new QueryEndpoint(getCaseViewOperation,
            getCaseHistoryViewOperation,
            getEventTriggerOperation,
            searchQueryOperation,
            fieldMapSanitizerOperation,
            findSearchInputOperation,
            findWorkbasketInputOperation,
            getCaseTypesOperation,
            getUserProfileOperation
        );
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
    void shouldCallGetEventTriggerOperationForDraft() {
        CaseEventTrigger caseEventTrigger = new CaseEventTrigger();
        doReturn(caseEventTrigger).when(getEventTriggerOperation).executeForDraft(any(), any(), any(), any(), any(), any());
        queryEndpoint.getEventTriggerForDraft("userId", "jurisdictionId", "caseTypeId", "draftId", "eventTriggerId", false);
        verify(getEventTriggerOperation).executeForDraft("userId", "jurisdictionId", "caseTypeId", "draftId", "eventTriggerId", false);
    }

    @Test
    void shouldCallFindWorkBasketOperation() {
        List<WorkbasketInput> workBasketResults = new ArrayList<>();
        when(findWorkbasketInputOperation.execute("TEST", "TEST-CASE-TYPE", CAN_READ)).thenReturn(workBasketResults);
        queryEndpoint.findWorkbasketInputDetails("22", "TEST", "TEST-CASE-TYPE");
        verify(findWorkbasketInputOperation, times(1)).execute("TEST", "TEST-CASE-TYPE", CAN_READ);
    }

    @Test
    void shouldCallGetCaseViewOperationWithEvent() {
        CaseHistoryView caseView = new CaseHistoryView();
        doReturn(caseView).when(getCaseHistoryViewOperation).execute("jurisdictionId", "caseTypeId", "caseId", 11L);

        CaseHistoryView response = queryEndpoint.getCaseHistoryForEvent("jurisdictionId", "caseTypeId", "caseId", 11L);

        assertSame(caseView, response);
        verify(getCaseHistoryViewOperation, times(1)).execute("jurisdictionId", "caseTypeId", "caseId", 11L);
    }

    @Test
    @DisplayName("Should call Get User Profile Operation")
    void shouldCallGetUserProfileOperation() {
        JurisdictionDisplayProperties j1 = new JurisdictionDisplayProperties();
        JurisdictionDisplayProperties j2 = new JurisdictionDisplayProperties();
        JurisdictionDisplayProperties[] jurisdictions = {j1, j2};
        UserProfile userProfile = new UserProfile();
        userProfile.setJurisdictions(jurisdictions);
        doReturn(userProfile).when(getUserProfileOperation).execute(CAN_CREATE);

        List<JurisdictionDisplayProperties> response = queryEndpoint.getJurisdictions("create");

        assertEquals(jurisdictions.length, response.size());
        assertThat(response.get(0), is(j1));
        assertThat(response.get(1), is(j2));
        verify(getUserProfileOperation, times(1)).execute(CAN_CREATE);
    }

    @Test
    @DisplayName("Should throw bad request Exception when access is not correct")
    void shouldThrowBadRequest() {
        assertThrows(BadRequestException.class, () -> queryEndpoint.getJurisdictions("creat"));
    }
}
