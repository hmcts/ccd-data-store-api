package uk.gov.hmcts.ccd.domain.service.startevent;

import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;

public interface StartEventOperation {
    StartEventTrigger triggerStartForCaseType(String caseTypeId,
                                              String eventId,
                                              Boolean ignoreWarning);

    StartEventTrigger triggerStartForCase(String caseReference,
                                          String eventId,
                                          Boolean ignoreWarning);

    StartEventTrigger triggerStartForDraft(String draftReference,
                                           Boolean ignoreWarning);
}
