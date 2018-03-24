package uk.gov.hmcts.ccd.domain.model.definition;

public class SearchResult {
    private SearchResultField[] fields;

    public SearchResultField[] getFields() {
        return fields;
    }

    public void setFields(SearchResultField[] fields) {
        this.fields = fields;
    }
}
