package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import java.util.HashMap;
import java.util.List;

import java.util.Optional;
import java.util.Set;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;

import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentPermissions;
import uk.gov.hmcts.ccd.v2.external.domain.Permission;

public class GetCaseDocumentsOperationTest {

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
    private AccessControlService accessControlService;

    private GetCaseDocumentOperation caseDocumentsOperation;

    private static final String CASE_REFERENCE = "1234123412341238";
    private static final String CASE_TYPE_ID = "BEFTA_CASETYPE_2_1";
    private static final String JURISDICTION_ID = "BEFTA_JURISDICTION_2";
    private static final String CASE_DOCUMENT_ID = "a780ee98-3136-4be9-bf56-a46f8da1bc97";
    private static final String CASE_DOCUMENT_ID_INVALID = "a780ee98-3136-4be9-bf56-a46f8da1bc9@";
    private static final String USER_ID = "test_user_id";
    private static final String CASE_DOCUMENT_ID_NOT_IN_CASE_DETAILS = "a780ee98-3136-4be9-bf56-a46f8da1bc85";
    private static final String CASE_DOCUMENT_FIELD_2_ID = "e16f2ae0-d6ce-4bd0-a652-47b3c4d86292";




    private final CaseType caseType = new CaseType();
    private final Set<String> userRoles = Sets.newHashSet("role1", "role2");
    private final List<String> caseRoles = Collections.emptyList();
    private CaseField documentCaseField;
    private CaseField complexCaseField;
    private CaseField collectionCaseField;
    private CaseField documentInComplexCaseField;
    private List<CaseField> caseFields;
    private CaseDetails caseDetails;
    private Optional<CaseDetails> caseDetailsOptional;
    private HashMap<String,JsonNode> caseDetailsData;
    private final DocumentPermissions documentPermissions = DocumentPermissions.builder()
        .id(CASE_DOCUMENT_ID)
        .permissions(Arrays.asList(Permission.READ))
        .build();

    @BeforeEach
    public void setUp() throws IOException {

        MockitoAnnotations.initMocks(this);
        documentCaseField = buildCaseField("document-type-case-field.json");
        complexCaseField = buildCaseField("complex-type-case-field.json");
        collectionCaseField = buildCaseField("collection-type-case-field.json");
        caseDetailsData = buildCaseDetailData("case-details-data.json");
        documentInComplexCaseField = buildCaseField("complex-type-with-document-case-field.json");

        caseFields = Arrays.asList(documentCaseField,complexCaseField,collectionCaseField,documentInComplexCaseField);
        caseDetailsOptional = Optional.of(new CaseDetails());
        doReturn(userRoles).when(userRepository).getUserRoles();
        doReturn(USER_ID).when(userRepository).getUserId();
        doReturn(caseRoles).when(caseUserRepository).findCaseRoles(Long.valueOf(CASE_REFERENCE), USER_ID);
        doReturn(Boolean.TRUE).when(documentIdValidationService).validateDocumentUUID(CASE_DOCUMENT_ID);
        caseDocumentsOperation = new GetCaseDocumentOperation(getCaseOperation, caseTypeService, userRepository, caseUserRepository,
            documentIdValidationService, accessControlService);
    }



