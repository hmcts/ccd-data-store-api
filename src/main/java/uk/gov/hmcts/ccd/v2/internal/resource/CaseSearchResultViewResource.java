package uk.gov.hmcts.ccd.v2.internal.resource;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema
public class CaseSearchResultViewResource extends RepresentationModel {

    @Schema(description = "Headers for each case type")
    private List<SearchResultViewHeaderGroup> headers;
    @Schema(description = "All cases across case types")
    private List<SearchResultViewItem> cases;
    @Schema(description = "Total number of search results (including results not returned due to pagination)")
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
