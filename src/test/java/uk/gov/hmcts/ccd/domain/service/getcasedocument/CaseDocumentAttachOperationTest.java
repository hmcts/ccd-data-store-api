package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItem;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItemType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.definition.Version;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;
import uk.gov.hmcts.ccd.v2.V2;

class CaseDocumentAttachOperationTest {

    private static final String EVENT_ID = "SomeEvent";
    private static final String EVENT_NAME = "Some event";
    private static final String EVENT_SUMMARY = "Some event summary";
    private static final String EVENT_DESC = "Some event description";
    private static final String CASE_TYPE_ID = "TestCaseType";
    private static final Integer VERSION = 67;
    private static final String IDAM_ID = "23";
    private static final String IDAM_FNAME = "Pierre";
    private static final String IDAM_LNAME = "Martin";
    private static final String IDAM_EMAIL = "pmartin@hmcts.test";
    private static final Boolean IGNORE_WARNING = Boolean.TRUE;
    private static final String STATE_ID = "CREATED_ID";
    private static final String STATE_NAME = "Created name";
    private static final String CASE_UID = "1234123412341236";
    private static final String CASE_ID = "45677";
    public static final String DESCRIPTION = "Description";
    public static final String URL = "http://www.yahooo.com";
    public static final SignificantItemType DOCUMENT = SignificantItemType.DOCUMENT;
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

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
    private CaseAuditEventRepository caseAuditEventRepository;
    @Mock
    private CaseTypeService caseTypeService;
    @Mock
    private CallbackInvoker callbackInvoker;
    @Mock
    private SecurityClassificationService securityClassificationService;
    @Mock
    private CaseUserRepository caseUserRepository;

    @Mock
    private CaseDetails caseDetails;

    @Mock
    private CaseDetails savedCaseDetails;

    @Mock
    private UIDService uidService;

    @Mock
    private UserAuthorisation userAuthorisation;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private HttpServletRequest request;

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
        state = buildState();
        doReturn("http://localhost:4455").when(applicationParams).getCaseDocumentAmApiHost();
        doReturn("/cases/documents/attachToCase").when(applicationParams).getAttachDocumentPath();

        doReturn(STATE_ID).when(savedCaseDetails).getState();

        doReturn(state).when(caseTypeService).findState(caseType, STATE_ID);

        doReturn(CASE_UID).when(uidService).generateUID();

        doReturn(savedCaseDetails).when(caseDetailsRepository).set(caseDetails);

        doReturn(CASE_ID).when(savedCaseDetails).getId();

    }

    private AboutToSubmitCallbackResponse buildResponse() {
        final AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = new AboutToSubmitCallbackResponse();
        aboutToSubmitCallbackResponse.setState(Optional.of("somestring"));
        final SignificantItem significantItem = new SignificantItem();
        significantItem.setType(SignificantItemType.DOCUMENT.name());
        significantItem.setDescription(DESCRIPTION);
        significantItem.setUrl(URL);
        aboutToSubmitCallbackResponse.setSignificantItem(significantItem);
        return aboutToSubmitCallbackResponse;
    }

    private CaseState buildState() {
        final CaseState caseState = new CaseState();
        caseState.setName(STATE_NAME);
        return caseState;
    }


    @Test
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

    }

    @Test
    @DisplayName("should extract only documents with hashcode from Case Data")
    void shouldExtractDocumentFromCaseData() throws IOException {

        Map<String, JsonNode> dataMap = buildCaseData("SubmitTransactionDocumentUpload.json");
        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder().documents(new ArrayList<>()).build();
        Set<String> documentSet = new HashSet<>();

        submitCaseTransaction.extractDocumentFields(caseDocumentsMetadata, dataMap, documentSet);
        Set<String> expectedSet = Sets.newHashSet("388a1ce0-f132-4680-90e9-5e782721cabb",
                                                  "f0550adc-eaea-4232-b52f-1c4ac0534d60",
                                                  "5c4b5564-a29f-47d3-8c51-50e2d4629435");
        assertAll(
            () -> assertEquals(documentSet, expectedSet));
    }

    @Test
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
    }

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
