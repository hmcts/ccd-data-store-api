package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CaseDataProcessor {

    private final List<AbstractFieldProcessor> fieldProcessors;

    @Autowired
    public CaseDataProcessor(List<AbstractFieldProcessor> fieldProcessors) {
         this.fieldProcessors = fieldProcessors;
    }

    public Map<String, JsonNode> process(final Map<String, JsonNode> data,
                                         final CaseType caseType,
                                         final CaseEvent event) {
        Map<String, JsonNode> processedData = new HashMap<>();

        data.entrySet().stream().forEach(entry -> {
            CaseField caseField = caseType.getCaseField(entry.getKey()).get();
            CaseEventField caseEventField = event.getCaseEventField(entry.getKey()).get();

            JsonNode result = entry.getValue();
            for (AbstractFieldProcessor processor : fieldProcessors) {
                result = processor.execute(result, caseField, caseEventField);
            }

            processedData.put(entry.getKey(), result);
        });

        return processedData;
    }
}
