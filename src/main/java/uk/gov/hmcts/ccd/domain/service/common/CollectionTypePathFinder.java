package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import io.vavr.control.Either;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldMetadata;
import uk.gov.hmcts.ccd.domain.types.BaseType;

import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;

@Slf4j
@Named("collectionTypePathFinder")
public class CollectionTypePathFinder implements PathFinder {
    private static final String VALUE_FIELD = "value";

    private final PathFinder simpleTypePathFinder;

    @Inject
    public CollectionTypePathFinder(final PathFinder simpleTypePathFinder) {
        this.simpleTypePathFinder = simpleTypePathFinder;
    }

    @Override
    public Boolean matches(@NonNull final BaseType type) {
        return false;
    }

    @Override
    public Either<RecursionParams, List<CaseFieldMetadata>> extractCaseFieldData(
        final Map.Entry<String, JsonNode> nodeEntry,
        final CaseFieldDefinition caseFieldDefinition,
        final String fieldIdPrefix,
        final String fieldType,
        final List<CaseFieldMetadata> paths
    ) {

        return null;
    }

}
