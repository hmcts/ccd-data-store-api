package uk.gov.hmcts.ccd.endpoint.std;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchResponsePayload;
import uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchParser;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchService;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Provider PACT verification for POST /globalSearch (GlobalSearchEndpoint). Verifies the consumer
 * contract published by xui_webapp (state "Search for case id").
 */
@Provider("ccdDataStoreAPI_search")
@PactBroker(url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "${PACT_BRANCH_NAME:Dev}")},
    providerTags = "${pactbroker.providerTags:master}",
    enablePendingPacts = "${pactbroker.enablePending:true}")
@IgnoreNoPactsToVerify
@ExtendWith(SpringExtension.class)
public class GlobalSearchProviderTest {

    @Mock
    private CaseSearchOperation caseSearchOperation;

    @Mock
    private GlobalSearchService globalSearchService;

    @Mock
    private GlobalSearchParser globalSearchParser;


    @BeforeEach
    void before(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(new GlobalSearchEndpoint(
            caseSearchOperation, globalSearchService, globalSearchParser));
        if (context != null) {
            context.setTarget(testTarget);
        }
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }


    @State("Search for case id")
    public void searchForCaseId() {
        when(caseSearchOperation.execute(any(), anyBoolean()))
            .thenReturn(new CaseSearchResult(1L, Collections.emptyList()));
        when(globalSearchParser.filterCases(anyList(), any())).thenReturn(Collections.emptyList());

        GlobalSearchResponsePayload.Result result = GlobalSearchResponsePayload.Result.builder()
            .stateId("PREPARE_FOR_HEARING")
            .processForAccess("SPECIFIC")
            .caseReference("1675871084353511")
            .otherReferences(Collections.emptyList())
            .ccdJurisdictionId("PUBLICLAW")
            .ccdJurisdictionName("Public Law")
            .ccdCaseTypeId("CARE_SUPERVISION_EPO")
            .ccdCaseTypeName("Public Law Applications")
            .build();

        GlobalSearchResponsePayload payload = GlobalSearchResponsePayload.builder()
            .resultInfo(GlobalSearchResponsePayload.ResultInfo.builder()
                .casesReturned(1)
                .caseStartRecord(1)
                .moreResultsToGo(false)
                .build())
            .results(List.of(result))
            .build();

        when(globalSearchService.transformResponse(any(), any(), anyList())).thenReturn(payload);
    }
}
