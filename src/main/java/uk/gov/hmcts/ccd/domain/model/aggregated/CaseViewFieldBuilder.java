package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplex;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparingInt;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

@Named
@Singleton
public class CaseViewFieldBuilder {

    public CaseViewField build(CaseField caseField, CaseEventField eventField) {
        final CaseViewField field = new CaseViewField();

        field.setId(eventField.getCaseFieldId());
        sortComplexCaseFields(caseField, eventField.getCaseEventFieldComplex(), "");
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

    public void sortComplexCaseFields(final CommonField caseField, final List<CaseEventFieldComplex> caseEventFieldComplexes, final String listElementCode) {
        if (caseField.isCompound()) {
            List<CaseField> children = caseField.getFieldType().getChildren();
            children.forEach(childField -> {
                String newListElementCode = StringUtils.isBlank(listElementCode) ? childField.getId() : listElementCode + "." + childField.getId();
                if (childField.isCollectionFieldType()) {
                    sortComplexCaseFields(childField, getNestedComplexFields(caseEventFieldComplexes, newListElementCode), newListElementCode);
                } else if (childField.isComplexFieldType()) {
                    sortComplexCaseFields(childField, getNestedComplexFields(caseEventFieldComplexes, newListElementCode), newListElementCode);
                }
            });
            List<CaseField> sortedFields = getSortedComplexTypeFields(caseEventFieldComplexes, children, listElementCode);
            if (!sortedFields.isEmpty()) {
                caseField.getFieldType().setChildren(sortedFields);
            }
        }
    }

    private List<CaseEventFieldComplex> getNestedComplexFields(final List<CaseEventFieldComplex> caseEventComplexFields, final String listElementCode) {
        return Optional.ofNullable(caseEventComplexFields)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .filter(caseEventFieldComplex -> caseEventFieldComplex.getReference().startsWith(listElementCode))
            .collect(Collectors.toList());
    }

    private List<CaseField> getSortedComplexTypeFields(final List<CaseEventFieldComplex> caseEventComplexFields, final List<CaseField> children, String listElementCode) {
        final List<CaseField> sortedCaseFields = Lists.newArrayList();
        final List<String> orderedEventComplexFieldReferences = getOrderingOfComplexFieldsForEventFieldIfPresent(caseEventComplexFields, listElementCode);
        if (orderedEventComplexFieldReferences.isEmpty()) {
            sortedCaseFields.addAll(children.stream()
                                        .filter(field -> field.getOrder() != null)
                                        .sorted(comparingInt(CaseField::getOrder))
                                        .collect(Collectors.toList()));
        } else {
            final Map<String, CaseField> childrenCaseIdToCaseField = convertComplexTypeChildrenToMap(children);
            orderedEventComplexFieldReferences.forEach(reference -> sortedCaseFields.add(childrenCaseIdToCaseField.remove(getReference(listElementCode, reference))));
            addRemainingInEncounterOrder(sortedCaseFields, childrenCaseIdToCaseField);
        }
        return sortedCaseFields;
    }

    private String getReference(final String listElementCode, final String reference) {
        return StringUtils.isBlank(listElementCode) ? reference : substringAfterLast(reference, ".");
    }

    private void addRemainingInEncounterOrder(final List<CaseField> sortedCaseFields, final Map<String, CaseField> childrenCaseIdToCaseField) {
        sortedCaseFields.addAll(childrenCaseIdToCaseField.values());
    }

    private List<String> getOrderingOfComplexFieldsForEventFieldIfPresent(final List<CaseEventFieldComplex> caseEventComplexFields, String listElementCode) {
        if (isNotEmpty(caseEventComplexFields)) {
            return caseEventComplexFields.stream()
                .filter(field -> field.getOrder() != null && isFieldReferenceALeaf(listElementCode, field))
                .sorted(comparingInt(CaseEventFieldComplex::getOrder))
                .map(CaseEventFieldComplex::getReference)
                .collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    private boolean isFieldReferenceALeaf(final String listElementCode, final CaseEventFieldComplex field) {
        return StringUtils.isBlank(listElementCode) ?
            !field.getReference().contains(".") : !substringAfterLast(field.getReference(), listElementCode + ".").contains(".");
    }

    private Map<String, CaseField> convertComplexTypeChildrenToMap(final List<CaseField> children) {
        return children.stream().collect(Collectors.toMap(CaseField::getId,
                                                          Function.identity(),
                                                          (v1, v2) -> v1,
                                                          LinkedHashMap::new));
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
