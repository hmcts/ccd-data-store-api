package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.processor.DisplayContextParameter;
import uk.gov.hmcts.ccd.domain.service.processor.DisplayContextParameterType;

public class SearchResultViewColumn implements CommonViewHeader {

    @JsonProperty("case_field_id")
    private String caseFieldId;
    @JsonProperty("case_field_type")
    private FieldTypeDefinition caseFieldTypeDefinition;
    private String label;
    private Integer order;
    private boolean metadata;
    @JsonProperty("display_context_parameter")
    private String displayContextParameter;

    public SearchResultViewColumn() {
        // Default constructor for JSON mapper
    }

    public SearchResultViewColumn(final String caseFieldId,
                                  final FieldTypeDefinition caseFieldTypeDefinition,
                                  final String label,
                                  final Integer order,
                                  final boolean metadata,
                                  final String displayContextParameter) {
        this.caseFieldId = caseFieldId;
        this.caseFieldTypeDefinition = caseFieldTypeDefinition;
        this.label = label;
        this.order = order;
        this.metadata = metadata;
        this.displayContextParameter = displayContextParameter;
    }

    @Override
    public String getCaseFieldId() {
        return caseFieldId;
    }

    @Override
    public FieldTypeDefinition getCaseFieldTypeDefinition() {
        return caseFieldTypeDefinition;
    }

    public String getLabel() {
        return label;
    }

    public Integer getOrder() {
        return order;
    }

    @Override
    public boolean isMetadata() {
        return metadata;
    }

    @Override
    public String getDisplayContextParameter() {
        return displayContextParameter;
    }

    public Optional<DisplayContextParameter> getDisplayContextParameterOfType(DisplayContextParameterType displayContextParameterType) {
        return DisplayContextParameter.getDisplayContextParameterOfType(getDisplayContextParameter(), displayContextParameterType);
    }
}
