package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;

public class SearchResultViewColumn {

    @JsonProperty("case_field_id")
    private String caseFieldId;
    @JsonProperty("case_field_type")
    private FieldTypeDefinition caseFieldTypeDefinition;
    private String label;
    private Integer order;
    private boolean metadata;

    public SearchResultViewColumn() {
        // Default constructor for JSON mapper
    }

    public SearchResultViewColumn(final String caseFieldId,
                                  final FieldTypeDefinition caseFieldTypeDefinition,
                                  final String label,
                                  final Integer order,
                                  final boolean metadata) {
        this.caseFieldId = caseFieldId;
        this.caseFieldTypeDefinition = caseFieldTypeDefinition;
        this.label = label;
        this.order = order;
        this.metadata = metadata;
    }

    public String getCaseFieldId() {
        return caseFieldId;
    }

    public FieldTypeDefinition getCaseFieldTypeDefinition() {
        return caseFieldTypeDefinition;
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
}
