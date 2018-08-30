package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.io.IOException;
import java.util.List;

import uk.gov.hmcts.ccd.domain.model.search.CaseDetailsSearchResult;

public interface CaseDetailsSearchOperation {

    CaseDetailsSearchResult execute(List<String> caseTypesId, String query) throws IOException;

}
