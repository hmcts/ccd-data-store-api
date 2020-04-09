package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.service.processor.DisplayContextParameter;
import uk.gov.hmcts.ccd.domain.service.processor.DisplayContextParameterType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class SearchResultViewColumn {

    @JsonProperty("case_field_id")
    private String caseFieldId;
    @JsonProperty("case_field_type")
    private FieldType caseFieldType;
    private String label;
    private Integer order;
    private boolean metadata;
    @JsonProperty("display_context_parameter")
    private String displayContextParameter;

    public SearchResultViewColumn() {
        // Default constructor for JSON mapper
    }

    public SearchResultViewColumn(final String caseFieldId,
                                  final FieldType caseFieldType,
                                  final String label,
                                  final Integer order,
                                  final boolean metadata,
                                  final String displayContextParameter) {
        this.caseFieldId = caseFieldId;
        this.caseFieldType = caseFieldType;
        this.label = label;
        this.order = order;
        this.metadata = metadata;
        this.displayContextParameter = displayContextParameter;
    }

    public String getCaseFieldId() {
        return caseFieldId;
    }

    public FieldType getCaseFieldType() {
        return caseFieldType;
    }

    public String getLabel() {
        return label;
    }

    public Integer getOrder() {
        return order;
    }

    public boolean isMetadata() {
        return metadata;
    }

    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public Optional<DisplayContextParameter> getDisplayContextParameterOfType(DisplayContextParameterType displayContextParameterType) {
        return DisplayContextParameter.getDisplayContextParameterOfType(getDisplayContextParameter(), displayContextParameterType);
    }

    public Optional<CommonField> getNestedField(String path) {
        final List<String> pathElements = Arrays.asList(path.trim().split("\\."));
        if (pathElements.size() == 1) {
            return Optional.empty();
        }

        return getCaseFieldType().getNestedField(pathElements.stream().skip(1).collect(Collectors.joining(".")));
    }
}
