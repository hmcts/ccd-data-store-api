package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldMetadata;
import uk.gov.hmcts.ccd.domain.types.BaseType;

import java.util.List;
import java.util.Map;
import javax.inject.Named;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;

@Slf4j
@Named("complexCaseTypeMetadataExtractor")
public class ComplexCaseTypeMetadataExtractor implements CaseFieldMetadataExtractor {

    @Override
    public Boolean matches(@NonNull final BaseType type) {
        return type == BaseType.get(COMPLEX);
    }

    @Override
    public Either<RecursionParams, List<CaseFieldMetadata>> extractCaseFieldData(
        @NonNull final Map.Entry<String, JsonNode> nodeEntry,
        @NonNull final CaseFieldDefinition caseFieldDefinition,
        @NonNull final String fieldIdPrefix,
        @NonNull final String fieldType,
        @NonNull final List<CaseFieldMetadata> paths
    ) {

        final RecursionParams recursionParams = new RecursionParams(
            JacksonUtils.convertValue(nodeEntry.getValue()),
            caseFieldDefinition.getFieldTypeDefinition().getComplexFields(),
            fieldIdPrefix + nodeEntry.getKey() + FIELD_SEPARATOR,
            paths,
            fieldType
        );

        return Either.left(recursionParams);
    }

}
