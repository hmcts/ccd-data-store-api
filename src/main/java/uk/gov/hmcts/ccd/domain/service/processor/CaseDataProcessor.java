package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CaseDataProcessor {

    private final List<AbstractFieldProcessor> fieldProcessors;
    private final UIDefinitionRepository uiDefinitionRepository;
    private final EventTriggerService eventTriggerService;

    @Autowired
    public CaseDataProcessor(final List<AbstractFieldProcessor> fieldProcessors,
                             final UIDefinitionRepository uiDefinitionRepository,
                             final EventTriggerService eventTriggerService) {
         this.fieldProcessors = fieldProcessors;
         this.uiDefinitionRepository = uiDefinitionRepository;
         this.eventTriggerService = eventTriggerService;
    }

    public Map<String, JsonNode> process(final Map<String, JsonNode> data,
                                         final CaseType caseType,
                                         final CaseEvent event) {
        if (MapUtils.isEmpty(data)) {
            return data;
        }

        final List<WizardPageField> wizardPageFields = uiDefinitionRepository.getWizardPageCollection(caseType.getId(), event.getId())
            .stream()
            .map(WizardPage::getWizardPageFields)
            .flatMap(List::stream)
            .collect(Collectors.toList());

        Map<String, JsonNode> processedData = new HashMap<>();

        data.entrySet().stream().forEach(entry -> {
            Optional<CaseField> caseField = caseType.getCaseField(entry.getKey());
            Optional<CaseEventField> caseEventField = event.getCaseEventField(entry.getKey());

            JsonNode result = entry.getValue();
            if (!isNullOrEmpty(result) && caseField.isPresent() && caseEventField.isPresent()) {
                for (AbstractFieldProcessor processor : fieldProcessors) {
                    result = processor.execute(result, caseField.get(), caseEventField.get(),
                        wizardPageFields.stream().filter(f -> f.getCaseFieldId().equals(caseField.get().getId())).findAny().get());
                }
            }

            processedData.put(entry.getKey(), result);
        });

        return processedData;
    }

    public Map<String, JsonNode> process(final Map<String, JsonNode> data,
                                         final CaseType caseType,
                                         final String eventId) {
        return process(data, caseType, eventTriggerService.findCaseEvent(caseType, eventId));
    }

    private boolean isNullOrEmpty(final JsonNode node) {
        return node == null
            || node.isNull()
            || (node.isTextual() && (null == node.asText() || node.asText().trim().length() == 0))
            || (node.isObject() && node.toString().equals("{}"));
    }
}
