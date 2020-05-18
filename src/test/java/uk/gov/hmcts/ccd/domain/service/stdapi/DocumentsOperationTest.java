package uk.gov.hmcts.ccd.domain.service.stdapi;

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

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

@AutoConfigureWireMock(port = 0)
@DirtiesContext
public class DocumentsOperationTest extends BaseTest {

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

        documentsOperation.getPrintableDocumentsForCase(TEST_CASE_REFERENCE);
    }

    @Test
    public void shouldReturnNoDocumentsIfNoDocumentsRetrieved() throws Exception {
        stubFor(post(urlMatching(TEST_URL + ".*"))
            .willReturn(okJson(mapper.writeValueAsString(new ArrayList<>())).withStatus(200)));

        final List<Document> results = documentsOperation.getPrintableDocumentsForCase(TEST_CASE_REFERENCE);
        assertEquals("Incorrect number of documents", 0, results.size());
    }

    @Test
    public void shouldReturnDocumentsIfDocumentsRetrieved() throws Exception {
        final List<Document> TEST_DOCUMENTS = buildDocuments();
        stubFor(post(urlMatching(TEST_URL + ".*"))
            .willReturn(okJson(mapper.writeValueAsString(TEST_DOCUMENTS)).withStatus(200)));

        final List<Document> results = documentsOperation.getPrintableDocumentsForCase(TEST_CASE_REFERENCE);
        assertEquals("Incorrect number of documents", TEST_DOCUMENTS.size(), results.size());

        for (int i = 0; i < results.size(); i++) {
            assertEquals("Incorrect description", results.get(i).getDescription(), TEST_DOCUMENTS.get(i).getDescription());
            assertEquals("Incorrect name", results.get(i).getName(), TEST_DOCUMENTS.get(i).getName());
            assertEquals("Incorrect url", results.get(i).getUrl(), TEST_DOCUMENTS.get(i).getUrl());
            assertEquals("Incorrect type", results.get(i).getType(), TEST_DOCUMENTS.get(i).getType());
        }
    }

    private List<Document> buildDocuments() {
        final Document TEST_DOC_1 = new Document();
        TEST_DOC_1.setDescription("TEST_DOC_1_DESC");
        TEST_DOC_1.setName("TEST_DOC_1_NAME");
        TEST_DOC_1.setType("TEST_DOC_1_TYPE");
        TEST_DOC_1.setUrl("TEST_DOC_1_URL");
        final Document TEST_DOC_2 = new Document();
        TEST_DOC_2.setDescription("TEST_DOC_2_DESC");
        TEST_DOC_2.setName("TEST_DOC_2_NAME");
        TEST_DOC_2.setType("TEST_DOC_2_TYPE");
        TEST_DOC_2.setUrl("TEST_DOC_2_URL");
        final List<Document> TEST_DOCUMENTS = new ArrayList<>();
        TEST_DOCUMENTS.add(TEST_DOC_1);
        TEST_DOCUMENTS.add(TEST_DOC_2);
        return TEST_DOCUMENTS;
    }
}
