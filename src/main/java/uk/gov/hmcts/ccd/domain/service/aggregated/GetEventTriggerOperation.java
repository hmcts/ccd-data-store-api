package uk.gov.hmcts.ccd.domain.service.aggregated;

import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;

public interface GetEventTriggerOperation {

    CaseUpdateViewEvent executeForCaseType(String caseTypeId,
                                           String eventId,
                                           Boolean ignoreWarning);

    CaseUpdateViewEvent executeForCase(String caseReference,
                                       String eventId,
                                       Boolean ignoreWarning);

    CaseUpdateViewEvent executeForDraft(String draftReference,
                                        Boolean ignoreWarning);
}
