package uk.gov.hmcts.ccd.domain.service.aggregated;

import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;

public interface GetCaseViewOperation {

    CaseView execute(String jurisdictionId, String caseTypeId, String caseReference);

    CaseHistoryView execute(String jurisdictionId, String caseTypeId, String caseReference, Long eventId);
}
