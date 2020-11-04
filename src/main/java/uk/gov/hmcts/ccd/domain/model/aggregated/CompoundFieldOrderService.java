package uk.gov.hmcts.ccd.domain.model.aggregated;

import com.google.common.collect.Lists;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.commons.collections.CollectionUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventFieldComplexDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;

import static java.util.Comparator.comparingInt;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;

/**
 * This service sorts nested fields of a compound field (complex or collection) according to
 * caseEventComplexFields collection. If partial set of nested fields is provided via caseEventComplexFields then
 * the remaining fields are added at the end in encounter order.
 * It will sort on its elements order values. No sorting is performed for empty collection.
 */
@Named
@Singleton
public class CompoundFieldOrderService {

    public static final String ROOT = "";

    public void sortNestedFieldsFromCaseEventComplexFields(final CaseFieldDefinition caseFieldDefinition,
                                                           final List<CaseEventFieldComplexDefinition>
                                                               caseEventComplexFields,
                                                           final String listElementCode) {
        if (CollectionUtils.isNotEmpty(caseEventComplexFields) && caseFieldDefinition.isCompoundFieldType()) {
            List<CaseFieldDefinition> children = caseFieldDefinition.getFieldTypeDefinition().getChildren();
            children.forEach(childField -> {
                String newListElementCode =
                    isBlank(listElementCode) ? childField.getId() : listElementCode + "." + childField.getId();
                sortNestedFieldsFromCaseEventComplexFields(childField,
                            getNestedComplexFields(caseEventComplexFields, newListElementCode), newListElementCode);
            });
            List<CaseFieldDefinition> sortedFields =
                getSortedCompoundTypeFields(caseEventComplexFields, children, listElementCode);
            caseFieldDefinition.getFieldTypeDefinition().setChildren(sortedFields);
        }
    }

    private List<CaseEventFieldComplexDefinition> getNestedComplexFields(final List<CaseEventFieldComplexDefinition>
                                                                             caseEventComplexFields,
                                                                         final String listElementCode) {
        return caseEventComplexFields
            .stream()
            .filter(caseEventFieldComplexDefinition ->
                caseEventFieldComplexDefinition.getReference().startsWith(listElementCode))
            .collect(Collectors.toList());
    }

    private List<CaseFieldDefinition> getSortedCompoundTypeFields(final List<CaseEventFieldComplexDefinition>
                                                                      caseEventComplexFields,
                                                                  final List<CaseFieldDefinition> children,
                                                                  String listElementCode) {
        final List<String> sortedFieldsFromEventFieldOverride =
            getSortedFieldsFromEventFieldOverride(children, caseEventComplexFields, listElementCode);
        if (sortedFieldsFromEventFieldOverride.isEmpty()) {
            return children;
        } else {
            return getSortedFieldsFromEventFieldOverride(children, listElementCode, sortedFieldsFromEventFieldOverride);
        }
    }

    private String getReference(final String listElementCode, final String reference) {
        return isBlank(listElementCode) ? reference : substringAfterLast(reference, ".");
    }

    private void addRemainingInEncounterOrder(final List<CaseFieldDefinition> sortedCaseFieldDefinitions,
                                              final Map<String, CaseFieldDefinition> childrenCaseIdToCaseField) {
        sortedCaseFieldDefinitions.addAll(childrenCaseIdToCaseField.values());
    }

    private List<String> getSortedFieldsFromEventFieldOverride(final List<CaseFieldDefinition> children,
                                                               final List<CaseEventFieldComplexDefinition>
                                                                   caseEventComplexFields,
                                                               String listElementCode) {
        return caseEventComplexFields.stream()
            .filter(field -> hasOrderAndIsLeaf(children, listElementCode, field))
            .sorted(comparingInt(CaseEventFieldComplexDefinition::getOrder))
            .map(CaseEventFieldComplexDefinition::getReference)
            .collect(Collectors.toList());
    }

    private List<CaseFieldDefinition> getSortedFieldsFromEventFieldOverride(final List<CaseFieldDefinition> children,
                                                                            final String listElementCode,
                                                                            final List<String>
                                                                                orderedEventComplexFieldReferences) {
        final List<CaseFieldDefinition> sortedCaseFieldDefinitions = Lists.newArrayList();
        final Map<String, CaseFieldDefinition> childrenCaseIdToCaseField =
            convertComplexTypeChildrenToOrderedMap(children);
        orderedEventComplexFieldReferences.stream()
            .map(reference -> childrenCaseIdToCaseField.remove(getReference(listElementCode, reference)))
            .filter(Objects::nonNull)
            .forEach(sortedCaseFieldDefinitions::add);
        addRemainingInEncounterOrder(sortedCaseFieldDefinitions, childrenCaseIdToCaseField);
        return sortedCaseFieldDefinitions;
    }

    private boolean hasOrderAndIsLeaf(final List<CaseFieldDefinition> children,
                                      final String listElementCode,
                                      final CaseEventFieldComplexDefinition field) {
        return field.getOrder() != null && isFieldReferenceALeaf(children, listElementCode, field);
    }

    private boolean isFieldReferenceALeaf(final List<CaseFieldDefinition> children,
                                          final String listElementCode,
                                          final CaseEventFieldComplexDefinition field) {
        return isBlank(listElementCode) ? isTopLevelLeaf(children, field) : isNestedLeaf(listElementCode, field);
    }

    private boolean isNestedLeaf(final String listElementCode, final CaseEventFieldComplexDefinition field) {
        String substringAfterLast = substringAfterLast(field.getReference(), listElementCode + ".");
        return !isBlank(substringAfterLast) && !substringAfterLast.contains(".");
    }

    private boolean isTopLevelLeaf(final List<CaseFieldDefinition> children,
                                   final CaseEventFieldComplexDefinition field) {
        return children.stream().anyMatch(caseField -> field.getReference().equals(caseField.getId()));
    }

    private Map<String, CaseFieldDefinition> convertComplexTypeChildrenToOrderedMap(final List<CaseFieldDefinition>
                                                                                        children) {
        return children.stream().collect(Collectors.toMap(CaseFieldDefinition::getId,
                                                          Function.identity(),
                                                          ((v1, v2) -> v1),
                                                          LinkedHashMap::new));
    }

}
