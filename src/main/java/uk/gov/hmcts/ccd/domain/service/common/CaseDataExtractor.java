package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.types.BaseType;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.service.common.PathFinder.FIELD_SEPARATOR;

@Named
@Slf4j
public class CaseDataExtractor {
    private static final String EMPTY_STRING = "";
    private static final String VALUE_FIELD = "value";
    private static final String DOCUMENT_TYPE_ID = "Document";

    private final PathFinder simpleTypePathFinder;
    private final PathFinder complexTypePathFinder;

    @Inject
    public CaseDataExtractor(final PathFinder simpleTypePathFinder,
                             final PathFinder complexTypePathFinder) {
        this.simpleTypePathFinder = simpleTypePathFinder;
        this.complexTypePathFinder = complexTypePathFinder;
    }

    public List<CaseFieldMetadata> extractFieldTypePaths(final Map<String, JsonNode> data,
                                                         final List<CaseFieldDefinition> caseFieldDefinitions,
                                                         final String fieldType) {
        return extractFieldTypePaths(
            data,
            caseFieldDefinitions,
            EMPTY_STRING,
            Collections.emptyList(),
            fieldType
        );
    }

    private List<CaseFieldMetadata> extractFieldTypePaths(final Map<String, JsonNode> data,
                                                          final List<CaseFieldDefinition> caseFieldDefinitions,
                                                          final String fieldIdPrefix,
                                                          final List<CaseFieldMetadata> paths,
                                                          final String fieldType) {
        return (data == null)
            ? Collections.emptyList()
            : data.entrySet().stream()
            .map(entry -> extractMetadata(entry, caseFieldDefinitions, fieldIdPrefix, paths, fieldType))
            .flatMap(List::stream)
            .collect(Collectors.toUnmodifiableList());
    }

    private List<CaseFieldMetadata> extractMetadata(final Map.Entry<String, JsonNode> caseDataPair,
                                                    final List<CaseFieldDefinition> caseFieldDefinitions,
                                                    final String fieldIdPrefix,
                                                    final List<CaseFieldMetadata> paths,
                                                    final String fieldType) {
        return caseFieldDefinitions.stream()
            .filter(caseField -> caseField.getId().equalsIgnoreCase(caseDataPair.getKey()))
            .findAny()
            .map(caseField -> extractField(
                caseDataPair,
                caseField,
                fieldIdPrefix,
                paths,
                fieldType
            ))
            .orElseGet(Collections::emptyList);
    }

    private List<CaseFieldMetadata> extractField(final Map.Entry<String, JsonNode> nodeEntry,
                                                 final CaseFieldDefinition caseFieldDefinition,
                                                 final String fieldIdPrefix,
                                                 final List<CaseFieldMetadata> paths,
                                                 final String fieldType) {

        final String caseFieldType = caseFieldDefinition.getFieldTypeDefinition().getType();

        // TODO: is this defensive block necessary?
        //  Is it even possible to successfully import a CaseFieldDefinition with an invalid caseFieldType?
        if (!BaseType.contains(caseFieldType)) {
            log.debug("Ignoring Unknown Type: " + caseFieldType);
            return Collections.emptyList();
        }

        final BaseType baseFieldType = BaseType.get(caseFieldType);

        if (BaseType.get(COLLECTION) == baseFieldType) {
            return extractCollectionField(nodeEntry, caseFieldDefinition, fieldIdPrefix, fieldType, paths);
        }

        final Either<PathFinder.RecursionParams, List<CaseFieldMetadata>> either =
            Stream.of(simpleTypePathFinder, complexTypePathFinder)
                .filter(extractor -> extractor.matches(baseFieldType))
                .findFirst()
                .orElseThrow()
                .extractCaseFieldData(nodeEntry, caseFieldDefinition, fieldIdPrefix, fieldType, paths);

        return either.fold(
            this::leftMapper,
            right -> right
        );
    }

