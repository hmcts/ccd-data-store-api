package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.List;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

public interface SearchOperation {

    List<CaseDetails> execute(String caseTypeId, String query);

}
