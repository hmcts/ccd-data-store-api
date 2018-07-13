package uk.gov.hmcts.ccd.domain.service.getdraft;

import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;

public interface GetDraftOperation {

    /**
     *
     * @param draftId a unique draft id
     * @return DraftResponse when found; null otherwise
     *
     */
    DraftResponse execute(String draftId);
}
