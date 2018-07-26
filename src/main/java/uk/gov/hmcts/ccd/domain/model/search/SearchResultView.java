package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchResultView {
    @JsonProperty("columns")
    private SearchResultViewColumn[] searchResultViewColumns;
    @JsonProperty("results")
    private SearchResultViewItem[] searchResultViewItems;

    public SearchResultView() {
        // Default constructor for JSON mapper
    }

    public SearchResultView(final SearchResultViewColumn[] searchResultViewColumns,
                            final SearchResultViewItem[] searchResultViewItems) {
        this.searchResultViewColumns = searchResultViewColumns;
        this.searchResultViewItems = searchResultViewItems;
    }

    public SearchResultViewColumn[] getSearchResultViewColumns() {
        return searchResultViewColumns;
    }

    public SearchResultViewItem[] getSearchResultViewItems() {
        return searchResultViewItems;
    }
}
