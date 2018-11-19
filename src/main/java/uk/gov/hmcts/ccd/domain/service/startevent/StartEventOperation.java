package uk.gov.hmcts.ccd.domain.service.startevent;

import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;

public interface StartEventOperation {
    StartEventTrigger triggerStartForCaseType(String caseTypeId,
                                              String eventTriggerId,
                                              Boolean ignoreWarning);

    /**
     *
     * @param uid
     * @param jurisdictionId
     * @param caseTypeId
     * @param eventTriggerId
     * @param ignoreWarning
     * @return When found, start event trigger for given case type and event trigger id
     */
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
