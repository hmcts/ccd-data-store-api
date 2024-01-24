package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.ThrowableAssert.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CaseDocumentServiceTest extends TestFixtures {
    @Mock
    private CaseService caseService;

    @Mock
    private CaseDocumentUtils documentUtils;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private CaseDocumentAmApiClient caseDocumentAmApiClient;

    @InjectMocks
    private CaseDocumentService underTest;

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String CASE_DETAIL_FIELD = "dataTestField1";
    private static final String UPLOAD_TIMESTAMP = "upload_timestamp";

    private final String urlGoogle = "https://www.google.com";
    private final String urlYahoo = "https://www.yahoo.com";
    private final String urlMicrosoft = "https://www.microsoft.com";
    private final String urlElastic = "https://www.elastic.com";
    private final String urlApple = "https://www.apple.com";

    @Test
    void testShouldReturnClonedCaseDetailsWithoutHashes() throws Exception {
        // Given
        final Map<String, JsonNode> data = fromFileAsMap("new-document-with-hashtoken.json");
        final CaseDetails caseDetails = buildCaseDetails(data);
        final Map<String, JsonNode> dataWithoutHashes = fromFileAsMap("new-document-with-removed-hashtoken.json");

        doReturn(caseDetails).when(caseService).clone(caseDetails);
        doCallRealMethod().when(documentUtils).findDocumentNodes(anyMap());

        // When
        final CaseDetails actualClonedCaseDetails = underTest.stripDocumentHashes(caseDetails);

        // Then
        verify(caseService).clone(caseDetails);
        verify(documentUtils, times(2)).findDocumentNodes(anyMap());

        assertThat(actualClonedCaseDetails)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x.getJurisdiction()).isEqualTo(JURISDICTION_ID);
                assertThat(x.getReference()).isEqualTo(REFERENCE);
                assertThat(x.getState()).isEqualTo(STATE);
                assertThat(x.getData()).isEqualTo(dataWithoutHashes);
            });
    }

    @Test
    void testShouldReturnCloneOfOriginalCaseDetailsWhenNoDocumentsPresent() throws Exception {
        // Given
        final Map<String, JsonNode> data = fromFileAsMap("text-type-case-field.json");
        final CaseDetails caseDetails = buildCaseDetails(data);

        doReturn(true).when(applicationParams).isDocumentHashCloneEnabled();
        doReturn(caseDetails).when(caseService).clone(caseDetails);
        doCallRealMethod().when(documentUtils).findDocumentNodes(anyMap());

        // When
        final CaseDetails actualClonedCaseDetails = underTest.stripDocumentHashes(caseDetails);

        // Then
        verify(caseService).clone(caseDetails);
        verify(documentUtils).findDocumentNodes(anyMap());

        assertThat(actualClonedCaseDetails)
            .isNotNull()
            .satisfies(x -> {
                assertThat(x.getJurisdiction()).isEqualTo(JURISDICTION_ID);
                assertThat(x.getReference()).isEqualTo(REFERENCE);
                assertThat(x.getState()).isEqualTo(STATE);
                assertThat(x.getData()).isEqualTo(data);
            });
    }

    @Test
    void testShouldRaiseExceptionWhenHashTokensAreMissing() {
        // GIVEN
        doReturn(true).when(applicationParams).isDocumentHashCheckingEnabled();
        doReturn(List.of(HASH_TOKEN_B2)).when(documentUtils).getViolatingDocuments(anyList());

        // WHEN
        final Throwable thrown = catchThrowable(() -> underTest.validate(List.of(HASH_TOKEN_B2)));

        // THEN
        verify(documentUtils).getViolatingDocuments(anyList());

        assertThat(thrown)
            .isInstanceOf(ValidationException.class)
            .hasMessageStartingWith("Document hashTokens are missing for the documents: ");
    }

    @Test
    void testShouldBuildValidDocumentHashTokens() {
        // Given
        final Map<String, JsonNode> preCallbackCaseData = emptyMap();
        final Map<String, JsonNode> postCallbackCaseData = emptyMap();

        doReturn(emptyList(), emptyList()).when(documentUtils).findDocumentsHashes(anyMap());
        doReturn(emptySet()).when(documentUtils).getTamperedHashes(anyList(), anyList());
        doReturn(List.of(HASH_TOKEN_A1, HASH_TOKEN_A2))
            .when(documentUtils).buildDocumentHashToken(anyList(), anyList(), anyList());
        doReturn(true).when(applicationParams).isDocumentHashCheckingEnabled();
        doReturn(emptyList()).when(documentUtils).getViolatingDocuments(anyList());

        // When
        final List<DocumentHashToken> result = underTest.extractDocumentHashToken(
            preCallbackCaseData,
            postCallbackCaseData
        );

        // Then
        assertThat(result)
            .isNotNull()
            .hasSameElementsAs(List.of(HASH_TOKEN_A1, HASH_TOKEN_A2));

        verify(documentUtils, times(3)).findDocumentsHashes(anyMap());
        verify(documentUtils).getTamperedHashes(anyList(), anyList());
        verify(documentUtils).buildDocumentHashToken(anyList(), anyList(), anyList());
        verify(documentUtils).getViolatingDocuments(anyList());
        verify(applicationParams).isDocumentHashCheckingEnabled();
    }

    @Test
    void testShouldRaiseValidationException() {
        // Given
        final Map<String, JsonNode> preCallbackCaseData = emptyMap();
        final Map<String, JsonNode> postCallbackCaseData = emptyMap();

        doReturn(emptyList(), emptyList()).when(documentUtils).findDocumentsHashes(anyMap());
        doReturn(emptySet()).when(documentUtils).getTamperedHashes(anyList(), anyList());
        doReturn(List.of(HASH_TOKEN_B1, HASH_TOKEN_B2))
            .when(documentUtils).buildDocumentHashToken(anyList(), anyList(), anyList());
        doReturn(true).when(applicationParams).isDocumentHashCheckingEnabled();
        doReturn(List.of(HASH_TOKEN_B2)).when(documentUtils).getViolatingDocuments(anyList());

        // When
        final Throwable thrown = catchThrowable(() -> underTest.extractDocumentHashToken(
            preCallbackCaseData,
            postCallbackCaseData
        ));

        // Then
        assertThat(thrown)
            .isInstanceOf(ValidationException.class)
            .hasMessageStartingWith("Document hashTokens are missing for the documents: ");

        verify(documentUtils, times(3)).findDocumentsHashes(anyMap());
        verify(documentUtils).getTamperedHashes(anyList(), anyList());
        verify(documentUtils).buildDocumentHashToken(anyList(), anyList(), anyList());
        verify(documentUtils).getViolatingDocuments(anyList());
        verify(applicationParams).isDocumentHashCheckingEnabled();
    }

    @Test
    void testShouldRaiseServiceException() {
        // Given
        final Map<String, JsonNode> preCallbackCaseData = emptyMap();
        final Map<String, JsonNode> postCallbackCaseData = emptyMap();

        doReturn(emptyList(), emptyList()).when(documentUtils).findDocumentsHashes(anyMap());
        doReturn(Set.of("tampered")).when(documentUtils).getTamperedHashes(anyList(), anyList());

        // When
        final Throwable thrown = catchThrowable(() -> underTest.extractDocumentHashToken(
            preCallbackCaseData,
            postCallbackCaseData
        ));

        // Then
        assertThat(thrown)
            .isInstanceOf(ServiceException.class)
            .hasMessageStartingWith("Callback attempted to change the hashToken of the following documents:");

        verify(documentUtils, times(3)).findDocumentsHashes(anyMap());
        verify(documentUtils).getTamperedHashes(anyList(), anyList());
        verifyNoMoreInteractions(documentUtils);
    }

    @Test
    void testShouldApplyCaseDocumentPatch() {
        // Given
        doReturn(true).when(applicationParams).isAttachDocumentEnabled();
        final List<DocumentHashToken> documentHashTokens = List.of(HASH_TOKEN_A1, HASH_TOKEN_A2);
        doNothing().when(caseDocumentAmApiClient).applyPatch(any(CaseDocumentsMetadata.class));

        // When
        underTest.attachCaseDocuments(CASE_REFERENCE, CASE_TYPE_ID, JURISDICTION_ID, documentHashTokens);

        // Then
        verify(caseDocumentAmApiClient).applyPatch(any(CaseDocumentsMetadata.class));
    }

    @Test
    void testShouldNotApplyCaseDocumentPatchWhenFeaturedDisabled() {
        // Given
        doReturn(false).when(applicationParams).isAttachDocumentEnabled();
        final List<DocumentHashToken> documentHashTokens = List.of(HASH_TOKEN_A1, HASH_TOKEN_A2);

        // When
        underTest.attachCaseDocuments(CASE_REFERENCE, CASE_TYPE_ID, JURISDICTION_ID, documentHashTokens);

        // Then
        verify(caseDocumentAmApiClient, never()).applyPatch(any(CaseDocumentsMetadata.class));
    }

    @Test
    void testShouldNotApplyPatchWhenNoDocumentHashes() {
        // When
        underTest.attachCaseDocuments(CASE_REFERENCE, CASE_TYPE_ID, JURISDICTION_ID, emptyList());

        // Then
        verifyNoMoreInteractions(caseDocumentAmApiClient);
    }

    @ParameterizedTest
    @MethodSource("provideValidHashTokenParameters")
    void testShouldCheckForViolations(final List<DocumentHashToken> documentHashTokens) {
        // GIVEN
        doReturn(true).when(applicationParams).isDocumentHashCheckingEnabled();
        doReturn(emptyList()).when(documentUtils).getViolatingDocuments(anyList());

        // WHEN/THEN
        assertThatCode(() -> underTest.validate(documentHashTokens))
            .doesNotThrowAnyException();
    }

    @SuppressWarnings("unused")
    private static Stream<Arguments> provideValidHashTokenParameters() {
        return Stream.of(
            Arguments.of(emptyList()),
            Arguments.of(List.of(HASH_TOKEN_A1, HASH_TOKEN_A2))
        );
    }

    @Test
    void testShouldNotCheckDocumentsForHashToken() {
        // GIVEN
        doReturn(false).when(applicationParams).isDocumentHashCheckingEnabled();

        // WHEN/THEN
        assertThatCode(() -> underTest.validate(emptyList()))
            .doesNotThrowAnyException();

        verifyNoMoreInteractions(documentUtils);
    }

    @Test
    void testFindUrlsNotInOriginal() {
        List<String> dbUrls = List.of(urlGoogle, urlMicrosoft, urlYahoo);
        List<String> requestUrls = List.of(urlGoogle, urlMicrosoft, urlYahoo, urlElastic, urlApple);
        List<String> urlResults = underTest.findUrlsNotInOriginal(dbUrls, requestUrls);

        assertEquals(2, urlResults.size());
        assertTrue(urlResults.contains(urlApple));
        assertTrue(urlResults.contains(urlElastic));
    }

    @Test
    void testFindAllUrlsNotInOriginal() {
        List<String> requestUrls = List.of(urlGoogle, urlMicrosoft, urlYahoo, urlElastic, urlApple);
        List<String> dbUrls = new ArrayList<>();
        List<String> urlResults = underTest.findUrlsNotInOriginal(dbUrls, requestUrls);

        assertEquals(5, urlResults.size());
        assertTrue(urlResults.contains(urlApple));
        assertTrue(urlResults.contains(urlElastic));
        assertTrue(urlResults.contains(urlGoogle));
        assertTrue(urlResults.contains(urlMicrosoft));
        assertTrue(urlResults.contains(urlYahoo));
    }

    @Test
    void testFindNoUrlsInOriginal() {
        List<String> requestUrls = new ArrayList<>();
        List<String> dbUrls = List.of(urlGoogle, urlMicrosoft, urlYahoo, urlElastic, urlApple);
        List<String> urlResults = underTest.findUrlsNotInOriginal(dbUrls, requestUrls);

        assertEquals(0, urlResults.size());
    }

    @Test
    void testFindDocuments() {
        Map<String, JsonNode> dataMap = new HashMap<>();
        JsonNode result = generateTestNode(jsonString);
        dataMap.put("testNode", result);

        List<JsonNode> listDocuments = underTest.findNodes(dataMap.values());

        assertTrue(listDocuments.size() > 0);
        assertEquals(18, listDocuments.size());

        listDocuments.forEach(e -> {
            System.out.println(e.get("document_url"));
        });
    }

    //    @Test
    //    void testAddTimestamp() {
    //        Map<String, JsonNode> dataMapOriginal = new HashMap<>();
    //        JsonNode resultOriginal = generateTestNode(jsonStringOriginal);
    //        dataMapOriginal.put("testNode", resultOriginal);
    //        CaseDetails caseDetailsDb = new CaseDetails();
    //        caseDetailsDb.setReference(Long.valueOf(1));
    //        caseDetailsDb.setData(dataMapOriginal);
    //
    //        when(caseService.getCaseDetailsByCaseReference(any())).thenReturn(caseDetailsDb);
    //
    //        Map<String, JsonNode> dataMap = new HashMap<>();
    //        JsonNode result = generateTestNode(jsonString);
    //        dataMapOriginal.put("testNode", result);
    //        CaseDetails caseDetails = new CaseDetails();
    //        caseDetailsDb.setReference(caseDetailsDb.getReference());
    //        caseDetailsDb.setData(dataMap);
    //
    //        underTest.addUploadTimestamps(caseDetails);
    //
    //        assertTrue(caseDetails.getData().isEmpty());
    //
    //    }

    @Test
    void testInsertTimestampIfNotAlreadyPresent() {
        JsonNode jsonNode = generateTestNode(jsonDocumentNode);
        String uploadTimestamp = ZonedDateTime.now().toString();
        assertFalse(jsonNode.has(UPLOAD_TIMESTAMP));

        underTest.insertUploadTimestamp(jsonNode, uploadTimestamp);
        assertTrue(jsonNode.has(UPLOAD_TIMESTAMP));
        assertEquals(uploadTimestamp, jsonNode.get(UPLOAD_TIMESTAMP).textValue());
        System.out.println(jsonNode.asText());
    }

    @Test
    void testDoNotInsertTimestampIfAlreadyPresent() {
        final String uploadTimestamp = ZonedDateTime.now().minusNanos(5).toString();
        JsonNode jsonNode = generateTestNode(jsonDocumentNode);
        assertFalse(jsonNode.has(UPLOAD_TIMESTAMP));

        underTest.insertUploadTimestamp(jsonNode, uploadTimestamp);
        assertTrue(jsonNode.has(UPLOAD_TIMESTAMP));
        assertEquals(uploadTimestamp, jsonNode.get(UPLOAD_TIMESTAMP).textValue());

        String uploadTimestampNew = ZonedDateTime.now().toString();
        underTest.insertUploadTimestamp(jsonNode, uploadTimestampNew);
        assertNotEquals(uploadTimestampNew, jsonNode.get(UPLOAD_TIMESTAMP).textValue());
        assertEquals(uploadTimestamp, jsonNode.get(UPLOAD_TIMESTAMP).textValue());

        System.out.println(jsonNode.asText());
    }

    @Test
    void testFindNewDocuments() {
        List<String> listUrlsDb = generateListOfUrls(jsonStringOriginal);
        assertEquals(13, listUrlsDb.size());

        List<String> listUrlsRequest = generateListOfUrls(jsonString);
        assertEquals(18, listUrlsRequest.size());

        List<String> listUrlsNew = underTest.findUrlsNotInOriginal(listUrlsDb, listUrlsRequest);

        assertTrue(listUrlsNew.size() > 0);
        assertEquals(5, listUrlsNew.size());

        listUrlsNew.forEach(e -> {
            System.out.println(e);
        });
    }

    private List<String> generateListOfUrls(String jsonString) {
        JsonNode result = generateTestNode(jsonString);
        Map<String, JsonNode> dataMap = Maps.newHashMap();
        dataMap.put("testNode", result);
        return underTest.findDocumentUrls(dataMap.values());
    }


    private Map<String, JsonNode> buildData(String... dataFieldIds) {
        Map<String, JsonNode> dataMap = Maps.newHashMap();
        Lists.newArrayList(dataFieldIds)
            .forEach(dataFieldId -> dataMap.put(dataFieldId, JSON_NODE_FACTORY.textNode(dataFieldId)));
        return dataMap;
    }

    private JsonNode generateTestNode(String json) {

        // Create ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode jsonNode = null;

        try {
            // Parse JSON string to JsonNode
            jsonNode = objectMapper.readTree(json);

        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return jsonNode;

    }

    private static String jsonDocumentNode = "{\n"
        + "  \"document\": {\n"
        + "     \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/63122c23-3665-4dd1-8f81-03d0cb86cac4\",\n"
        + "     \"document_filename\": \"PD36Q letter.pdf\",\n"
        + "     \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/63122c23-3665-4dd1-8f81-03d0cb86cac4/binary\"\n"
        + "   }\n"
        + "}";

    private static String jsonString = "{\n"
        + "    \"id\": \"1675936805799936\",\n"
        + "    \"additionalApplicationsBundle\": [\n"
        + "        {\n"
        + "            \"id\": \"6f5418ac-e59a-42f6-84d0-a9d97c519a4a\",\n"
        + "            \"uploadedDateTime\": \"27-Feb-2023 09:44:18 am\",\n"
        + "            \"otherApplicationsBundle\": {\n"
        + "                \"author\": \"prl_aat_solicitor@mailinator.com\",\n"
        + "                \"document\": {\n"
        + "                    \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/63122c23-3665-4dd1-8f81-03d0cb86cac4\",\n"
        + "                    \"document_filename\": \"PD36Q letter.pdf\",\n"
        + "                    \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/63122c23-3665-4dd1-8f81-03d0cb86cac4/binary\",\n"
        + "                    \"upload_timestamp\": \"2023-03-01T12:34:56\"\n"
        + "                },\n"
        + "                \"newDocument4\": {\n"
        + "                    \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1aaa\",\n"
        + "                    \"document_filename\": \"NewDoc4.pdf\",\n"
        + "                    \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1aaa/binary\",\n"
        + "                    \"upload_timestamp\": \"2024-01-11T17:22:30\"\n"
        + "                }\n"
        + "            }\n"
        + "        }\n"
        + "    ],\n"
        + "   \"orderCollection\": "
        + "   [\n"
        + "   {\n"
        + "       \"id\": \"14cadd3a-1afd-46c1-8805-bd16bdcdb489\",\n"
        + "       \"orderDocument\": {\n"
        + "       \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/f94d7ea6-fbed-4b5d-8155-4c4851c277c8\",\n"
        + "       \"document_filename\": \"Welsh_ChildArrangements_Specific_Prohibited_Steps_C43.pdf\",\n"
        + "       \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/f94d7ea6-fbed-4b5d-8155-4c4851c277c8/binary\"\n"
        + "       },\n"
        + "       \"newDocument5\": {\n"
        + "       \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1bbb\",\n"
        + "       \"document_filename\": \"NewDoc5.pdf\",\n"
        + "       \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1bbb/binary\",\n"
        + "       \"upload_timestamp\": \"2024-01-11T17:22:30\"\n"
        + "       }\n"
        + "   }\n"
        + "   ],\n"
        + "\"previewOrderDoc\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/20555d12-2ee7-4cf3-b827-3a0d9f13753c\",\n"
        + "   \"document_filename\": \"ChildArrangements_Specific_Prohibited_Steps_C43_Draft.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/20555d12-2ee7-4cf3-b827-3a0d9f13753c/binary\"\n"
        + "},\n"
        + "\"finalWelshDocument\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/d6172765-31b2-4985-bbf5-e70ff3280459\",\n"
        + "   \"document_filename\": \"C100FinalDocumentWelsh.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/d6172765-31b2-4985-bbf5-e70ff3280459/binary\"\n"
        + "},\n"
        + "\"submitAndPayDownloadApplicationLink\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/31d4c664-c8a8-447e-a5ea-57c3594ee78e\",\n"
        + "   \"document_filename\": \"Draft_C100_application.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/31d4c664-c8a8-447e-a5ea-57c3594ee78e/binary\"\n"
        + "},\n"
        + "\"draftConsentOrderFile\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/ebefdc86-b523-474e-a3bc-c06a931f1174\",\n"
        + "   \"document_filename\": \"Draft consent order - Smith.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/ebefdc86-b523-474e-a3bc-c06a931f1174/binary\"\n"
        + "},\n"
        + "\"c8WelshDocument\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/40101fec-d005-47a6-b9e9-13c6a97f1dae\",\n"
        + "   \"document_filename\": \"C8Document_Welsh.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/40101fec-d005-47a6-b9e9-13c6a97f1dae/binary\"\n"
        + "},\n"
        + "\"newDocument3\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1zzz\",\n"
        + "   \"document_filename\": \"NewDoc3.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1yyy/zzz\",\n"
        + "   \"upload_timestamp\": \"2024-01-11T17:22:30\"\n"
        + "},\n"
        + "\"previewOrderDocWelsh\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/01f852b8-edaa-488e-8463-2acefad20c87\",\n"
        + "   \"document_filename\": \"Welsh_ChildArrangements_Specific_Prohibited_Steps_C43_Draft.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/01f852b8-edaa-488e-8463-2acefad20c87/binary\"\n"
        + "},\n"
        + "\"c1AWelshDocument\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/e81c1756-d7c1-4c9a-8d0d-45d96a13b417\",\n"
        + "   \"document_filename\": \"C1A_Document_Welsh.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/e81c1756-d7c1-4c9a-8d0d-45d96a13b417/binary\"\n"
        + "},\n"
        + "\"c8Document\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c2300009-51ec-405d-9770-df67ec4e6bc2\",\n"
        + "   \"document_filename\": \"C8Document.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c2300009-51ec-405d-9770-df67ec4e6bc2/binary\"\n"
        + "},\n"
        + "\"finalDocument\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/2cfd7aff-37f6-4b6d-9104-770331d0ee06\",\n"
        + "   \"document_filename\": \"C100FinalDocument.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/2cfd7aff-37f6-4b6d-9104-770331d0ee06/binary\"\n"
        + "},\n"
        + "\"submitAndPayDownloadApplicationWelshLink\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1b6b\",\n"
        + "   \"document_filename\": \"Draft_C100_application_welsh.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1b6b/binary\"\n"
        + "},\n"
        + "\"newDocument1\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1xxx\",\n"
        + "   \"document_filename\": \"NewDoc1.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1xxx/binary\",\n"
        + "   \"upload_timestamp\": \"2024-01-11T17:22:30\"\n"
        + "},\n"
        + "\"newDocument2\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1yyy\",\n"
        + "   \"document_filename\": \"NewDoc2.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1yyy/binary\",\n"
        + "   \"upload_timestamp\": \"2024-01-11T17:22:30\"\n"
        + "},\n"
        + "\"c1ADocument\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/ca1f2ba2-21a6-4b75-9c6f-3f1c5b5564be\",\n"
        + "   \"document_filename\": \"C1A_Document.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/ca1f2ba2-21a6-4b75-9c6f-3f1c5b5564be/binary\"\n"
        + "}\n"
        + "}";

    private static String jsonStringOriginal = "{\n"
        + "    \"id\": \"1675936805799936\",\n"
        + "    \"additionalApplicationsBundle\": [\n"
        + "        {\n"
        + "            \"id\": \"6f5418ac-e59a-42f6-84d0-a9d97c519a4a\",\n"
        + "            \"uploadedDateTime\": \"27-Feb-2023 09:44:18 am\",\n"
        + "            \"otherApplicationsBundle\": {\n"
        + "                \"author\": \"prl_aat_solicitor@mailinator.com\",\n"
        + "                \"document\": {\n"
        + "                    \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/63122c23-3665-4dd1-8f81-03d0cb86cac4\",\n"
        + "                    \"document_filename\": \"PD36Q letter.pdf\",\n"
        + "                    \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/63122c23-3665-4dd1-8f81-03d0cb86cac4/binary\",\n"
        + "                    \"upload_timestamp\": \"2023-03-01T12:34:56\"\n"
        + "                }\n"
        + "            }\n"
        + "        }\n"
        + "    ],\n"
        + "   \"orderCollection\": "
        + "   [\n"
        + "   {\n"
        + "       \"id\": \"14cadd3a-1afd-46c1-8805-bd16bdcdb489\",\n"
        + "       \"orderDocument\": {\n"
        + "       \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/f94d7ea6-fbed-4b5d-8155-4c4851c277c8\",\n"
        + "       \"document_filename\": \"Welsh_ChildArrangements_Specific_Prohibited_Steps_C43.pdf\",\n"
        + "       \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/f94d7ea6-fbed-4b5d-8155-4c4851c277c8/binary\"\n"
        + "       }\n"
        + "   }\n"
        + "   ],\n"
        + "\"previewOrderDoc\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/20555d12-2ee7-4cf3-b827-3a0d9f13753c\",\n"
        + "   \"document_filename\": \"ChildArrangements_Specific_Prohibited_Steps_C43_Draft.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/20555d12-2ee7-4cf3-b827-3a0d9f13753c/binary\"\n"
        + "},\n"
        + "\"finalWelshDocument\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/d6172765-31b2-4985-bbf5-e70ff3280459\",\n"
        + "   \"document_filename\": \"C100FinalDocumentWelsh.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/d6172765-31b2-4985-bbf5-e70ff3280459/binary\"\n"
        + "},\n"
        + "\"submitAndPayDownloadApplicationLink\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/31d4c664-c8a8-447e-a5ea-57c3594ee78e\",\n"
        + "   \"document_filename\": \"Draft_C100_application.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/31d4c664-c8a8-447e-a5ea-57c3594ee78e/binary\"\n"
        + "},\n"
        + "\"draftConsentOrderFile\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/ebefdc86-b523-474e-a3bc-c06a931f1174\",\n"
        + "   \"document_filename\": \"Draft consent order - Smith.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/ebefdc86-b523-474e-a3bc-c06a931f1174/binary\"\n"
        + "},\n"
        + "\"c8WelshDocument\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/40101fec-d005-47a6-b9e9-13c6a97f1dae\",\n"
        + "   \"document_filename\": \"C8Document_Welsh.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/40101fec-d005-47a6-b9e9-13c6a97f1dae/binary\"\n"
        + "},\n"
        + "\"previewOrderDocWelsh\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/01f852b8-edaa-488e-8463-2acefad20c87\",\n"
        + "   \"document_filename\": \"Welsh_ChildArrangements_Specific_Prohibited_Steps_C43_Draft.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/01f852b8-edaa-488e-8463-2acefad20c87/binary\"\n"
        + "},\n"
        + "\"c1AWelshDocument\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/e81c1756-d7c1-4c9a-8d0d-45d96a13b417\",\n"
        + "   \"document_filename\": \"C1A_Document_Welsh.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/e81c1756-d7c1-4c9a-8d0d-45d96a13b417/binary\"\n"
        + "},\n"
        + "\"c8Document\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c2300009-51ec-405d-9770-df67ec4e6bc2\",\n"
        + "   \"document_filename\": \"C8Document.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c2300009-51ec-405d-9770-df67ec4e6bc2/binary\"\n"
        + "},\n"
        + "\"finalDocument\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/2cfd7aff-37f6-4b6d-9104-770331d0ee06\",\n"
        + "   \"document_filename\": \"C100FinalDocument.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/2cfd7aff-37f6-4b6d-9104-770331d0ee06/binary\"\n"
        + "},\n"
        + "\"submitAndPayDownloadApplicationWelshLink\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1b6b\",\n"
        + "   \"document_filename\": \"Draft_C100_application_welsh.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1b6b/binary\"\n"
        + "},\n"
        + "\"c1ADocument\": {\n"
        + "   \"document_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/ca1f2ba2-21a6-4b75-9c6f-3f1c5b5564be\",\n"
        + "   \"document_filename\": \"C1A_Document.pdf\",\n"
        + "   \"document_binary_url\": \"http://dm-store-aat.service.core-compute-aat.internal/documents/ca1f2ba2-21a6-4b75-9c6f-3f1c5b5564be/binary\"\n"
        + "}\n"
        + "}";

}
