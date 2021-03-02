package uk.gov.hmcts.ccd.domain.service.processor.date;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.Ints;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.common.CommonDCPModel;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaInput;
import uk.gov.hmcts.ccd.domain.model.search.CriteriaType;
import uk.gov.hmcts.ccd.domain.service.aggregated.DefaultGetCriteriaOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCriteriaOperation;
import uk.gov.hmcts.ccd.domain.service.processor.date.DateTimeFormatParser;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataProcessingException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static uk.gov.hmcts.ccd.domain.model.common.DisplayContextParameterType.DATETIMEENTRY;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DATE;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DATETIME;
import static uk.gov.hmcts.ccd.domain.service.processor.date.DateTimeFormatParser.DATE_FORMAT;
import static uk.gov.hmcts.ccd.domain.service.processor.date.DateTimeFormatParser.DATE_TIME_FORMAT;

@Component
public class DateTimeSearchInputProcessor {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    private final DateTimeFormatParser dateTimeFormatParser;
    private final GetCriteriaOperation getCriteriaOperation;

    @Autowired
    public DateTimeSearchInputProcessor(final DateTimeFormatParser dateTimeFormatParser,
                                        @Qualifier(DefaultGetCriteriaOperation.QUALIFIER)
                                        final GetCriteriaOperation getCriteriaOperation) {
        this.dateTimeFormatParser = dateTimeFormatParser;
        this.getCriteriaOperation = getCriteriaOperation;
    }

    public Map<String, String> executeQueryParams(String view, MetaData metadata, Map<String, String> queryParameters) {
        final List<? extends CriteriaInput> criteriaInputs = getCriteriaInputs(view, metadata);

        Map<String, String> newParams = new HashMap<>();
        queryParameters.forEach((fieldId, value) -> {
            final Optional<? extends CriteriaInput> input =
                findCriteriaInputField(criteriaInputs, fieldId.split("\\.")[0]);

            if (input.isPresent()) {
                if (isComplexPath(fieldId) && input.get().getDisplayContextParameters().isEmpty()) {
                    handleNested(fieldId, value, input.get(), newParams);
                } else {
                    handleTopLevel(fieldId, value, input.get(), newParams);
                }
            } else {
                newParams.put(fieldId, value);
            }
        });

        return newParams;
    }

    public MetaData executeMetadata(String view, MetaData metadata) {
        getCriteriaInputs(view, metadata).stream()
            .filter(i -> i.getField().isMetadata() && !i.getDisplayContextParameters().isEmpty())
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
                if (input.hasDisplayContextParameter(DATETIMEENTRY)
                    && MetaData.DATE_FIELDS.contains(field)
                    && metadata.getOptionalMetadata(field).isPresent()) {
                    metadata.setOptionalMetadata(field,
                        processValue(id, input,
                            metadata.getOptionalMetadata(field).get(), input.getField().getType()));
                }
            });

        return metadata;
    }

    private Optional<? extends CriteriaInput> findCriteriaInputField(List<? extends CriteriaInput>
                                                                         criteriaInputs, String fieldId) {
        return criteriaInputs.stream()
            .filter(i -> i.getField().getId().equals(fieldId))
            .findAny();
    }

    private List<? extends CriteriaInput> getCriteriaInputs(String view, MetaData metadata) {
        return getCriteriaOperation.execute(metadata.getCaseTypeId(), null,
            view == null ? CriteriaType.SEARCH : CriteriaType.valueOf(view));
    }

    private void handleTopLevel(String fieldPath, String queryValue, CriteriaInput criteriaInput,
                                Map<String, String> newParams) {
        if (criteriaInput.hasDisplayContextParameter(DATETIMEENTRY)) {
            newParams.put(fieldPath,
                processValue(fieldPath, criteriaInput, queryValue, criteriaInput.getField().getType()));
        } else {
            newParams.put(fieldPath, queryValue);
        }
    }

    private void handleNested(String fieldPath, String queryValue, CriteriaInput criteriaInput,
                              Map<String, String> newParams) {
        final Optional<CommonField> field =
            criteriaInput.getField().getType().getNestedField(fieldPath, true);

        if (field.isPresent() && field.get().hasDisplayContextParameter(DATETIMEENTRY)) {
            newParams.put(fieldPath,
                processValue(fieldPath, field.get(), queryValue, field.get().getFieldTypeDefinition()));
        } else {
            newParams.put(fieldPath, queryValue);
        }
    }

    private boolean isComplexPath(String path) {
        final String[] splitPath = path.split("\\.");
        return splitPath.length > 1 && Ints.tryParse(splitPath[1]) == null;
    }

    private String processValue(String id, CommonDCPModel dcpObject, String value, FieldTypeDefinition fieldType) {
        try {
            if (fieldType.getType().equals(DATE)) {
                return dateTimeFormatParser.convertDateToIso8601(format(dcpObject, fieldType), value);
            } else if (fieldType.getType().equals(DATETIME)) {
                return dateTimeFormatParser.convertDateTimeToIso8601(format(dcpObject, fieldType), value);
            } else if (fieldType.isCollectionFieldType()) {
                return processValue(id, dcpObject, value, fieldType.getCollectionFieldTypeDefinition());
            } else {
                return value;
            }
        } catch (Exception e) {
            throw new DataProcessingException().withDetails(
                String.format("Unable to process search input %s with value %s. Expected format: %s",
                    id,
                    value,
                    format(dcpObject, fieldType))
            );
        }
    }

    private String format(CommonDCPModel dcpObject, FieldTypeDefinition fieldType) {
        return dcpObject.getDisplayContextParameterValue(DATETIMEENTRY)
            .orElseGet(() -> fieldType.getType().equals(DATE) ? DATE_FORMAT : DATE_TIME_FORMAT);
    }
}
