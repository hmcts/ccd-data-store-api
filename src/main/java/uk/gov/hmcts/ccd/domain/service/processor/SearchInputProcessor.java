package uk.gov.hmcts.ccd.domain.service.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.google.common.primitives.Ints;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaInput;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaType;
import uk.gov.hmcts.ccd.domain.service.aggregated.DefaultGetCriteriaOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCriteriaOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataProcessingException;

@Component
public class SearchInputProcessor {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    private final DateTimeFormatParser dateTimeFormatParser;
    private final GetCriteriaOperation getCriteriaOperation;

    @Autowired
    public SearchInputProcessor(final DateTimeFormatParser dateTimeFormatParser,
                                @Qualifier(DefaultGetCriteriaOperation.QUALIFIER) final GetCriteriaOperation getCriteriaOperation) {
        this.dateTimeFormatParser = dateTimeFormatParser;
        this.getCriteriaOperation = getCriteriaOperation;
    }

    public Map<String, String> executeQueryParams(String view, MetaData metadata, Map<String, String> queryParameters) {
        final List<? extends CriteriaInput> criteriaInputs = getCriteriaInputs(view, metadata);

        Map<String, String> newParams = new HashMap<>();
        queryParameters.entrySet().stream().forEach(entry -> {
            final Optional<? extends CriteriaInput> input = criteriaInputs.stream()
                .filter(i -> i.getField().getId().equals(entry.getKey().split("\\.")[0]))
                .findAny();

            if (input.isPresent()) {
                if (isComplexPath(entry.getKey()) && Strings.isNullOrEmpty(input.get().getDisplayContextParameter())) {
                    handleNested(entry.getKey(), entry.getValue(), input.get(), newParams);
                } else {
                    handleTopLevel(entry.getKey(), entry.getValue(), input.get(), newParams);
                }
            } else {
                newParams.put(entry.getKey(), entry.getValue());
            }
        });

        return newParams;
    }

    public MetaData executeMetadata(String view, MetaData metadata) {
        getCriteriaInputs(view, metadata).stream()
            .filter(i -> i.getField().isMetadata() && !Strings.isNullOrEmpty(i.getDisplayContextParameter()))
            .forEach(input -> {
                final String id = input.getField().getId();
                MetaData.CaseField field;
                try {
                    field = MetaData.CaseField.valueOfReference(id);
                } catch (IllegalArgumentException ex) {
                    throw new DataProcessingException().withDetails(
                        String.format("Unable to process unknown metadata field %s.", id)
                    );
                }
                if (DisplayContextParameter
                        .hasDisplayContextParameterType(input.getDisplayContextParameter(), DisplayContextParameterType.DATETIMEENTRY)
                    && MetaData.DATE_FIELDS.contains(field)
                    && metadata.getOptionalMetadata(field).isPresent()) {
                    metadata.setOptionalMetadata(field,
                        processValue(id, input.getDisplayContextParameter(),
                            metadata.getOptionalMetadata(field).get(), input.getField().getType()));
                }
        });

        return metadata;
    }

    private List<? extends CriteriaInput> getCriteriaInputs(String view, MetaData metadata) {
        return getCriteriaOperation.execute(metadata.getCaseTypeId(), null,
            view == null ? CriteriaType.SEARCH : CriteriaType.valueOf(view));
    }

    private void handleTopLevel(String fieldPath, String queryValue, CriteriaInput criteriaInput, Map<String, String> newParams) {
        if (DisplayContextParameter
            .hasDisplayContextParameterType(criteriaInput.getDisplayContextParameter(), DisplayContextParameterType.DATETIMEENTRY)) {
            newParams.put(fieldPath,
                processValue(fieldPath, criteriaInput.getDisplayContextParameter(), queryValue, criteriaInput.getField().getType()));
        } else {
            newParams.put(fieldPath, queryValue);
        }
    }

    private void handleNested(String fieldPath, String queryValue, CriteriaInput criteriaInput, Map<String, String> newParams) {
        final Optional<CommonField> field = criteriaInput.getField().getType().getNestedField(fieldPath, true);

        if (field.isPresent() && DisplayContextParameter
            .hasDisplayContextParameterType(field.get().getDisplayContextParameter(), DisplayContextParameterType.DATETIMEENTRY)) {
            newParams.put(fieldPath,
                processValue(fieldPath, field.get().getDisplayContextParameter(), queryValue, field.get().getFieldTypeDefinition()));
        } else {
            newParams.put(fieldPath, queryValue);
        }
    }

    private boolean isComplexPath(String path) {
        final String[] splitPath = path.split("\\.");
        return splitPath.length > 1 && Ints.tryParse(splitPath[1]) == null;
    }

    private String processValue(String id, String displayContextParameter, String value, FieldTypeDefinition fieldType) {
        try {
            if (fieldType.getType().equals(FieldTypeDefinition.DATE)) {
                return dateTimeFormatParser.convertDateToIso8601(format(displayContextParameter, fieldType), value);
            } else if (fieldType.getType().equals(FieldTypeDefinition.DATETIME)) {
                return dateTimeFormatParser.convertDateTimeToIso8601(format(displayContextParameter, fieldType), value);
            } else if (fieldType.getType().equals(FieldTypeDefinition.COLLECTION)) {
                return processValue(id, displayContextParameter, value, fieldType.getCollectionFieldTypeDefinition());
            } else {
                return value;
            }
        } catch (Exception e) {
            throw new DataProcessingException().withDetails(
                String.format("Unable to process input %s with value %s. Expected format: %s",
                    id,
                    value,
                    format(displayContextParameter, fieldType))
            );
        }
    }

    private String format(String displayContextParameter, FieldTypeDefinition fieldType) {
        return DisplayContextParameter
            .getDisplayContextParameterOfType(displayContextParameter, DisplayContextParameterType.DATETIMEENTRY)
            .map(DisplayContextParameter::getValue)
            .orElseGet(() -> fieldType.getType().equals(FieldTypeDefinition.DATE) ?
                DateTimeFormatParser.DATE_FORMAT :
                DateTimeFormatParser.DATE_TIME_FORMAT);
    }
}
