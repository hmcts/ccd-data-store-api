package uk.gov.hmcts.ccd.endpoint.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.model.callbacks.EventTokenProperties.JURISDICTION_ID;
import static uk.gov.hmcts.ccd.domain.model.search.CriteriaType.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.casedetails.search.FieldMapSanitizeOperation;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.WorkbasketInput;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCaseHistoryViewOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCaseViewOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseTypesOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCriteriaOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetEventTriggerOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetUserProfileOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation;
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
    private GetCriteriaOperation getCriteriaOperation;
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
                                          getCriteriaOperation,
                                          getCaseTypesOperation,
                                          getUserProfileOperation
        );
    }

    @Test
    void shouldFailIfAccessParamInvalid() {
        assertThrows(ResourceNotFoundException.class, () -> queryEndpoint.getCaseTypes(JURISDICTION_ID, "INVALID"));
    }

    @Test
    void shouldCallGetCaseViewOperation() {
        CaseView caseView = new CaseView();
        doReturn(caseView).when(getCaseViewOperation).execute(any());
        queryEndpoint.findCase("jurisdictionId", "caseTypeId", "caseId");
        verify(getCaseViewOperation, times(1)).execute("caseId");
    }

    @Test
    void shouldCallGetEventTriggerOperationForDraft() {
        CaseUpdateViewEvent caseUpdateViewEvent = new CaseUpdateViewEvent();
        doReturn(caseUpdateViewEvent).when(getEventTriggerOperation).executeForDraft(any(), any());
        queryEndpoint.getEventTriggerForDraft("userId", "jurisdictionId", "caseTypeId", "draftId", "eventId", false);
        verify(getEventTriggerOperation).executeForDraft("draftId", false);
    }

    @Test
    void shouldCallFindWorkBasketOperation() {
        List<WorkbasketInput> workBasketResults = new ArrayList<>();
        doReturn(workBasketResults).when(getCriteriaOperation).execute("TEST-CASE-TYPE", CAN_READ, WORKBASKET);
        queryEndpoint.findWorkbasketInputDetails("22", "TEST", "TEST-CASE-TYPE");
        verify(getCriteriaOperation, times(1)).execute("TEST-CASE-TYPE", CAN_READ, WORKBASKET);
    }

    @Test
    void shouldCallGetCaseViewOperationWithEvent() {
        CaseHistoryView caseView = new CaseHistoryView();
        doReturn(caseView).when(getCaseHistoryViewOperation).execute("caseId", 11L);

        CaseHistoryView response = queryEndpoint.getCaseHistoryForEvent("jurisdictionId", "caseTypeId", "caseId", 11L);

        assertSame(caseView, response);
        verify(getCaseHistoryViewOperation, times(1)).execute("caseId", 11L);
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

    @Nested
    @DisplayName("search")
    class Search {

        @Test
        @DisplayName("Should call search query operation")
        void shouldCallSearchQueryOperation() {
            Map<String, String> params = new HashMap<>();
            Map<String, String> sanitised = new HashMap<>();
            SearchResultView searchResultView = new SearchResultView();
            when(fieldMapSanitizerOperation.execute(params)).thenReturn(sanitised);
            when(searchQueryOperation.execute(eq(null), any(MetaData.class), eq(sanitised))).thenReturn(searchResultView);

            SearchResultView result = queryEndpoint.searchNew("DIVORCE", "DIVORCE", params);

            assertThat(result, is(searchResultView));
            verify(searchQueryOperation).execute(eq(null), any(MetaData.class), eq(sanitised));
        }

    }
}
