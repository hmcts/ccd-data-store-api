package uk.gov.hmcts.ccd.domain.service.startevent;

import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;

public interface StartEventOperation {
    StartEventTrigger triggerStartForCaseType(String uid,
                                              String jurisdictionId,
                                              String caseTypeId,
                                              String eventTriggerId,
                                              Boolean ignoreWarning);

    StartEventTrigger triggerStartForCase(String uid,
                                          String jurisdictionId,
                                          String caseTypeId,
                                          String caseReference,
                                          String eventTriggerId,
                                          Boolean ignoreWarning);

    StartEventTrigger triggerStartForDraft(String uid,
                                          String jurisdictionId,
                                          String caseTypeId,
                                          String draftReference,
                                          String eventTriggerId,
                                          Boolean ignoreWarning);
}
