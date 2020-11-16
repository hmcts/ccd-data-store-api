package uk.gov.hmcts.ccd.domain.service.createevent;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

public interface CreateEventOperation {
    CaseDetails createCaseEvent(String caseReference,
                                String onBehalfOfUser,
                                CaseDataContent caseDataContent);
}
