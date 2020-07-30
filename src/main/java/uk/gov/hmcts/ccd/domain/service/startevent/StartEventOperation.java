package uk.gov.hmcts.ccd.domain.service.startevent;

import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;

public interface StartEventOperation {
    StartEventResult triggerStartForCaseType(String caseTypeId,
                                             String eventId,
                                             Boolean ignoreWarning);

    StartEventResult triggerStartForCase(String caseReference,
                                         String eventId,
                                         Boolean ignoreWarning);

    StartEventResult triggerStartForDraft(String draftReference,
                                          Boolean ignoreWarning);
}
