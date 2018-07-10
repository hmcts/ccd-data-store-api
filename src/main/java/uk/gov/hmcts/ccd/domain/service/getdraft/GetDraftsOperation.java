package uk.gov.hmcts.ccd.domain.service.getdraft;

import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;

import java.util.List;

public interface GetDraftsOperation {
    List<DraftResponse> execute();
}
