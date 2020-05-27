package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class SearchResultViewItem {
    @JsonProperty("case_id")
    private String caseId;
    @JsonProperty("case_fields")
    private Map<String, Object> caseFields;
    @JsonProperty("case_fields_formatted")
    private Map<String, Object> caseFieldsFormatted;

    public SearchResultViewItem() {
        // Default constructor for JSON mapper
    }

    public SearchResultViewItem(final String caseId,
                                final Map<String, Object> caseFields,
                                final Map<String, Object> caseFieldsFormatted) {
        this.caseId = caseId;
        this.caseFields = caseFields;
        this.caseFieldsFormatted = caseFieldsFormatted;
    }

    public String getCaseId() {
        return caseId;
    }

    public Map<String, Object> getCaseFields() {
        return caseFields;
    }

    public Map<String, Object> getCaseFieldsFormatted() {
        return caseFieldsFormatted;
    }
}
