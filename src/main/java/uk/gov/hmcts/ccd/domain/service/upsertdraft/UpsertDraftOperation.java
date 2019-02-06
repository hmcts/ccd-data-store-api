package uk.gov.hmcts.ccd.domain.service.upsertdraft;

import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

public interface UpsertDraftOperation {
    DraftResponse executeSave(String caseTypeId, CaseDataContent caseDataContent);

    DraftResponse executeUpdate(String caseTypeId, String draftId, CaseDataContent caseDataContent);

}
