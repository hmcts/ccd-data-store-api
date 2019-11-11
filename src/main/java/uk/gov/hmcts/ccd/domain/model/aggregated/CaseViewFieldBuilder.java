package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.google.common.collect.Lists;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplex;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Comparator.comparingInt;
import static java.util.Optional.ofNullable;

@Named
@Singleton
public class CaseViewFieldBuilder {

    public CaseViewField build(CaseField caseField, CaseEventField eventField) {
        final CaseViewField field = new CaseViewField();

        field.setId(eventField.getCaseFieldId());
        sortComplexCaseFields(caseField, eventField);
        field.setFieldType(caseField.getFieldType());
        field.setHidden(caseField.getHidden());
        field.setHintText(ofNullable(eventField.getHintText()).orElse(caseField.getHintText()));
        field.setLabel(ofNullable(eventField.getLabel()).orElse(caseField.getLabel()));
        field.setSecurityLabel(caseField.getSecurityLabel());
        field.setDisplayContext(eventField.getDisplayContext());
        field.setDisplayContextParameter(eventField.getDisplayContextParamter());
        field.setShowCondition(eventField.getShowCondition());
        field.setShowSummaryChangeOption(eventField.getShowSummaryChangeOption());
        field.setShowSummaryContentOption(eventField.getShowSummaryContentOption());
        field.setAccessControlLists(caseField.getAccessControlLists());
        field.setMetadata(caseField.isMetadata());

        caseField.propagateACLsToNestedFields();

        return field;
    }

    public void sortComplexCaseFields(final CommonField caseField, final CaseEventField eventField) {
        if (caseField.isCompound()) {
            List<CaseField> children = caseField.getFieldType().getChildren();
            children.forEach(childField -> {
                if (childField.isCollectionFieldType()) {
                    sortComplexCaseFields(childField, eventField);
                } else if (childField.isComplexFieldType()) {
                    sortComplexCaseFields(childField, eventField);
                }
            });
            List<CaseField> sortedFields = getSortedComplexTypeFields(eventField, children);
            if (!sortedFields.isEmpty()) {
                caseField.getFieldType().setChildren(sortedFields);
            }
        }
    }

    private List<CaseField> getSortedComplexTypeFields(final CaseEventField eventField, final List<CaseField> children) {
        final List<CaseField> sortedCaseFields = Lists.newArrayList();
        final List<String> orderedEventComplexFieldReferences = getOrderingOfComplexFieldsForEventFieldIfPresent(eventField);
        if (orderedEventComplexFieldReferences.isEmpty()) {
            sortedCaseFields.addAll(children.stream()
                                        .filter(field -> field.getOrder() != null)
                                        .sorted(comparingInt(CaseField::getOrder))
                                        .collect(Collectors.toList()));
        } else {
            final Map<String, CaseField> childrenCaseIdToCaseField = convertComplexTypeChildrenToMap(children);
            orderedEventComplexFieldReferences.forEach(reference -> sortedCaseFields.add(childrenCaseIdToCaseField.get(reference)));
        }
        return sortedCaseFields;
    }

    private List<String> getOrderingOfComplexFieldsForEventFieldIfPresent(final CaseEventField eventField) {
        if (eventField != null) {
            return eventField.getCaseEventFieldComplex().stream()
                .filter(field -> field.getOrder() != null)
                .sorted(comparingInt(CaseEventFieldComplex::getOrder))
                .map(CaseEventFieldComplex::getReference)
                .collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    private Map<String, CaseField> convertComplexTypeChildrenToMap(final List<CaseField> children) {
        return children.stream().collect(Collectors.toMap(CaseField::getId, Function.identity()));
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
