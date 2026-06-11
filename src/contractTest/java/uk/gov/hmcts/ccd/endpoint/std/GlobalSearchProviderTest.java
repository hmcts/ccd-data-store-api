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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.WireMockBaseContractTest;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchResponsePayload;
import uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchParser;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchService;
import uk.gov.hmcts.ccd.v2.external.controller.TestIdamConfiguration;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * Provider PACT verification for the Global Search endpoint
 * (POST /globalSearch, served by {@link GlobalSearchEndpoint}).
 *
 * <p>Verifies the consumer contract published by xui_webapp under the provider name
 * {@code ccdDataStoreAPI_search} (see rpx-xui-webapp api/test/pact/pact-tests/wa2/getSearchResults.spec.ts).</p>
 */
@Provider("ccdDataStoreAPI_search")
@PactBroker(url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "${PACT_BRANCH_NAME:Dev}")},
    providerTags = "${pactbroker.providerTags:master}",
    enablePendingPacts = "${pactbroker.enablePending:true}")
@TestPropertySource(locations = "/application.properties")
@WebMvcTest({GlobalSearchEndpoint.class})
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("SECURITY_MOCK")
@ContextConfiguration(classes = {TestIdamConfiguration.class})
@IgnoreNoPactsToVerify
@ExtendWith(SpringExtension.class)
public class GlobalSearchProviderTest extends WireMockBaseContractTest {

    @MockitoBean
    ApplicationParams applicationParams;

    @MockitoBean
    SecurityUtils securityUtils;

    @MockitoBean
    @Qualifier("AuthorisedCaseSearchOperation")
    CaseSearchOperation caseSearchOperation;

    @MockitoBean
    GlobalSearchService globalSearchService;

    @MockitoBean
    GlobalSearchParser globalSearchParser;

    @Autowired
    GlobalSearchEndpoint globalSearchEndpoint;

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        if (context != null) {
            context.verifyInteraction();
        }
    }

    @BeforeEach
    void before(PactVerificationContext context) {
        System.getProperties().setProperty("pact.verifier.publishResults", "true");
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(globalSearchEndpoint);
        if (context != null) {
            context.setTarget(testTarget);
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
