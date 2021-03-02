package uk.gov.hmcts.ccd.domain.service.stdapi;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
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
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

public class DocumentsOperationTest extends WireMockBaseTest {
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
        ReflectionTestUtils.setField(documentsOperation, "securityUtils", securityUtils);

        caseDetails.setJurisdiction(TEST_JURISDICTION);
        caseDetails.setCaseTypeId(TEST_CASE_TYPE);

        setupUIDService();

        final CaseDetailsRepository mockCaseDetailsRepository = Mockito.mock(DefaultCaseDetailsRepository.class);
        Mockito.when(mockCaseDetailsRepository.findByReference(TEST_CASE_REFERENCE)).thenReturn(caseDetailsOptional);
        ReflectionTestUtils.setField(documentsOperation, "caseDetailsRepository", mockCaseDetailsRepository);

        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setPrintableDocumentsUrl("http://localhost:" + wiremockPort + TEST_URL);
        final CaseTypeService mockCaseTypeService = Mockito.mock(CaseTypeService.class);
        Mockito.when(mockCaseTypeService.getCaseTypeForJurisdiction(TEST_CASE_TYPE, TEST_JURISDICTION))
                .thenReturn(caseTypeDefinition);
        ReflectionTestUtils.setField(documentsOperation, "caseTypeService", mockCaseTypeService);
    }

    private void setupUIDService() {
        reset(uidService);
        when(uidService.validateUID(anyString())).thenCallRealMethod();
        when(uidService.checkSum(anyString())).thenCallRealMethod();
        when(uidService.checkSum(anyString(), anyBoolean())).thenCallRealMethod();
    }

    @Test(expected = BadRequestException.class)
    public void shouldThrowBadRequestExceptionIfCaseReferenceInvalid() {
        final String testCaseReference = "Invalid";

        documentsOperation.getPrintableDocumentsForCase(testCaseReference);
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
        final List<Document> testDocuments = buildDocuments();
        stubFor(post(urlMatching(TEST_URL + ".*"))
                    .willReturn(okJson(mapper.writeValueAsString(testDocuments)).withStatus(200)));

        final List<Document> results = documentsOperation.getPrintableDocumentsForCase(TEST_CASE_REFERENCE);
        assertEquals("Incorrect number of documents", testDocuments.size(), results.size());

        for (int i = 0; i < results.size(); i++) {
            assertEquals("Incorrect description", results.get(i).getDescription(), testDocuments.get(i)
                    .getDescription());
            assertEquals("Incorrect name",results.get(i).getName(), testDocuments.get(i).getName());
            assertEquals("Incorrect url",results.get(i).getUrl(), testDocuments.get(i).getUrl());
            assertEquals("Incorrect type",results.get(i).getType(), testDocuments.get(i).getType());
        }
    }

    private List<Document> buildDocuments() {
        final Document testDoc1 = new Document();
        testDoc1.setDescription("TEST_DOC_1_DESC");
        testDoc1.setName("TEST_DOC_1_NAME");
        testDoc1.setType("TEST_DOC_1_TYPE");
        testDoc1.setUrl("TEST_DOC_1_URL");
        final Document testDoc2 = new Document();
        testDoc2.setDescription("TEST_DOC_2_DESC");
        testDoc2.setName("TEST_DOC_2_NAME");
        testDoc2.setType("TEST_DOC_2_TYPE");
        testDoc2.setUrl("TEST_DOC_2_URL");
        final List<Document> testDocuments = new ArrayList<>();
        testDocuments.add(testDoc1);
        testDocuments.add(testDoc2);
        return testDocuments;
    }
}
