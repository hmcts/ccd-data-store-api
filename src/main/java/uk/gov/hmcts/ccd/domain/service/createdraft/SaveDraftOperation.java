package uk.gov.hmcts.ccd.domain.service.createdraft;

import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;

public interface SaveDraftOperation {
    Draft saveDraft(String uid,
                          String jurisdictionId,
                          String caseTypeId,
                          String eventTriggerId,
                          CaseDataContent caseDataContent);
}
