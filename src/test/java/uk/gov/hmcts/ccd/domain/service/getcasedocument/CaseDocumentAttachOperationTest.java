package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
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
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

class caseDocumentAttacherTest {

    public static final String COMPLEX = "Complex";
    public static final String COLLECTION = "Collection";
    public static final String DOCUMENT_CASE_FIELD_URL_ATTRIBUTE = "document_url";
    public static final String HASH_TOKEN_STRING = "hashToken";

    private CaseDetails caseDetails;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CaseDocumentAttacher caseDocumentAttacher;

    @BeforeEach
    void setup() throws IOException {
        MockitoAnnotations.initMocks(this);
        caseDetails = new CaseDetails();
        caseDetailsBefore = buildCaseData("case-detail-before-update.json");
        caseDataContent = buildCaseData("case-detail-after-update.json");
        caseDetails.setData(caseDetailsBefore);
        doReturn("http://localhost:4455").when(applicationParams).getCaseDocumentAmApiHost();
        doReturn("/cases/documents/attachToCase").when(applicationParams).getAttachDocumentPath();
        ResponseEntity<String> responseEntity = new ResponseEntity<String>("Success", HttpStatus.OK);
        doReturn(new HttpHeaders()).when(securityUtils).authorizationHeaders();

        doReturn(responseEntity).when(restTemplate).exchange(
            ArgumentMatchers.anyString(),
            ArgumentMatchers.any(HttpMethod.class),
            ArgumentMatchers.any(),
            ArgumentMatchers.<Class<String>>any());
    }

    HashMap<String, JsonNode> caseDetailsBefore;
    HashMap<String, JsonNode> caseDataContent;


    @Test
    @DisplayName("should return document fields differences once updated ")
    void shouldReturnDeltaWhenDocumentFieldsUpdate() {
        Set<String> expectedOutput = new HashSet();
        expectedOutput.add("8da17150-c001-47d7-bfeb-3dabed9e0976");

        final Set<String> output = caseDocumentAttacher.differenceBeforeAndAfterInCaseDetails(caseDetails, caseDataContent);

        assertAll(
            () -> assertEquals(output, expectedOutput)

                 );
    }

    @Test
    @DisplayName("should return document fields differences once document Fields inside Complex Field ")
    void shouldReturnDeltaWhenDocumentFieldsInsideComplexElement() throws IOException {
        HashMap<String, JsonNode> caseDataContent = buildCaseData("case-detail-after-with-complexFields-update.json");
        Set<String> expectedOutput = new HashSet();
        expectedOutput.add("8da17150-c001-47d7-bfeb-3dabed9e0976");

        final Set<String> output = caseDocumentAttacher.differenceBeforeAndAfterInCaseDetails(caseDetails, caseDataContent);

        assertAll(
            () -> assertEquals(output, expectedOutput)

                 );
    }


    @Test
    @DisplayName("should return empty document set in case of CaseDataContent is empty")
    void shouldReturnEmptyDocumentSet() {
        caseDataContent = null;
        Set<String> expectedOutput = new HashSet();
        final Set<String> output = caseDocumentAttacher.differenceBeforeAndAfterInCaseDetails(caseDetails, caseDataContent);

        assertAll(
            () -> assertEquals(output, expectedOutput)

                 );
    }

    @Test
    @DisplayName(
        "should  filter the Case Document Meta Data while  2 documents with hashcode coming from request and  2 documents without hash token from callback " +
        "response")
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

        caseDocumentAttacher.filterDocumentFields(caseDocumentsMetadata, beforeCallBack, afterCallBack);

        List<DocumentHashToken> actual = caseDocumentsMetadata.getDocumentHashToken();

