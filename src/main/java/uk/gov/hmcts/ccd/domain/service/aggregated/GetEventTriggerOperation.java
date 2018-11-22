package uk.gov.hmcts.ccd.domain.service.aggregated;

import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;

public interface GetEventTriggerOperation {

    CaseEventTrigger executeForCaseType(String caseTypeId,
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
    CaseEventTrigger executeForCaseType(String uid,
                                        String jurisdictionId,
                                        String caseTypeId,
                                        String eventTriggerId,
                                        Boolean ignoreWarning);

    CaseEventTrigger executeForCase(String uid,
                                    String jurisdictionId,
                                    String caseTypeId,
                                    String caseReference,
                                    String eventTriggerId,
                                    Boolean ignoreWarning);

    CaseEventTrigger executeForDraft(String uid,
                                    String jurisdictionId,
                                    String caseTypeId,
                                    String draftReference,
                                    String eventTriggerId,
                                    Boolean ignoreWarning);
}
