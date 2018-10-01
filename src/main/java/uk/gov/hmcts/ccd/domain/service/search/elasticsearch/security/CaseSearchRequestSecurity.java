package uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security;

import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest;

public interface CaseSearchRequestSecurity {

    CaseSearchRequest secureRequest(CaseSearchRequest caseSearchRequest);

}
