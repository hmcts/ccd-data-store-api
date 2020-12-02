package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.UIDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPageField;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FieldProcessorService {

    private final List<CaseDataFieldProcessor> caseDataFieldProcessors;
    private final List<CaseViewFieldProcessor> caseViewFieldProcessors;
    private final UIDefinitionRepository uiDefinitionRepository;
    private final EventTriggerService eventTriggerService;

    @Autowired
    public FieldProcessorService(final List<CaseDataFieldProcessor> caseDataFieldProcessors,
                                 final List<CaseViewFieldProcessor> caseViewFieldProcessors,
                                 final UIDefinitionRepository uiDefinitionRepository,
                                 final EventTriggerService eventTriggerService) {
        this.caseDataFieldProcessors = caseDataFieldProcessors;
        this.caseViewFieldProcessors = caseViewFieldProcessors;
        this.uiDefinitionRepository = uiDefinitionRepository;
        this.eventTriggerService = eventTriggerService;
    }

    public List<CaseViewField> processCaseViewFields(final List<CaseViewField> fields,
                                                     final CaseTypeDefinition caseType,
                                                     final CaseEventDefinition event) {
        return fields.stream()
            .map(field -> processCaseViewField(field, getWizardPageFields(caseType.getId(), event.getId())))
            .collect(Collectors.toList());
    }

    public CaseViewField processCaseViewField(final CaseViewField field) {
        CaseViewField result = field;
        for (CaseViewFieldProcessor processor : caseViewFieldProcessors) {
            result = processor.execute(result, null);
        }
        return result;
    }

    private CaseViewField processCaseViewField(final CaseViewField field,
                                               final List<WizardPageField> wizardPageFields) {
        CaseViewField result = field;
        for (CaseViewFieldProcessor processor : caseViewFieldProcessors) {
            result = processor.execute(result, wizardPageField(wizardPageFields, field.getId()));
        }
        return result;
    }

    public Map<String, JsonNode> processData(final Map<String, JsonNode> data,
                                             final CaseTypeDefinition caseTypeDefinition,
                                             final CaseEventDefinition caseEventDefinition) {
        if (MapUtils.isEmpty(data)) {
            return data;
        }

        final List<WizardPageField> wizardPageFields = getWizardPageFields(caseTypeDefinition.getId(),
            caseEventDefinition.getId());

        Map<String, JsonNode> processedData = new HashMap<>();

        data.forEach((fieldId, node) -> {
            final Optional<CaseFieldDefinition> caseField = caseTypeDefinition.getCaseField(fieldId);
            final Optional<CaseEventFieldDefinition> caseEventField = caseEventDefinition.getCaseEventField(fieldId);

            if (!isNullOrEmpty(node) && caseField.isPresent() && caseEventField.isPresent()) {
                for (CaseDataFieldProcessor processor : caseDataFieldProcessors) {
                    node = processor.execute(node, caseField.get(), caseEventField.get(),
                        wizardPageField(wizardPageFields, caseField.get().getId()));
                }
            }

            processedData.put(fieldId, node);
        });

        return processedData;
    }

    public Map<String, JsonNode> processData(final Map<String, JsonNode> data,
                                             final CaseTypeDefinition caseTypeDefinition,
                                             final String eventId) {
        return processData(data, caseTypeDefinition, eventTriggerService.findCaseEvent(caseTypeDefinition, eventId));
    }

    private List<WizardPageField> getWizardPageFields(String caseTypeId, String eventId) {
        return uiDefinitionRepository.getWizardPageCollection(caseTypeId, eventId)
            .stream()
            .map(WizardPage::getWizardPageFields)
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    private WizardPageField wizardPageField(List<WizardPageField> wizardPageFields, String fieldId) {
        return wizardPageFields.stream().filter(f -> f.getCaseFieldId().equals(fieldId)).findAny().orElse(null);
    }

    private boolean isNullOrEmpty(final JsonNode node) {
        return node == null
            || node.isNull()
            || (node.isTextual() && (null == node.asText() || node.asText().trim().length() == 0))
            || (node.isObject() && node.toString().equals("{}"));
    }
}
