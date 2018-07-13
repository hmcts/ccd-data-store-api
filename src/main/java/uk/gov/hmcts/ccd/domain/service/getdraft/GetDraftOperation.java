package uk.gov.hmcts.ccd.domain.service.getdraft;

import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;

import java.util.Optional;

public interface GetDraftOperation {

    /**
     *
     * @param draftId a unique draft id
     * @return Optional containing DraftResponse when found; empty optional otherwise
     *
     */
    Optional<DraftResponse> execute(String draftId);
}
