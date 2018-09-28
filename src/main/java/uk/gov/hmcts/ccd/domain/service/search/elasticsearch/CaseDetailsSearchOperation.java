package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import uk.gov.hmcts.ccd.domain.model.search.CaseDetailsSearchResult;


public interface CaseDetailsSearchOperation {

    CaseDetailsSearchResult execute(String caseTypeId, String jsonQuery);

}
