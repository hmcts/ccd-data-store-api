package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.google.common.collect.Lists;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplex;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FixedListItem;

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

import static java.lang.Integer.valueOf;
import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

/**
 * This service sorts nested fields of a compound field (complex or collection) in the following order:
 * 1) If caseEventComplexFields is non empty array it will sort on its elements order values.
 *      (EventToComplexTypes has at least one value for DisplayOrder column defined it will sort according to this column)
 * 2) If caseEventComplexFields is empty array then it will sort caseFields' children order values.
 *      (ComplexTypes tab's DisplayOrder column)
 *
 * Additionally it sorts the field type's FixedListItem list of case field of FixedList, FixedRadioList and MultiSelectList types.
 */
@Named
@Singleton
public class CompoundFieldOrderService {

    public static final String ROOT = "";

    public void sortNestedFields(final CaseField caseField, final List<CaseEventFieldComplex> caseEventComplexFields, final String listElementCode) {
        if (caseField.isCompound()) {
            List<CaseField> children = caseField.getFieldType().getChildren();
            children.forEach(childField -> {
                String newListElementCode = isBlank(listElementCode) ? childField.getId() : listElementCode + "." + childField.getId();
                sortNestedFields(childField, getNestedComplexFields(caseEventComplexFields, newListElementCode), newListElementCode);
                sortIfFixedListType(childField);
            });
            List<CaseField> sortedFields = getSortedCompoundTypeFields(caseEventComplexFields, children, listElementCode);
            caseField.getFieldType().setChildren(sortedFields);
        } else sortIfFixedListType(caseField);
    }

    private void sortIfFixedListType(final CaseField childField) {
        if (childField.isOneOfFixedListType()) {
            List<FixedListItem> sortedItems = getSortedFixedListItems(childField.getFieldType().getFixedListItems());
            childField.getFieldType().setFixedListItems(sortedItems);
        }
    }

    private List<FixedListItem> getSortedFixedListItems(final List<FixedListItem> fixedListItems) {
        final List<FixedListItem> sortedItems = Lists.newArrayList();
        sortedItems.addAll(fixedListItems.stream()
                                    .filter(field -> field.getOrder() != null)
                                    .sorted(comparing(field -> valueOf(field.getOrder())))
                                    .collect(Collectors.toList()));
        return sortedItems;
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
            return getSortedFieldsFromCaseField(children);
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

    private List<CaseField> getSortedFieldsFromCaseField(final List<CaseField> children) {
        final List<CaseField> sortedCaseFields = Lists.newArrayList();
        sortedCaseFields.addAll(children.stream()
                                    .filter(field -> field.getOrder() != null)
                                    .sorted(comparingInt(CaseField::getOrder))
                                    .collect(Collectors.toList()));
        return sortedCaseFields.isEmpty() ? children : sortedCaseFields;
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
