package uk.gov.hmcts.ccd.domain.service.globalsearch;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchResponsePayload;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;

@Service
public interface GlobalSearchService {

    CrossCaseTypeSearchRequest assembleSearchQuery(GlobalSearchRequestPayload payload);

    GlobalSearchResponsePayload transformResponse(GlobalSearchRequestPayload requestPayload,
                                                  CaseSearchResult caseSearchResult);
}
