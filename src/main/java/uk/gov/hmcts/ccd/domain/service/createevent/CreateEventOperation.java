package uk.gov.hmcts.ccd.domain.service.createevent;

import uk.gov.hmcts.ccd.domain.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

public interface CreateEventOperation {
    CaseDetails createCaseEvent(String caseReference,
                                CaseDataContent caseDataContent);
}
