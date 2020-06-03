package uk.gov.hmcts.ccd.v2.internal.resource;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewHeaderGroup;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.UICaseSearchResult;

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
        String useCase = "ORGCASES";

        UICaseSearchResult uiCaseSearchResult = new UICaseSearchResult(headers, cases, total, useCase);

        CaseSearchResultViewResource resource = new CaseSearchResultViewResource(uiCaseSearchResult);

        assertAll(
            () -> assertThat(resource.getCases(), sameInstance(cases)),
            () -> assertThat(resource.getHeaders(), sameInstance(headers)),
            () -> assertThat(resource.getTotal(), sameInstance(total)),
            () -> assertThat(resource.getUseCase(), sameInstance(useCase))
        );
    }
}
