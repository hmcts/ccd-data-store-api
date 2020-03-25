package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.*;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class FieldProcessorService {

    private final List<CaseDataFieldProcessor> caseDataFieldProcessors;
    private final UIDefinitionRepository uiDefinitionRepository;
    private final EventTriggerService eventTriggerService;

    @Autowired
    public FieldProcessorService(final List<CaseDataFieldProcessor> caseDataFieldProcessors,
                                 final UIDefinitionRepository uiDefinitionRepository,
                                 final EventTriggerService eventTriggerService) {
         this.caseDataFieldProcessors = caseDataFieldProcessors;
         this.uiDefinitionRepository = uiDefinitionRepository;
         this.eventTriggerService = eventTriggerService;
    }

    public Map<String, JsonNode> processData(final Map<String, JsonNode> data,
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
            final Optional<CaseField> caseField = caseType.getCaseField(entry.getKey());
            final Optional<CaseEventField> caseEventField = event.getCaseEventField(entry.getKey());

            JsonNode result = entry.getValue();
            if (!isNullOrEmpty(result) && caseField.isPresent() && caseEventField.isPresent()) {
                for (CaseDataFieldProcessor processor : caseDataFieldProcessors) {
                    result = processor.execute(result, caseField.get(), caseEventField.get(),
                        wizardPageFields.stream().filter(f -> f.getCaseFieldId().equals(caseField.get().getId())).findAny().orElse(null));
                }
            }

            processedData.put(entry.getKey(), result);
        });

        return processedData;
    }

    public Map<String, JsonNode> processData(final Map<String, JsonNode> data,
                                             final CaseType caseType,
                                             final String eventId) {
        return processData(data, caseType, eventTriggerService.findCaseEvent(caseType, eventId));
    }

    private boolean isNullOrEmpty(final JsonNode node) {
        return node == null
            || node.isNull()
            || (node.isTextual() && (null == node.asText() || node.asText().trim().length() == 0))
            || (node.isObject() && node.toString().equals("{}"));
    }
}
