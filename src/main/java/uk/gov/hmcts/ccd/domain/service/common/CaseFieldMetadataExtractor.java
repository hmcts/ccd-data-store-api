package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import lombok.Value;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldMetadata;
import uk.gov.hmcts.ccd.domain.types.BaseType;

import java.util.List;
import java.util.Map;

public interface CaseFieldMetadataExtractor {

    String FIELD_SEPARATOR = ".";

    Boolean matches(final BaseType type);

    Either<RecursionParams, List<CaseFieldMetadata>> extractCaseFieldData(final Map.Entry<String, JsonNode> nodeEntry,
                                                                          final CaseFieldDefinition caseFieldDefinition,
                                                                          final String fieldIdPrefix,
                                                                          final String fieldType,
                                                                          final List<CaseFieldMetadata> paths);

    @Value
    class RecursionParams {
        Map<String, JsonNode> data;
        List<CaseFieldDefinition> caseFieldDefinitions;
        String fieldIdPrefix;
        List<CaseFieldMetadata> paths;
        String fieldType;
    }

}
