package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.domain.service.processor.DisplayContextParameter;
import uk.gov.hmcts.ccd.domain.service.processor.DisplayContextParameterType;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

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
        if (StringUtils.isBlank(path) || path.trim().split("\\.").length == 1 || this.getCaseFieldType().getChildren().isEmpty()) {
            return Optional.empty();
        }
        List<String> pathElements = Arrays.stream(path.trim().split("\\.")).collect(toList());
        pathElements.remove(0);

        return reduce(this.getCaseFieldType().getChildren(), pathElements);
    }

    private Optional<CommonField> reduce(List<CaseField> caseFields, List<String> pathElements) {
        String firstPathElement = pathElements.get(0);

        Optional<CaseField> optionalCaseField = caseFields.stream().filter(e -> e.getId().equals(firstPathElement)).findFirst();
        if (optionalCaseField.isPresent()) {
            CommonField caseField = optionalCaseField.get();

            if (pathElements.size() == 1) {
                return Optional.of(caseField);
            } else {
                List<CaseField> newCaseFields = caseField.getFieldType().getChildren();
                List<String> tail = pathElements.subList(1, pathElements.size());

                return reduce(newCaseFields, tail);
            }
        } else {
            return Optional.empty();
        }
    }
}
