package uk.gov.hmcts.ccd.domain.service.message;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;

public interface MessageService {

    void handleMessage(CaseEventDefinition caseEventDefinition,
                                 CaseDetails caseDetails, String oldState);
}
