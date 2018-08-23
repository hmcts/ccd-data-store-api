package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.List;

public interface SearchOperation {

    List<CaseDetailsElastic> execute(String caseTypeId, String query);

}
