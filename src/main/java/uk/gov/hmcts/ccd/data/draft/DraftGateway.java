package uk.gov.hmcts.ccd.data.draft;

import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDataContentDraft;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDataContentDraft;

public interface DraftGateway {

    Draft save(CreateCaseDataContentDraft draft);

    Draft update(UpdateCaseDataContentDraft draft, String draftId);

}
