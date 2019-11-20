package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.google.common.collect.Lists;
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
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

/**
 * This service sorts nested fields of a compound field (complex or collection) according to caseEventComplexFields collection.
 * It will sort on its elements order values. No sorting is performed for empty collection.
 *
 * Additionally it sorts the field type's FixedListItem list of case field of FixedList,
 * FixedRadioList and MultiSelectList types.
 */
@Named
@Singleton
public class CompoundFieldOrderService {

    public static final String ROOT = "";

    public void sortNestedFieldsFromCaseEventComplexFields(final CaseField caseField, final List<CaseEventFieldComplex> caseEventComplexFields, final String listElementCode) {
        if (!caseEventComplexFields.isEmpty()) {
            if (caseField.isCompoundFieldType()) {
                List<CaseField> children = caseField.getFieldType().getChildren();
                children.forEach(childField -> {
                    String newListElementCode = isBlank(listElementCode) ? childField.getId() : listElementCode + "." + childField.getId();
                    sortNestedFieldsFromCaseEventComplexFields(childField, getNestedComplexFields(caseEventComplexFields, newListElementCode), newListElementCode);
                });
                List<CaseField> sortedFields = getSortedCompoundTypeFields(caseEventComplexFields, children, listElementCode);
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

    private List<CaseField> getSortedCompoundTypeFields(final List<CaseEventFieldComplex> caseEventComplexFields, final List<CaseField> children, String listElementCode) {
        final List<String> sortedFieldsFromEventFieldOverride = getSortedFieldsFromEventFieldOverride(caseEventComplexFields, listElementCode);
        if (sortedFieldsFromEventFieldOverride.isEmpty()) {
            return children;
        } else {
            return getSortedFieldsFromEventFieldOverride(children, listElementCode, sortedFieldsFromEventFieldOverride);
        }
    }

    private List<CaseField> getSortedFieldsFromEventFieldOverride(final List<CaseField> children, final String listElementCode, final List<String> orderedEventComplexFieldReferences) {
        final List<CaseField> sortedCaseFields = Lists.newArrayList();
        final Map<String, CaseField> childrenCaseIdToCaseField = convertComplexTypeChildrenToOrderedMap(children);
        orderedEventComplexFieldReferences.forEach(reference -> sortedCaseFields.add(childrenCaseIdToCaseField.remove(getReference(listElementCode, reference))));
        addRemainingInEncounterOrder(sortedCaseFields, childrenCaseIdToCaseField);
        return sortedCaseFields;
    }

    private String getReference(final String listElementCode, final String reference) {
        return isBlank(listElementCode) ? reference : substringAfterLast(reference, ".");
    }

    private void addRemainingInEncounterOrder(final List<CaseField> sortedCaseFields, final Map<String, CaseField> childrenCaseIdToCaseField) {
        sortedCaseFields.addAll(childrenCaseIdToCaseField.values());
    }

    private List<String> getSortedFieldsFromEventFieldOverride(final List<CaseEventFieldComplex> caseEventComplexFields, String listElementCode) {
        if (isNotEmpty(caseEventComplexFields)) {
            return caseEventComplexFields.stream()
                .filter(field -> hasOrderAndIsLeaf(listElementCode, field))
                .sorted(comparingInt(CaseEventFieldComplex::getOrder))
                .map(CaseEventFieldComplex::getReference)
                .collect(Collectors.toList());
        }
        return Lists.newArrayList();
    }

    private boolean hasOrderAndIsLeaf(final String listElementCode, final CaseEventFieldComplex field) {
        return field.getOrder() != null && isFieldReferenceALeaf(listElementCode, field);
    }

    private boolean isFieldReferenceALeaf(final String listElementCode, final CaseEventFieldComplex field) {
        return isBlank(listElementCode) ? isTopLevelLeaf(field) : isNotTopLevelLeaf(listElementCode, field);
    }

    private boolean isNotTopLevelLeaf(final String listElementCode, final CaseEventFieldComplex field) {
        return !substringAfterLast(field.getReference(), listElementCode + ".").contains(".");
    }

    private boolean isTopLevelLeaf(final CaseEventFieldComplex field) {
        return !field.getReference().contains(".");
    }

    private Map<String, CaseField> convertComplexTypeChildrenToOrderedMap(final List<CaseField> children) {
        return children.stream().collect(Collectors.toMap(CaseField::getId,
                                                          Function.identity(),
                                                          (v1, v2) -> v1,
                                                          LinkedHashMap::new));
    }

}
