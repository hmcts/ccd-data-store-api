package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;


public interface CaseSearchOperation {

    CaseSearchResult execute(CrossCaseTypeSearchRequest request, boolean dataClassification);

}
