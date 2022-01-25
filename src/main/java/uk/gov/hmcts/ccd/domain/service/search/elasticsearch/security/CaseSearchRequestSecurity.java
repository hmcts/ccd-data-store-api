package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;

public interface CaseSearchRequestSecurity {

    CaseSearchRequest createSecuredSearchRequest(CaseSearchRequest caseSearchRequest);

    CrossCaseTypeSearchRequest createSecuredSearchRequest(CrossCaseTypeSearchRequest request);

}
