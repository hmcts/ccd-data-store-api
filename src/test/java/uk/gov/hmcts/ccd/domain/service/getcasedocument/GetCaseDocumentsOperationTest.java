package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocument;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;
import uk.gov.hmcts.ccd.v2.external.domain.Permission;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;

public class GetCaseDocumentsOperationTest {

    private static final ObjectMapper mapper = new ObjectMapper();
    public static final String TEST_CASE_TYPE = "TEST_CASE_TYPE";
    public static final String TEST_JURISDICTION = "TEST_JURISDICTION";
    public static final String TEST_CASE_REFERENCE = "1504259907353537";
    public static final String TEST_DOCUMENT_ID = "a780ee98-3136-4be9-bf56-a46f8da1bc97";
    public static final String TEST_DOCUMENT_INVALID = "a780ee98-3136-4be9-bf56-a46f8da1bc9@";
    private static final String USER_ID = "26";
    private static final String CASEWORKER_PROBATE_LOA1 = "caseworker-probate-loa1";
    private static final String CASEWORKER_PROBATE_LOA3 = "caseworker-probate-loa3";
    private static final String CASEWORKER_DIVORCE = "caseworker-divorce-loa3";
    private static final String CASE_REFERENCE = "1234123412341238";
    private static final String CASE_TYPE_ID = "BEFTA_CASETYPE_2_1";
    private static final String JURISDICTION_ID = "BEFTA_JURISDICTION_2";
    private static final String CASE_DOCUMENT_ID = "a780ee98-3136-4be9-bf56-a46f8da1bc97";
    private static final String DOCUMENT_URL = "http://dm-store:8080/documents/a780ee98-3136-4be9-bf56-a46f8da1bc97";
    private static final String DOCUMENT_NAME = "Sample_document.txt";
    private static final String DOCUMENT_TYPE = "Document";

    @Mock
    private GetCaseOperation getCaseOperation;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CaseUserRepository caseUserRepository;
    @Mock
    private CaseTypeService caseTypeService;
    @Mock
    private DocumentIdValidationService documentIdValidationService;

    @Mock
    private GetCaseDocumentOperation caseDocumentsOperation;


    private CaseDetails caseDetails = new CaseDetails();
    private Optional<CaseDetails> caseDetailsOptional;
    private final CaseType caseType = new CaseType();
    private final Set<String> userRoles = Sets
        .newHashSet(CASEWORKER_DIVORCE, CASEWORKER_PROBATE_LOA1, CASEWORKER_PROBATE_LOA3);
    private final List<String> caseRoles = Collections.emptyList();
    private final CaseDocumentMetadata caseDocumentMetadata =
        CaseDocumentMetadata.builder().caseId("CaseId").caseTypeId("CaseTypeId").build();
    private final CaseDocument caseDocument = CaseDocument.builder()
        .id(CASE_DOCUMENT_ID)
        .url(DOCUMENT_URL)
        .name(DOCUMENT_NAME)
        .type(DOCUMENT_TYPE)
        .permissions(Arrays.asList(Permission.READ, Permission.UPDATE))
        .build();
    private String documentCaseField = "DocumentField1";
    Map<String, JsonNode> caseData = caseDetails.getData();

    //private CaseField DocumentCaseField = newCaseField().withId("DocumentField1")
    // .withFieldType(aFieldType().withId("DocumentField1").withType("Document").build()).build();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetailsOptional = Optional.of(new CaseDetails());
        doReturn(caseDetailsOptional).when(getCaseOperation).execute(TEST_CASE_REFERENCE);
        doReturn(userRoles).when(userRepository).getUserRoles();
        doReturn(USER_ID).when(userRepository).getUserId();
        doReturn(caseRoles).when(caseUserRepository).findCaseRoles(Long.valueOf(TEST_CASE_REFERENCE), USER_ID);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(TEST_CASE_TYPE, TEST_JURISDICTION);
        doReturn(Boolean.TRUE).when(documentIdValidationService).validateDocumentUUID(TEST_DOCUMENT_ID);
        doReturn(Boolean.FALSE).when(documentIdValidationService).validateDocumentUUID(TEST_DOCUMENT_INVALID);
    //caseDocumentsOperation = new GetCaseDocumentOperation(getCaseOperation,caseTypeService,userRepository,caseUserRepository,documentIdValidationService);
    }


    @Nested
    @DisplayName("getDocumentMetadata")
    class getDocumentMetadata {
        @Test
        @DisplayName("should return CaseDocumentMetadata")
        void shouldCallGetDocumentMetadata() {
            doReturn(caseDocumentMetadata).when(caseDocumentsOperation).getCaseDocumentMetadata(TEST_CASE_REFERENCE,TEST_DOCUMENT_ID);
            //doReturn(new CaseDocument()).when(caseDocumentsOperation).getCaseDocument(new CaseDetails(),TEST_DOCUMENT_ID);
            CaseDocumentMetadata result = caseDocumentsOperation.getCaseDocumentMetadata(TEST_CASE_REFERENCE,TEST_DOCUMENT_ID);
            assertAll(
                () -> assertThat(result.getCaseId(), is(caseDocumentMetadata.getCaseId())),
                () -> assertThat(result.getCaseTypeId(), is(caseDocumentMetadata.getCaseTypeId()))
            );
        }

        @Test
        @DisplayName("should return CaseDocument")
        void shouldCallGetCaseDocument() {
            doReturn(caseDocument).when(caseDocumentsOperation).getCaseDocument(caseDetails,TEST_DOCUMENT_ID);
            CaseDocument result = caseDocumentsOperation.getCaseDocument(caseDetails,TEST_DOCUMENT_ID);
            assertAll(
                () -> assertThat(result.getId(), is(caseDocument.getId())),
                () -> assertThat(result.getUrl(), is(caseDocument.getUrl())),
                () -> assertThat(result.getName(), is(caseDocument.getName())),
                () -> assertThat(result.getType(), is(caseDocument.getType()))
            );
        }

        @Test
        @DisplayName("should return Document CaseField")
        void shouldCallGetDocumentCaseField() {
            doReturn(documentCaseField).when(caseDocumentsOperation).getDocumentCaseField(caseData,TEST_DOCUMENT_ID);
            String result = caseDocumentsOperation.getDocumentCaseField(caseData,TEST_DOCUMENT_ID);
            assertAll(
                () -> assertThat(result, is(documentCaseField))
            );
        }

    }


}
