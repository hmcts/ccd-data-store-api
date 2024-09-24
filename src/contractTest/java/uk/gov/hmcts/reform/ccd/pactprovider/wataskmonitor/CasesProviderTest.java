package uk.gov.hmcts.reform.ccd.pactprovider.wataskmonitor;

import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.IgnoreNoPactsToVerify;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactBroker;
import au.com.dius.pact.provider.junitsupport.loader.VersionSelector;
import au.com.dius.pact.provider.spring.junit5.MockMvcTestTarget;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;
import uk.gov.hmcts.reform.ccd.pactprovider.wataskmonitor.controller.CasesRestController;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

// Consolidated Pact Provider Test for "WA TASK MONITOR" , version 1.   [Copied from CCD-5224]

@Provider("ccdDataStoreAPI_WorkAllocation1")
@PactBroker(
    url = "${PACT_BROKER_FULL_URL:http://localhost:9292}",
    consumerVersionSelectors = {@VersionSelector(tag = "master")})
@IgnoreNoPactsToVerify
//@PactFolder("target/pacts/reform/wataskmonitor/StartEventProviderTest")
@ExtendWith(SpringExtension.class)
public class CasesProviderTest {

    @Mock
    private StartEventOperation startEventOperation;

    @Mock
    private CreateEventOperation createEventOperation;

    @Mock
    private CreateCaseOperation createCaseOperation;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void beforeCreate(PactVerificationContext context) {
        MockMvcTestTarget testTarget = new MockMvcTestTarget();
        testTarget.setControllers(new CasesRestController(startEventOperation, createEventOperation,
            createCaseOperation));
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

    @State("A Start Event for a Caseworker is  requested")
    public void startEventForCaseworker() throws JsonProcessingException {
        when(startEventOperation.triggerStartForCase(anyString(), anyString(), anyBoolean()))
            .thenReturn(mockStartEventResult());
    }

    @State("A Start for a Caseworker is requested")
    public void startCaseForCaseworker() throws JsonProcessingException {
        when(startEventOperation.triggerStartForCaseType(anyString(), anyString(), anyBoolean()))
            .thenReturn(mockStartEventResult());
    }

    @State("A Submit Event for a Caseworker is requested")
    public void createCaseEventForCaseWorker() throws JsonProcessingException {
        when(createEventOperation.createCaseEvent(anyString(), any(CaseDataContent.class)))
            .thenReturn(mockCaseDetails());
    }

    @State("A Submit for a Caseworker is requested")
    public void saveCaseDetailsForCaseWorker() throws JsonProcessingException {
        when(createCaseOperation.createCaseDetails(anyString(), any(CaseDataContent.class), anyBoolean()))
            .thenReturn(mockCaseDetails());
    }

    private StartEventResult mockStartEventResult() throws JsonProcessingException {
        StartEventResult startEventResult = new StartEventResult();
        startEventResult.setToken("someToken");
        startEventResult.setEventId("startAppeal");
        startEventResult.setCaseDetails(mockCaseDetails());
        return startEventResult;
    }

    private CaseDetails mockCaseDetails() throws JsonProcessingException {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(1L);
        caseDetails.setCaseTypeId("Asylum");
        caseDetails.setJurisdiction("IA");
        caseDetails.setSecurityClassification(SecurityClassification.PUBLIC);
        caseDetails.setAfterSubmitCallbackResponseEntity(new ResponseEntity<>(HttpStatus.OK));
        caseDetails.setState("appealStarted");
        caseDetails.setData(new HashMap<String, JsonNode>() {{
                put("appealReferenceNumber", new TextNode("DRAFT"));
                put("appealType", new TextNode("protection"));
                put("appellantDateOfBirth", new TextNode("1990-12-07"));
                put("appellantFamilyName", new TextNode("Smith"));
                put("appellantGivenNames", new TextNode("Bob"));
                put("appellantNameForDisplay", new TextNode("Bob Smith"));
                put("appellantTitle", new TextNode("Mr"));
                put("applicationOutOfTimeExplanation", new TextNode("test case"));
                put("caseManagementLocation", objectMapper.readTree("{\"baseLocation\":\"765324\",\"region\":"
                    + "\"1\"}"));
                put("currentCaseStateVisibleToLegalRepresentative", new TextNode("appealStarted"));
                put("homeOfficeDecisionDate", new TextNode("2019-08-01"));
                put("homeOfficeReferenceNumber", new TextNode("000123456"));
                put("legalRepCompanyAddress", objectMapper.readTree("{\"AddressLine1\":\"\",\"AddressLine2\":"
                    + "\"\",\"AddressLine3\":\"\",\"Country\":\"\",\"PostCode\":\"\",\"PostTown\":\"\"}"));
                put("legalRepCompanyName", new TextNode(""));
                put("staffLocation", new TextNode("Taylor House"));
                put("submissionOutOfTime", new TextNode("Yes"));
                put("subscriptions", objectMapper.readTree("[{\"id\":\"1\",\"value\":{\"email\":"
                    + "\"test@example.com\",\"mobileNumber\":\"0111111111\",\"subscriber\":\"appellant\","
                    + "\"wantsEmail\":\"Yes\",\"wantsSms\":\"Yes\"}}]"));
                put("uploadAddendumEvidenceLegalRepActionAvailable", new TextNode("No"));
                put("uploadAdditionalEvidenceActionAvailable", new TextNode("No"));
                put("uploadTheNoticeOfDecisionDocs", objectMapper.readTree("[{\"id\":\"1\",\"value\":"
                    + "{\"description\":\"some notice of decision description\",\"document\":{\"document_binary_url\":"
                    + "\"http://dm-store-aat.service.core-compute-aat.internal/documents/"
                    + "7f63ca9b-c361-49ab-aa8c-8fbdb6bc2936\",\"document_filename\":"
                    + "\"some-notice-of-decision-letter.pdf\",\"document_url\":"
                    + "\"http://dm-store-aat.service.core-compute-aat.internal/documents/"
                    + "7f63ca9b-c361-49ab-aa8c-8fbdb6bc2936\"}}}]"));
            }
        });
        return caseDetails;
    }
}
