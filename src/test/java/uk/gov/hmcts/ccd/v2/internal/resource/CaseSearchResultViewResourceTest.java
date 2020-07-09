package uk.gov.hmcts.ccd.v2.internal.resource;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.CaseSearchResultView;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.*;

class CaseSearchResultViewResourceTest {

    @Test
    void shouldCopyUiCaseSearchResult() {
        List<SearchResultViewHeaderGroup> headers = new ArrayList<>();
        List<SearchResultViewItem> cases = new ArrayList<>();
        Long total = 3L;

        CaseSearchResultView caseSearchResultView = new CaseSearchResultView(headers, cases, total);

        CaseSearchResultViewResource resource = new CaseSearchResultViewResource(caseSearchResultView);

        assertAll(
            () -> assertThat(resource.getCases(), sameInstance(cases)),
            () -> assertThat(resource.getHeaders(), sameInstance(headers)),
            () -> assertThat(resource.getTotal(), sameInstance(total))
        );
    }
}
