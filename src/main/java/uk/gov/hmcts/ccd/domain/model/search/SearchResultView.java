package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SearchResultView {
    @JsonProperty("columns")
    private List<SearchResultViewColumn> searchResultViewColumns;
    @JsonProperty("results")
    private List<SearchResultViewItem> searchResultViewItems;

    public SearchResultView() {
        // Default constructor for JSON mapper
    }

    public SearchResultView(final List<SearchResultViewColumn> searchResultViewColumns,
                            final List<SearchResultViewItem> searchResultViewItems) {
        this.searchResultViewColumns = searchResultViewColumns;
        this.searchResultViewItems = searchResultViewItems;
    }

    public List<SearchResultViewColumn> getSearchResultViewColumns() {
        return searchResultViewColumns;
    }

    public List<SearchResultViewItem> getSearchResultViewItems() {
        return searchResultViewItems;
    }
}
