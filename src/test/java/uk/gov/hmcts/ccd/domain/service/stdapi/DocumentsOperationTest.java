package uk.gov.hmcts.ccd.domain.service.stdapi;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.launchdarkly.client.LDClient;
import com.launchdarkly.client.LDUser;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

@AutoConfigureWireMock(port = 0)
@DirtiesContext
public class DocumentsOperationTest extends BaseTest {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Value("${wiremock.server.port}")
    protected Integer wiremockPort;

    private CaseDetails caseDetails = new CaseDetails();
    private Optional<CaseDetails> caseDetailsOptional = Optional.of(caseDetails);

    @Inject
    private DocumentsOperation documentsOperation;

    @Inject
    protected UIDService uidService;
    public static final String TEST_CASE_TYPE = "TEST_CASE_TYPE";
    public static final String TEST_JURISDICTION = "TEST_JURISDICTION";
    public static final String TEST_CASE_REFERENCE = "1504259907353537";
    public static final String TEST_URL = "/test-document-callback";

    @Before
    public void setUp() {
        final SecurityUtils securityUtils = Mockito.mock(SecurityUtils.class);
        Mockito.when(securityUtils.authorizationHeaders()).thenReturn(new HttpHeaders());
        ReflectionTestUtils.setField(documentsOperation, "securityUtils", securityUtils);

        caseDetails.setJurisdiction(TEST_JURISDICTION);
        caseDetails.setCaseTypeId(TEST_CASE_TYPE);

        setupUIDService();

        final CaseDetailsRepository mockCaseDetailsRepository = Mockito.mock(DefaultCaseDetailsRepository.class);
        Mockito.when(mockCaseDetailsRepository.findByReference(TEST_CASE_REFERENCE)).thenReturn(caseDetailsOptional);
        ReflectionTestUtils.setField(documentsOperation, "caseDetailsRepository", mockCaseDetailsRepository);

        final CaseType caseType = new CaseType();
        caseType.setPrintableDocumentsUrl("http://localhost:" + wiremockPort + TEST_URL);
        final CaseTypeService mockCaseTypeService = Mockito.mock(CaseTypeService.class);
        Mockito.when(mockCaseTypeService.getCaseTypeForJurisdiction(TEST_CASE_TYPE, TEST_JURISDICTION)).thenReturn(caseType);
        ReflectionTestUtils.setField(documentsOperation, "caseTypeService", mockCaseTypeService);

        final LDClient mockLdClient = Mockito.mock(LDClient.class);
        Mockito.when(mockLdClient.boolVariation(any(String.class), any(LDUser.class), any(Boolean.class))).thenReturn(false);
        ReflectionTestUtils.setField(documentsOperation, "ldClient", mockLdClient);
    }

    private void setupUIDService() {
        reset(uidService);
        when(uidService.validateUID(anyString())).thenCallRealMethod();
        when(uidService.checkSum(anyString())).thenCallRealMethod();
        when(uidService.checkSum(anyString(), anyBoolean())).thenCallRealMethod();
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestExceptionIfCaseReferenceInvalid() throws Exception {
        final String TEST_CASE_REFERENCE = "Invalid";

        documentsOperation.getPrintableDocumentsForCase(TEST_CASE_REFERENCE, "ccd-data-store-api");
    }

    @Test
    public void shouldReturnDocumentsIfDocumentsRetrieved() throws Exception {
        final List<Document> TEST_DOCUMENTS = buildDocuments();
        stubFor(post(urlMatching(TEST_URL + ".*"))
                    .willReturn(okJson(mapper.writeValueAsString(TEST_DOCUMENTS)).withStatus(200)));

        final List<Document> results = documentsOperation.getPrintableDocumentsForCase(TEST_CASE_REFERENCE,
            "ccd-data-store-api");
        assertEquals("Incorrect number of documents", TEST_DOCUMENTS.size(), results.size());

        for (int i = 0; i < results.size(); i++) {
            assertEquals("Incorrect description", results.get(i).getDescription(), TEST_DOCUMENTS.get(i).getDescription());
            assertEquals("Incorrect name",results.get(i).getName(), TEST_DOCUMENTS.get(i).getName());
            assertEquals("Incorrect url",results.get(i).getUrl(), TEST_DOCUMENTS.get(i).getUrl());
            assertEquals("Incorrect type",results.get(i).getType(), TEST_DOCUMENTS.get(i).getType());
        }
    }

    @Test
    public void shouldReturnDocumentsFromCaseDocumentApiIfFlagSet() {
        final LDClient mockLdClient = Mockito.mock(LDClient.class);
        Mockito.when(mockLdClient.boolVariation(any(String.class), any(LDUser.class), any(Boolean.class))).thenReturn(true);
        ReflectionTestUtils.setField(documentsOperation, "ldClient", mockLdClient);

        Document expectedDocument = new Document();
        expectedDocument.setName("SAMPLE FROM NEW CASE DOCUMENT API");
        expectedDocument.setDescription("SAMPLE FROM NEW CASE DOCUMENT API");
        expectedDocument.setType("SAMPLE FROM NEW CASE DOCUMENT API");
        expectedDocument.setUrl("SAMPLE FROM NEW CASE DOCUMENT API");
        List<Document> expectedResponse = ImmutableList.of(expectedDocument);

        final List<Document> actualResponse = documentsOperation.getPrintableDocumentsForCase(TEST_CASE_REFERENCE,
            "ccd-data-store-api");

        for (int i = 0; i < actualResponse.size(); i++) {
            assertEquals("Incorrect description", actualResponse.get(i).getDescription(), expectedResponse.get(i).getDescription());
            assertEquals("Incorrect name", actualResponse.get(i).getName(), expectedResponse.get(i).getName());
            assertEquals("Incorrect url", actualResponse.get(i).getUrl(), expectedResponse.get(i).getUrl());
            assertEquals("Incorrect type", actualResponse.get(i).getType(), expectedResponse.get(i).getType());
        }
    }

    private List<Document> buildDocuments() {
        Document documentFromDocStoreApi = new Document();
        documentFromDocStoreApi.setName("Screenshot 2019-09-26 at 13.06.47.png");
        documentFromDocStoreApi.setDescription("Evidence screen capture");
        documentFromDocStoreApi.setType("png");
        documentFromDocStoreApi.setUrl("http://dm-store-aat.service.core-compute-aat.internal"
            + "/documents/1d9e2f5f-2114-4748-b01c-70481000ce6d/binary");
        return ImmutableList.of(documentFromDocStoreApi);
    }
}
