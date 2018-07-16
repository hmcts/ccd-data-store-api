package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchResultView {
    @JsonProperty("columns")
    private SearchResultViewColumn[] searchResultViewColumns;
    @JsonProperty("results")
    private SearchResultViewItem[] searchResultViewItems;
    @JsonProperty("result_error")
    private String resultError;

    public SearchResultView() {
        // Default constructor for JSON mapper
    }

    public SearchResultView(final SearchResultViewColumn[] searchResultViewColumns,
                            final SearchResultViewItem[] searchResultViewItems,
                            final String resultError) {
        this.searchResultViewColumns = searchResultViewColumns;
        this.searchResultViewItems = searchResultViewItems;
        this.resultError = resultError;
    }

    public SearchResultViewColumn[] getSearchResultViewColumns() {
        return searchResultViewColumns;
    }

    public SearchResultViewItem[] getSearchResultViewItems() {
        return searchResultViewItems;
    }

    public String getResultError() {
        return resultError;
    }
}
