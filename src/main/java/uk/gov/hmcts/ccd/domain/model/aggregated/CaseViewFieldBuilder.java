package uk.gov.hmcts.ccd.domain.model.aggregated;

import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CompoundFieldOrderService.ROOT;

@Named
@Singleton
public class CaseViewFieldBuilder {

    private final CompoundFieldOrderService compoundFieldOrderService;

    public CaseViewFieldBuilder(final CompoundFieldOrderService compoundFieldOrderService) {
        this.compoundFieldOrderService = compoundFieldOrderService;
    }

    public CaseViewField build(CaseFieldDefinition caseFieldDefinition, CaseEventFieldDefinition eventFieldDefinition) {
        final CaseViewField field = new CaseViewField();

        field.setId(eventFieldDefinition.getCaseFieldId());
        buildFieldType(caseFieldDefinition, eventFieldDefinition, field);
        field.setHidden(caseFieldDefinition.getHidden());
        field.setHintText(ofNullable(eventFieldDefinition.getHintText()).orElse(caseFieldDefinition.getHintText()));
        field.setLabel(ofNullable(eventFieldDefinition.getLabel()).orElse(caseFieldDefinition.getLabel()));
        field.setSecurityLabel(caseFieldDefinition.getSecurityLabel());
        field.setDisplayContext(eventFieldDefinition.getDisplayContext());
        field.setDisplayContextParameter(eventFieldDefinition.getDisplayContextParameter());
        field.setShowCondition(eventFieldDefinition.getShowCondition());
        field.setShowSummaryChangeOption(eventFieldDefinition.getShowSummaryChangeOption());
        field.setShowSummaryContentOption(eventFieldDefinition.getShowSummaryContentOption());
        field.setRetainHiddenValue(eventFieldDefinition.getRetainHiddenValue());
        field.setPublish(eventFieldDefinition.getPublish());
        field.setPublishAs(eventFieldDefinition.getPublishAs());
        field.setAccessControlLists(caseFieldDefinition.getAccessControlLists());
        field.setMetadata(caseFieldDefinition.isMetadata());
        field.setFormattedValue(caseFieldDefinition.getFormattedValue());

        caseFieldDefinition.propagateACLsToNestedFields();

        return field;
    }

    public CaseViewField build(CaseFieldDefinition caseFieldDefinition,
                               CaseEventFieldDefinition eventField,
                               Object value) {
        final CaseViewField field = build(caseFieldDefinition, eventField);
        field.setValue(value);

        return field;
    }

    public List<CaseViewField> build(List<CaseFieldDefinition> caseFieldDefinitions,
                                     List<CaseEventFieldDefinition> eventFields, Map<String, ?> data) {
        final Map<String, CaseFieldDefinition> caseFieldMap = caseFieldDefinitions.stream()
            .collect(Collectors.toMap(CaseFieldDefinition::getId, Function.identity()));

        return eventFields.stream()
            .filter(eventField -> caseFieldMap.containsKey(eventField.getCaseFieldId()))
            .map(eventField -> build(caseFieldMap.get(eventField.getCaseFieldId()),
                                     eventField,
                                     data != null ? data.get(eventField.getCaseFieldId()) : null))
            .collect(Collectors.toList());
    }

    private void buildFieldType(final CaseFieldDefinition caseFieldDefinition,
                                final CaseEventFieldDefinition eventFieldDefinition,
                                final CaseViewField caseViewField) {
        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(caseFieldDefinition,
            eventFieldDefinition.getCaseEventFieldComplexDefinitions(), ROOT);
        caseViewField.setFieldTypeDefinition(caseFieldDefinition.getFieldTypeDefinition());
    }
}
