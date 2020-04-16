package uk.gov.hmcts.ccd.domain.model.aggregated;

import uk.gov.hmcts.ccd.domain.model.definition.CaseEventField;
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

    public CaseViewField build(CaseFieldDefinition caseFieldDefinition, CaseEventField eventField) {
        final CaseViewField field = new CaseViewField();

        field.setId(eventField.getCaseFieldId());
        buildFieldType(caseFieldDefinition, eventField, field);
        field.setHidden(caseFieldDefinition.getHidden());
        field.setHintText(ofNullable(eventField.getHintText()).orElse(caseFieldDefinition.getHintText()));
        field.setLabel(ofNullable(eventField.getLabel()).orElse(caseFieldDefinition.getLabel()));
        field.setSecurityLabel(caseFieldDefinition.getSecurityLabel());
        field.setDisplayContext(eventField.getDisplayContext());
        field.setDisplayContextParameter(eventField.getDisplayContextParamter());
        field.setShowCondition(eventField.getShowCondition());
        field.setShowSummaryChangeOption(eventField.getShowSummaryChangeOption());
        field.setShowSummaryContentOption(eventField.getShowSummaryContentOption());
        field.setAccessControlLists(caseFieldDefinition.getAccessControlLists());
        field.setMetadata(caseFieldDefinition.isMetadata());

        caseFieldDefinition.propagateACLsToNestedFields();

        return field;
    }

    private void buildFieldType(final CaseFieldDefinition caseFieldDefinition, final CaseEventField eventField, final CaseViewField field) {
        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(caseFieldDefinition, eventField.getCaseEventFieldComplex(), ROOT);
        field.setFieldType(caseFieldDefinition.getFieldType());
    }

    public CaseViewField build(CaseFieldDefinition caseFieldDefinition, CaseEventField eventField, Object value) {
        final CaseViewField field = build(caseFieldDefinition, eventField);
        field.setValue(value);

        return field;
    }

    public List<CaseViewField> build(List<CaseFieldDefinition> caseFieldDefinitions, List<CaseEventField> eventFields, Map<String, ?> data) {
        final Map<String, CaseFieldDefinition> caseFieldMap = caseFieldDefinitions.stream()
            .collect(Collectors.toMap(CaseFieldDefinition::getId, Function.identity()));

        return eventFields.stream()
            .filter(eventField -> caseFieldMap.containsKey(eventField.getCaseFieldId()))
            .map(eventField -> build(caseFieldMap.get(eventField.getCaseFieldId()), eventField, data != null ? data.get(eventField.getCaseFieldId()) : null))
            .collect(Collectors.toList());
    }
}
