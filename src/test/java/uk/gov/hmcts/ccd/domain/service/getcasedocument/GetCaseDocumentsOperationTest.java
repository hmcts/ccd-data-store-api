package uk.gov.hmcts.ccd.domain.service.getcasedocument;/*
package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.AccessControlListBuilder.anAcl;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseFieldBuilder.newCaseField;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocument;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;
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
    private static final String DOCUMENT_URL = "http://dm-store:8080/documents/a780ee98-3136-4be9-bf56-a46f8da1bc97";
    private static final String DOCUMENT_NAME = "Sample_document.txt";
    private static final String DOCUMENT_TYPE = "Document";
    private static final String USER_ID = "test_user_id";

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TypeReference jsonMap = new TypeReference<HashMap<String, JsonNode>>() {
    };
    private final CaseType caseType = new CaseType();
    private final Set<String> userRoles = Sets.newHashSet("role1", "role2");
    private final List<String> caseRoles = Collections.emptyList();
    private static final AccessControlList acl1 = anAcl().withRole("role1").withCreate(true).withRead(true).withUpdate(true).withDelete(false).build();
    private static final AccessControlList acl2 = anAcl().withRole("role2").withCreate(true).withRead(true).withUpdate(false).withDelete(true).build();
    private static final AccessControlList acl3 = anAcl().withRole("role3").withCreate(false).withRead(false).withUpdate(true).withDelete(false).build();
    private static final FieldType documentFieldType = aFieldType().withId("Document").withType("Document").build();
    private static final FieldType collectionFieldType = aFieldType().withId("collectionField1").withType("Collection").build();
    private static final FieldType complexFieldType = aFieldType().withId("complexField1").withType("Complex").build();
    private static final CaseField CASE_FIELD = newCaseField()
        .withFieldType(documentFieldType)
        .withId("DocumentField1")
        .withAcl(acl1)
        .withAcl(acl2)
        .withAcl(acl3)
        .build();

    private static final CaseField COLLECTION_FIELD = newCaseField()
        .withFieldType(collectionFieldType)
        .withId("collectionField1")
        .withAcl(acl1)
        .withAcl(acl2)
        .withAcl(acl3)
        .build();

    private static final CaseField COMPLEX_FIELD = newCaseField()
        .withFieldType(complexFieldType)
        .withId("complexField1")
        .withAcl(acl1)
        .withAcl(acl2)
        .withAcl(acl3)
        .build();

    private CaseDetails caseDetails;
    private Optional<CaseDetails> caseDetailsOptional;
    private final CaseDocumentMetadata caseDocumentMetadata = CaseDocumentMetadata.builder()
                                                                                  .caseId("CaseId")
                                                                                  .caseTypeId("CaseTypeId")
                                                                                  .build();
    private final CaseDocument caseDocument = CaseDocument.builder()
                                                          .id(CASE_DOCUMENT_ID)
                                                          .url(DOCUMENT_URL)
                                                          .name(DOCUMENT_NAME)
                                                          .type(DOCUMENT_TYPE)
                                                          .permissions(Arrays.asList(Permission.READ, Permission.UPDATE))
                                                          .build();

    public GetCaseDocumentsOperationTest() throws IOException {
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        caseDetailsOptional = Optional.of(new CaseDetails());
        doReturn(userRoles).when(userRepository).getUserRoles();
        doReturn(USER_ID).when(userRepository).getUserId();
        doReturn(caseRoles).when(caseUserRepository).findCaseRoles(Long.valueOf(CASE_REFERENCE), USER_ID);
        doReturn(Boolean.TRUE).when(documentIdValidationService).validateDocumentUUID(CASE_DOCUMENT_ID);
        caseDocumentsOperation = new GetCaseDocumentOperation(getCaseOperation, caseTypeService, userRepository, caseUserRepository,
                                                              documentIdValidationService, accessControlService);
    }

    @Nested
    @DisplayName("getCaseDocumentMetadata")
    class GetCaseDocumentMetadata {

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
        @DisplayName("should return CaseDocumentMetadata")
        void shouldCallGetCaseDocumentMetadata() throws IOException {

            caseDetails = new CaseDetails();
            caseDetails.setJurisdiction(JURISDICTION_ID);
            caseDetails.setCaseTypeId(CASE_TYPE_ID);
            caseDetails.setId(CASE_REFERENCE);
            caseDetails.setReference(new Long(CASE_REFERENCE));
            caseDetails.setState("state1");
            caseDetails.setData(MAPPER.convertValue(MAPPER.readTree(
                "{  \"DocumentField1\": { "
                +
                "           \"document_url\": \"http://dm-store:8080/documents/a780ee98-3136-4be9-bf56-a46f8da1bc97\","
                +
                "           \"document_binary_url\": \"http://dm-store:8080/documents/a780ee98-3136-4be9-bf56-a46f8da1bc97/binary\","
                +
                "           \"document_filename\": \"bin1.pdf\""
                +
                "       }\n,"
                +
                "       \"DocumentField2\": {"
                +
                "           \"document_url\": \"http://dm-store:8080/documents/ae51935b-b093-4c49-b6b6-9685c75ad932\","
                +
                "           \"document_binary_url\": \"http://dm-store:8080/documents/ae51935b-b093-4c49-b6b6-9685c75ad932/binary\","
                +
                "           \"document_filename\": \"bin2.pdf\""
                +
                "       }\n"
                +
                "    }\n"), jsonMap));

            doReturn(Optional.of(caseDetails)).when(getCaseOperation).execute(CASE_REFERENCE);
            doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);
            caseType.setCaseFields(Arrays.asList(CASE_FIELD, COLLECTION_FIELD, COMPLEX_FIELD));
            doReturn(caseType).when(caseTypeService).getCaseTypeForJurisdiction(CASE_TYPE_ID, JURISDICTION_ID);

            doReturn(new ObjectMapper().readTree("{  \"DocumentField1\": { "
                                                 +
                                                 "           \"document_url\": \"http://dm-store:8080/documents/a780ee98-3136-4be9-bf56-a46f8da1bc97\","
                                                 +
                                                 "           \"document_binary_url\": \"http://dm-store:8080/documents/a780ee98-3136-4be9-bf56-a46f8da1bc97/binary\","
                                                 +
                                                 "           \"document_filename\": \"bin1.pdf\""
                                                 +
                                                 "       }\n,"
                                                 +
                                                 "       \"DocumentField2\": {"
                                                 +
                                                 "           \"document_url\": \"http://dm-store:8080/documents/ae51935b-b093-4c49-b6b6-9685c75ad932\","
                                                 +
                                                 "           \"document_binary_url\": \"http://dm-store:8080/documents/ae51935b-b093-4c49-b6b6-9685c75ad932/binary\","
                                                 +
                                                 "           \"document_filename\": \"bin2.pdf\""
                                                 +
                                                 "       }\n"
                                                 +
                                                 "    }\n")).when(accessControlService).filterCaseFieldsByAccess(
                ArgumentMatchers.any(JsonNode.class),
                ArgumentMatchers.any(List.class),
                ArgumentMatchers.any(Set.class),
                eq(AccessControlService.CAN_READ),
                anyBoolean());

            CaseDocumentMetadata caseDocumentMetadata = caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID);
            assertAll(
                () -> assertThat(caseDocumentMetadata.getCaseId(), is(caseDetails.getReferenceAsString())),
                () -> assertThat(caseDocumentMetadata.getCaseTypeId(), is(caseDetails.getCaseTypeId())),
                () -> assertThat(caseDocumentMetadata.getJurisdictionId(), is(caseDetails.getJurisdiction()))
                     );
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
            //caseDetails.setId(CASE_REFERENCE);
            //caseDetails.setReference(new Long(CASE_REFERENCE));
            caseDetails.setState("state1");

            assertThrows(CaseNotFoundException.class, () -> caseDocumentsOperation.getCaseDocumentMetadata(CASE_REFERENCE, CASE_DOCUMENT_ID));
        }

        private Map<String, JsonNode> buildData(String... dataFieldIds) {
            Map<String, JsonNode> dataMap = new HashMap<>();
            asList(dataFieldIds).forEach(dataFieldId -> {
                dataMap.put(dataFieldId, JSON_NODE_FACTORY.textNode(dataFieldId));
            });
            return dataMap;
        }
    }
}
*/
