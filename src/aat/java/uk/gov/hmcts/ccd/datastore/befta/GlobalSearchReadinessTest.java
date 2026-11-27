package uk.gov.hmcts.ccd.datastore.befta;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import uk.gov.hmcts.befta.data.HttpTestData;
import uk.gov.hmcts.befta.data.HttpTestDataSource;
import uk.gov.hmcts.befta.data.ResponseData;
import uk.gov.hmcts.befta.exception.FunctionalTestException;
import uk.gov.hmcts.befta.factory.HttpTestDataSourceFactory;
import uk.gov.hmcts.befta.player.BackEndFunctionalTestScenarioContext;
import uk.gov.hmcts.befta.util.DynamicValueInjector;
import uk.gov.hmcts.befta.util.MapVerifier;
import uk.gov.hmcts.befta.util.Retryable;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GlobalSearchReadinessTest {

    private static final String CASE_REFERENCE = "1234567890123456";
    private static final HttpTestDataSource DATA_SOURCE =
        HttpTestDataSourceFactory.createHttpTestDataSource(new String[]{"features"});

    private final DataStoreTestAutomationAdapter adapter = new DataStoreTestAutomationAdapter();

    @ParameterizedTest
    @CsvSource({
        "F-1005_WaitForCreatedCase,F-1005_CreateCasePreRequisiteCaseworker,FT_GlobalSearch",
        "F-1005_WaitForUpdatedCase,F-1005_CreateCasePreRequisiteCaseworker,FT_GlobalSearch",
        "F-1005_WaitForSecondCase,F-1005_CreateSecondCasePreRequisiteCaseworker,FT_GlobalSearch",
        "F-1005_WaitForMasterCase,F-1005_CreateCaseMasterCaseType,FT_MasterCaseType",
        "F-1005_WaitForCallbackCase,F-1005_CreateCaseWithCallback,FT_GlobalSearch"
    })
    void configuresPollingOnlyForReadinessProbe(String fixtureId, String creationId, String caseType) {
        HttpTestData data = DATA_SOURCE.getDataForTestCall(fixtureId);
        BackEndFunctionalTestScenarioContext root = mock(BackEndFunctionalTestScenarioContext.class);
        BackEndFunctionalTestScenarioContext creation = mock(BackEndFunctionalTestScenarioContext.class);
        HttpTestData creationData = mock(HttpTestData.class);
        ResponseData creationResponse = mock(ResponseData.class);
        when(root.getChildContexts()).thenReturn(Map.of(creationId, creation));
        when(creation.getTestData()).thenReturn(creationData);
        when(creationData.getActualResponse()).thenReturn(creationResponse);
        when(creationResponse.getBody()).thenReturn(Map.of("id", Long.parseLong(CASE_REFERENCE)));
        BackEndFunctionalTestScenarioContext probe = mock(BackEndFunctionalTestScenarioContext.class);
        when(probe.getParentContext()).thenReturn(root);
        when(probe.getTestData()).thenReturn(data);
        when(probe.getCustomValues()).thenReturn(key -> adapter.calculateCustomValue(probe, key));
        // Authentication is handled by BEFTA before request preparation; keep this check offline.
        data.getRequest().getHeaders().clear();

        new DynamicValueInjector(adapter, data, probe).injectDataFromContextBeforeApiCall();

        Map<?, ?> criteria = (Map<?, ?>) data.getRequest().getBody().get("searchCriteria");
        assertEquals(List.of(CASE_REFERENCE), criteria.get("caseReferences"));
        assertEquals(List.of(caseType), criteria.get("CCDCaseTypeIds"));
        if (fixtureId.equals("F-1005_WaitForUpdatedCase")) {
            assertEquals(List.of(Map.of("addressLine1", "updated Address")), criteria.get("parties"));
        } else {
            assertFalse(criteria.containsKey("parties"));
        }

        ArgumentCaptor<Retryable> policy = ArgumentCaptor.forClass(Retryable.class);
        verify(probe).setRetryableTag(policy.capture());
        verify(root, never()).setRetryableTag(any());
        verify(creation, never()).setRetryableTag(any());
        assertEquals(3000, policy.getValue().getDelay());
        assertEquals(11, policy.getValue().getMaxAttempts());
        assertTrue(policy.getValue().getStatusCodes().isEmpty());
        assertFalse(policy.getValue().getNonRetryableHttpMethods().contains("POST"));
        Pattern emptyResults = Pattern.compile(policy.getValue().getMatch().get(0));
        assertTrue(emptyResults.matcher("{\"resultInfo\": {\"casesReturned\": 0, \"caseStartRecord\": 1}}").find());
        assertTrue(emptyResults.matcher("{\"resultInfo\": {\"casesReturned\" : 0 }}").find());
        assertFalse(emptyResults.matcher("{\"resultInfo\": {\"casesReturned\": 1}}").find());
        assertFalse(emptyResults.matcher("{\"status\":401,\"error\":\"Unauthorized\"}").find());

        Map<String, Object> ready = Map.of(
            "resultInfo", Map.of("casesReturned", 1, "caseStartRecord", 1, "moreResultsToGo", false),
            "results", List.of(Map.of("caseReference", CASE_REFERENCE, "stateId", "CaseCreated")));
        assertTrue(MapVerifier.createMapVerifier("body", 20)
                       .verifyMap(data.getExpectedResponse().getBody(), ready).isVerified());
        Map<String, Object> notReady = Map.of(
            "resultInfo", Map.of("casesReturned", 0, "caseStartRecord", 1, "moreResultsToGo", false),
            "results", List.of());
        assertFalse(MapVerifier.createMapVerifier("body", 20)
                        .verifyMap(data.getExpectedResponse().getBody(), notReady).isVerified());
    }

    @Test
    void refusesToEnablePollingForCaseCreation() {
        BackEndFunctionalTestScenarioContext context = mock(BackEndFunctionalTestScenarioContext.class);
        HttpTestData data = mock(HttpTestData.class);
        when(context.getTestData()).thenReturn(data);
        when(data.getMethod()).thenReturn("POST");
        when(data.getUri()).thenReturn("/caseworkers/user/jurisdictions/jurisdiction/case-types/type/cases");

        assertThrows(FunctionalTestException.class, () -> adapter.calculateCustomValue(
            context, "caseReferenceForGlobalSearchReadiness_CreateCase"));
        verify(context, never()).setRetryableTag(any());
    }
}
