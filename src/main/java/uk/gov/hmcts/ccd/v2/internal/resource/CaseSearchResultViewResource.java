package uk.gov.hmcts.ccd.v2.internal.resource;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.springframework.hateoas.RepresentationModel;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.UICaseSearchHeader;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.UICaseSearchResult;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CaseSearchResultViewResource extends RepresentationModel {

    private List<UICaseSearchHeader> headers;
    private List<SearchResultViewItem> cases;
    private Long total;

    public CaseSearchResultViewResource(@NonNull UICaseSearchResult uiCaseSearchResult) {
        copyProperties(uiCaseSearchResult);
    }

    private void copyProperties(UICaseSearchResult uiCaseSearchResult) {
        this.headers = uiCaseSearchResult.getHeaders();
        this.cases = uiCaseSearchResult.getCases();
        this.total = uiCaseSearchResult.getTotal();
    }
}
