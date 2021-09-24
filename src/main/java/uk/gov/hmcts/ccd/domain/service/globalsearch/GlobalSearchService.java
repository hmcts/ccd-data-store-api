package uk.gov.hmcts.ccd.domain.service.globalsearch;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.dto.globalsearch.GlobalSearchResponse;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;

@Service
public interface GlobalSearchService {

    CrossCaseTypeSearchRequest assembleSearchQuery(GlobalSearchRequestPayload payload);

    GlobalSearchResponse transformResponse(GlobalSearchRequestPayload requestPayload,
                                           CaseSearchResult caseSearchResult);
}
