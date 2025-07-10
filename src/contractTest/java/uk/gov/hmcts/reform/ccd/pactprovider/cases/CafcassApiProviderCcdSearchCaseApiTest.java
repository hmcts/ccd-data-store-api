package uk.gov.hmcts.reform.ccd.pactprovider.cases;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.CaseSearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.ccd.domain.service.search.CaseSearchResultViewGenerator;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchQueryHelper;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchSortService;
import uk.gov.hmcts.ccd.v2.internal.controller.UICaseSearchController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.doReturn;

@Slf4j
@ExtendWith(SpringExtension.class)
@Provider("ccd_data")
@PactBroker(url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "Dev")})
@TestPropertySource(locations = "/application.properties")
@ActiveProfiles("SECURITY_MOCK")
public class CafcassApiProviderCcdSearchCaseApiTest {

    @Mock
    private CaseSearchOperation caseSearchOperation;

    @Mock
    private CaseSearchResultViewGenerator caseSearchResultViewGenerator;

    @Mock
    private ElasticsearchSortService elasticsearchSortService;

    @Mock
    private ElasticsearchQueryHelper elasticsearchQueryHelper;

    @BeforeEach
    void before(PactVerificationContext context) {
        UICaseSearchController controller = new UICaseSearchController(
            caseSearchOperation,
            elasticsearchQueryHelper,
            caseSearchResultViewGenerator,
            elasticsearchSortService
        );

        MockMvcTestTarget target = new MockMvcTestTarget();
        target.setControllers(controller);
        if (context != null) {
            context.setTarget(target);
        }
    }

    @State("Search Cases exist in the datetime range for CafCass in CCD Store")
    public void setupCafcassStoreState() {
        ObjectMapper mapper = new ObjectMapper();

        Map<String, JsonNode> caseData = new HashMap<>();
        caseData.put("applicantSolicitorEmailAddress", mapper.valueToTree("prl-e2etestsolicitor@mailinator.com"));
        caseData.put("confidentialDetails", mapper.valueToTree(Map.of("isConfidentialDetailsAvailable", "No")));
        caseData.put("childrenKnownToLocalAuthority", mapper.valueToTree("no"));
        caseData.put("children", mapper.valueToTree(List.of()));
        caseData.put("miamExemptionsTable", mapper.valueToTree(Map.of(
            "reasonsForMiamExemption", "Urgency",
            "domesticViolenceEvidence", "",
            "urgencyEvidence", "Any delay caused by MIAM would cause unreasonable hardship to the prospective applicant",
            "childProtectionEvidence", "",
            "previousAttendenceEvidence", "",
            "otherGroundsEvidence", ""
        )));
        caseData.put("summaryTabForOrderAppliedFor", mapper.valueToTree(Map.of(
            "ordersApplyingFor", "Child Arrangements Order",
            "typeOfChildArrangementsOrder", "Live with order"
        )));
        caseData.put("applicants", mapper.valueToTree(List.of()));
        caseData.put("respondents", mapper.valueToTree(List.of()));
        caseData.put("applicantsConfidentialDetails", mapper.valueToTree(List.of()));
        caseData.put("courtName", mapper.valueToTree("West London Family Court"));
        caseData.put("dateSubmitted", mapper.valueToTree("2022-08-22"));
        caseData.put("solicitorName", mapper.valueToTree("E2E Test Solicitor"));
        caseData.put("submitAndPayDownloadApplicationLink", mapper.valueToTree(Map.of(
            "document_filename", "Draft_C100_application.pdf",
            "document_id", "e7226e49-fc92-4c12-bac5-e3e50ee4ff15"
        )));
        caseData.put("otherPeopleInTheCaseTable", mapper.valueToTree(List.of()));

        SearchResultViewItem viewItem = new SearchResultViewItem();

        CaseSearchResultView resultView = new CaseSearchResultView(
            List.of(),
            List.of(viewItem),
            1L
        );

        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId("PRLAPPS");
        caseDetails.setState("SUBMITTED_PAID");
        caseDetails.setJurisdiction("PRIVATELAW");
        caseDetails.setId("16611647");
        caseDetails.setCreatedDate(LocalDateTime.of(2022, 8, 22, 10, 39, 43));
        caseDetails.setLastModified(LocalDateTime.of(2022, 8, 22, 10, 44, 54));
        caseDetails.setData(caseData);

        CaseSearchResult caseSearchResult = new CaseSearchResult(1L, List.of(caseDetails));

        doReturn(caseSearchResult)
            .when(caseSearchOperation)
            .execute(any(CrossCaseTypeSearchRequest.class), anyBoolean());

        doReturn(resultView)
            .when(caseSearchResultViewGenerator)
            .execute(anyString(), eq(caseSearchResult), any(), any());
    }
}