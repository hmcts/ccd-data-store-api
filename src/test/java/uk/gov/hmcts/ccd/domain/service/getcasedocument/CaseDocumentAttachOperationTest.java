package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.Version;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;

class CaseDocumentAttachOperationTest {

    private static final String CASE_TYPE_ID = "TestCaseType";
    private static final Integer VERSION = 67;

    private static final String CASE_UID = "1234123412341236";
    private static final String CASE_ID = "45677";
    public static final String COMPLEX = "Complex";
    public static final String COLLECTION = "Collection";
    public static final String DOCUMENT_CASE_FIELD_URL_ATTRIBUTE = "document_url";
    public static final String DOCUMENT_CASE_FIELD_BINARY_ATTRIBUTE = "document_binary_url";
    public static final String BAD_REQUEST_EXCEPTION_DOCUMENT_INVALID = "DocumentId is not valid";
    public static final String HASH_CODE_STRING = "hashcode";
    public static final String CONTENT_TYPE = "content-type";
    public static final String BINARY = "/binary";
    public static final String CASE_DATA_PARSING_EXCEPTION = "Exception while extracting the document fields from Case payload";
    public static final String DOCUMENTS_ALTERED_OUTSIDE_TRANSACTION = "The documents have been altered outside the create case transaction";


    @Mock
    private CaseDetailsRepository caseDetailsRepository;

    @Mock
    private CaseDetails caseDetails;

    @Mock
    private CaseDetails savedCaseDetails;

    @Mock
    private UIDService uidService;

    @Mock
    private ApplicationParams applicationParams;

    @InjectMocks
    private CaseDocumentAttachOperation caseDocumentAttachOperation;

    private Event event;
    private CaseType caseType;
    private IdamUser idamUser;
    private CaseEvent eventTrigger;
    private CaseState state;

    @BeforeEach
    void setup() {
        MockitoAnnotations.initMocks(this);
        caseType = buildCaseType();
        doReturn("http://localhost:4455").when(applicationParams).getCaseDocumentAmApiHost();
        doReturn("/cases/documents/attachToCase").when(applicationParams).getAttachDocumentPath();

    }

/*    @Test
    @DisplayName("should persist V2.1 Case creation event")
    void shouldPersistV2Event() throws IOException {
        doReturn(V2.MediaType.CREATE_CASE_2_1).when(request).getContentType();
        CaseDetails inputCaseDetails = new CaseDetails();


        Map<String, JsonNode> dataMap = buildCaseData("SubmitTransactionDocumentUpload.json");
        inputCaseDetails.setData(dataMap);
        doReturn(dataMap).when(this.caseDetails).getData();
        doReturn(inputCaseDetails).when(caseDetailsRepository).set(inputCaseDetails);
        ResponseEntity<Boolean> responseEntity = new ResponseEntity<Boolean>(true, HttpStatus.OK);
        doReturn(responseEntity).when(restTemplate).exchange(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any());

    }*/

    @Test
    @DisplayName("should extract only documents with hashcode from Case Data")
    void shouldExtractDocumentFromCaseData() throws IOException {

        Map<String, JsonNode> dataMap = buildCaseData("SubmitTransactionDocumentUpload.json");
        Map<String, String> documentMap = new HashMap<>();

        caseDocumentAttachOperation.extractDocumentFieldsBeforeCallback(dataMap, documentMap);

        Map<String, String> expectedMap = Stream.of(new String[][] {
            {"388a1ce0-f132-4680-90e9-5e782721cabb", "57e7fdf75e281aaa03a0f50f93e7b10bbebff162cf67a4531c4ec2509d615c0a"},
            {"f0550adc-eaea-4232-b52f-1c4ac0534d60", "UyWGSBgJexcS1i0fTp6QUyWGSBgJexcS1i0fTp6QUyWGSBgJexcS1i0fTp6QUyWGSBgJexcS1i0fTp6Q"},
            {"5c4b5564-a29f-47d3-8c51-50e2d4629435", "6a7e12164534a0c2252a94b308a2a185e46f89ab639c5342027b9cd393068bc"},
            {"7b8930ef-2bcd-44cd-8a78-1ae0b1f5a0ec", "7b8930ef-2bcd-44cd-8a78-17b8930ef-27b8930ef-2bcd-44cd-8a78-1ae0b1f5a0ec"},
            }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        assertAll(
            () -> assertEquals(documentMap, expectedMap));
    }

    /*@Test
    @DisplayName("should filter documents after callback to service")
    void shouldFilterDocumentFieldsAfterCallback() throws IOException {
        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata
            .builder()
            .documents(Arrays.asList(CaseDocument.builder().id("DocumentId1").build(),
                                     CaseDocument.builder().id("DocumentId2").build(),
                                     CaseDocument.builder().id("DocumentId3").build(),
                                     CaseDocument.builder().id("DocumentId4").build(),
                                     CaseDocument.builder().id("DocumentId5").build()))
            .build();

        Set<String> documentSetBeforeCallback = Stream.of("DocumentId1", "DocumentId2", "DocumentId3")
                                                      .collect(Collectors.toSet());
        Set<String> documentSetAfterCallback = Stream.of("DocumentId1", "DocumentId2", "DocumentId4", "DocumentId5")
                                                     .collect(Collectors.toSet());

        submitCaseTransaction.filterDocumentFields(caseDocumentsMetadata, documentSetBeforeCallback, documentSetAfterCallback);
        Set<String> expectedSet = Sets.newHashSet("DocumentId1",
                                                  "DocumentId2",
                                                  "DocumentId4", "DocumentId5");

        Set<String> filteredDocumentIds = caseDocumentsMetadata.getDocuments()
                                                               .stream().map(CaseDocument::getId)
                                                               .collect(Collectors.toSet());
        assertAll(
            () -> assertEquals(documentSetAfterCallback, expectedSet),
            () -> assertEquals(filteredDocumentIds, expectedSet));
    }*/

    private CaseType buildCaseType() {
        final Version version = new Version();
        version.setNumber(VERSION);
        final CaseType caseType = new CaseType();
        caseType.setId(CASE_TYPE_ID);
        caseType.setVersion(version);
        return caseType;
    }

    static HashMap<String, JsonNode> buildCaseData(String fileName) throws IOException {
        InputStream inputStream =
            CaseDocumentAttachOperationTest.class.getClassLoader().getResourceAsStream("mappings/".concat(fileName));

        return
            new ObjectMapper().readValue(inputStream, new TypeReference<HashMap<String, JsonNode>>() {
            });
    }
}
