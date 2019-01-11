package uk.gov.hmcts.ccd.domain.service.createevent;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

public interface CreateEventOperation {
    CaseDetails createCaseEvent(String uid,
                                String jurisdictionId,
                                String caseTypeId,
                                String caseReference,
                                CaseDataContent caseDataContent);
}
