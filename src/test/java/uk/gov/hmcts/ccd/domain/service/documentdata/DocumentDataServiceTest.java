package uk.gov.hmcts.ccd.domain.service.documentdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.documentdata.CollectionData;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class DocumentDataServiceTest {

    private static final ObjectMapper MAPPER = JacksonUtils.MAPPER;

    @Mock
    private CreateEventOperation createEventOperation;

    @InjectMocks
    private DocumentDataServiceImpl documentDataService;

    @Test
    void shouldBuildCorrectCaseDataContentStructureFromComplexAttributePath() {
        ArgumentCaptor<CaseDataContent> captor = ArgumentCaptor.forClass(CaseDataContent.class);
        documentDataService.updateDocumentCategoryId("100", 1,
            "ComplexField.DocumentField1", "categoryIdValue");
        verify(createEventOperation).createCaseSystemEvent(any(), captor.capture(), any(), any(), any());
        CaseDataContent actualCaseDataContent = captor.getValue();
        Map<String, JsonNode> expectedData = new HashMap<>();
        String documentData = "{\"DocumentField1\": {\"categoryId\": \"categoryIdValue\"}}";
        JsonNode data = MAPPER.convertValue(documentData, JsonNode.class);
        expectedData.put("ComplexField", data);
        assertEquals(expectedData, actualCaseDataContent.getData());
    }

    @Test
    void shouldBuildCaseDataContentStructureFromTopLevelAttributePath() {
        ArgumentCaptor<CaseDataContent> captor = ArgumentCaptor.forClass(CaseDataContent.class);
        documentDataService.updateDocumentCategoryId("100", 1,
            "DocumentField1", "categoryIdValue");
        verify(createEventOperation).createCaseSystemEvent(any(), captor.capture(), any(), any(), any());
        CaseDataContent actualCaseDataContent = captor.getValue();
        Map<String, JsonNode> expectedData = new HashMap<>();
        String documentData = "{\"categoryId\": \"categoryIdValue\"}";
        JsonNode data = MAPPER.convertValue(documentData, JsonNode.class);
        expectedData.put("DocumentField1", data);
        assertEquals(expectedData, actualCaseDataContent.getData());
    }

    @Test
    void shouldBuildCaseDataContentStructureFromCollectionAttributePath() {
        ArgumentCaptor<CaseDataContent> captor = ArgumentCaptor.forClass(CaseDataContent.class);
        String id = "ef05ef05aca4-6816-4f6d-af21-cbc0394e2a56";
        documentDataService.updateDocumentCategoryId("100", 1,
            "CollectionField[" + id + "]", "categoryIdValue");
        verify(createEventOperation).createCaseSystemEvent(any(), captor.capture(), any(), any(), any());
        CollectionData collectionData = new CollectionData();
        collectionData.setId(id);
        Map<String, String> valueMap = new HashMap<>();
        valueMap.put("categoryId", "categoryIdValue");
        collectionData.setValue(valueMap);
        JsonNode data = MAPPER.convertValue(collectionData, JsonNode.class);
        Map<String, JsonNode> expectedData = new HashMap<>();
        expectedData.put("CollectionField", data);
        CaseDataContent actualCaseDataContent = captor.getValue();
        assertEquals(expectedData, actualCaseDataContent.getData());
    }

    @Test
    void shouldBuildCaseDataContentStructureFromComplexCollectionAttributePath() {
        ArgumentCaptor<CaseDataContent> captor = ArgumentCaptor.forClass(CaseDataContent.class);
        String id = "ef05ef05aca4-6816-4f6d-af21-cbc0394d2a56";
        documentDataService.updateDocumentCategoryId("100", 1,
            "something.respondentDocuments[" + id + "].document", "categoryIdValue");
        verify(createEventOperation).createCaseSystemEvent(any(), captor.capture(), any(), any(), any());
        CaseDataContent actualCaseDataContent = captor.getValue();
        Map<String, JsonNode> expectedData = new HashMap<>();
        String documentData = "\"respondentDocuments\"[{\"id\": \"" + id + "\",\"value\":{\"document\":"
            + " {\"categoryId\": \"categoryIdValue\"}}}]";
        JsonNode data = MAPPER.convertValue(documentData, JsonNode.class);
        expectedData.put("something", data);
        assertEquals(expectedData, actualCaseDataContent.getData());
    }
}
