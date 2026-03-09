package uk.gov.hmcts.ccd.domain.service.stdapi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Collections;
import java.util.Set;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

@DirtiesContext
public class DocumentsOperationTest extends WireMockBaseTest {

    private final CaseDetails caseDetails = new CaseDetails();
    private final Optional<CaseDetails> caseDetailsOptional = Optional.of(caseDetails);

    @Inject
    private DocumentsOperation documentsOperation;

    @Inject
    protected UIDService uidService;

    public static final String TEST_CASE_TYPE = "TEST_CASE_TYPE";
    public static final String TEST_JURISDICTION = "TEST_JURISDICTION";
    public static final String TEST_CASE_REFERENCE = "1504259907353537";
    public static final String TEST_URL = "/test-document-callback";

    private AccessControlService accessControlService;

    @BeforeEach
    public void setUp() {
        ReflectionTestUtils.setField(documentsOperation, "securityUtils", securityUtils);

        caseDetails.setReference(Long.valueOf(TEST_CASE_REFERENCE));
        caseDetails.setJurisdiction(TEST_JURISDICTION);
        caseDetails.setCaseTypeId(TEST_CASE_TYPE);

        setupUIDService();
        setupCaseDetailsRepository();
        setupCaseTypeService();
        setupAuthorisationMocks(true);
    }

    private void setupUIDService() {
        reset(uidService);
        when(uidService.validateUID(anyString())).thenCallRealMethod();
        when(uidService.checkSum(anyString())).thenCallRealMethod();
        when(uidService.checkSum(anyString(), anyBoolean())).thenCallRealMethod();
    }

    private void setupCaseDetailsRepository() {
        CaseDetailsRepository mockRepo = Mockito.mock(DefaultCaseDetailsRepository.class);
        when(mockRepo.findByReference(TEST_CASE_REFERENCE)).thenReturn(caseDetailsOptional);
        ReflectionTestUtils.setField(documentsOperation, "caseDetailsRepository", mockRepo);
    }

    private void setupCaseTypeService() {
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId(TEST_CASE_TYPE);
        caseTypeDefinition.setPrintableDocumentsUrl(hostUrl + TEST_URL);

        CaseTypeService mockCaseTypeService = Mockito.mock(CaseTypeService.class);
        when(mockCaseTypeService.getCaseTypeForJurisdiction(TEST_CASE_TYPE, TEST_JURISDICTION))
            .thenReturn(caseTypeDefinition);

        ReflectionTestUtils.setField(documentsOperation, "caseTypeService", mockCaseTypeService);
    }

    private void setupAuthorisationMocks(boolean allowAccess) {
        CaseAccessService caseAccessService = Mockito.mock(CaseAccessService.class);
        accessControlService = Mockito.mock(AccessControlService.class);

        Set<AccessProfile> profiles = allowAccess
            ? Set.of(new AccessProfile("caseworker"))
            : Collections.emptySet();

        when(caseAccessService.getAccessProfilesByCaseReference(TEST_CASE_REFERENCE))
            .thenReturn(profiles);

        when(accessControlService.canAccessCaseTypeWithCriteria(
            any(), any(), any()))
            .thenReturn(allowAccess);

        ReflectionTestUtils.setField(documentsOperation, "caseAccessService", caseAccessService);
        ReflectionTestUtils.setField(documentsOperation, "accessControlService", accessControlService);
    }

    @Test
    public void shouldThrowBadRequestExceptionIfCaseReferenceInvalid() {
        assertThrows(BadRequestException.class,
            () -> documentsOperation.getPrintableDocumentsForCase("Invalid"));
    }

    @Test
    public void shouldReturnNoDocumentsIfNoDocumentsRetrieved() throws Exception {
        stubFor(post(urlMatching(TEST_URL + ".*"))
            .willReturn(okJson(mapper.writeValueAsString(new ArrayList<>())).withStatus(200)));

        List<Document> results =
            documentsOperation.getPrintableDocumentsForCase(TEST_CASE_REFERENCE);

        Assertions.assertEquals(0, results.size());
    }

    @Test
    public void shouldReturnDocumentsIfDocumentsRetrieved() throws Exception {
        List<Document> testDocuments = buildDocuments();

        stubFor(post(urlMatching(TEST_URL + ".*"))
            .willReturn(okJson(mapper.writeValueAsString(testDocuments)).withStatus(200)));

        List<Document> results =
            documentsOperation.getPrintableDocumentsForCase(TEST_CASE_REFERENCE);

        Assertions.assertEquals(testDocuments.size(), results.size());
        Assertions.assertEquals(testDocuments.getFirst().getName(), results.getFirst().getName());
    }

    @Test
    public void shouldThrowValidationExceptionIfUserHasNoCaseAccess() {
        setupAuthorisationMocks(false);

        assertThrows(ValidationException.class,
            () -> documentsOperation.getPrintableDocumentsForCase(TEST_CASE_REFERENCE));
    }

    @Test
    public void shouldThrowNotFoundIfUserLacksCaseTypeReadAccess() {
        setupAuthorisationMocks(true);

        when(accessControlService.canAccessCaseTypeWithCriteria(
            any(), any(), any()))
            .thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
            () -> documentsOperation.getPrintableDocumentsForCase(TEST_CASE_REFERENCE));
    }

    @Test
    public void shouldReturnEmptyListWhenDocumentServiceReturnsNull() {
        stubFor(post(urlMatching(TEST_URL + ".*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")));

        List<Document> results =
            documentsOperation.getPrintableDocumentsForCase(TEST_CASE_REFERENCE);

        assertNotNull(results);
        Assertions.assertEquals(0, results.size());
    }

    @Test
    public void shouldThrowServiceExceptionWhenPrintableDocumentsUrlIsNull() {
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId(TEST_CASE_TYPE);
        caseTypeDefinition.setPrintableDocumentsUrl(null);

        CaseTypeService mockCaseTypeService = Mockito.mock(CaseTypeService.class);
        when(mockCaseTypeService.getCaseTypeForJurisdiction(TEST_CASE_TYPE, TEST_JURISDICTION))
            .thenReturn(caseTypeDefinition);

        ReflectionTestUtils.setField(documentsOperation, "caseTypeService", mockCaseTypeService);

        assertThrows(ServiceException.class,
            () -> documentsOperation.getPrintableDocumentsForCase(TEST_CASE_REFERENCE));
    }

    @Test
    public void shouldThrowServiceExceptionWhenPrintableDocumentsUrlIsEmpty() {
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setId(TEST_CASE_TYPE);
        caseTypeDefinition.setPrintableDocumentsUrl("");

        CaseTypeService mockCaseTypeService = Mockito.mock(CaseTypeService.class);
        when(mockCaseTypeService.getCaseTypeForJurisdiction(TEST_CASE_TYPE, TEST_JURISDICTION))
            .thenReturn(caseTypeDefinition);

        ReflectionTestUtils.setField(documentsOperation, "caseTypeService", mockCaseTypeService);

        assertThrows(ServiceException.class,
            () -> documentsOperation.getPrintableDocumentsForCase(TEST_CASE_REFERENCE));
    }

    private List<Document> buildDocuments() {
        Document d1 = new Document();
        d1.setName("DOC1");
        d1.setDescription("DESC1");
        d1.setType("TYPE1");
        d1.setUrl("URL1");

        Document d2 = new Document();
        d2.setName("DOC2");
        d2.setDescription("DESC2");
        d2.setType("TYPE2");
        d2.setUrl("URL2");

        return List.of(d1, d2);
    }
}
