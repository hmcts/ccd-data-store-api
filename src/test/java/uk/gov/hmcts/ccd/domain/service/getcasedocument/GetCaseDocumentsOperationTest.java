package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentPermissions;
import uk.gov.hmcts.ccd.v2.external.domain.Permission;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public class GetCaseDocumentsOperationTest {

    @Mock
    private GetCaseOperation getCaseOperation;
    @Mock
    private DocumentIdValidationService documentIdValidationService;
    private GetCaseDocumentOperation caseDocumentsOperation;

    private static final String CASE_REFERENCE = "1234123412341238";
    private static final String CASE_TYPE_ID = "BEFTA_CASETYPE_2_1";
    private static final String JURISDICTION_ID = "BEFTA_JURISDICTION_2";

    private static final String CASE_DOCUMENT_ID = "a780ee98-3136-4be9-bf56-a46f8da1bc97";
    private static final String CASE_DOCUMENT_ID_IN_COMPLEX_TYPE = "84f04693-56ae-4aad-97e8-d1fc7592acea";
    private static final String CASE_DOCUMENT_ID_IN_A_COLLECTION = "f6d623f2-db67-4a01-ae6e-3b6ee14a8b20";
    private static final String CASE_DOCUMENT_ID_IN_A_COMPLEX_COLLECTION = "19de0db3-37c6-4191-a81d-c31a1379a9ca";
    private static final String CASE_DOCUMENT_ID_INVALID = "a780ee98-3136-4be9-bf56-a46f8da1bc9@";
    private static final String CASE_DOCUMENT_ID_NOT_IN_CASE_DETAILS = "a780ee98-3136-4be9-bf56-a46f8da1bc85";

    private final DocumentPermissions documentPermissions = DocumentPermissions.builder()
        .id(CASE_DOCUMENT_ID)
        .permissions(Arrays.asList(Permission.READ))
        .build();

    private  CaseDetails caseDetails;

    @BeforeEach
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);
        caseDocumentsOperation = new GetCaseDocumentOperation(getCaseOperation, documentIdValidationService);

        caseDetails = prepareCaseDetails();
        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);

        doReturn(Boolean.TRUE).when(documentIdValidationService).validateDocumentUUID(anyString());
    }


    @Test
    @DisplayName("should throw Bad Request exception when document Id is invalid")
    void shouldThrowBadRequestWhenDocumentIdInvalid() {
        doReturn(Boolean.FALSE).when(documentIdValidationService).validateDocumentUUID(CASE_DOCUMENT_ID_INVALID);
        assertAll(
            () -> assertThrows(BadRequestException.class,
                () -> caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID_INVALID)),
            () -> verify(documentIdValidationService).validateDocumentUUID(CASE_DOCUMENT_ID_INVALID)
        );
    }

    @Test
    @DisplayName("should throw Bad Request exception when document Id is null")
    void shouldThrowBadRequestWhenDocumentIdNull() {
        doReturn(Boolean.FALSE).when(documentIdValidationService).validateDocumentUUID(null);
        assertThrows(BadRequestException.class,
            () -> caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, null));
    }

    @Test
    @DisplayName("should throw CaseNotFoundException when case does not exist")
    void shouldThrowCaseNotFoundWhenCaseNotExist() {
        doReturn(Optional.empty()).when(getCaseOperation).execute(CASE_REFERENCE);
        assertThrows(CaseNotFoundException.class,
            () -> caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID));
    }

    @Test
    @DisplayName("should throw CaseNotFoundException when case reference  is null ")
    void shouldThrowCaseNotFoundWhenCaseReferenceIsNull() {
        doReturn(Optional.empty()).when(getCaseOperation).execute(null);
        assertThrows(CaseNotFoundException.class,
            () -> caseDocumentsOperation.getCaseDocumentMetadata(null, CASE_DOCUMENT_ID));
    }

    @Test
    @DisplayName("should return CaseDocumentMetadata for simple document")
    void shouldReturnGetCaseDocumentMetadataForSimpleDocument() {
        CaseDocumentMetadata caseDocumentMetadata =
            caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID);

        assertAll(
            () -> assertThat(caseDocumentMetadata.getCaseId(), is(caseDetails.getReferenceAsString())),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getId(), is(CASE_DOCUMENT_ID)),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getPermissions(),
                                is(documentPermissions.getPermissions()))
        );
    }

    @Test
    @DisplayName("should return CaseDocumentMetadata for document in a complex type")
    void shouldReturnGetCaseDocumentMetadataForComplexDocument() {

        CaseDocumentMetadata caseDocumentMetadata =
            caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID_IN_COMPLEX_TYPE);

        assertAll(
            () -> assertThat(caseDocumentMetadata.getCaseId(), is(caseDetails.getReferenceAsString())),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getId(),
                is(CASE_DOCUMENT_ID_IN_COMPLEX_TYPE)),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getPermissions(),
                is(documentPermissions.getPermissions()))
        );
    }

    @Test
    @DisplayName("should return CaseDocumentMetadata for document in a collection")
    void shouldReturnGetCaseDocumentMetadataForCollectionDocument() {

        CaseDocumentMetadata caseDocumentMetadata =
            caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID_IN_A_COLLECTION);

        assertAll(
            () -> assertThat(caseDocumentMetadata.getCaseId(), is(caseDetails.getReferenceAsString())),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getId(),
                is(CASE_DOCUMENT_ID_IN_A_COLLECTION)),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getPermissions(),
                is(documentPermissions.getPermissions()))
        );
    }

    @Test
    @DisplayName("should return CaseDocumentMetadata for document in a collection of complex type")
    void shouldReturnGetCaseDocumentMetadataForDocumentInCollectionOfComplexType() {

        CaseDocumentMetadata caseDocumentMetadata =
            caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID_IN_A_COMPLEX_COLLECTION);

        assertAll(
            () -> assertThat(caseDocumentMetadata.getCaseId(), is(caseDetails.getReferenceAsString())),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getId(),
                is(CASE_DOCUMENT_ID_IN_A_COMPLEX_COLLECTION)),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getPermissions(),
                is(documentPermissions.getPermissions()))
        );
    }

    @Test
    @DisplayName("should throw CaseDocumentNotFoundException when document is not found")
    void shouldThrowCaseDocumentNotFoundExceptionWhenDocumentNotAccessible() throws IOException {

        assertThrows(CaseDocumentNotFoundException.class,
            () -> caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID_NOT_IN_CASE_DETAILS));
    }

    static HashMap<String, JsonNode> buildCaseDetailData(String fileName) throws IOException {
        InputStream inputStream =
            GetCaseDocumentsOperationTest.class.getClassLoader().getResourceAsStream("tests/".concat(fileName));
        return
            new ObjectMapper().readValue(inputStream, new TypeReference<>() {
            });
    }

    private CaseDetails prepareCaseDetails() throws IOException  {
        HashMap<String,JsonNode> caseDetailsData = buildCaseDetailData("case-details-data.json");
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setReference(Long.valueOf(CASE_REFERENCE));
        caseDetails.setData(caseDetailsData);
        return caseDetails;
    }
}

