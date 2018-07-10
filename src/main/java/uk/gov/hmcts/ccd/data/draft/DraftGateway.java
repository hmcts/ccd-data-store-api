package uk.gov.hmcts.ccd.data.draft;

import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraft;

import java.util.List;

public interface DraftGateway {

    Long save(CreateCaseDraft draft);

    DraftResponse get(String draftId);

    List<DraftResponse> getAll();

    DraftResponse update(UpdateCaseDraft draft, String draftId);

}
