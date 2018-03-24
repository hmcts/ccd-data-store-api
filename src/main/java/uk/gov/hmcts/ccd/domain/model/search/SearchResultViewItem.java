package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public class SearchResultViewItem {
    @JsonProperty("case_id")
    private String caseId;
    @JsonProperty("case_fields")
    private Map<String, JsonNode> caseFields;

    public SearchResultViewItem() {
        // Default constructor for JSON mapper
    }

    public SearchResultViewItem(final String caseId,
                                final Map<String, JsonNode> caseFields) {
        this.caseId = caseId;
        this.caseFields = caseFields;
    }

    public String getCaseId() {
        return caseId;
    }

    public Map<String, JsonNode> getCaseFields() {
        return caseFields;
    }
}
