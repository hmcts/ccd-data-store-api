package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
        if (data == null || data.isEmpty()) {
            return data;
        }

        Map<String, JsonNode> processedData = new HashMap<>();

        data.entrySet().stream().forEach(entry -> {
            Optional<CaseField> caseField = caseType.getCaseField(entry.getKey());
            Optional<CaseEventField> caseEventField = event.getCaseEventField(entry.getKey());

            JsonNode result = entry.getValue();

            if (!caseField.isPresent() && !caseEventField.isPresent()) {
                for (AbstractFieldProcessor processor : fieldProcessors) {
                    result = processor.execute(result, caseField.get(), caseEventField.get());
                }
            }

            processedData.put(entry.getKey(), result);
        });

        return processedData;
    }
}