    private List<CaseFieldMetadata> leftMapper(PathFinder.RecursionParams left) {
        return extractFieldTypePaths(
            left.getData(),
            left.getCaseFieldDefinitions(),
            left.getFieldIdPrefix(),
            left.getPaths(),
            left.getFieldType()
        );
    }

    private List<CaseFieldMetadata> extractCollectionField(final Map.Entry<String, JsonNode> nodeEntry,
                                                           final CaseFieldDefinition caseFieldDefinition,
                                                           final String fieldIdPrefix,
                                                           final String fieldType,
                                                           final List<CaseFieldMetadata> paths) {
        final List<CaseFieldMetadata> extractionResults = simpleTypePathFinder.extractCaseFieldData(
            nodeEntry,
            caseFieldDefinition,
            fieldIdPrefix,
            fieldType,
            paths
        ).get();

        List<CaseFieldMetadata> tempList = new ArrayList<>(extractionResults);

        final Iterator<JsonNode> collectionIterator = nodeEntry.getValue().iterator();

        int index = 0;
        while (collectionIterator.hasNext()) {
            final JsonNode itemValue = collectionIterator.next();
            tempList.addAll(
                extractCollectionItem(
                    caseFieldDefinition.getFieldTypeDefinition().getCollectionFieldTypeDefinition(),
                    new AbstractMap.SimpleEntry<>(Integer.toString(index), itemValue),
                    fieldIdPrefix + nodeEntry.getKey() + FIELD_SEPARATOR,
                    paths,
                    fieldType,
                    caseFieldDefinition.getCategoryId()
                )
            );
            index++;
        }

        return Collections.unmodifiableList(tempList);
    }

    private List<CaseFieldMetadata> extractCollectionItem(final FieldTypeDefinition fieldTypeDefinition,
                                                          final Map.Entry<String, JsonNode> nodeEntry,
                                                          final String fieldIdPrefix,
                                                          final List<CaseFieldMetadata> paths,
                                                          final String fieldType,
                                                          final String categoryId) {
        final String index = nodeEntry.getKey();
        final String itemFieldId = fieldIdPrefix + index;

        final JsonNode itemValue = nodeEntry.getValue().get(VALUE_FIELD);

        if (null == itemValue) {
            return Collections.emptyList();
        }

        if (shouldTreatAsValueNode(fieldTypeDefinition, itemValue)) {
            if (!BaseType.contains(fieldTypeDefinition.getType())) {
                log.debug("Ignoring Unknown Type:" + fieldTypeDefinition.getType() + " " + itemFieldId);
                return Collections.emptyList();
            }
            final CaseFieldDefinition caseFieldDefinition = new CaseFieldDefinition();
            caseFieldDefinition.setFieldTypeDefinition(fieldTypeDefinition);
            caseFieldDefinition.setId(index);
            caseFieldDefinition.setCategoryId(categoryId);

            return simpleTypePathFinder.extractCaseFieldData(
                nodeEntry,
                caseFieldDefinition,
                fieldIdPrefix,
                fieldType,
                paths
            ).get();
        } else if (itemValue.isObject()) {
            return extractFieldTypePaths(
                JacksonUtils.convertValue(itemValue),
                fieldTypeDefinition.getComplexFields(),
                itemFieldId + FIELD_SEPARATOR,
                paths,
                fieldType
            );
        }

        return Collections.emptyList();
    }

    private boolean shouldTreatAsValueNode(FieldTypeDefinition fieldTypeDefinition, JsonNode itemValue) {
        return itemValue.isValueNode()
            || fieldTypeDefinition.getType().equalsIgnoreCase(DOCUMENT_TYPE_ID)
            || isDynamicListNode(fieldTypeDefinition);
    }

    private boolean isDynamicListNode(FieldTypeDefinition fieldTypeDefinition) {
        return fieldTypeDefinition.getType().equalsIgnoreCase("DynamicList")
            || fieldTypeDefinition.getType().equalsIgnoreCase("DynamicMultiSelectList")
            || fieldTypeDefinition.getType().equalsIgnoreCase("DynamicRadioList");
    }

}
