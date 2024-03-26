package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.DOCUMENT_URL;
import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.UPLOAD_TIMESTAMP;

@ExtendWith(MockitoExtension.class)
class CaseDocumentTimestampServiceTest {

    @Mock
    private ApplicationParams applicationParams;

    @InjectMocks
    private CaseDocumentTimestampService underTest;

    private final String urlGoogle = "https://www.google.com";
    private final String urlYahoo = "https://www.yahoo.com";
    private final String urlMicrosoft = "https://www.microsoft.com";
    private final String urlElastic = "https://www.elastic.com";
    private final String urlApple = "https://www.apple.com";

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

        assertFalse(listDocuments.isEmpty());
        assertEquals(18, listDocuments.size());

        listDocuments.forEach(e -> System.out.println(e.get(DOCUMENT_URL)));
    }

    @Test
    void testAddTimestamp() {

        Map<String, JsonNode> dataMapOriginal = new HashMap<>();
        JsonNode resultOriginal = generateTestNode(jsonStringOriginal);
        dataMapOriginal.put("testNode", resultOriginal);
        CaseDetails caseDetailsDb = new CaseDetails();
        caseDetailsDb.setReference(1L);
        caseDetailsDb.setData(dataMapOriginal);

        Map<String, JsonNode> dataMap = new HashMap<>();
        JsonNode result = generateTestNode(jsonString);
        dataMap.put("testNode", result);
        CaseDetails caseDetails = new CaseDetails();
        caseDetails.setReference(caseDetailsDb.getReference());
        caseDetails.setData(dataMap);

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
        JsonNode jsonNode = generateTestNode(jsonDocumentNode);
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
        JsonNode jsonNode = generateTestNode(jsonDocumentNodeWithValidTimestamp);

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
        JsonNode jsonNode = generateTestNode(jsonDocumentNodeWithNullTimestamp);

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
        List<String> listUrlsDb = generateListOfUrls(jsonStringOriginal);
        assertEquals(13, listUrlsDb.size());

        List<String> listUrlsRequest = generateListOfUrls(jsonString);
        assertEquals(18, listUrlsRequest.size());

        List<String> listUrlsNew = underTest.findUrlsNotInOriginal(listUrlsDb, listUrlsRequest);

        assertFalse(listUrlsNew.isEmpty());
        assertEquals(5, listUrlsNew.size());

        listUrlsNew.forEach(System.out::println);
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
        JsonNode node = generateTestNode(jsonDocumentNode);

        assertTrue(underTest.isToBeUpdatedWithTimestamp(node));
    }

    @Test
    void isToBeUpdatedWithTimestampForUploadTimestampWithNullValue() {
        JsonNode node = generateTestNode(jsonDocumentNode);
        ((ObjectNode) node).put(UPLOAD_TIMESTAMP, (String) null);

        assertTrue(underTest.isToBeUpdatedWithTimestamp(node));
    }

    @Test
    void isNotToBeUpdatedWithTimestampForUploadTimestampWithValue() {
        JsonNode node = generateTestNode(jsonDocumentNode);
        ((ObjectNode) node).put(UPLOAD_TIMESTAMP, "2010-11-12T00:00:00Z");

        assertFalse(underTest.isToBeUpdatedWithTimestamp(node));
    }

    private List<String> generateListOfUrls(String jsonString) {
        JsonNode result = generateTestNode(jsonString);
        Map<String, JsonNode> dataMap = Maps.newHashMap();
        dataMap.put("testNode", result);
        return underTest.findDocumentUrls(dataMap.values());
    }

    private JsonNode generateTestNode(String json) {

        // Create ObjectMapper
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode jsonNode;

        try {
            // Parse JSON string to JsonNode
            jsonNode = objectMapper.readTree(json);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return jsonNode;

    }

    private static final String jsonDocumentNode = """
            {
              "document": {
                 "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/63122c23-3665-4dd1-8f81-03d0cb86cac4",
                 "document_filename": "PD36Q letter.pdf",
                 "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/63122c23-3665-4dd1-8f81-03d0cb86cac4/binary"
               }
            }""";

    private static final String jsonDocumentNodeWithNullTimestamp = """
            {
              "document": {
                 "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/63122c23-3665-4dd1-8f81-03d0cb86cac4",
                 "document_filename": "PD36Q letter.pdf",
                 "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/63122c23-3665-4dd1-8f81-03d0cb86cac4/binary",
                 "upload_timestamp": null
               }
            }""";

    private static final String jsonDocumentNodeWithValidTimestamp = """
            {
              "document": {
                 "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/63122c23-3665-4dd1-8f81-03d0cb86cac4",
                 "document_filename": "PD36Q letter.pdf",
                 "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/63122c23-3665-4dd1-8f81-03d0cb86cac4/binary",
                 "upload_timestamp": "2010-11-12T01:02:03.000000000"
               }
            }""";

    private static final String jsonString = """
            {
                "id": "1675936805799936",
                "additionalApplicationsBundle": [
                    {
                        "id": "6f5418ac-e59a-42f6-84d0-a9d97c519a4a",
                        "uploadedDateTime": "27-Feb-2023 09:44:18 am",
                        "otherApplicationsBundle": {
                            "author": "prl_aat_solicitor@mailinator.com",
                            "document": {
                                "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/63122c23-3665-4dd1-8f81-03d0cb86cac4",
                                "document_filename": "PD36Q letter.pdf",
                                "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/63122c23-3665-4dd1-8f81-03d0cb86cac4/binary",
                                "upload_timestamp": "2023-03-01T12:34:56.000000000"
                            },
                            "newDocument4": {
                                "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1aaa",
                                "document_filename": "NewDoc4.pdf",
                                "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1aaa/binary",
                                "upload_timestamp": "2024-01-11T17:22:30"
                            }
                        }
                    }
                ],
               "orderCollection":    [
               {
                   "id": "14cadd3a-1afd-46c1-8805-bd16bdcdb489",
                   "orderDocument": {
                   "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/f94d7ea6-fbed-4b5d-8155-4c4851c277c8",
                   "document_filename": "Welsh_ChildArrangements_Specific_Prohibited_Steps_C43.pdf",
                   "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/f94d7ea6-fbed-4b5d-8155-4c4851c277c8/binary"
                   },
                   "newDocument5": {
                   "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1bbb",
                   "document_filename": "NewDoc5.pdf",
                   "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1bbb/binary",
                   "upload_timestamp": "2024-01-11T17:22:30"
                   }
               }
               ],
            "previewOrderDoc": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/20555d12-2ee7-4cf3-b827-3a0d9f13753c",
               "document_filename": "ChildArrangements_Specific_Prohibited_Steps_C43_Draft.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/20555d12-2ee7-4cf3-b827-3a0d9f13753c/binary"
            },
            "finalWelshDocument": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/d6172765-31b2-4985-bbf5-e70ff3280459",
               "document_filename": "C100FinalDocumentWelsh.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/d6172765-31b2-4985-bbf5-e70ff3280459/binary"
            },
            "submitAndPayDownloadApplicationLink": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/31d4c664-c8a8-447e-a5ea-57c3594ee78e",
               "document_filename": "Draft_C100_application.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/31d4c664-c8a8-447e-a5ea-57c3594ee78e/binary"
            },
            "draftConsentOrderFile": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/ebefdc86-b523-474e-a3bc-c06a931f1174",
               "document_filename": "Draft consent order - Smith.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/ebefdc86-b523-474e-a3bc-c06a931f1174/binary"
            },
            "c8WelshDocument": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/40101fec-d005-47a6-b9e9-13c6a97f1dae",
               "document_filename": "C8Document_Welsh.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/40101fec-d005-47a6-b9e9-13c6a97f1dae/binary"
            },
            "newDocument3": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1zzz",
               "document_filename": "NewDoc3.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1yyy/zzz",
               "upload_timestamp": "2024-01-11T17:22:30"
            },
            "previewOrderDocWelsh": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/01f852b8-edaa-488e-8463-2acefad20c87",
               "document_filename": "Welsh_ChildArrangements_Specific_Prohibited_Steps_C43_Draft.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/01f852b8-edaa-488e-8463-2acefad20c87/binary"
            },
            "c1AWelshDocument": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/e81c1756-d7c1-4c9a-8d0d-45d96a13b417",
               "document_filename": "C1A_Document_Welsh.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/e81c1756-d7c1-4c9a-8d0d-45d96a13b417/binary"
            },
            "c8Document": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c2300009-51ec-405d-9770-df67ec4e6bc2",
               "document_filename": "C8Document.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c2300009-51ec-405d-9770-df67ec4e6bc2/binary"
            },
            "finalDocument": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/2cfd7aff-37f6-4b6d-9104-770331d0ee06",
               "document_filename": "C100FinalDocument.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/2cfd7aff-37f6-4b6d-9104-770331d0ee06/binary"
            },
            "submitAndPayDownloadApplicationWelshLink": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1b6b",
               "document_filename": "Draft_C100_application_welsh.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1b6b/binary"
            },
            "newDocument1": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1xxx",
               "document_filename": "NewDoc1.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1xxx/binary",
               "upload_timestamp": "2024-01-11T17:22:30"
            },
            "newDocument2": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1yyy",
               "document_filename": "NewDoc2.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1yyy/binary",
               "upload_timestamp": "2024-01-11T17:22:30"
            },
            "c1ADocument": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/ca1f2ba2-21a6-4b75-9c6f-3f1c5b5564be",
               "document_filename": "C1A_Document.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/ca1f2ba2-21a6-4b75-9c6f-3f1c5b5564be/binary"
            }
            }""";

    private static final String jsonStringOriginal = """
            {
                "id": "1675936805799936",
                "additionalApplicationsBundle": [
                    {
                        "id": "6f5418ac-e59a-42f6-84d0-a9d97c519a4a",
                        "uploadedDateTime": "27-Feb-2023 09:44:18 am",
                        "otherApplicationsBundle": {
                            "author": "prl_aat_solicitor@mailinator.com",
                            "document": {
                                "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/63122c23-3665-4dd1-8f81-03d0cb86cac4",
                                "document_filename": "PD36Q letter.pdf",
                                "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/63122c23-3665-4dd1-8f81-03d0cb86cac4/binary",
                                "upload_timestamp": "2023-03-01T12:34:56"
                            }
                        }
                    }
                ],
               "orderCollection":    [
               {
                   "id": "14cadd3a-1afd-46c1-8805-bd16bdcdb489",
                   "orderDocument": {
                   "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/f94d7ea6-fbed-4b5d-8155-4c4851c277c8",
                   "document_filename": "Welsh_ChildArrangements_Specific_Prohibited_Steps_C43.pdf",
                   "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/f94d7ea6-fbed-4b5d-8155-4c4851c277c8/binary"
                   }
               }
               ],
            "previewOrderDoc": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/20555d12-2ee7-4cf3-b827-3a0d9f13753c",
               "document_filename": "ChildArrangements_Specific_Prohibited_Steps_C43_Draft.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/20555d12-2ee7-4cf3-b827-3a0d9f13753c/binary"
            },
            "finalWelshDocument": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/d6172765-31b2-4985-bbf5-e70ff3280459",
               "document_filename": "C100FinalDocumentWelsh.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/d6172765-31b2-4985-bbf5-e70ff3280459/binary"
            },
            "submitAndPayDownloadApplicationLink": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/31d4c664-c8a8-447e-a5ea-57c3594ee78e",
               "document_filename": "Draft_C100_application.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/31d4c664-c8a8-447e-a5ea-57c3594ee78e/binary"
            },
            "draftConsentOrderFile": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/ebefdc86-b523-474e-a3bc-c06a931f1174",
               "document_filename": "Draft consent order - Smith.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/ebefdc86-b523-474e-a3bc-c06a931f1174/binary"
            },
            "c8WelshDocument": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/40101fec-d005-47a6-b9e9-13c6a97f1dae",
               "document_filename": "C8Document_Welsh.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/40101fec-d005-47a6-b9e9-13c6a97f1dae/binary"
            },
            "previewOrderDocWelsh": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/01f852b8-edaa-488e-8463-2acefad20c87",
               "document_filename": "Welsh_ChildArrangements_Specific_Prohibited_Steps_C43_Draft.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/01f852b8-edaa-488e-8463-2acefad20c87/binary"
            },
            "c1AWelshDocument": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/e81c1756-d7c1-4c9a-8d0d-45d96a13b417",
               "document_filename": "C1A_Document_Welsh.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/e81c1756-d7c1-4c9a-8d0d-45d96a13b417/binary"
            },
            "c8Document": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c2300009-51ec-405d-9770-df67ec4e6bc2",
               "document_filename": "C8Document.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c2300009-51ec-405d-9770-df67ec4e6bc2/binary"
            },
            "finalDocument": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/2cfd7aff-37f6-4b6d-9104-770331d0ee06",
               "document_filename": "C100FinalDocument.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/2cfd7aff-37f6-4b6d-9104-770331d0ee06/binary"
            },
            "submitAndPayDownloadApplicationWelshLink": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1b6b",
               "document_filename": "Draft_C100_application_welsh.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/c50985d5-04cd-4f36-a9db-630bcd4c1b6b/binary"
            },
            "c1ADocument": {
               "document_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/ca1f2ba2-21a6-4b75-9c6f-3f1c5b5564be",
               "document_filename": "C1A_Document.pdf",
               "document_binary_url": "http://dm-store-aat.service.core-compute-aat.internal/documents/ca1f2ba2-21a6-4b75-9c6f-3f1c5b5564be/binary"
            }
            }""";

}
