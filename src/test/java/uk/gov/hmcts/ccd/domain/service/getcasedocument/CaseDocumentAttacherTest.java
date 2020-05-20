package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.DocumentTokenException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

public class CaseDocumentAttacherTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ApplicationParams applicationParams;

    @InjectMocks
    private CaseDocumentAttacher caseDocumentAttacher;

    HashMap<String, JsonNode> caseDetailsBefore;
    HashMap<String, JsonNode> caseDataContent;
    HashMap<String, JsonNode> newDocumentWithoutHashToken;
    CaseDetails caseDetails;
    Map<String, String> beforeCallBack;
    CaseDocumentsMetadata caseDocumentsMetadata;

    public static final String COMPLEX = "Complex";
    public static final String COLLECTION = "Collection";
    public static final String DOCUMENT_URL = "document_url";
    public static final String HASH_TOKEN_STRING = "hashToken";
    public static final String CMC_EVENT_UPDATE = "ReviewedPaperResponse";

    @BeforeEach
    public void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        caseDetails = new CaseDetails();
        caseDetailsBefore = buildCaseData("case-detail-before-update.json");
        caseDataContent = buildCaseData("case-detail-after-update.json");
        newDocumentWithoutHashToken = buildCaseData("new-document-with-hashtoken.json");

        caseDetails.setData(caseDetailsBefore);
        ResponseEntity<String> responseEntity = new ResponseEntity<>("Success", HttpStatus.OK);
        doReturn(new HttpHeaders()).when(securityUtils).authorizationHeaders();
        doReturn("http://localhost:4455").when(applicationParams).getCaseDocumentAmApiHost();
        doReturn("/cases/documents/attachToCase").when(applicationParams).getAttachDocumentPath();
        doReturn(responseEntity).when(restTemplate).exchange(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any());

    }

    @Test
    @DisplayName("should not return document fields difference in case of non document fields update ")
    void shouldNotReturnDeltaInCaseOfNonDocumentFieldsUpdate() throws IOException {
        HashMap<String, JsonNode> caseDataContent = buildCaseData("case-detail-plain-fields-update.json");
        HashMap<String, JsonNode> caseDetailsBefore = buildCaseData("case-detail-before.json");
        Set<String> expectedOutput = new HashSet<>();
        final Set<String> output = caseDocumentAttacher.differenceBeforeAndAfterInCaseDetails(caseDetailsBefore, caseDataContent);

        assertAll(
            () -> assertEquals(expectedOutput, output));
    }

    @Test
    @DisplayName(
        "should filter the Case Document Meta Data while 2 documents with hashcode from request and 2 new documents without hash token from callback "
        + "response")
    void shouldFilterCaseDocumentMetaData_With_Scenario_1() {

        prepareInputs();

        Map<String, String> afterCallBack = new HashMap<>();
        afterCallBack.put("320233b8-fb61-4b58-8731-23c83638c9c6", null);
        afterCallBack.put("f5bd63a2-65c5-435e-a972-98ed658ad7d6", null);
        afterCallBack.put("b6ee2bff-8244-431f-94ec-9d8ecace8dd6", null);
        afterCallBack.put("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292", null);

        List<DocumentHashToken> expected = Arrays.asList(DocumentHashToken.builder().id("b6ee2bff-8244-431f-94ec-9d8ecace8dd6")
                .hashToken("4d49edc151423fb7b2e1f22d89a2d041b43").build(),
            DocumentHashToken.builder().id("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292")
                .hashToken("4d49edc151423fb7b2e1f22d87b2d041b34").build());

        caseDocumentAttacher.consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata, beforeCallBack, afterCallBack);

        List<DocumentHashToken> actual = caseDocumentsMetadata.getDocumentHashToken();

        assertAll(
            () -> assertEquals(expected, actual));
    }

    @Test
    @DisplayName(
        "should filter the Case Document Meta Data while 2 documents with hashcode from request and 2 new documents with hash token from callback "
        + "response")
    void shouldFilterCaseDocumentMetaData_With_Scenario_2() {
        prepareInputs();
        Map<String, String> afterCallBack = new HashMap<>();
        afterCallBack.put("320233b8-fb61-4b58-8731-23c83638c9c6", "4d49edc151423fb7b2e1f22d89a2d041b53");
        afterCallBack.put("f5bd63a2-65c5-435e-a972-98ed658ad7d6", "4d49edc151423fb7b2e1f22d89a2d041b63");
        afterCallBack.put("b6ee2bff-8244-431f-94ec-9d8ecace8dd6", null);
        afterCallBack.put("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292", null);

        List<DocumentHashToken> expected = Arrays.asList(DocumentHashToken.builder().id("f5bd63a2-65c5-435e-a972-98ed658ad7d6")
                .hashToken("4d49edc151423fb7b2e1f22d89a2d041b63").build(),
            DocumentHashToken.builder().id("320233b8-fb61-4b58-8731-23c83638c9c6")
                .hashToken("4d49edc151423fb7b2e1f22d89a2d041b53").build(),
            DocumentHashToken.builder().id("b6ee2bff-8244-431f-94ec-9d8ecace8dd6")
                .hashToken("4d49edc151423fb7b2e1f22d89a2d041b43").build(),
            DocumentHashToken.builder().id("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292")
                .hashToken("4d49edc151423fb7b2e1f22d87b2d041b34").build()
        );

        caseDocumentAttacher.consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata, beforeCallBack, afterCallBack);

        List<DocumentHashToken> actual = caseDocumentsMetadata.getDocumentHashToken();

        assertAll(
            () -> assertEquals(expected, actual));
    }

    @Test
    @DisplayName(
        "should filter the Case Document Meta Data while 2 documents with hashcode from request and replace 1 documents without hash token from callback"
        + " response")
    void shouldFilterCaseDocumentMetaData_With_Scenario_3() {
        prepareInputs();

        Map<String, String> afterCallBack = new HashMap<>();
        afterCallBack.put("b6ee2bff-8244-431f-94ec-9d8ecace8dd6", null);
        afterCallBack.put("f5bd63a2-65c5-435e-a972-98ed658ad7d6", null);

        List<DocumentHashToken> expected = Collections.singletonList(
            DocumentHashToken.builder().id("b6ee2bff-8244-431f-94ec-9d8ecace8dd6")
                .hashToken("4d49edc151423fb7b2e1f22d89a2d041b43").build());

        caseDocumentAttacher.consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata, beforeCallBack, afterCallBack);

        List<DocumentHashToken> actual = caseDocumentsMetadata.getDocumentHashToken();

        assertAll(
            () -> assertEquals(expected, actual));
    }

    @Test
    @DisplayName(
        "should filter the Case Document Meta Data while 2 documents with hashcode from request and replace 1 documents with hash token from callback "
        + "response")
    void shouldFilterCaseDocumentMetaData_With_Scenario_4() {
        prepareInputs();
        Map<String, String> afterCallBack = new HashMap<>();
        afterCallBack.put("b6ee2bff-8244-431f-94ec-9d8ecace8dd6", null);
        afterCallBack.put("f5bd63a2-65c5-435e-a972-98ed658ad7d6", "4d49edc151423fb7b2e1f22d89a2d041b63");


        List<DocumentHashToken> expected = Arrays.asList(
            DocumentHashToken.builder().id("f5bd63a2-65c5-435e-a972-98ed658ad7d6")
                .hashToken("4d49edc151423fb7b2e1f22d89a2d041b63").build(),
            DocumentHashToken.builder().id("b6ee2bff-8244-431f-94ec-9d8ecace8dd6")
                .hashToken("4d49edc151423fb7b2e1f22d89a2d041b43").build()
        );

        caseDocumentAttacher.consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata, beforeCallBack, afterCallBack);

        List<DocumentHashToken> actual = caseDocumentsMetadata.getDocumentHashToken();

        assertAll(
            () -> assertEquals(expected, actual));
    }

    @Test
    @DisplayName("should filter the Case Document Meta Data while 2 documents with hashcode from request and no response from callback ")
    void shouldFilterCaseDocumentMetaData_With_Scenario_5() {
        prepareInputs();
        Map<String, String> afterCallBack = new HashMap<>();
        List<DocumentHashToken> expected = Arrays.asList(
            DocumentHashToken.builder().id("b6ee2bff-8244-431f-94ec-9d8ecace8dd6")
                .hashToken("4d49edc151423fb7b2e1f22d89a2d041b43").build(),
            DocumentHashToken.builder().id("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292")
                .hashToken("4d49edc151423fb7b2e1f22d87b2d041b34").build()
        );

        caseDocumentAttacher.consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata, beforeCallBack, afterCallBack);

        List<DocumentHashToken> actual = caseDocumentsMetadata.getDocumentHashToken();

        assertAll(
            () -> assertEquals(expected, actual));
    }

    @Test
    @DisplayName("should throw Service Exception with 500 While all hashToken tempered of user provided documents by Callback Service ")
    void shouldThrowExceptionWhileHashTokenTempered_Scenario_6() {
        prepareInputs();
        Map<String, String> afterCallBack = new HashMap<>();
        afterCallBack.put("320233b8-fb61-4b58-8731-23c83638c9c6", "4d49edc151423fb7b2e1f22d89a2d041b53");
        afterCallBack.put("f5bd63a2-65c5-435e-a972-98ed658ad7d6", "4d49edc151423fb7b2e1f22d89a2d041b63");
        afterCallBack.put("b6ee2bff-8244-431f-94ec-9d8ecace8dd6", "4d49edc151423fb7b2e1f22d89a2d056234");
        afterCallBack.put("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292", "4d49edc151423fb7b2e1f2230975328jk89");

        ServiceException exception = Assertions.assertThrows(ServiceException.class,
            () -> caseDocumentAttacher
                .consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata, beforeCallBack, afterCallBack));
        Assertions.assertTrue(exception.getMessage().contains(
            "call back attempted to change the hashToken of the following documents:[b6ee2bff-8244-431f-94ec-9d8ecace8dd6, "
            + "e16f2ae0-d6ce-4bd0-a652-47b3c4d86292]"));
    }

    @Test
    @DisplayName("should throw Service Exception with 500 While only one hashToken tempered of user provided documents by Callback Service ")
    void shouldThrowExceptionWhileHashTokenTempered_Scenario_7() {
        prepareInputs();
        Map<String, String> afterCallBack = new HashMap<>();
        afterCallBack.put("320233b8-fb61-4b58-8731-23c83638c9c6", "4d49edc151423fb7b2e1f22d89a2d041b53");
        afterCallBack.put("f5bd63a2-65c5-435e-a972-98ed658ad7d6", "4d49edc151423fb7b2e1f22d89a2d041b63");
        afterCallBack.put("b6ee2bff-8244-431f-94ec-9d8ecace8dd6", "4d49edc151423fb7b2e1f22d89a2d056234");
        afterCallBack.put("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292", null);


        ServiceException exception = Assertions.assertThrows(ServiceException.class,
            () -> caseDocumentAttacher
                .consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata, beforeCallBack, afterCallBack));
        Assertions.assertTrue(
            exception.getMessage().contains("call back attempted to change the hashToken of the following documents:[b6ee2bff-8244-431f-94ec-9d8ecace8dd6]"));
    }


    @Test
    @DisplayName("should return document fields differences once updated ")
    void shouldReturnDeltaWhenDocumentFieldsUpdate() {
        Set<String> expectedOutput = new HashSet<>();
        expectedOutput.add("8da17150-c001-47d7-bfeb-3dabed9e0976");
        expectedOutput.add("320233b8-fb61-4b58-8731-23c83638c9c6");
        expectedOutput.add("b5eb1f0e-64cd-4ccb-996a-6915c28fa65d");

        final Set<String> output = caseDocumentAttacher.differenceBeforeAndAfterInCaseDetails(caseDetails.getData(), caseDataContent);

        assertAll(
            () -> assertEquals(expectedOutput, output));
    }

    @Test
    @DisplayName("should return document fields differences once document Fields inside Complex Field ")
    void shouldReturnDeltaWhenDocumentFieldsInsideComplexElement() throws IOException {
        HashMap<String, JsonNode> caseDataContent = buildCaseData("case-detail-after-with-complexFields-update.json");
        Set<String> expectedOutput = new HashSet<>();
        expectedOutput.add("8da17150-c001-47d7-bfeb-3dabed9e0976");
        expectedOutput.add("8da17150-c001-47d7-bfeb-3dabed9e0222");
        expectedOutput.add("335a9a09-7a51-40e9-8196-6b9e26fce6ff");
        expectedOutput.add("b5eb1f0e-64cd-4ccb-996a-6915c28fa65d");

        final Set<String> output = caseDocumentAttacher.differenceBeforeAndAfterInCaseDetails(caseDetails.getData(), caseDataContent);

        assertAll(
            () -> assertEquals(expectedOutput, output));
    }

    @Test
    @DisplayName("should return empty document set in case of CaseDataContent is empty")
    void shouldReturnEmptyDocumentSet() {
        caseDataContent = null;
        Set<String> expectedOutput = new HashSet<>();
        final Set<String> output = caseDocumentAttacher.differenceBeforeAndAfterInCaseDetails(caseDetails.getData(), caseDataContent);

        assertAll(
            () -> assertEquals(expectedOutput, output));
    }

    @Test
    @DisplayName(
        "should filter the Case Document Meta Data while 2 documents with hashcode coming from request and 2 documents without hash token from callback "
        + "response")
    void shouldFilterCaseDocumentMetaData() {
        Map<String, String> beforeCallBack = new HashMap<>();
        beforeCallBack.put("b6ee2bff-8244-431f-94ec-9d8ecace8dd6", "4d49edc151423fb7b2e1f22d89a2d041b43");
        beforeCallBack.put("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292", "4d49edc151423fb7b2e1f22d87b2d041b34");

        Map<String, String> afterCallBack = new HashMap<>();
        afterCallBack.put("320233b8-fb61-4b58-8731-23c83638c9c6", null);
        afterCallBack.put("f5bd63a2-65c5-435e-a972-98ed658ad7d6", null);
        afterCallBack.put("b6ee2bff-8244-431f-94ec-9d8ecace8dd6", null);
        afterCallBack.put("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292", null);

        CaseDocumentsMetadata caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId("12345556")
            .caseTypeId("BEFTA_CASETYPE_2")
            .jurisdictionId("BEFTA_JURISDICTION_2")
            .documentHashToken(new ArrayList<>())
            .build();
        List<DocumentHashToken> expected = Arrays.asList(DocumentHashToken.builder().id("b6ee2bff-8244-431f-94ec-9d8ecace8dd6")
                .hashToken("4d49edc151423fb7b2e1f22d89a2d041b43").build(),
            DocumentHashToken.builder().id("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292")
                .hashToken("4d49edc151423fb7b2e1f22d87b2d041b34").build());

        caseDocumentAttacher.consolidateDocumentsWithHashTokenAfterCallBack(caseDocumentsMetadata, beforeCallBack, afterCallBack);

        List<DocumentHashToken> actual = caseDocumentsMetadata.getDocumentHashToken();

        assertAll(
            () -> assertEquals(expected, actual));
    }

    @Test
    @DisplayName("should extract only the documents having hashToken from Case Data")
    void shouldExtractDocumentsFromCaseDataBeforeCallBack() throws IOException {

        Map<String, JsonNode> dataMap = buildCaseData("SubmitTransactionDocumentUpload.json");
        caseDocumentAttacher.documentsBeforeCallback = new HashMap<>();

        caseDocumentAttacher.extractDocumentsWithHashTokenBeforeCallbackForCreateCase(dataMap);

        Map<String, String> expectedMap = Stream.of(new String[][]{
            {"388a1ce0-f132-4680-90e9-5e782721cabb", "57e7fdf75e281aaa03a0f50f93e7b10bbebff162cf67a4531c4ec2509d615c0a"},
            {"f0550adc-eaea-4232-b52f-1c4ac0534d60", "UyWGSBgJexcS1i0fTp6QUyWGSBgJexcS1i0fTp6QUyWGSBgJexcS1i0fTp6QUyWGSBgJexcS1i0fTp6Q"},
            {"5c4b5564-a29f-47d3-8c51-50e2d4629435", "6a7e12164534a0c2252a94b308a2a185e46f89ab639c5342027b9cd393068bc"},
            {"7b8930ef-2bcd-44cd-8a78-1ae0b1f5a0ec", "7b8930ef-2bcd-44cd-8a78-17b8930ef-27b8930ef-2bcd-44cd-8a78-1ae0b1f5a0ec"},
            {"95f048e7-9574-4a3d-9189-986015276fd7", "adfadfafgsdgsadgwrgsrg"},
            {"3ca8b55e-76fe-4723-bb60-4e14e950bb0c", "sdg455sfgsfgfsgfsgsgsdgsdgsdgdsgs"}
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        assertAll(
            () -> assertEquals(expectedMap, caseDocumentAttacher.documentsBeforeCallback));
    }

    @Test
    @DisplayName("Verify that hashToken is removed from the case data payload.")
    void shouldRemoveHashTokenFromDocuments() throws IOException {

        Map<String, JsonNode> dataMap = buildCaseData("SubmitTransactionDocumentUpload.json");
        caseDocumentAttacher.documentsBeforeCallback = new HashMap<>();

        caseDocumentAttacher.extractDocumentsWithHashTokenBeforeCallbackForCreateCase(dataMap);

        JsonNode documentField9 = dataMap.get("DocumentField4");

        assertAll(
            () -> assertNotNull(documentField9.get(DOCUMENT_URL)),
            () -> assertNull(documentField9.get(HASH_TOKEN_STRING)));
    }

    @Test
    @DisplayName("should throw exception while getting documents without hashToken from Case Data")
    void shouldThrowExceptionWhileExtractingDocumentsFromCaseData() throws IOException {

        Map<String, JsonNode> dataMap = buildCaseData("SubmitTransactionBadHashTokenUpload.json");
        Map<String, String> documentMap = new HashMap<>();

        Assertions.assertThrows(BadRequestException.class,
            () -> caseDocumentAttacher.extractDocumentsWithHashTokenBeforeCallbackForCreateCase(dataMap));
    }

    @Test
    @DisplayName("should throw exception while getting documents without hashToken from Case Data")
    void shouldThrowExceptionWhileNewDocumentWithoutHashToken() throws IOException {

        Assertions.assertThrows(BadRequestException.class,
            () -> caseDocumentAttacher.extractDocumentsWithHashTokenBeforeCallbackForUpdate(
                buildCaseData("case-detail-after-with-complexFields-update.json"), caseDetails));
    }

    @Test
    @DisplayName("should throw exception while getting documents without hashToken from Case Data")
    void shouldExtractDocumentsWithHashToken() throws IOException {
        Map<String, String> expectedMap = new HashMap<>();
        expectedMap.put("8da17150-c001-47d7-bfeb-3dabed9e0976", "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886tytu");
        expectedMap.put("c1f160ca-cf52-4c0a-8376-3b51c340d00c", "36fc7aa586a54bffc2982ed490c4503f4aca875b7160c9d24b6690276886617d");
        caseDocumentAttacher.extractDocumentsWithHashTokenBeforeCallbackForUpdate(newDocumentWithoutHashToken, caseDetails);
        assertAll(
            () -> assertEquals(caseDocumentAttacher.documentsBeforeCallback, expectedMap));

    }

    @Test
    @DisplayName("should throw Bad Request exception while getting documents without appropriate documentId")
    void shouldThrowExceptionWhileParingDocumentId() throws IOException {

        Map<String, JsonNode> dataMap = buildCaseData("SubmitTransactionBadDocumentUpload.json");
        Map<String, String> documentMap = new HashMap<>();

        Assertions.assertThrows(BadRequestException.class,
            () -> caseDocumentAttacher.extractDocumentsWithHashTokenBeforeCallbackForCreateCase(dataMap));
    }

    @Test
    @DisplayName("should build Case Document Metadata after callback response")
    void shouldExtractDocumentsFromCaseDataAfterCallBack() throws IOException {

        Map<String, JsonNode> dataMap = buildCaseData("SubmitTransactionDocumentUpload.json");
        caseDetails = new CaseDetails();
        caseDetails.setData(dataMap);
        caseDetails.setReference(1111122222333334L);
        caseDetails.setCaseTypeId("BEFTA_CASETYPE_2");

        caseDocumentAttacher.caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId("11111122222333334")
            .caseTypeId("BEFTA_CASETYPE_2")
            .documentHashToken(new ArrayList<>())
            .build();

        caseDocumentAttacher.extractDocumentsAfterCallBack(caseDetails, true);
        Map<String, String> expectedMap = Stream.of(new String[][]{
            {"388a1ce0-f132-4680-90e9-5e782721cabb", "57e7fdf75e281aaa03a0f50f93e7b10bbebff162cf67a4531c4ec2509d615c0a"},
            {"f0550adc-eaea-4232-b52f-1c4ac0534d60", "UyWGSBgJexcS1i0fTp6QUyWGSBgJexcS1i0fTp6QUyWGSBgJexcS1i0fTp6QUyWGSBgJexcS1i0fTp6Q"},
            {"5c4b5564-a29f-47d3-8c51-50e2d4629435", "6a7e12164534a0c2252a94b308a2a185e46f89ab639c5342027b9cd393068bc"},
            {"7b8930ef-2bcd-44cd-8a78-1ae0b1f5a0ec", "7b8930ef-2bcd-44cd-8a78-17b8930ef-27b8930ef-2bcd-44cd-8a78-1ae0b1f5a0ec"},
            {"95f048e7-9574-4a3d-9189-986015276fd7", "adfadfafgsdgsadgwrgsrg"},
            {"3ca8b55e-76fe-4723-bb60-4e14e950bb0c", "sdg455sfgsfgfsgfsgsgsdgsdgsdgdsgs"}
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        assertAll(
            () -> assertEquals(expectedMap, caseDocumentAttacher.documentsAfterCallback));
    }

    @Test
    @DisplayName("Should call the Case Document AM API to attach document to a case")
    void shouldCallRestClientToAttachDocumentToCase() {
        caseDocumentAttacher.caseDocumentsMetadata =
            CaseDocumentsMetadata.builder()
                .documentHashToken(Collections.singletonList(
                    DocumentHashToken.builder().id("388a1ce0-f132-4680-90e9-5e782721cabb")
                        .hashToken(
                            "57e7fdf75e281aaa03a0f50f93e7b10bbebff162cf67a4531c4ec2509d615c0a").build()
                )).build();

        caseDocumentAttacher.restCallToAttachCaseDocuments();
        verify(restTemplate, times(1)).exchange(ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any());
    }


    @Test
    @DisplayName("Should Not call the Case Document AM API to attach document to a case for empty payload")
    void shouldNotCallRestClientToAttachDocumentToCaseForNoEligibleDocuments() {
        caseDocumentAttacher.caseDocumentsMetadata =
            CaseDocumentsMetadata.builder()
                .documentHashToken(Collections.emptyList()).build();

        caseDocumentAttacher.restCallToAttachCaseDocuments();
        verify(restTemplate, times(0)).exchange(ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any());
    }

    @Test
    @DisplayName("Should throw Forbidden exception when user passes an invalid hashToken")
    void shouldThrowForbiddenExceptionForinvalidHashToken() {
        doThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN, "documentId123")).when(restTemplate).exchange(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any());

        caseDocumentAttacher.caseDocumentsMetadata =
            CaseDocumentsMetadata
                .builder()
                .documentHashToken(Collections.singletonList(DocumentHashToken.builder().id("388a1ce0-f132-4680-90e9-5e782721cabb")
                    .hashToken("57e7fdf75e281aaa03a0f50f93e7b10bbebff162cf67a4531c4ec2509d615c0a").build())
                ).build();

        Assertions.assertThrows(DocumentTokenException.class,
            () -> caseDocumentAttacher.restCallToAttachCaseDocuments());
    }

    @Test
    @DisplayName("Should throw Bad request exception when input params have validation issues.")
    void shouldThrowBadRequestExceptionWhenDocumentIdIsInvalid() {
        doThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "The input parameter does not comply with the required pattern"))
            .when(restTemplate).exchange(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any());

        caseDocumentAttacher.caseDocumentsMetadata =
            CaseDocumentsMetadata
                .builder()
                .documentHashToken(Collections.singletonList(DocumentHashToken.builder().id("388a1ce0-f132-4680-90e9-5e782721cabb")
                    .hashToken("57e7fdf75e281aaa03a0f50f93e7b10bbebff162cf67a4531c4ec2509d615c0a").build())).build();

        Assertions.assertThrows(BadSearchRequest.class,
            () -> caseDocumentAttacher.restCallToAttachCaseDocuments());
    }

    @Test
    @DisplayName("Should throw Resource Not found exception when a document does not exists in document store")
    void shouldThrowResourceNotFoundExceptionWhenDocumentIsMissing() {
        doThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "The resource 388a1ce0-f132-4680-90e9-5e782721cabb was not found"))
            .when(restTemplate).exchange(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any());

        caseDocumentAttacher.caseDocumentsMetadata =
            CaseDocumentsMetadata
                .builder()
                .documentHashToken(Collections.singletonList(DocumentHashToken.builder().id("388a1ce0-f132-4680-90e9-5e782721cabb")
                    .hashToken("57e7fdf75e281aaa03a0f50f93e7b10bbebff162cf67a4531c4ec2509d615c0a").build())).build();

        Assertions.assertThrows(ResourceNotFoundException.class,
            () -> caseDocumentAttacher.restCallToAttachCaseDocuments());
    }

    @Test
    @DisplayName("Should throw a Service exception when the downstream application fails")
    void shouldThrowServiceExceptionWhenDownstreamApplicationFails() {
        doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
            .when(restTemplate).exchange(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any());

        caseDocumentAttacher.caseDocumentsMetadata =
            CaseDocumentsMetadata
                .builder()
                .documentHashToken(Collections.singletonList(DocumentHashToken.builder().id("388a1ce0-f132-4680-90e9-5e782721cabb")
                    .hashToken("57e7fdf75e281aaa03a0f50f93e7b10bbebff162cf67a4531c4ec2509d615c0a").build())).build();

        Assertions.assertThrows(ServiceException.class,
            () -> caseDocumentAttacher.restCallToAttachCaseDocuments());
    }

    @Test
    @DisplayName("Should throw Resource Not found exception when a document does not exists in document store")
    void shouldThroServiceExceptionWhenDownstreapApplicationFails() {
        doThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
            .when(restTemplate).exchange(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any());

        caseDocumentAttacher.caseDocumentsMetadata =
            CaseDocumentsMetadata
                .builder()
                .documentHashToken(Collections.singletonList(DocumentHashToken.builder().id("388a1ce0-f132-4680-90e9-5e782721cabb")
                    .hashToken("57e7fdf75e281aaa03a0f50f93e7b10bbebff162cf67a4531c4ec2509d615c0a").build())).build();

        Assertions.assertThrows(ServiceException.class,
            () -> caseDocumentAttacher.restCallToAttachCaseDocuments());
    }

    @Test
    @DisplayName("Should throw Service exception when a document hashToken is altered by a Service")
    void shouldThrowServiceExceptionWhenHashtokenIsAlteredByService() {
        doThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN, "The resource", "388a1ce0-f132-4680-90e9-5e782721cabb".getBytes(), StandardCharsets.UTF_8))
            .when(restTemplate).exchange(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any());

        caseDocumentAttacher.caseDocumentsMetadata =
            CaseDocumentsMetadata
                .builder()
                .documentHashToken(Collections.singletonList(DocumentHashToken
                    .builder()
                    .id("388a1ce0-f132-4680-90e9-5e782721cabb")
                    .hashToken("57e7fdf75e281aaa03a0f50f93e7b10bbebff162cf67a4531c4ec2509d615c0a")
                    .build())).build();
        caseDocumentAttacher.documentAfterCallbackOriginalCopy
            .put("388a1ce0-f132-4680-90e9-5e782721cabb", "57e7fdf75e281aaa03a0f50f93e7b10bbebff162cf67a4531c4ec2509d615c0a");

        Assertions.assertThrows(ServiceException.class,
            () -> caseDocumentAttacher.restCallToAttachCaseDocuments());
    }

    @Test
    @DisplayName("Should throw Document Token Exception when user has provided an invalid document token.")
    void shouldThrowDocumentTokenExceptionWhenUserHasProvidedInvalidDocument() {
        doThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN, "The resource", "388a1ce0-f132-4680-90e9-5e782721cabb".getBytes(), StandardCharsets.UTF_8))
            .when(restTemplate).exchange(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any());

        caseDocumentAttacher.caseDocumentsMetadata =
            CaseDocumentsMetadata
                .builder()
                .documentHashToken(Collections.singletonList(DocumentHashToken
                    .builder()
                    .id("388a1ce0-f132-4680-90e9-5e782721cabb")
                    .hashToken("57e7fdf75e281aaa03a0f50f93e7b10bbebff162cf67a4531c4ec2509d615c0a")
                    .build())).build();
        caseDocumentAttacher.documentAfterCallbackOriginalCopy
            .put("TestDocument", "57e7fdf75e281aaa03a0f50f93e7b10bbebff162cf67a4531c4ec2509d615c0a");

        Assertions.assertThrows(DocumentTokenException.class,
            () -> caseDocumentAttacher.restCallToAttachCaseDocuments());
    }

    @Test
    @DisplayName("should call caseDocumentAttachOperation and filter the documents for create case scenario without callback")
    void shouldFilterCaseDocumentMetaDataCreateScenarioWithoutCallback() {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(1111122222333334L);
        caseDetails.setCaseTypeId("BEFTA_CASETYPE_2");
        caseDetails.setData(caseDetailsBefore);
        prepareInputs();
        List<DocumentHashToken> expected = Arrays.asList(
            DocumentHashToken.builder().id("b6ee2bff-8244-431f-94ec-9d8ecace8dd6")
                .hashToken("4d49edc151423fb7b2e1f22d89a2d041b43").build(),
            DocumentHashToken.builder().id("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292")
                .hashToken("4d49edc151423fb7b2e1f22d87b2d041b34").build()
        );

        caseDocumentAttacher.documentsBeforeCallback = Stream.of(new String[][]{
            {"b6ee2bff-8244-431f-94ec-9d8ecace8dd6", "4d49edc151423fb7b2e1f22d89a2d041b43"},
            {"e16f2ae0-d6ce-4bd0-a652-47b3c4d86292", "4d49edc151423fb7b2e1f22d87b2d041b34"}
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        caseDocumentAttacher.caseDocumentAttachOperation(caseDetails,  false);
        List<DocumentHashToken> actual = caseDocumentAttacher.caseDocumentsMetadata.getDocumentHashToken();

        assertAll(
            () -> assertEquals(expected, actual));
    }

    @Test
    @DisplayName("should call caseDocumentAttachOperation and filter the documents for create case scenario with callback")
    void shouldFilterCaseDocumentMetaDataCreateScenarioWithCallback() {
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(1111122222333334L);
        caseDetails.setCaseTypeId("BEFTA_CASETYPE_2");
        caseDetails.setData(caseDetailsBefore);
        prepareInputs();
        List<DocumentHashToken> expected = Collections.singletonList(
            DocumentHashToken.builder().id("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292")
                .hashToken("4d49edc151423fb7b2e1f22d87b2d041b34").build()
        );

        caseDocumentAttacher.documentsBeforeCallback = Stream.of(new String[][]{
            {"b6ee2bff-8244-431f-94ec-9d8ecace8dd6", "4d49edc151423fb7b2e1f22d89a2d041b43"},
            {"e16f2ae0-d6ce-4bd0-a652-47b3c4d86292", "4d49edc151423fb7b2e1f22d87b2d041b34"}
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        caseDocumentAttacher.caseDocumentAttachOperation(caseDetails,  true);
        List<DocumentHashToken> actual = caseDocumentAttacher.caseDocumentsMetadata.getDocumentHashToken();

        assertAll(
            () -> assertEquals(expected, actual));
    }

    @Test
    @DisplayName("should call caseDocumentAttachOperation and filter the documents for UPDATE case scenario with callback")
    void shouldFilterCaseDocumentMetaDataUpdateScenarioWithoutCallback() throws IOException {
        caseDetailsBefore = buildCaseData("case-detail-before-update.json");
        caseDataContent = buildCaseData("case-detail-after-update.json");
        CaseDetails existingCaseDetails = new CaseDetails();
        existingCaseDetails.setReference(1111122222333334L);
        existingCaseDetails.setCaseTypeId("BEFTA_CASETYPE_2");
        existingCaseDetails.setData(caseDetailsBefore);

        CaseDetails caseUpdatePayload = new CaseDetails();
        caseUpdatePayload.setReference(1111122222333334L);
        caseUpdatePayload.setCaseTypeId("BEFTA_CASETYPE_2");
        caseUpdatePayload.setData(caseDataContent);

        prepareInputs();
        List<DocumentHashToken> expected = Collections.singletonList(
            DocumentHashToken.builder().id("8da17150-c001-47d7-bfeb-3dabed9e0976")
                .hashToken("41134reqrfadfed49edc151423fb7b2e1f22d87b2d041b34").build()
        );
        caseDocumentAttacher.caseDocumentsMetadata  = CaseDocumentsMetadata.builder()
            .caseId("12345556")
            .caseTypeId("BEFTA_CASETYPE_2")
            .jurisdictionId("BEFTA_JURISDICTION_2")
            .documentHashToken(Collections.singletonList(
           DocumentHashToken.builder().id("8da17150-c001-47d7-bfeb-3dabed9e0976")
               .hashToken("41134reqrfadfed49edc151423fb7b2e1f22d87b2d041b34").build()))
            .build();

        caseDocumentAttacher.documentsBeforeCallback = Stream.of(new String[][]{
            {"b6ee2bff-8244-431f-94ec-9d8ecace8dd6", "4d49edc151423fb7b2e1f22d89a2d041b43"},
            {"e16f2ae0-d6ce-4bd0-a652-47b3c4d86292", "4d49edc151423fb7b2e1f22d87b2d041b34"}
        }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        caseDocumentAttacher.findDifferenceWithExistingCaseDetail(existingCaseDetails,caseUpdatePayload);
        List<DocumentHashToken> actual = caseDocumentAttacher.caseDocumentsMetadata.getDocumentHashToken();

        assertAll(
            () -> assertEquals(expected, actual));
    }




    static HashMap<String, JsonNode> buildCaseData(String fileName) throws IOException {
        InputStream inputStream =
            CaseDocumentAttacherTest.class.getClassLoader().getResourceAsStream("tests/".concat(fileName));
        return new ObjectMapper().readValue(inputStream, new TypeReference<HashMap<String, JsonNode>>() {
        });
    }

    private void prepareInputs() {
        beforeCallBack = new HashMap<>();
        beforeCallBack.put("b6ee2bff-8244-431f-94ec-9d8ecace8dd6", "4d49edc151423fb7b2e1f22d89a2d041b43");
        beforeCallBack.put("e16f2ae0-d6ce-4bd0-a652-47b3c4d86292", "4d49edc151423fb7b2e1f22d87b2d041b34");

        caseDocumentsMetadata = CaseDocumentsMetadata.builder()
            .caseId("12345556")
            .caseTypeId("BEFTA_CASETYPE_2")
            .jurisdictionId("BEFTA_JURISDICTION_2")
            .documentHashToken(new ArrayList<>())
            .build();
    }
}
