package uk.gov.hmcts.ccd.domain.service.documentdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.documentdata.CollectionData;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DocumentDataServiceImpl implements DocumentDataService {

    private final CreateEventOperation createEventOperation;
    private static final ObjectMapper MAPPER = JacksonUtils.MAPPER;


    @Autowired
    public DocumentDataServiceImpl(@Qualifier("authorised") CreateEventOperation createEventOperation) {
        this.createEventOperation = createEventOperation;
    }

    @Override
    public void updateDocumentCategoryId(String caseReference, Integer caseVersion, String attributePath,
                                         String categoryId) {
        createEventOperation.createCaseSystemEvent(caseReference, createCaseDataContent(categoryId, attributePath),
            caseVersion, attributePath, categoryId);
    }

    private CaseDataContent createCaseDataContent(String categoryId, String attributePath) {

        //creates data structure from attribute path to check access to field
        Map<String, JsonNode> data = new HashMap<>();
        CaseDataContent content = new CaseDataContent();
        StringBuilder stringBuilder = new StringBuilder();
        if (attributePath.contains(".")) {
            List<String> paths = Arrays.asList(attributePath.split("\\."));
            List<Character> brackets = new ArrayList<>();
            for (int i = 1; i < paths.size(); i++) {
                if (paths.get(i).contains("[")) {
                    brackets.add(']');
                    brackets.add('}');
                    int idStart = paths.get(i).indexOf("[");
                    int idEnd = paths.get(i).indexOf("]");
                    String key = paths.get(i).substring(0, idStart);
                    String id = paths.get(i).substring(idStart + 1, idEnd);
                    stringBuilder.append("\"").append(key).append("\"").append("[{\"id\": \"").append(id)
                        .append("\",\"value\":");
                } else {
                    brackets.add('}');
                    stringBuilder.append("{\"").append(paths.get(i)).append("\": ");
                }
            }
            stringBuilder.append("{\"categoryId\": \"").append(categoryId).append("\"}");
            Collections.reverse(brackets);
            for (Character bracket : brackets) {
                stringBuilder.append(bracket);
            }
            data.put(paths.get(0), MAPPER.convertValue(stringBuilder, JsonNode.class));
        } else if (attributePath.contains("[")) {
            int idStart = attributePath.indexOf("[");
            int idEnd = attributePath.indexOf("]");
            String id = attributePath.substring(idStart + 1, idEnd);
            CollectionData collectionData = new CollectionData();
            collectionData.setId(id);
            Map<String, String> valueMap = new HashMap<>();
            valueMap.put("categoryId", categoryId);
            collectionData.setValue(valueMap);
            data.put(attributePath.substring(0, idStart), MAPPER.convertValue(collectionData, JsonNode.class));
        } else {
            stringBuilder.append("{\"categoryId\": \"").append(categoryId).append("\"}");
            data.put(attributePath, MAPPER.convertValue(stringBuilder, JsonNode.class));
        }
        content.setData(data);
        Event event = new Event();
        event.setEventId("DocumentUpdated");
        content.setEvent(event);
        return content;
    }
}
