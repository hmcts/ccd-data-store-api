package uk.gov.hmcts.ccd.domain.service.globalsearch;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.dto.globalsearch.GlobalSearchResponse;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;

@Service
public interface GlobalSearchService {

    void assembleSearchQuery(GlobalSearchRequestPayload payload);

    GlobalSearchResponse transformResponse(GlobalSearchRequestPayload requestPayload,
                                           CaseSearchResult caseSearchResult);
}
