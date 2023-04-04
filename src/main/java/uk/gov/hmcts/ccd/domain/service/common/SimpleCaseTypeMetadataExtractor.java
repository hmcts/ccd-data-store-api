package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import lombok.NonNull;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.types.BaseType;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;

import static java.util.Collections.singletonList;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COMPLEX;

@Named("simpleCaseTypeMetadataExtractor")
public class SimpleCaseTypeMetadataExtractor implements CaseFieldMetadataExtractor {
    @Override
    public Boolean matches(@NonNull final BaseType type) {
        return type != BaseType.get(COMPLEX) && type != BaseType.get(COLLECTION);
    }

    @Override
    public Either<RecursionParams, List<CaseFieldMetadata>> extractCaseFieldData(
        @NonNull final Map.Entry<String, JsonNode> nodeEntry,
        @NonNull final CaseFieldDefinition caseFieldDefinition,
        @NonNull final String fieldIdPrefix,
        @NonNull final String fieldType,
        @NonNull final List<CaseFieldMetadata> paths
    ) {
        final Optional<FieldTypeDefinition> optionalFieldTypeDefinition =
                Optional.ofNullable(caseFieldDefinition.getFieldTypeDefinition());
        final Optional<CaseFieldMetadata> optionalCaseFieldMetadata;

        if (FieldTypeDefinition.DOCUMENT.equals(fieldType)) {
            optionalCaseFieldMetadata = optionalFieldTypeDefinition.map(
                    fieldTypeDefinition -> (fieldType.equals(fieldTypeDefinition.getType())
                                            || fieldType.equals(fieldTypeDefinition.getId()))
                    ? new CaseFieldMetadata(fieldIdPrefix + nodeEntry.getKey(), caseFieldDefinition.getCategoryId())
                    : null);
        } else {
            optionalCaseFieldMetadata = optionalFieldTypeDefinition.map(
                    fieldTypeDefinition -> fieldType.equals(fieldTypeDefinition.getId())
                    ? new CaseFieldMetadata(fieldIdPrefix + nodeEntry.getKey(), caseFieldDefinition.getCategoryId())
                    : null);
        }

        final List<CaseFieldMetadata> results = optionalCaseFieldMetadata
            .map(metadata -> Stream.of(paths, singletonList(metadata))
                .flatMap(List::stream)
                .collect(Collectors.toUnmodifiableList()))
            .orElse(paths);

        return Either.right(results);
    }
}
