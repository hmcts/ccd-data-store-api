package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.io.IOException;
import java.util.List;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

public interface CaseDetailsSearchOperation {

    List<CaseDetails> execute(List<String> caseTypesId, String query) throws IOException;

}
