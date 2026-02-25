package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseValidationException;
import uk.gov.hmcts.ccd.test.CaseFieldDefinitionBuilder;
import uk.gov.hmcts.ccd.TestFixtures;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.DOCUMENT_URL;
import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.UPLOAD_TIMESTAMP;

@ExtendWith(MockitoExtension.class)
class CaseDocumentTimestampServiceTest {

    @Mock
    private ApplicationParams applicationParams;

    private CaseDocumentTimestampService underTest;

    private final String urlGoogle = "https://www.google.com";
    private final String urlYahoo = "https://www.yahoo.com";
    private final String urlMicrosoft = "https://www.microsoft.com";
    private final String urlElastic = "https://www.elastic.com";
    private final String urlApple = "https://www.apple.com";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Clock FIXED_CLOCK =
        Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);
    private final JsonNode jsonRequestNode =
        TestFixtures.readJsonTestsResource("case-document-timestamp-request.json");
    private final JsonNode jsonOriginalNode =
        TestFixtures.readJsonTestsResource("case-document-timestamp-original.json");
    private final JsonNode jsonDocumentNode =
        TestFixtures.readJsonTestsResource("case-document-node.json");
    private final JsonNode jsonDocumentNodeWithNullTimestamp =
        TestFixtures.readJsonTestsResource("case-document-node-null-timestamp.json");
    private final JsonNode jsonDocumentNodeWithValidTimestamp =
        TestFixtures.readJsonTestsResource("case-document-node-with-timestamp.json");

    @org.junit.jupiter.api.BeforeEach
    void init() {
        underTest = new CaseDocumentTimestampService(FIXED_CLOCK, applicationParams);
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
        JsonNode result = TestFixtures.copyJsonNode(jsonRequestNode);
        dataMap.put("testNode", result);

        List<JsonNode> listDocuments = underTest.findNodes(dataMap.values());

        assertFalse(listDocuments.isEmpty());
        assertEquals(18, listDocuments.size());

        listDocuments.forEach(e -> System.out.println(e.get(DOCUMENT_URL)));
    }

    @Test
    void testAddTimestamp() {

        Map<String, JsonNode> dataMapOriginal = new HashMap<>();
        JsonNode resultOriginal = TestFixtures.copyJsonNode(jsonOriginalNode);
        dataMapOriginal.put("testNode", resultOriginal);
        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setCaseTypeId("CASE_TYPE_TIMESTAMP");
        caseDetailsDb.setReference(1L);
        caseDetailsDb.setData(dataMapOriginal);

        Map<String, JsonNode> dataMap = new HashMap<>();
        JsonNode result = TestFixtures.copyJsonNode(jsonRequestNode);
        dataMap.put("testNode", result);
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setCaseTypeId("CASE_TYPE_TIMESTAMP");
        caseDetails.setReference(caseDetailsDb.getReference());
        caseDetails.setData(dataMap);

        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of("CASE_TYPE_TIMESTAMP"));

        final int countExpectedChanges = 5;

        List<String> lstDocumentUrls = underTest.findUrlsNotInOriginal(caseDetails, caseDetailsDb);
        assertEquals(countExpectedChanges, lstDocumentUrls.size());

        underTest.addUploadTimestamps(caseDetails, caseDetailsDb);

        Collection<JsonNode> nodes = underTest.findNodes(caseDetails.getData().values());

        assertFalse(lstDocumentUrls.isEmpty());
        List<JsonNode> lstJsonNodes = underTest.findNodes(nodes);
        System.out.println("lstDocumentUrls:" + lstDocumentUrls);
        AtomicInteger countChanges = new AtomicInteger();
        lstJsonNodes.forEach(node -> {
            if (lstDocumentUrls.contains(node.get(DOCUMENT_URL).asText())) {
                countChanges.getAndIncrement();
                System.out.println("node - " + node.get(DOCUMENT_URL).asText() + " : " + node.get(UPLOAD_TIMESTAMP));
                assertTrue(node.has(UPLOAD_TIMESTAMP));
            }
        });
        assertEquals(countExpectedChanges, countChanges.get());
    }

    @Test
    void testInsertTimestampIfNotAlreadyPresent() {
        JsonNode jsonNode = TestFixtures.copyJsonNode(jsonDocumentNode);
        String uploadTimestamp = LocalDateTime.now().toString();
        assertFalse(jsonNode.has(UPLOAD_TIMESTAMP));

        underTest.insertUploadTimestamp(jsonNode, uploadTimestamp);
        assertTrue(jsonNode.has(UPLOAD_TIMESTAMP));
        assertEquals(uploadTimestamp, jsonNode.get(UPLOAD_TIMESTAMP).textValue());
        System.out.println(jsonNode.asText());
    }

    @Test
    void testDoNotInsertTimestampIfAlreadyPresent() {
        final String uploadTimestamp = LocalDateTime.now().minusNanos(5).toString();
        JsonNode jsonNode = TestFixtures.copyJsonNode(jsonDocumentNodeWithValidTimestamp);

        underTest.insertUploadTimestamp(jsonNode, uploadTimestamp);
        assertTrue(jsonNode.has(UPLOAD_TIMESTAMP));
        assertEquals(uploadTimestamp, jsonNode.get(UPLOAD_TIMESTAMP).textValue());

        String uploadTimestampNew = LocalDateTime.now().toString();
        underTest.insertUploadTimestamp(jsonNode, uploadTimestampNew);
        assertNotEquals(uploadTimestampNew, jsonNode.get(UPLOAD_TIMESTAMP).textValue());
        assertEquals(uploadTimestamp, jsonNode.get(UPLOAD_TIMESTAMP).textValue());

        System.out.println(jsonNode.asText());
    }

    @Test
    void insertTimestampIfPresentButNull() {
        final String uploadTimestamp = LocalDateTime.now().minusNanos(5).toString();
        JsonNode jsonNode = TestFixtures.copyJsonNode(jsonDocumentNodeWithNullTimestamp);

        underTest.insertUploadTimestamp(jsonNode, uploadTimestamp);
        assertTrue(jsonNode.has(UPLOAD_TIMESTAMP));
        assertEquals(uploadTimestamp, jsonNode.get(UPLOAD_TIMESTAMP).textValue());
        System.out.println(jsonNode.asText());

        String uploadTimestampNew = LocalDateTime.now().toString();
        underTest.insertUploadTimestamp(jsonNode, uploadTimestampNew);
        assertNotEquals(uploadTimestampNew, jsonNode.get(UPLOAD_TIMESTAMP).textValue());
        assertEquals(uploadTimestamp, jsonNode.get(UPLOAD_TIMESTAMP).textValue());

        System.out.println(jsonNode.asText());
    }

    @Test
    void testFindNewDocuments() {
        List<String> listUrlsDb = generateListOfUrls(jsonOriginalNode);
        assertEquals(13, listUrlsDb.size());

        List<String> listUrlsRequest = generateListOfUrls(jsonRequestNode);
        assertEquals(18, listUrlsRequest.size());

        List<String> listUrlsNew = underTest.findUrlsNotInOriginal(listUrlsDb, listUrlsRequest);

        assertFalse(listUrlsNew.isEmpty());
        assertEquals(5, listUrlsNew.size());

        listUrlsNew.forEach(System.out::println);
    }

    @ParameterizedTest
    @MethodSource("htmlValidationCases")
    void htmlValidation(String regex, String filename, boolean allowed) {
        final String caseTypeId = "CASE_TYPE_HTML_PARAM";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails modified = caseDetailsWithData(caseTypeId,
            Map.of("documentField", createDocumentNode("http://dm/documents/param", filename)));
        CaseDetails original = caseDetailsWithData(caseTypeId, Map.of());

        CaseTypeDefinition caseTypeDefinition =
            buildCaseTypeWithDocumentField("documentField", regex);

        if (allowed) {
            underTest.addUploadTimestamps(modified, original, caseTypeDefinition);
            assertTrue(modified.getData().get("documentField").has(UPLOAD_TIMESTAMP));
        } else {
            assertThrows(CaseValidationException.class, () ->
                underTest.addUploadTimestamps(modified, original, caseTypeDefinition));
        }
    }

    private static Stream<Arguments> htmlValidationCases() {
        return Stream.of(
            Arguments.of(".pdf,.docx", "test.html", false),
            Arguments.of(".pdf,.html", "test.html", true),
            Arguments.of(".pdf,.docx", "test.HTML", false),
            Arguments.of(".PDF,.HTML,.DOCX", "test.HTML", true),
            Arguments.of("html,pdf,docx", "test.html", true),
            Arguments.of("pdf, htm, docx", "test.htm", true),
            Arguments.of(".*\\.(html|pdf|docx)$", "test.html", true),
            Arguments.of(".*\\.(pdf|docx)$", "test.html", false),
            Arguments.of(".*\\.(HTML|PDF)$", "TEST.HTML", true),
            Arguments.of("", "test.html", false)
        );
    }


    @Test
    void testIsCaseTypeUploadTimestampFeatureEnabledForNullCaseType() {
        String caseType = "Case Type 1";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(null);

        assertFalse(underTest.isCaseTypeUploadTimestampFeatureEnabled(caseType));
    }

    @Test
    void testIsCaseTypeUploadTimestampFeatureEnabledForExpectedCaseType() {
        final String caseType = "Case Type 1";

        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseType));

        assertTrue(underTest.isCaseTypeUploadTimestampFeatureEnabled(caseType));
    }

    @Test
    void testIsCaseTypeUploadTimestampFeatureEnabledForNotExpectedCaseType() {
        final String caseType = "Case Type Not This One";

        when(applicationParams.getUploadTimestampFeaturedCaseTypes())
            .thenReturn(List.of("Another Case Type"));

        assertFalse(underTest.isCaseTypeUploadTimestampFeatureEnabled(caseType));
    }

    @Test
    void isToBeUpdatedWithTimestampForNoUploadTimestamp() {
        JsonNode node = TestFixtures.copyJsonNode(jsonDocumentNode);

        assertTrue(underTest.isToBeUpdatedWithTimestamp(node));
    }

    @Test
    void isToBeUpdatedWithTimestampForUploadTimestampWithNullValue() {
        JsonNode node = TestFixtures.copyJsonNode(jsonDocumentNode);
        ((ObjectNode) node).put(UPLOAD_TIMESTAMP, (String) null);

        assertTrue(underTest.isToBeUpdatedWithTimestamp(node));
    }

    @Test
    void isNotToBeUpdatedWithTimestampForUploadTimestampWithValue() {
        JsonNode node = TestFixtures.copyJsonNode(jsonDocumentNode);
        ((ObjectNode) node).put(UPLOAD_TIMESTAMP, "2010-11-12T00:00:00Z");

        assertFalse(underTest.isToBeUpdatedWithTimestamp(node));
    }

    @Test
    void shouldRejectHtmlWithHtmExtension() {
        final String caseTypeId = "CASE_TYPE_HTML_HTM_REJECT";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        caseDetailsModified.setData(Map.of(
            "documentField", createDocumentNode("http://dm/documents/1", "test.htm")
        ));

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        final CaseTypeDefinition caseTypeDefinition =
            buildCaseTypeWithDocumentField("documentField", ".pdf,.docx");

        assertThrows(CaseValidationException.class, () ->
            underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition));
    }

    @Test
    void shouldRejectHtmlWithUpperCaseExtension() {
        final String caseTypeId = "CASE_TYPE_HTML_UPPER_REJECT";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        caseDetailsModified.setData(Map.of(
            "documentField", createDocumentNode("http://dm/documents/1", "test.HTML")
        ));

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        final CaseTypeDefinition caseTypeDefinition =
            buildCaseTypeWithDocumentField("documentField", ".pdf,.docx");

        assertThrows(CaseValidationException.class, () ->
            underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition));
    }

    @Test
    void shouldAllowNonHtmlDocumentWithoutRegex() {
        final String caseTypeId = "CASE_TYPE_NO_REGEX";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        Map<String, JsonNode> data = new HashMap<>();
        data.put("documentField", createDocumentNode("http://dm/documents/1", "test.pdf"));
        caseDetailsModified.setData(data);

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        final CaseTypeDefinition caseTypeDefinition =
            buildCaseTypeWithDocumentField("documentField", null);

        underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition);
        JsonNode documentNode = caseDetailsModified.getData().get("documentField");
        assertTrue(documentNode.has(UPLOAD_TIMESTAMP));
    }

    @Test
    void shouldAllowHtmlWithRegexPattern() {
        final String caseTypeId = "CASE_TYPE_HTML_REGEX";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        Map<String, JsonNode> data = new HashMap<>();
        data.put("documentField", createDocumentNode("http://dm/documents/2", "test.html"));
        caseDetailsModified.setData(data);

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        final CaseTypeDefinition caseTypeDefinition =
            buildCaseTypeWithDocumentField("documentField", ".*\\.(html|pdf|docx)$");

        underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition);
        JsonNode documentNode = caseDetailsModified.getData().get("documentField");
        assertTrue(documentNode.has(UPLOAD_TIMESTAMP));
    }

    @Test
    void shouldRejectHtmlWithRegexPatternNotMatching() {
        final String caseTypeId = "CASE_TYPE_HTML_REGEX_REJECT";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        caseDetailsModified.setData(Map.of(
            "documentField", createDocumentNode("http://dm/documents/1", "test.html")
        ));

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        final CaseTypeDefinition caseTypeDefinition =
            buildCaseTypeWithDocumentField("documentField", ".*\\.(pdf|docx)$");

        assertThrows(CaseValidationException.class, () ->
            underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition));
    }

    @Test
    void shouldAllowHtmlWithCommaDelimitedExtensionsList() {
        final String caseTypeId = "CASE_TYPE_HTML_COMMA_LIST";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        Map<String, JsonNode> data = new HashMap<>();
        data.put("documentField", createDocumentNode("http://dm/documents/3", "test.htm"));
        caseDetailsModified.setData(data);

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        final CaseTypeDefinition caseTypeDefinition =
            buildCaseTypeWithDocumentField("documentField", "pdf, htm, docx");

        underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition);
        JsonNode documentNode = caseDetailsModified.getData().get("documentField");
        assertTrue(documentNode.has(UPLOAD_TIMESTAMP));
    }

    @Test
    void shouldAllowHtmlWithExtensionListWithoutDots() {
        final String caseTypeId = "CASE_TYPE_HTML_NO_DOTS";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        Map<String, JsonNode> data = new HashMap<>();
        data.put("documentField", createDocumentNode("http://dm/documents/4", "test.html"));
        caseDetailsModified.setData(data);

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        final CaseTypeDefinition caseTypeDefinition =
            buildCaseTypeWithDocumentField("documentField", "html,pdf,docx");

        underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition);
        JsonNode documentNode = caseDetailsModified.getData().get("documentField");
        assertTrue(documentNode.has(UPLOAD_TIMESTAMP));
    }

    @Test
    void shouldHandleDocumentWithoutFilename() {
        final String caseTypeId = "CASE_TYPE_NO_FILENAME";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        Map<String, JsonNode> data = new HashMap<>();
        ObjectNode node = objectMapper.createObjectNode();
        node.put(DOCUMENT_URL, "http://dm/documents/5");
        // No document_filename field
        data.put("documentField", node);
        caseDetailsModified.setData(data);

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        final CaseTypeDefinition caseTypeDefinition =
            buildCaseTypeWithDocumentField("documentField", ".pdf");

        underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition);
        JsonNode documentNode = caseDetailsModified.getData().get("documentField");
        assertTrue(documentNode.has(UPLOAD_TIMESTAMP));
    }

    @Test
    void shouldHandleDocumentWithNullFilename() {
        final String caseTypeId = "CASE_TYPE_NULL_FILENAME";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        Map<String, JsonNode> data = new HashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode node = objectMapper.createObjectNode();
        node.put(DOCUMENT_URL, "http://dm/documents/6");
        node.putNull("document_filename");
        data.put("documentField", node);
        caseDetailsModified.setData(data);

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        final CaseTypeDefinition caseTypeDefinition =
            buildCaseTypeWithDocumentField("documentField", ".pdf");

        underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition);
        JsonNode documentNode = caseDetailsModified.getData().get("documentField");
        assertTrue(documentNode.has(UPLOAD_TIMESTAMP));
    }

    @Test
    void shouldHandleInvalidRegexPattern() {
        final String caseTypeId = "CASE_TYPE_INVALID_REGEX";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        caseDetailsModified.setData(Map.of(
            "documentField", createDocumentNode("http://dm/documents/7", "test.html")
        ));

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        final CaseTypeDefinition caseTypeDefinition =
            buildCaseTypeWithDocumentField("documentField", "[invalid(regex");

        assertThrows(CaseValidationException.class, () ->
            underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition));
    }

    @Test
    void shouldHandleEmptyRegexString() {
        final String caseTypeId = "CASE_TYPE_EMPTY_REGEX";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        Map<String, JsonNode> data = new HashMap<>();
        data.put("documentField", createDocumentNode("http://dm/documents/8", "test.html"));
        caseDetailsModified.setData(data);

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        final CaseTypeDefinition caseTypeDefinition =
            buildCaseTypeWithDocumentField("documentField", "");

        assertThrows(CaseValidationException.class, () ->
            underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition));
    }

    @Test
    void shouldHandleBlankRegexString() {
        final String caseTypeId = "CASE_TYPE_BLANK_REGEX";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        Map<String, JsonNode> data = new HashMap<>();
        data.put("documentField", createDocumentNode("http://dm/documents/9", "test.html"));
        caseDetailsModified.setData(data);

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        final CaseTypeDefinition caseTypeDefinition =
            buildCaseTypeWithDocumentField("documentField", "   ");

        assertThrows(CaseValidationException.class, () ->
            underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition));
    }

    @Test
    void shouldHandleCommaOnlyRegexString() {
        final String caseTypeId = "CASE_TYPE_COMMA_ONLY";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        caseDetailsModified.setData(Map.of(
            "documentField", createDocumentNode("http://dm/documents/10", "test.html")
        ));

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        final CaseTypeDefinition caseTypeDefinition =
            buildCaseTypeWithDocumentField("documentField", ",,,");

        assertThrows(CaseValidationException.class, () ->
            underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition));
    }

    @Test
    void shouldHandleComplexFieldWithDocumentInside() {
        final String caseTypeId = "CASE_TYPE_COMPLEX_FIELD";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        Map<String, JsonNode> data = new HashMap<>();

        ObjectNode complexNode = objectMapper.createObjectNode();
        complexNode.set("nestedDocument", createDocumentNode("http://dm/documents/11", "test.html"));
        data.put("complexField", complexNode);
        caseDetailsModified.setData(data);

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        FieldTypeDefinition nestedDocType = new FieldTypeDefinition();
        nestedDocType.setType(FieldTypeDefinition.DOCUMENT);
        nestedDocType.setRegularExpression(".pdf");

        CaseFieldDefinition nestedField = new CaseFieldDefinition();
        nestedField.setId("nestedDocument");
        nestedField.setFieldTypeDefinition(nestedDocType);

        FieldTypeDefinition complexType = new FieldTypeDefinition();
        complexType.setType(FieldTypeDefinition.COMPLEX);
        complexType.setComplexFields(List.of(nestedField));

        CaseFieldDefinition complexField = new CaseFieldDefinition();
        complexField.setId("complexField");
        complexField.setFieldTypeDefinition(complexType);

        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(List.of(complexField));

        assertThrows(CaseValidationException.class, () ->
            underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition));
    }

    @Test
    void shouldHandleCollectionOfDocumentsWithHtml() {
        final String caseTypeId = "CASE_TYPE_COLLECTION";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        Map<String, JsonNode> data = new HashMap<>();

        ObjectNode collectionNode = objectMapper.createArrayNode().addObject();
        collectionNode.set("value", createDocumentNode("http://dm/documents/12", "test.html"));

        ObjectNode arrayWrapper = objectMapper.createObjectNode();
        arrayWrapper.set("docCollection", objectMapper.createArrayNode().add(collectionNode));
        data.put("docCollection", arrayWrapper.get("docCollection"));
        caseDetailsModified.setData(data);

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        FieldTypeDefinition documentType = new FieldTypeDefinition();
        documentType.setType(FieldTypeDefinition.DOCUMENT);
        documentType.setRegularExpression(".pdf");

        FieldTypeDefinition collectionType = new FieldTypeDefinition();
        collectionType.setType(FieldTypeDefinition.COLLECTION);
        collectionType.setCollectionFieldTypeDefinition(documentType);

        CaseFieldDefinition collectionField = new CaseFieldDefinition();
        collectionField.setId("docCollection");
        collectionField.setFieldTypeDefinition(collectionType);

        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(List.of(collectionField));

        assertThrows(CaseValidationException.class, () ->
            underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition));
    }

    @Test
    void shouldHandleMixedCaseHtmlInExtensionList() {
        final String caseTypeId = "CASE_TYPE_MIXED_CASE";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        Map<String, JsonNode> data = new HashMap<>();
        data.put("documentField", createDocumentNode("http://dm/documents/13", "test.HTML"));
        caseDetailsModified.setData(data);

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        final CaseTypeDefinition caseTypeDefinition =
            buildCaseTypeWithDocumentField("documentField", ".PDF,.HTML,.DOCX");

        underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition);
        JsonNode documentNode = caseDetailsModified.getData().get("documentField");
        assertTrue(documentNode.has(UPLOAD_TIMESTAMP));
    }

    @Test
    void shouldHandleCaseInsensitiveRegexMatching() {
        final String caseTypeId = "CASE_TYPE_CASE_INSENSITIVE";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails caseDetailsModified = new CaseDetails();
        caseDetailsModified.setCaseTypeId(caseTypeId);
        Map<String, JsonNode> data = new HashMap<>();
        data.put("documentField", createDocumentNode("http://dm/documents/14", "TEST.HTML"));
        caseDetailsModified.setData(data);

        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setData(Map.of());

        final CaseTypeDefinition caseTypeDefinition =
            buildCaseTypeWithDocumentField("documentField", ".*\\.(html|pdf)$");

        underTest.addUploadTimestamps(caseDetailsModified, caseDetailsDb, caseTypeDefinition);
        JsonNode documentNode = caseDetailsModified.getData().get("documentField");
        assertTrue(documentNode.has(UPLOAD_TIMESTAMP));
    }

    @Test
    void shouldAddTimestampWhenCaseFieldDefinitionsEmpty() {
        final String caseTypeId = "CASE_TYPE_EMPTY_FIELD_DEFS";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        // Arrange data with a new document so addUploadTimestamps has work to do
        JsonNode requestNode = TestFixtures.copyJsonNode(jsonDocumentNode);
        CaseDetails modified = new CaseDetails();
        modified.setCaseTypeId(caseTypeId);
        modified.setData(Map.of("doc", requestNode));

        CaseDetails original = new CaseDetails();
        original.setCaseTypeId(caseTypeId);
        original.setData(Map.of());

        // Empty field definitions should fall back to non-schema processing branch
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(List.of());

        underTest.addUploadTimestamps(modified, original, caseTypeDefinition);

        JsonNode documentNode = modified.getData().get("doc").get("document");
        assertTrue(documentNode.has(UPLOAD_TIMESTAMP));
    }

    @Test
    void getDocumentUrlsHandlesNullAndEmptyCaseDetails() {
        assertTrue(underTest.getDocumentUrls((CaseDetails) null).isEmpty());

        CaseDetails emptyDataCase = new CaseDetails();
        emptyDataCase.setData(null);

        assertTrue(underTest.getDocumentUrls(emptyDataCase).isEmpty());
    }

    @Test
    void shouldExerciseGuardClausesAndNonNewDocuments() {
        final String caseTypeId = "CASE_TYPE_GUARDS";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        Map<String, JsonNode> data = new HashMap<>();

        // Field present but missing document_url -> hits documentNode null/!has(DOCUMENT_URL)
        ObjectNode docNoUrl = objectMapper.createObjectNode();
        docNoUrl.put("document_filename", "file.pdf");
        data.put("docNoUrl", docNoUrl);

        // Field missing entirely -> fieldValue null branch
        // (no entry for missingDoc)

        // Field with null type definition -> fieldTypeDefinition null branch
        ObjectNode nullTypeNode = objectMapper.createObjectNode();
        nullTypeNode.put(DOCUMENT_URL, "http://dm/documents/nulltype");
        data.put("nullTypeField", nullTypeNode);

        // Complex field whose value is not an object -> non-object guard
        data.put("complexField", objectMapper.getNodeFactory().textNode("notAnObject"));

        // Collection field that is not an array -> !isArray guard
        data.put("collectionNotArray", objectMapper.getNodeFactory().textNode("notArray"));

        // Collection with item missing \"value\" -> itemValue null branch
        ObjectNode arrayItemNoValue = objectMapper.createObjectNode();
        data.put("collectionMissingValue", objectMapper.createArrayNode().add(arrayItemNoValue));

        // Collection with null collection type definition -> collectionType null branch
        ObjectNode arrayItemWithValue = objectMapper.createObjectNode();
        arrayItemWithValue.set("value", createDocumentNode("http://dm/documents/notnew", "doc.pdf"));
        data.put("collectionNullType", objectMapper.createArrayNode().add(arrayItemWithValue));

        // Document that exists in DB already -> documentUrlsNew does not contain -> early return
        ObjectNode existingDoc = createDocumentNode("http://dm/documents/already", "existing.pdf");
        data.put("existingDoc", existingDoc);

        final CaseDetails modified = caseDetailsWithData(caseTypeId, data);

        // DB has the same document URL to make it "not new"
        final CaseDetails original = caseDetailsWithData(caseTypeId, Map.of("existingDoc", existingDoc));

        // Build field definitions covering each guard
        List<CaseFieldDefinition> fieldDefinitions = new ArrayList<>();

        fieldDefinitions.add(caseField("docNoUrl", FieldTypeDefinition.DOCUMENT, null));

        CaseFieldDefinition missingDocDef = new CaseFieldDefinition();
        missingDocDef.setId("missingDoc");
        FieldTypeDefinition missingDocType = new FieldTypeDefinition();
        missingDocType.setType(FieldTypeDefinition.DOCUMENT);
        missingDocDef.setFieldTypeDefinition(missingDocType);
        fieldDefinitions.add(missingDocDef);

        CaseFieldDefinition nullTypeDef = new CaseFieldDefinition();
        nullTypeDef.setId("nullTypeField");
        nullTypeDef.setFieldTypeDefinition(null);
        fieldDefinitions.add(nullTypeDef);

        FieldTypeDefinition complexType = new FieldTypeDefinition();
        complexType.setType(FieldTypeDefinition.COMPLEX);
        CaseFieldDefinition complexDef = new CaseFieldDefinition();
        complexDef.setId("complexField");
        complexDef.setFieldTypeDefinition(complexType);
        fieldDefinitions.add(complexDef);

        FieldTypeDefinition collectionNoArrayType = new FieldTypeDefinition();
        collectionNoArrayType.setType(FieldTypeDefinition.COLLECTION);
        CaseFieldDefinition collectionNoArrayDef = new CaseFieldDefinition();
        collectionNoArrayDef.setId("collectionNotArray");
        collectionNoArrayDef.setFieldTypeDefinition(collectionNoArrayType);
        fieldDefinitions.add(collectionNoArrayDef);

        FieldTypeDefinition collectionWithDocType = new FieldTypeDefinition();
        collectionWithDocType.setType(FieldTypeDefinition.COLLECTION);
        FieldTypeDefinition innerDocType = new FieldTypeDefinition();
        innerDocType.setType(FieldTypeDefinition.DOCUMENT);
        collectionWithDocType.setCollectionFieldTypeDefinition(innerDocType);
        CaseFieldDefinition collectionMissingValueDef = new CaseFieldDefinition();
        collectionMissingValueDef.setId("collectionMissingValue");
        collectionMissingValueDef.setFieldTypeDefinition(collectionWithDocType);
        fieldDefinitions.add(collectionMissingValueDef);

        FieldTypeDefinition collectionNullInnerType = new FieldTypeDefinition();
        collectionNullInnerType.setType(FieldTypeDefinition.COLLECTION);
        collectionNullInnerType.setCollectionFieldTypeDefinition(null);
        CaseFieldDefinition collectionNullTypeDef = new CaseFieldDefinition();
        collectionNullTypeDef.setId("collectionNullType");
        collectionNullTypeDef.setFieldTypeDefinition(collectionNullInnerType);
        fieldDefinitions.add(collectionNullTypeDef);

        fieldDefinitions.add(caseField("existingDoc", FieldTypeDefinition.DOCUMENT, null));

        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(fieldDefinitions);

        // Should complete without throwing; guards exercised, existing doc not timestamped
        underTest.addUploadTimestamps(modified, original, caseTypeDefinition);

        // Ensure existing doc not updated (since not new)
        JsonNode existing = modified.getData().get("existingDoc");
        assertFalse(existing.has(UPLOAD_TIMESTAMP));
    }

    @Test
    void shouldCoverRemainingBranchesAndDisabledFeature() {
        // Feature disabled path
        underTest.addUploadTimestamps(new CaseDetails(), new CaseDetails());

        final String caseTypeId = "CASE_TYPE_GUARDS_2";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        // Null data map should be ignored gracefully (data == null guard)
        CaseDetails nullDataDetails = new CaseDetails();
        nullDataDetails.setCaseTypeId(caseTypeId);
        nullDataDetails.setData(null);

        CaseTypeDefinition docOnlyDef = new CaseTypeDefinition();
        docOnlyDef.setCaseFieldDefinitions(List.of(caseField("missingDoc", FieldTypeDefinition.DOCUMENT, null)));

        underTest.addUploadTimestamps(nullDataDetails, new CaseDetails(), docOnlyDef);

        // Build dataset to hit remaining branches
        Map<String, JsonNode> data = new HashMap<>();

        // Missing field (fieldValue null) -> line 165
        // no entry for "missingDoc" in data map

        // Collection with Document items (processCollection happy path)
        ObjectNode collectionItemVal = createDocumentNode("http://dm/documents/collectionDoc", "file.pdf");
        ObjectNode wrapper = objectMapper.createObjectNode();
        wrapper.set("value", collectionItemVal);
        data.put("collectionWithDoc", objectMapper.createArrayNode().add(wrapper));

        // Collection with Complex items (branch to addUploadTimestampToDocument inside collection)
        ObjectNode nestedDoc = createDocumentNode("http://dm/documents/complexDoc", "file.pdf");
        ObjectNode complexValue = objectMapper.createObjectNode();
        complexValue.set("nested", nestedDoc);
        ObjectNode complexWrapper = objectMapper.createObjectNode();
        complexWrapper.set("value", complexValue);
        data.put("collectionWithComplex", objectMapper.createArrayNode().add(complexWrapper));

        // Document with null document_url -> line 239
        ObjectNode docNullUrl = objectMapper.createObjectNode();
        docNullUrl.putNull(DOCUMENT_URL);
        docNullUrl.put("document_filename", "file.pdf");
        data.put("docNullUrl", docNullUrl);

        final CaseDetails modified = caseDetailsWithData(caseTypeId, data);
        final CaseDetails original = caseDetailsWithData(caseTypeId, Map.of());

        FieldTypeDefinition collectionDocType = new FieldTypeDefinition();
        collectionDocType.setType(FieldTypeDefinition.COLLECTION);
        FieldTypeDefinition docType = new FieldTypeDefinition();
        docType.setType(FieldTypeDefinition.DOCUMENT);
        collectionDocType.setCollectionFieldTypeDefinition(docType);
        CaseFieldDefinition collectionDocDef = new CaseFieldDefinition();
        collectionDocDef.setId("collectionWithDoc");
        collectionDocDef.setFieldTypeDefinition(collectionDocType);

        FieldTypeDefinition innerComplex = new FieldTypeDefinition();
        innerComplex.setType(FieldTypeDefinition.COMPLEX);
        innerComplex.setComplexFields(List.of(caseField("nested", FieldTypeDefinition.DOCUMENT, null)));
        FieldTypeDefinition collectionComplexType = new FieldTypeDefinition();
        collectionComplexType.setType(FieldTypeDefinition.COLLECTION);
        collectionComplexType.setCollectionFieldTypeDefinition(innerComplex);
        CaseFieldDefinition collectionComplexDef = new CaseFieldDefinition();
        collectionComplexDef.setId("collectionWithComplex");
        collectionComplexDef.setFieldTypeDefinition(collectionComplexType);

        CaseFieldDefinition docNullUrlDef = caseField("docNullUrl", FieldTypeDefinition.DOCUMENT, null);
        CaseFieldDefinition missingDocDef = caseField("missingDoc", FieldTypeDefinition.DOCUMENT, null);

        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(
            List.of(missingDocDef, collectionDocDef, collectionComplexDef, docNullUrlDef)
        );

        underTest.addUploadTimestamps(modified, original, caseTypeDefinition);

        // Document with null url should not gain timestamp
        assertFalse(modified.getData().get("docNullUrl").has(UPLOAD_TIMESTAMP));
        // Collection document should get timestamp as it's new
        JsonNode collectionDoc = modified.getData().get("collectionWithDoc").get(0).get("value");
        assertTrue(collectionDoc.has(UPLOAD_TIMESTAMP));
        // Nested complex collection document should also be timestamped
        JsonNode nested = modified.getData().get("collectionWithComplex").get(0).get("value").get("nested");
        assertTrue(nested.has(UPLOAD_TIMESTAMP));
    }

    @Test
    void shouldSkipFieldWhenValueMissing() {
        final String caseTypeId = "CASE_TYPE_MISSING_VALUE";
        when(applicationParams.getUploadTimestampFeaturedCaseTypes()).thenReturn(List.of(caseTypeId));

        CaseDetails modified = caseDetailsWithData(caseTypeId, Map.of("absent", objectMapper.nullNode()));
        CaseDetails original = caseDetailsWithData(caseTypeId, Map.of());

        CaseFieldDefinition missingField = caseField("absent", FieldTypeDefinition.DOCUMENT, null);
        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(List.of(missingField));

        // Should simply skip the missing field without exception
        underTest.addUploadTimestamps(modified, original, caseTypeDefinition);

        // Ensure no timestamp added to absent field entry
        JsonNode absentNode = modified.getData().get("absent");
        assertTrue(absentNode.isNull());
    }

    private List<String> generateListOfUrls(JsonNode node) {
        JsonNode result = TestFixtures.copyJsonNode(node);
        Map<String, JsonNode> dataMap = Maps.newHashMap();
        dataMap.put("testNode", result);
        return underTest.findDocumentUrls(dataMap.values());
    }

    private CaseTypeDefinition buildCaseTypeWithDocumentField(String fieldId, String regularExpression) {
        FieldTypeDefinition fieldTypeDefinition = new FieldTypeDefinition();
        fieldTypeDefinition.setType(FieldTypeDefinition.DOCUMENT);
        fieldTypeDefinition.setRegularExpression(regularExpression);

        CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
        caseFieldDefinition.setId(fieldId);
        caseFieldDefinition.setFieldTypeDefinition(fieldTypeDefinition);

        CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(List.of(caseFieldDefinition));
        return caseTypeDefinition;
    }

    private ObjectNode createDocumentNode(String url, String filename) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put(DOCUMENT_URL, url);
        node.put("document_filename", filename);
        return node;
    }

    private CaseDetails caseDetailsWithData(String caseTypeId, Map<String, JsonNode> data) {
        CaseDetails details = new CaseDetails();
        details.setCaseTypeId(caseTypeId);
        details.setData(data);
        return details;
    }

    private CaseFieldDefinition caseField(String id, String type, String regularExpression) {
        return new CaseFieldDefinitionBuilder(id)
            .withType(type)
            .withRegExp(regularExpression)
            .build();
    }


}