    @Test
    @DisplayName("should throw Bad Request exception when document Id is invalid")
    void shouldThrowBadRequestWhenDocumentIdInvalid() {
        doReturn(Boolean.FALSE).when(documentIdValidationService).validateDocumentUUID(CASE_DOCUMENT_ID_INVALID);
        assertAll(
            () -> assertThrows(BadRequestException.class, () -> caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID_INVALID)),
            () -> verify(documentIdValidationService).validateDocumentUUID(CASE_DOCUMENT_ID_INVALID)
        );
    }

    @Test
    @DisplayName("should throw Bad Request exception when document Id is null")
    void shouldThrowBadRequestWhenDocumentIdNull() {
        doReturn(Boolean.FALSE).when(documentIdValidationService).validateDocumentUUID(null);
        assertThrows(BadRequestException.class, () -> caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, null));
    }

    @Test
    @DisplayName("should throw CaseNotFoundException when case does not exist")
    void shouldThrowCaseNotFoundWhenCaseNotExist() {
        doReturn(caseDetailsOptional.empty()).when(getCaseOperation).execute(CASE_REFERENCE);
        assertThrows(CaseNotFoundException.class, () -> caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID));

    }

    @Test
    @DisplayName("should throw CaseNotFoundException when case reference  is null ")
    void shouldThrowCaseNotFoundWhenCaseReferenceIsNull() {
        caseDetails = new CaseDetails();
        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);
        assertThrows(CaseNotFoundException.class, () -> caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID));

    }

    @Test
    @DisplayName("should return CaseDocumentMetadata")
    void shouldCallGetCaseDocumentMetadata() throws IOException {

        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setId(CASE_REFERENCE);
        caseDetails.setReference(new Long(CASE_REFERENCE));
        caseDetails.setState("state1");
        caseDetails.setData(caseDetailsData);
        caseType.setCaseFields(caseFields);
        JsonNode expectedNode = buildJsonNode("document-field-node.json");
        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        doReturn(expectedNode).when(accessControlService).filterCaseFieldsByAccess(
            ArgumentMatchers.any(JsonNode.class),
            ArgumentMatchers.any(List.class),
            ArgumentMatchers.any(Set.class),
            eq(AccessControlService.CAN_READ),
            anyBoolean());

        CaseDocumentMetadata caseDocumentMetadata = caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID);
        assertAll(
            () -> assertThat(caseDocumentMetadata.getCaseId(), is(caseDetails.getReferenceAsString())),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions(), is(documentPermissions)),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getId(), is(documentPermissions.getId())),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getPermissions(), is(documentPermissions.getPermissions()))
        );
    }

    @Test
    @DisplayName("should throw CaseDocumentMetadataException  in case of documentId is not in caseDetails")
    void  shouldThrowCaseDocumentNotFoundExceptionWhenDocumentIdInvalid() throws IOException {

        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setId(CASE_REFERENCE);
        caseDetails.setReference(new Long(CASE_REFERENCE));
        caseDetails.setState("state1");
        caseDetails.setData(caseDetailsData);
        caseType.setCaseFields(caseFields);

        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        JsonNode expectedNode = buildJsonNode("document-field-node.json");
        doReturn(Boolean.TRUE).when(documentIdValidationService).validateDocumentUUID(CASE_DOCUMENT_ID_NOT_IN_CASE_DETAILS);
        doReturn(expectedNode).when(accessControlService).filterCaseFieldsByAccess(
            ArgumentMatchers.any(JsonNode.class),
            ArgumentMatchers.any(List.class),
            ArgumentMatchers.any(Set.class),
            eq(AccessControlService.CAN_READ),
            anyBoolean());

        assertThrows(CaseDocumentNotFoundException.class, () -> caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE,
            CASE_DOCUMENT_ID_NOT_IN_CASE_DETAILS));
    }

    @Test
    @DisplayName("should throw CaseDocumentMetadataException  in case of collection case field only")
    void  shouldThrowCaseDocumentNotFoundExceptionWhenCollectionTypeField() throws IOException {

        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setId(CASE_REFERENCE);
        caseDetails.setReference(new Long(CASE_REFERENCE));
        caseDetails.setState("state1");
        caseDetails.setData(caseDetailsData);
        caseType.setCaseFields(Arrays.asList(collectionCaseField));

        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        JsonNode expectedNode = buildJsonNode("document-field-node.json");
        doReturn(Boolean.TRUE).when(documentIdValidationService).validateDocumentUUID(CASE_DOCUMENT_ID_NOT_IN_CASE_DETAILS);
        doReturn(expectedNode).when(accessControlService).filterCaseFieldsByAccess(
            ArgumentMatchers.any(JsonNode.class),
            ArgumentMatchers.any(List.class),
            ArgumentMatchers.any(Set.class),
            eq(AccessControlService.CAN_READ),
            anyBoolean());

        assertThrows(CaseDocumentNotFoundException.class, () -> caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE,
            CASE_DOCUMENT_ID_NOT_IN_CASE_DETAILS));
    }


    @Test
    @DisplayName("should throw CaseDocumentNotFoundException for no qualifying document fields")
    void shouldThrowCaseDocumentNotFoundException() throws IOException {

        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setId(CASE_REFERENCE);
        caseDetails.setReference(new Long(CASE_REFERENCE));
        caseDetails.setState("state1");

        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        caseType.setCaseFields(Collections.emptyList());
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);

        assertThrows(CaseDocumentNotFoundException.class, () -> caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID));
    }

    @Test
    @DisplayName("should throw CaseNotFoundException if case is not found")
    void shouldThrowCaseNotFoundException() throws IOException {

        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);

        caseDetails.setState("state1");

        assertThrows(CaseNotFoundException.class, () -> caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID));
    }

    @Test
    @DisplayName("should Extract the document fields from CaseDefinition")
    void shouldExtractDocumentFieldsFromCaseDefinition() throws IOException {
        List<CaseField>  inputCaseField = new ArrayList<>();
        List<CaseField>  expectedCaseField = Arrays.asList(documentCaseField,documentInComplexCaseField.getFieldType().getComplexFields().get(3));

        caseDocumentsOperation.extractDocumentFieldsFromCaseDefinition(caseFields,inputCaseField);
        assertAll(
            () -> assertEquals(inputCaseField,expectedCaseField)

        );
    }

    @Test
    @DisplayName("should Extract the document fields from Collection Case Field")
    void shouldExtractDocumentFieldsFromCollectionTypeCaseField() throws IOException {
        List<CaseField> caseFields = Arrays.asList(buildCaseField("collection-type-with-document-case-field.json"));
        List<CaseField>  inputCaseField = new ArrayList<>();
        List<CaseField>  expectedCaseField = Arrays.asList(caseFields.get(0).getFieldType().getComplexFields().get(0));

        caseDocumentsOperation.extractDocumentFieldsFromCaseDefinition(caseFields,inputCaseField);
        assertAll(
            () -> assertEquals(inputCaseField,expectedCaseField)

        );
    }

    @Test
    @DisplayName("should not extract any document field in case of empty list")
    void shouldNotExtractAnyFieldsInCaseOfListEmpty() throws IOException {
        List<CaseField> caseFields = Collections.EMPTY_LIST;
        List<CaseField>  inputCaseField = new ArrayList<>();
        List<CaseField>  expectedCaseField = Collections.EMPTY_LIST;

        caseDocumentsOperation.extractDocumentFieldsFromCaseDefinition(caseFields,inputCaseField);
        assertAll(
            () -> assertEquals(inputCaseField,expectedCaseField)

        );
    }


    @Test
    @DisplayName("should return CaseDocumentMetadata")
    void shouldReturnCaseDocumentMetadataWhenDocumentFieldInsideComplexField() throws IOException {

        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setId(CASE_REFERENCE);
        caseDetails.setReference(new Long(CASE_REFERENCE));
        caseDetails.setState("state1");
        caseDetails.setData(caseDetailsData);
        caseType.setCaseFields(caseFields);
        JsonNode expectedNode = buildJsonNode("document-field-node.json");
        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        doReturn(expectedNode).when(accessControlService).filterCaseFieldsByAccess(
            ArgumentMatchers.any(JsonNode.class),
            ArgumentMatchers.any(List.class),
            ArgumentMatchers.any(Set.class),
            eq(AccessControlService.CAN_READ),
            anyBoolean());

        CaseDocumentMetadata caseDocumentMetadata = caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID);
        assertAll(
            () -> assertThat(caseDocumentMetadata.getCaseId(), is(caseDetails.getReferenceAsString())),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions(), is(documentPermissions)),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getId(), is(documentPermissions.getId())),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getPermissions(), is(documentPermissions.getPermissions()))
        );
    }

    @Test
    @DisplayName("should throw CaseDocumentNotFoundException  if url is  missing")
    void shouldThrowExceptionIfDocumentFieldUrlIsMissing() throws IOException {

        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setId(CASE_REFERENCE);
        caseDetails.setReference(new Long(CASE_REFERENCE));
        caseDetails.setState("state1");
        caseDetails.setData(caseDetailsData);
        caseType.setCaseFields(caseFields);
        JsonNode expectedNode = buildJsonNode("document-fields-without-url.json");
        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        doReturn(Boolean.TRUE).when(documentIdValidationService).validateDocumentUUID(CASE_DOCUMENT_FIELD_2_ID);
        doReturn(expectedNode).when(accessControlService).filterCaseFieldsByAccess(
            ArgumentMatchers.any(JsonNode.class),
            ArgumentMatchers.any(List.class),
            ArgumentMatchers.any(Set.class),
            eq(AccessControlService.CAN_READ),
            anyBoolean());

        assertThrows(CaseDocumentNotFoundException.class, () -> caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE,
            CASE_DOCUMENT_FIELD_2_ID));


    }

    @Test
    @DisplayName("should return CaseDocumentMetaData  if Binary url is  missing")
    void shouldReturnCaseDocumentMetaData() throws IOException {

        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setId(CASE_REFERENCE);
        caseDetails.setReference(new Long(CASE_REFERENCE));
        caseDetails.setState("state1");
        caseDetails.setData(caseDetailsData);
        caseType.setCaseFields(caseFields);
        JsonNode expectedNode = buildJsonNode("document-fields-without-url.json");
        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        doReturn(Boolean.TRUE).when(documentIdValidationService).validateDocumentUUID(CASE_DOCUMENT_ID);
        doReturn(expectedNode).when(accessControlService).filterCaseFieldsByAccess(
            ArgumentMatchers.any(JsonNode.class),
            ArgumentMatchers.any(List.class),
            ArgumentMatchers.any(Set.class),
            eq(AccessControlService.CAN_READ),
            anyBoolean());

        CaseDocumentMetadata caseDocumentMetadata = caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID);
        assertAll(
            () -> assertThat(caseDocumentMetadata.getCaseId(), is(caseDetails.getReferenceAsString())),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions(), is(documentPermissions)),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getId(), is(documentPermissions.getId())),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getPermissions(), is(documentPermissions.getPermissions()))
        );


    }

    @Test
    @DisplayName("should return CaseDocumentMetaData  if Collection Case Field is missing")
    void shouldReturnCaseDocumentMetaDataWithoutCollectionField() throws IOException {

        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setId(CASE_REFERENCE);
        caseDetails.setReference(new Long(CASE_REFERENCE));
        caseDetails.setState("state1");
        caseDetails.setData(caseDetailsData);
        caseFields = Arrays.asList(documentCaseField,complexCaseField);
        caseType.setCaseFields(caseFields);
        JsonNode expectedNode = buildJsonNode("document-field-node.json");
        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        doReturn(Boolean.TRUE).when(documentIdValidationService).validateDocumentUUID(CASE_DOCUMENT_ID);
        doReturn(expectedNode).when(accessControlService).filterCaseFieldsByAccess(
            ArgumentMatchers.any(JsonNode.class),
            ArgumentMatchers.any(List.class),
            ArgumentMatchers.any(Set.class),
            eq(AccessControlService.CAN_READ),
            anyBoolean());

        CaseDocumentMetadata caseDocumentMetadata = caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID);
        assertAll(
            () -> assertThat(caseDocumentMetadata.getCaseId(), is(caseDetails.getReferenceAsString())),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions(), is(documentPermissions)),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getId(), is(documentPermissions.getId())),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getPermissions(), is(documentPermissions.getPermissions()))
        );


    }


    @Test
    @DisplayName("should return CaseDocumentMetaData  if Complex Case Field is missing")
    void shouldReturnCaseDocumentMetaDataWithoutComplexField() throws IOException {

        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setId(CASE_REFERENCE);
        caseDetails.setReference(new Long(CASE_REFERENCE));
        caseDetails.setState("state1");
        caseDetails.setData(caseDetailsData);
        caseFields = Arrays.asList(documentCaseField,collectionCaseField);
        caseType.setCaseFields(caseFields);
        JsonNode expectedNode = buildJsonNode("document-field-node.json");
        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        doReturn(Boolean.TRUE).when(documentIdValidationService).validateDocumentUUID(CASE_DOCUMENT_ID);
        doReturn(expectedNode).when(accessControlService).filterCaseFieldsByAccess(
            ArgumentMatchers.any(JsonNode.class),
            ArgumentMatchers.any(List.class),
            ArgumentMatchers.any(Set.class),
            eq(AccessControlService.CAN_READ),
            anyBoolean());

        CaseDocumentMetadata caseDocumentMetadata = caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID);
        assertAll(
            () -> assertThat(caseDocumentMetadata.getCaseId(), is(caseDetails.getReferenceAsString())),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions(), is(documentPermissions)),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getId(), is(documentPermissions.getId())),
            () -> assertThat(caseDocumentMetadata.getDocumentPermissions().getPermissions(), is(documentPermissions.getPermissions()))
        );


    }

    @Test
    @DisplayName("should throw exception if Complex Case Field is missing")
    void shouldThrowExceptionWithoutDocumentField() throws IOException {

        caseDetails = new CaseDetails();
        caseDetails.setJurisdiction(JURISDICTION_ID);
        caseDetails.setCaseTypeId(CASE_TYPE_ID);
        caseDetails.setId(CASE_REFERENCE);
        caseDetails.setReference(new Long(CASE_REFERENCE));
        caseDetails.setState("state1");
        caseDetails.setData(caseDetailsData);
        caseFields = Arrays.asList(complexCaseField,collectionCaseField);
        caseType.setCaseFields(caseFields);
        JsonNode expectedNode = buildJsonNode("document-field-node.json");
        doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
        doReturn(Boolean.TRUE).when(documentIdValidationService).validateDocumentUUID(CASE_DOCUMENT_ID_NOT_IN_CASE_DETAILS);
        doReturn(expectedNode).when(accessControlService).filterCaseFieldsByAccess(
            ArgumentMatchers.any(JsonNode.class),
            ArgumentMatchers.any(List.class),
            ArgumentMatchers.any(Set.class),
            eq(AccessControlService.CAN_READ),
            anyBoolean());

        assertThrows(CaseDocumentNotFoundException.class, () -> caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE,
            CASE_DOCUMENT_ID_NOT_IN_CASE_DETAILS));


    }

    @Test
    @DisplayName("collection don't have complex field")
    void shouldNotExtractDocumentFieldWhileCollectionNotHaveComplexField() throws IOException {
        List<CaseField> caseFields = Arrays.asList(buildCaseField("collection-type-without-complexfield.json"));
        List<CaseField>  inputCaseField = new ArrayList<>();
        List<CaseField>  expectedCaseField = Arrays.asList();

        caseDocumentsOperation.extractDocumentFieldsFromCaseDefinition(caseFields,inputCaseField);
        assertAll(
            () -> assertEquals(inputCaseField,expectedCaseField)

        );
    }





    static CaseField buildCaseField(String fileName) throws IOException {
        InputStream inputStream =
            GetCaseDocumentsOperationTest.class.getClassLoader().getResourceAsStream("tests/".concat(fileName));
        return
            new ObjectMapper().readValue(inputStream, new TypeReference<CaseField>() {
            });
    }



    static HashMap<String, JsonNode> buildCaseDetailData(String fileName) throws IOException {
        InputStream inputStream =
            GetCaseDocumentsOperationTest.class.getClassLoader().getResourceAsStream("tests/".concat(fileName));
        return
            new ObjectMapper().readValue(inputStream, new TypeReference<HashMap<String, JsonNode>>() {
            });
    }

    static JsonNode buildJsonNode(String fileName) throws IOException {
        InputStream inputStream =
            GetCaseDocumentsOperationTest.class.getClassLoader().getResourceAsStream("tests/".concat(fileName));
        return
            new ObjectMapper().readValue(inputStream, new TypeReference<JsonNode>() {
            });
    }
}

