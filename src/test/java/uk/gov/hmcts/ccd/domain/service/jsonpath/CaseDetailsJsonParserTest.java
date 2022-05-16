package uk.gov.hmcts.ccd.domain.service.jsonpath;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.documentdata.DocumentData;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.ccd.config.JacksonUtils.MAPPER;

class CaseDetailsJsonParserTest {

    private static final String CASE_TYPE_ID = "Grant";
    private static final String STATE_ID = "STATE_1";

    private CaseDetailsJsonParser caseDetailsJsonParser;

    @BeforeEach
    void setUp() {
        caseDetailsJsonParser = new CaseDetailsJsonParser();
    }

    @Test
    void addCaseAccessCategoryIdWhenNotPresent() {
        CaseDetails existingCase = new CaseDetails();
        existingCase.setVersion(1);
        existingCase.setCaseTypeId(CASE_TYPE_ID);
        existingCase.setState(STATE_ID);
        Map<String, JsonNode> map1 = new HashMap<>();
        DocumentData documentData = new DocumentData();
        documentData.setUrl("someurl");
        map1.put("Document", MAPPER.convertValue(documentData, JsonNode.class));
        Map<String, JsonNode> map2 = new HashMap<>();
        map2.put("id", MAPPER.convertValue("12345", JsonNode.class));
        map2.put("value", MAPPER.convertValue(map1, JsonNode.class));
        List<Map<String, JsonNode>> collectionDataList = new ArrayList<>();
        collectionDataList.add(map2);
        Map<String, JsonNode> map3 = new HashMap<>();
        map3.put("DocumentField1", MAPPER.convertValue(collectionDataList, JsonNode.class));
        Map<String, JsonNode> map4 = new HashMap<>();
        map4.put("Complex", MAPPER.convertValue(map3, JsonNode.class));
        existingCase.setData(map4);
        String attributePath = "Complex.DocumentField1[12345].Document";
        caseDetailsJsonParser.updateCaseDocumentData(attributePath, "category1235", existingCase);
        assertCaseData(existingCase, "category1235");
    }

    @Test
    void updateCaseAccessCategoryIdWhenNotPresent() {
        CaseDetails existingCase = new CaseDetails();
        existingCase.setVersion(1);
        existingCase.setCaseTypeId(CASE_TYPE_ID);
        existingCase.setState(STATE_ID);
        Map<String, JsonNode> map1 = new HashMap<>();
        DocumentData documentData = new DocumentData();
        documentData.setUrl("someurl");
        documentData.setCategoryId("categoryId1234");
        map1.put("Document", MAPPER.convertValue(documentData, JsonNode.class));
        Map<String, JsonNode> map2 = new HashMap<>();
        map2.put("id", MAPPER.convertValue("12345", JsonNode.class));
        map2.put("value", MAPPER.convertValue(map1, JsonNode.class));
        List<Map<String, JsonNode>> collectionDataList = new ArrayList<>();
        collectionDataList.add(map2);
        Map<String, JsonNode> map3 = new HashMap<>();
        map3.put("DocumentField1", MAPPER.convertValue(collectionDataList, JsonNode.class));
        Map<String, JsonNode> map4 = new HashMap<>();
        map4.put("Complex", MAPPER.convertValue(map3, JsonNode.class));
        existingCase.setData(map4);
        String attributePath = "Complex.DocumentField1[12345].Document";
        caseDetailsJsonParser.updateCaseDocumentData(attributePath, "category4567", existingCase);
        assertCaseData(existingCase, "category4567");
    }

    private void assertCaseData(CaseDetails caseData, String categoryId) {
        try {
            String json = JacksonUtils.writeValueAsString(caseData.getData());
            assertTrue(json.contains(categoryId));
        } catch (JsonProcessingException jpe) {
            jpe.printStackTrace();
        }
    }
}
