package uk.gov.hmcts.ccd.domain.service.startevent;

import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;

public interface StartEventOperation {
    StartEventTrigger triggerStartForCaseType(Integer uid,
                                              String jurisdictionId,
                                              String caseTypeId,
                                              String eventTriggerId,
                                              Boolean ignoreWarning);

    StartEventTrigger triggerStartForCase(Integer uid,
                                          String jurisdictionId,
                                          String caseTypeId,
                                          String caseReference,
                                          String eventTriggerId,
                                          Boolean ignoreWarning);
}
