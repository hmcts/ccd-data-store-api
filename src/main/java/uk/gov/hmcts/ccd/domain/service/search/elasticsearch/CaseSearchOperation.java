package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import uk.gov.hmcts.ccd.domain.model.search.*;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.UICaseSearchResult;

import java.util.List;


public interface CaseSearchOperation {

    CaseSearchResult executeExternal(CrossCaseTypeSearchRequest request);

    UICaseSearchResult executeInternal(CaseSearchResult caseSearchResult,
                                       List<String> caseTypeIds,
                                       UseCase useCase);
}
