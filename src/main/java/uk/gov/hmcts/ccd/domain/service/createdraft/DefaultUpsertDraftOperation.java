package uk.gov.hmcts.ccd.domain.service.createdraft;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.draft.DefaultDraftRepository;
import uk.gov.hmcts.ccd.data.draft.DraftRepository;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDataContentDraft;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDataContentDraft;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDataContentDraft;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;

import javax.inject.Inject;

@Service
@Qualifier("default")
public class DefaultUpsertDraftOperation implements UpsertDraftOperation {

    private final DraftRepository draftRepository;
    private final ApplicationParams applicationParams;

    protected static final String CASE_DATA_CONTENT = "CaseDataContent";

    @Inject
    public DefaultUpsertDraftOperation(@Qualifier(DefaultDraftRepository.QUALIFIER) final DraftRepository draftRepository,
                                       ApplicationParams applicationParams) {
        this.draftRepository = draftRepository;
        this.applicationParams = applicationParams;
    }

    @Override
    public Draft saveDraft(final String uid,
                           final String jurisdictionId,
                           final String caseTypeId,
                           final String eventTriggerId,
                           final CaseDataContent caseDataContent) {
        CaseDataContentDraft caseDataContentDraft = buildCaseDataContentDraft(uid, jurisdictionId, caseTypeId, eventTriggerId, caseDataContent);
        CreateCaseDataContentDraft createCaseDataContentDraft = new CreateCaseDataContentDraft(caseDataContentDraft, CASE_DATA_CONTENT, applicationParams.getDraftMaxStaleDays());
        return draftRepository.save(createCaseDataContentDraft);
    }

    @Override
    public Draft updateDraft(final String uid,
                             final String jurisdictionId,
                             final String caseTypeId,
                             final String eventTriggerId,
                             final String draftId,
                             final CaseDataContent caseDataContent) {
        CaseDataContentDraft caseDataContentDraft = buildCaseDataContentDraft(uid, jurisdictionId, caseTypeId, eventTriggerId, caseDataContent);
        UpdateCaseDataContentDraft updateCaseDataContentDraft = new UpdateCaseDataContentDraft(caseDataContentDraft, CASE_DATA_CONTENT);
        return draftRepository.update(updateCaseDataContentDraft, draftId);
    }

    private CaseDataContentDraft buildCaseDataContentDraft(String uid, String jurisdictionId, String caseTypeId, String eventTriggerId, CaseDataContent caseDataContent) {
        return new CaseDataContentDraft(uid, jurisdictionId, caseTypeId, eventTriggerId, caseDataContent);
    }

}
