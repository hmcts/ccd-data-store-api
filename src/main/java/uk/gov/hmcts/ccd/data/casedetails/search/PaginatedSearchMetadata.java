package uk.gov.hmcts.ccd.data.casedetails.search;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaginatedSearchMetadata {

    private Integer totalResultsCount;
    private Integer totalPagesCount;
    private static final int valueOfEmpty = 0;

    public static final PaginatedSearchMetadata EMPTY = new PaginatedSearchMetadata(valueOfEmpty,valueOfEmpty);

    public PaginatedSearchMetadata() {
    }

    public PaginatedSearchMetadata(final int totalResultsCount, final int totalPagesCount) {
        this.totalResultsCount = totalResultsCount;
        this.totalPagesCount = totalPagesCount;
    }

    public void setTotalResultsCount(Integer totalResultsCount) {
        this.totalResultsCount = totalResultsCount;
    }

    @JsonProperty("total_results_count")
    public Integer getTotalResultsCount() {
        return totalResultsCount;
    }

    public void setTotalPagesCount(Integer totalPagesCount) {
        this.totalPagesCount = totalPagesCount;
    }

    @JsonProperty("total_pages_count")
    public Integer getTotalPagesCount() {
        return totalPagesCount;
    }
}
