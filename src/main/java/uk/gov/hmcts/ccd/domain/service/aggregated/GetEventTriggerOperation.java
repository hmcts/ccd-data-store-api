package uk.gov.hmcts.ccd.domain.service.aggregated;

import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;

public interface GetEventTriggerOperation {
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
