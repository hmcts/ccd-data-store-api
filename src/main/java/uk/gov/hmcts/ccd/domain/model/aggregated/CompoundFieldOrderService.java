package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
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
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

@Named
@Singleton
public class CompoundFieldOrderService {

    public void sortNestedFields(final CommonField caseField, final List<CaseEventFieldComplex> caseEventFieldComplexes, final String listElementCode) {
        if (caseField.isCompound()) {
            List<CaseField> children = caseField.getFieldType().getChildren();
            children.forEach(childField -> {
                String newListElementCode = StringUtils.isBlank(listElementCode) ? childField.getId() : listElementCode + "." + childField.getId();
                sortNestedFields(childField, getNestedComplexFields(caseEventFieldComplexes, newListElementCode), newListElementCode);
            });
            List<CaseField> sortedFields = getSortedCompoundTypeFields(caseEventFieldComplexes, children, listElementCode);
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

    private List<CaseField> getSortedCompoundTypeFields(final List<CaseEventFieldComplex> caseEventComplexFields, final List<CaseField> children, String listElementCode) {
        final List<String> orderedEventComplexFieldReferences = getCompoundFieldsOrderOverride(caseEventComplexFields, listElementCode);
        if (orderedEventComplexFieldReferences.isEmpty()) {
            return getSortedCompoundTypeFieldsFromCaseField(children);
        } else {
            return getSortedCompundTypeFieldsFromEventFieldOverride(children, listElementCode, orderedEventComplexFieldReferences);
        }
    }

    private List<CaseField> getSortedCompundTypeFieldsFromEventFieldOverride(final List<CaseField> children, final String listElementCode, final List<String> orderedEventComplexFieldReferences) {
        final List<CaseField> sortedCaseFields = Lists.newArrayList();
        final Map<String, CaseField> childrenCaseIdToCaseField = convertComplexTypeChildrenToOrderedMap(children);
        orderedEventComplexFieldReferences.forEach(reference -> sortedCaseFields.add(childrenCaseIdToCaseField.remove(getReference(listElementCode, reference))));
        addRemainingInEncounterOrder(sortedCaseFields, childrenCaseIdToCaseField);
        return sortedCaseFields;
    }

    private List<CaseField> getSortedCompoundTypeFieldsFromCaseField(final List<CaseField> children) {
        final List<CaseField> sortedCaseFields = Lists.newArrayList();
        sortedCaseFields.addAll(children.stream()
                                    .filter(field -> field.getOrder() != null)
                                    .sorted(comparingInt(CaseField::getOrder))
                                    .collect(Collectors.toList()));
        return sortedCaseFields;
    }

    private String getReference(final String listElementCode, final String reference) {
        return StringUtils.isBlank(listElementCode) ? reference : substringAfterLast(reference, ".");
    }

    private void addRemainingInEncounterOrder(final List<CaseField> sortedCaseFields, final Map<String, CaseField> childrenCaseIdToCaseField) {
        sortedCaseFields.addAll(childrenCaseIdToCaseField.values());
    }

    private List<String> getCompoundFieldsOrderOverride(final List<CaseEventFieldComplex> caseEventComplexFields, String listElementCode) {
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

    private Map<String, CaseField> convertComplexTypeChildrenToOrderedMap(final List<CaseField> children) {
        return children.stream().collect(Collectors.toMap(CaseField::getId,
                                                          Function.identity(),
                                                          (v1, v2) -> v1,
                                                          LinkedHashMap::new));
    }

}
