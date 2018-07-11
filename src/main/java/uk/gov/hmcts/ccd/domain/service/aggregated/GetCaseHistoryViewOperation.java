package uk.gov.hmcts.ccd.domain.service.aggregated;

import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;

public interface GetCaseHistoryViewOperation {

    CaseHistoryView execute(String jurisdictionId, String caseTypeId, String caseReference, Long eventId);
}
