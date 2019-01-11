package uk.gov.hmcts.ccd.domain.service.aggregated;

import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;

public interface GetEventTriggerOperation {

    CaseEventTrigger executeForCaseType(String caseTypeId,
                                        String eventTriggerId,
                                        Boolean ignoreWarning);

    CaseEventTrigger executeForCase(String caseReference,
                                    String eventTriggerId,
                                    Boolean ignoreWarning);

    CaseEventTrigger executeForDraft(String draftReference,
                                     Boolean ignoreWarning);
}
