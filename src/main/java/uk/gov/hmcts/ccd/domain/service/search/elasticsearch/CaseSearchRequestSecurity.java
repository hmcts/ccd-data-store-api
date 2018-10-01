package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

public interface CaseSearchRequestSecurity {

    void secureRequest(CaseSearchRequest caseSearchRequest);

}
