package uk.gov.hmcts.ccd.domain.model.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

import java.util.*;

import static java.util.stream.Collectors.toList;

public class CaseFieldPathUtils {

    private static final String SEPARATOR = ".";
    private static final String SEPARATOR_REGEX = "\\.";

    private CaseFieldPathUtils() {
    }

    public static Optional<CommonField> getFieldDefinitionByPath(CaseTypeDefinition caseTypeDefinition, String path) {
        if (StringUtils.isBlank(path)) {
            return Optional.empty();
        }
        List<String> pathElements = getPathElements(path);

        Optional<CaseFieldDefinition> topLevelCaseField = caseTypeDefinition.getCaseField(pathElements.get(0));

        return topLevelCaseField.flatMap(f -> getFieldDefinitionByPath(f, getPathElementsTailAsString(pathElements)));
    }

    public static Optional<CommonField> getFieldDefinitionByPath(CommonField commonField, String path) {
        if (StringUtils.isBlank(path)) {
            return Optional.of(commonField);
        }

        return getFieldDefinitionByPath(commonField.getFieldTypeDefinition(), path, false);
    }

    public static Optional<CommonField> getFieldDefinitionByPath(FieldTypeDefinition fieldTypeDefinition,
                                                                 String path,
                                                                 boolean pathIncludesParent) {
        if (StringUtils.isBlank(path) || fieldTypeDefinition.getChildren().isEmpty() || (pathIncludesParent && splitPath(path).length == 1)) {
            return Optional.empty();
        }
        List<String> pathElements = getPathElements(path);

        return reduce(fieldTypeDefinition.getChildren(), pathIncludesParent ? getPathElementsTail(pathElements) : pathElements);
    }

    public static JsonNode getCaseFieldNodeByPath(JsonNode node, String path) {
        List<String> pathElements = getPathElements(path);

        return reduce(node, pathElements);
    }

    public static List<String> getPathElements(String path) {
        return Arrays.stream(splitPath(path)).collect(toList());
    }

    private static String[] splitPath(String path) {
        return path.trim().split(SEPARATOR_REGEX);
    }

    private static Optional<CommonField> reduce(List<CaseFieldDefinition> caseFields, List<String> pathElements) {
        return caseFields.stream()
            .filter(e -> e.getId().equals(pathElements.get(0)))
            .findFirst()
            .flatMap(caseField -> {
                if (pathElements.size() == 1) {
                    return Optional.of(caseField);
                } else {
                    List<CaseFieldDefinition> newCaseFields = caseField.getFieldTypeDefinition().getChildren();
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

    private static List<String> getPathElementsTail(List<String> pathElements) {
        return pathElements.subList(1, pathElements.size());
    }

    private static String getPathElementsTailAsString(List<String> pathElements) {
        return StringUtils.join(getPathElementsTail(pathElements), SEPARATOR);
    }
}
