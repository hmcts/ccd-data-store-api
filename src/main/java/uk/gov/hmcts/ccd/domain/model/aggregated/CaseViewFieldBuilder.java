package uk.gov.hmcts.ccd.domain.model.aggregated;

import uk.gov.hmcts.ccd.domain.model.definition.CaseEventField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

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

    public CaseViewField build(CaseField caseField, CaseEventField eventField) {
        final CaseViewField field = new CaseViewField();

        field.setId(eventField.getCaseFieldId());
        buildFieldType(caseField, eventField, field);
        field.setHidden(caseField.getHidden());
        field.setHintText(ofNullable(eventField.getHintText()).orElse(caseField.getHintText()));
        field.setLabel(ofNullable(eventField.getLabel()).orElse(caseField.getLabel()));
        field.setSecurityLabel(caseField.getSecurityLabel());
        field.setDisplayContext(eventField.getDisplayContext());
        field.setDisplayContextParameter(eventField.getDisplayContextParameter());
        field.setShowCondition(eventField.getShowCondition());
        field.setShowSummaryChangeOption(eventField.getShowSummaryChangeOption());
        field.setShowSummaryContentOption(eventField.getShowSummaryContentOption());
        field.setAccessControlLists(caseField.getAccessControlLists());
        field.setMetadata(caseField.isMetadata());
        field.setFormattedValue(caseField.getFormattedValue());

        caseField.propagateACLsToNestedFields();

        return field;
    }

    private void buildFieldType(final CaseField caseField, final CaseEventField eventField, final CaseViewField field) {
        compoundFieldOrderService.sortNestedFieldsFromCaseEventComplexFields(caseField, eventField.getCaseEventFieldComplex(), ROOT);
        field.setFieldType(caseField.getFieldType());
    }

    public CaseViewField build(CaseField caseField, CaseEventField eventField, Object value) {
        final CaseViewField field = build(caseField, eventField);
        field.setValue(value);

        return field;
    }

    public List<CaseViewField> build(List<CaseField> caseFields, List<CaseEventField> eventFields, Map<String, ?> data) {
        final Map<String, CaseField> caseFieldMap = caseFields.stream()
            .collect(Collectors.toMap(CaseField::getId, Function.identity()));

        return eventFields.stream()
            .filter(eventField -> caseFieldMap.containsKey(eventField.getCaseFieldId()))
            .map(eventField -> build(caseFieldMap.get(eventField.getCaseFieldId()), eventField, data != null ? data.get(eventField.getCaseFieldId()) : null))
            .collect(Collectors.toList());
    }
}
