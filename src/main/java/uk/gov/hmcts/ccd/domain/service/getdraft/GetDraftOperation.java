package uk.gov.hmcts.ccd.domain.service.getdraft;

import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;

public interface GetDraftOperation {
    DraftResponse execute(String draftId);
}
