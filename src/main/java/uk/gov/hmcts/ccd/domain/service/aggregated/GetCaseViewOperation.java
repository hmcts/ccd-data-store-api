package uk.gov.hmcts.ccd.domain.service.aggregated;

import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;

public interface GetCaseViewOperation {

    CaseView execute(String caseReference);
}
