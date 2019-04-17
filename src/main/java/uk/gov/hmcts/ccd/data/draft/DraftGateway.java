package uk.gov.hmcts.ccd.data.draft;

import java.util.List;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraftRequest;

public interface DraftGateway {

    Long create(CreateCaseDraftRequest draft);

    DraftResponse get(String draftId);

    CaseDetails getCaseDetails(String draftId);

    List<DraftResponse> getAll();

    DraftResponse update(UpdateCaseDraftRequest draft, String draftId);

    void delete(String draftId);
}
