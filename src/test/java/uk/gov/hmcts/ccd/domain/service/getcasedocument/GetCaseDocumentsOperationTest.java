package uk.gov.hmcts.ccd.domain.service.getcasedocument;

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
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;

import java.util.Collections;
import java.util.List;
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


    //private CaseDetails caseDetails = new CaseDetails();
    private Optional<CaseDetails> caseDetails;
    private final CaseType caseType = new CaseType();
    private final Set<String> userRoles = Sets
        .newHashSet(CASEWORKER_DIVORCE, CASEWORKER_PROBATE_LOA1, CASEWORKER_PROBATE_LOA3);
    private final List<String> caseRoles = Collections.emptyList();
    private final CaseDocumentMetadata caseDocumentMetadata =
        CaseDocumentMetadata.builder().caseId("CaseId").caseTypeId("CaseTypeId").build();
    //private CaseField DocumentCaseField = newCaseField().withId("DocumentField1")
    // .withFieldType(aFieldType().withId("DocumentField1").withType("Document").build()).build();

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetails = Optional.of(new CaseDetails());
        doReturn(caseDetails).when(getCaseOperation).execute(TEST_CASE_REFERENCE);
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

    }


}
