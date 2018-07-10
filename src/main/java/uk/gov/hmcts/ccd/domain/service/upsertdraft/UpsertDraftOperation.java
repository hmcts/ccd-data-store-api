package uk.gov.hmcts.ccd.domain.service.upsertdraft;

import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

public interface UpsertDraftOperation {
    DraftResponse executeSave(String uid,
                              String jurisdictionId,
                              String caseTypeId,
                              String eventTriggerId,
                              CaseDataContent caseDataContent);

    DraftResponse executeUpdate(String uid,
                        String jurisdictionId,
                        String caseTypeId,
                        String eventTriggerId,
                        String draftId,
                        CaseDataContent caseDataContent);

}
