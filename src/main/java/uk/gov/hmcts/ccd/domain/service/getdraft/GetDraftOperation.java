package uk.gov.hmcts.ccd.domain.service.getdraft;

import uk.gov.hmcts.ccd.domain.model.draft.Draft;

public interface GetDraftOperation {
    Draft execute(String draftId);
}
