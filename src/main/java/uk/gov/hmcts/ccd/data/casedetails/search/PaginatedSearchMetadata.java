package uk.gov.hmcts.ccd.data.casedetails.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.io.Serializable;

@ToString
public class PaginatedSearchMetadata implements Serializable {

    private static final long serialVersionUID = 3129236643198519380L;
    private Integer totalResultsCount;
    private Integer totalPagesCount;


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
