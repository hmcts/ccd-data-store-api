package uk.gov.hmcts.ccd.domain.service.search.global;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchResponsePayload;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;

import java.util.List;

@Service
public interface GlobalSearchService {

    CrossCaseTypeSearchRequest assembleSearchQuery(GlobalSearchRequestPayload payload);

    GlobalSearchResponsePayload transformResponse(GlobalSearchRequestPayload requestPayload,
                                                  Long caseSearchResultTotal,
                                                  List<CaseDetails> filteredCaseList);
}
