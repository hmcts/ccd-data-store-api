package uk.gov.hmcts.ccd.domain.model.search;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.List;

public class SearchResultView implements Serializable {
    private static final long serialVersionUID = 217062515213288864L;
    @JsonProperty("columns")
    private List<SearchResultViewColumn> searchResultViewColumns;
    @JsonProperty("results")
    private List<SearchResultViewItem> searchResultViewItems;
    @JsonProperty("result_error")
    private String resultError;

    public SearchResultView() {
        // Default constructor for JSON mapper
    }

    public SearchResultView(final List<SearchResultViewColumn> searchResultViewColumns,
                            final List<SearchResultViewItem> searchResultViewItems,
                            final String resultError) {
        this.searchResultViewColumns = searchResultViewColumns;
        this.searchResultViewItems = searchResultViewItems;
        this.resultError = resultError;
    }

    public List<SearchResultViewColumn> getSearchResultViewColumns() {
        return searchResultViewColumns;
    }

    public List<SearchResultViewItem> getSearchResultViewItems() {
        return searchResultViewItems;
    }

    public String getResultError() {
        return resultError;
    }
}
