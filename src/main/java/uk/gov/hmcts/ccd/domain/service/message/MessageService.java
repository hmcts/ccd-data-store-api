package uk.gov.hmcts.ccd.domain.service.message;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.std.Event;

public interface MessageService {

    void handleMessage(Event event,
                                 CaseEventDefinition caseEventDefinition,
                                 CaseDetails caseDetails);
}
