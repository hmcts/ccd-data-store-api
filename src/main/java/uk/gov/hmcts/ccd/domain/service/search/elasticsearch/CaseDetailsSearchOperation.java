package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import uk.gov.hmcts.ccd.domain.model.search.CaseDetailsSearchResult;

import java.io.IOException;
import java.util.List;


public interface CaseDetailsSearchOperation {

    CaseDetailsSearchResult execute(List<String> caseTypeIds, String query) throws IOException;

}
