package uk.gov.hmcts.ccd.v2.internal.resource;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.CaseSearchResultView;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ApiModel(description = "")
public class CaseSearchResultViewResource extends RepresentationModel {

    @ApiModelProperty(value = "Headers for each case type")
    private List<SearchResultViewHeaderGroup> headers;
    @ApiModelProperty(value = "All cases across case types")
    private List<SearchResultViewItem> cases;
    @ApiModelProperty(value = "Total number of search results (including results not returned due to pagination)")
    private Long total;

    public CaseSearchResultViewResource(@NonNull CaseSearchResultView caseSearchResultView) {
        copyProperties(caseSearchResultView);
    }

    private void copyProperties(CaseSearchResultView caseSearchResultView) {
        this.headers = caseSearchResultView.getHeaders();
        this.cases = caseSearchResultView.getCases();
        this.total = caseSearchResultView.getTotal();
    }
}