        assertAll(
            () -> assertEquals(actual, expected)

                 );
    }

    @Test
    @DisplayName("should extract only the documents having hashToken from Case Data")
    void shouldExtractDocumentsFromCaseDataBeforeCallBack() throws IOException {

        Map<String, JsonNode> dataMap = buildCaseData("SubmitTransactionDocumentUpload.json");
        caseDocumentAttacher.documentTokenMap = new HashMap<>();

        caseDocumentAttacher.beforeCallbackPrepareDocumentMetaData(dataMap);

        Map<String, String> expectedMap = Stream.of(new String[][] {
            {"388a1ce0-f132-4680-90e9-5e782721cabb", "57e7fdf75e281aaa03a0f50f93e7b10bbebff162cf67a4531c4ec2509d615c0a"},
            {"f0550adc-eaea-4232-b52f-1c4ac0534d60", "UyWGSBgJexcS1i0fTp6QUyWGSBgJexcS1i0fTp6QUyWGSBgJexcS1i0fTp6QUyWGSBgJexcS1i0fTp6Q"},
            {"5c4b5564-a29f-47d3-8c51-50e2d4629435", "6a7e12164534a0c2252a94b308a2a185e46f89ab639c5342027b9cd393068bc"},
            {"7b8930ef-2bcd-44cd-8a78-1ae0b1f5a0ec", "7b8930ef-2bcd-44cd-8a78-17b8930ef-27b8930ef-2bcd-44cd-8a78-1ae0b1f5a0ec"},
            }).collect(Collectors.toMap(data -> data[0], data -> data[1]));

        assertAll(
            () -> assertEquals(caseDocumentAttacher.documentTokenMap, expectedMap));
    }

    @Test
    @DisplayName("Verify that hashToken is removed from the case data payload.")
    void shouldRemoveHashTokenFromDocuments() throws IOException {

        Map<String, JsonNode> dataMap = buildCaseData("SubmitTransactionDocumentUpload.json");
        caseDocumentAttacher.documentTokenMap = new HashMap<>();

        caseDocumentAttacher.beforeCallbackPrepareDocumentMetaData(dataMap);

        JsonNode documentField9 = dataMap.get("DocumentField4");

        assertAll(
            () -> assertNotNull(documentField9.get(DOCUMENT_CASE_FIELD_URL_ATTRIBUTE)),
            () -> assertNull(documentField9.get(HASH_TOKEN_STRING)));
    }

    @Test
    @DisplayName("should throw exception while getting documents without hashToken from Case Data")
    void shouldThrowExceptionWhileExtractingDocumentsFromCaseData() throws IOException {

        Map<String, JsonNode> dataMap = buildCaseData("SubmitTransactionBadDocumentUpload.json");
        Map<String, String> documentMap = new HashMap<>();

        Assertions.assertThrows(BadRequestException.class,
                                () -> caseDocumentAttacher.extractDocumentFieldsBeforeCallback(dataMap, documentMap));
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

        caseDocumentAttacher.afterCallbackPrepareDocumentMetaData(caseDetails, true);
        List<DocumentHashToken> listDocumentHashToken = Arrays.asList(
            DocumentHashToken.builder().id("388a1ce0-f132-4680-90e9-5e782721cabb")
                             .hashToken("57e7fdf75e281aaa03a0f50f93e7b10bbebff162cf67a4531c4ec2509d615c0a").build(),
            DocumentHashToken.builder().id("f0550adc-eaea-4232-b52f-1c4ac0534d60")
                             .hashToken("UyWGSBgJexcS1i0fTp6QUyWGSBgJexcS1i0fTp6QUyWGSBgJexcS1i0fTp6QUyWGSBgJexcS1i0fTp6Q").build(),
            DocumentHashToken.builder().id("5c4b5564-a29f-47d3-8c51-50e2d4629435").hashToken("6a7e12164534a0c2252a94b308a2a185e46f89ab639c5342027b9cd393068bc")
                             .build(),
            DocumentHashToken.builder().id("7b8930ef-2bcd-44cd-8a78-1ae0b1f5a0ec")
                             .hashToken("7b8930ef-2bcd-44cd-8a78-17b8930ef-27b8930ef-2bcd-44cd-8a78-1ae0b1f5a0ec").build());

        assertAll(
            () -> assertTrue(caseDocumentAttacher.caseDocumentsMetadata.getDocumentHashToken().containsAll(listDocumentHashToken)));
    }

    @Test
    @DisplayName("should throw exception while getting documents without hashcode from Case Data after callback response")
    void shouldThrowExceptionWhileExtractingDocumentsFromCaseDataAfterCallback() throws IOException {

        Map<String, JsonNode> dataMap = buildCaseData("SubmitTransactionBadDocumentUpload.json");
        Map<String, String> documentMap = new HashMap<>();

        Assertions.assertThrows(BadRequestException.class,
                                () -> caseDocumentAttacher.extractDocumentFieldsAfterCallback(null, dataMap, documentMap));
    }

    @Test
    @DisplayName("Should call the Case Document AM API to attach document to a case")
    void shouldCallRestclientToAttachDocumentToCase() {
        caseDocumentAttacher.caseDocumentsMetadata =
            CaseDocumentsMetadata.builder()
                                 .documentHashToken(Arrays.asList(
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

    static HashMap<String, JsonNode> buildCaseData(String fileName) throws IOException {
        InputStream inputStream =
            caseDocumentAttacherTest.class.getClassLoader().getResourceAsStream("tests/".concat(fileName));

        return
            new ObjectMapper().readValue(inputStream, new TypeReference<HashMap<String, JsonNode>>() {
            });
    }
}
