package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;

public class SearchResultViewColumn {

    @JsonProperty("case_field_id")
    private String caseFieldId;
    @JsonProperty("case_field_type")
    private FieldType caseFieldType;
    private String label;
    private Integer order;

    public SearchResultViewColumn() {
        // Default constructor for JSON mapper
    }

    public SearchResultViewColumn(final String caseFieldId,
                                  final FieldType caseFieldType,
                                  final String label,
                                  final Integer order) {
        this.caseFieldId = caseFieldId;
        this.caseFieldType = caseFieldType;
        this.label = label;
        this.order = order;
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
}
