package uk.gov.hmcts.ccd.domain.service.upsertdraft;

import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;

public interface UpsertDraftOperation {
    Draft executeSave(String uid,
                      String jurisdictionId,
                      String caseTypeId,
                      String eventTriggerId,
                      CaseDataContent caseDataContent);

    Draft executeUpdate(String uid,
                        String jurisdictionId,
                        String caseTypeId,
                        String eventTriggerId,
                        String draftId,
                        CaseDataContent caseDataContent);

}
