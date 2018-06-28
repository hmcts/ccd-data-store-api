package uk.gov.hmcts.ccd.data.draft;

import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDataContentDraft;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;

public interface DraftRepository {

    Draft set(CreateCaseDataContentDraft draft);

}
