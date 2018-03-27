package uk.gov.hmcts.ccd.domain.model.definition;

import java.io.Serializable;

public class SearchResult implements Serializable {
    private SearchResultField[] fields;

    public SearchResultField[] getFields() {
        return fields;
    }

    public void setFields(SearchResultField[] fields) {
        this.fields = fields;
    }
}
