package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

public class SearchResultViewItem {
    @JsonProperty("case_id")
    private String caseId;
    @JsonProperty("case_fields")
    private final Map<String, Object> caseFields = new HashMap<>();

    public SearchResultViewItem() {
        // Default constructor for JSON mapper
    }

    public SearchResultViewItem(final String caseId) {
        this.caseId = caseId;
    }

    public String getCaseId() {
        return caseId;
    }

    public Map<String, Object> getCaseFields() {
        return caseFields;
    }

    public <T> SearchResultViewItem addCaseFields(Map<String, T> caseFields) {
        this.caseFields.putAll(caseFields);
        return this;
    }
}
