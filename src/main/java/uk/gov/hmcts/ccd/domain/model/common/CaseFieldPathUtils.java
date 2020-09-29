package uk.gov.hmcts.ccd.domain.model.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

public class CaseFieldPathUtils {

    private static final String SEPARATOR = ".";
    private static final String SEPARATOR_REGEX = "\\.";

    private CaseFieldPathUtils() {
    }

    /**
     * Find a case field definition within a case type by its path.
     * Supports top level and nested field paths.
     *
     * @param caseTypeDefinition The case type within which to search
     * @param path The full stop (".") separated path
     * @return The case field; empty if no such field exists
     */
    public static <T extends CommonField> Optional<T> getFieldDefinitionByPath(CaseTypeDefinition caseTypeDefinition,
                                                                               String path) {
        if (StringUtils.isBlank(path)) {
            return Optional.empty();
        }
        List<String> pathElements = getPathElements(path);

        Optional<CaseFieldDefinition> topLevelCaseField = caseTypeDefinition.getCaseField(pathElements.get(0));

        return topLevelCaseField.flatMap(field ->
            getFieldDefinitionByPath((T) field, getPathElementsTailAsString(pathElements)));
    }

    /**
     * Find a nested case field definition with a case field itself by its path.
     *
     * @param caseFieldDefinition The case field definition within which to search
     * @param path The full stop (".") separated path
     * @return The nested field. The case field passed in is returned if the path is empty;
     *         empty if no such field exists
     */
    public static <T extends CommonField> Optional<T> getFieldDefinitionByPath(T caseFieldDefinition, String path) {
        if (StringUtils.isBlank(path)) {
            return Optional.of(caseFieldDefinition);
        }

        return getFieldDefinitionByPath(caseFieldDefinition.getFieldTypeDefinition(), path, false);
    }

    /**
     * Find a nested case field definition within a field type definition by its path.
     * For use with Complex fields.
     *
     * @param fieldTypeDefinition The field type definition to search within
     * @param path The full stop (".") separated path
     * @param pathIncludesParent Whether the path includes a parent field which should be discarded
     *                           e.g. if the path is ParentField.ChildField then the path searched
     *                           would simply be for ChildField
     * @return The nested field; empty if no such field exists
     */
    public static <T extends CommonField> Optional<T> getFieldDefinitionByPath(FieldTypeDefinition fieldTypeDefinition,
                                                                 String path,
                                                                 boolean pathIncludesParent) {
        if (StringUtils.isBlank(path)
            || fieldTypeDefinition.getChildren().isEmpty()
            || (pathIncludesParent && splitPath(path).length == 1)) {
            return Optional.empty();
        }
        List<String> pathElements = getPathElements(path);

        return reduce((List<T>)fieldTypeDefinition.getChildren(), pathIncludesParent
            ? getPathElementsTail(pathElements) : pathElements);
    }

    /**
     * Find a nested case field JsonNode by its case field path.
     *
     * @param node The node in which to search
     * @param path The full stop (".") separated path e.g. NestedLevel1.NestedLevel2
     * @return The node for the nested path; null if no node exists
     */
    public static JsonNode getNestedCaseFieldByPath(JsonNode node, String path) {
        List<String> pathElements = getPathElements(path);

        return reduce(node, pathElements);
    }

    public static List<String> getPathElements(String path) {
        return Arrays.stream(splitPath(path)).collect(toList());
    }

    private static String[] splitPath(String path) {
        return path.trim().split(SEPARATOR_REGEX);
    }

    private static <T extends CommonField> Optional<T> reduce(List<T> caseFields, List<String> pathElements) {
        return caseFields.stream()
            .filter(e -> e.getId().equals(pathElements.get(0)))
            .findFirst()
            .flatMap(caseField -> {
                if (pathElements.size() == 1) {
                    return Optional.of(caseField);
                } else {
                    List<T> newCaseFields = (List<T>) caseField.getFieldTypeDefinition().getChildren();
                    return reduce(newCaseFields, getPathElementsTail(pathElements));
                }
            });
    }

    private static JsonNode reduce(JsonNode caseFields, List<String> pathElements) {
        String firstPathElement = pathElements.get(0);

        JsonNode caseField = Optional.ofNullable(caseFields.get(firstPathElement)).orElse(null);

        if (caseField == null || pathElements.size() == 1) {
            return caseField;
        } else {
            return reduce(caseField, getPathElementsTail(pathElements));
        }
    }

    public static List<String> getPathElementsTail(List<String> pathElements) {
        return pathElements.subList(1, pathElements.size());
    }

    public static String getPathElementsTailAsString(List<String> pathElements) {
        return StringUtils.join(getPathElementsTail(pathElements), SEPARATOR);
    }
}
