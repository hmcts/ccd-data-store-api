package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class SearchResultViewItem implements CommonViewItem {
    @JsonProperty("case_id")
    private String caseId;
    @JsonProperty("case_fields")
    private Map<String, Object> fields;
    @JsonProperty("case_fields_formatted")
    private Map<String, Object> fieldsFormatted;

    public SearchResultViewItem() {
        // Default constructor for JSON mapper
    }

    public SearchResultViewItem(final String caseId,
                                final Map<String, Object> fields,
                                final Map<String, Object> fieldsFormatted) {
        this.caseId = caseId;
        this.fields = fields;
        this.fieldsFormatted = fieldsFormatted;
    }

    @Override
    public String getCaseId() {
        return caseId;
    }

    @Override
    public Map<String, Object> getFields() {
        return fields;
    }

    @Override
    public Map<String, Object> getFieldsFormatted() {
        return fieldsFormatted;
    }
}
