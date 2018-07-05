package uk.gov.hmcts.ccd.domain.service.createdraft;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.draft.DefaultDraftRepository;
import uk.gov.hmcts.ccd.data.draft.DraftRepository;
import uk.gov.hmcts.ccd.domain.model.draft.*;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

import javax.inject.Inject;

import static uk.gov.hmcts.ccd.domain.model.draft.CaseDraftBuilder.aCaseDraft;
import static uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDataContentDraftBuilder.aCreateCaseDraftBuilder;
import static uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDataContentDraftBuilder.anUpdateCaseDraftBuilder;

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
        return draftRepository.save(buildCreateCaseDraft(uid, jurisdictionId, caseTypeId, eventTriggerId, caseDataContent));
    }

    @Override
    public Draft updateDraft(final String uid,
                             final String jurisdictionId,
                             final String caseTypeId,
                             final String eventTriggerId,
                             final String draftId,
                             final CaseDataContent caseDataContent) {
        return draftRepository.update(buildUpdateCaseDraft(uid, jurisdictionId, caseTypeId, eventTriggerId, caseDataContent),
                                      draftId);
    }

    private CreateCaseDataContentDraft buildCreateCaseDraft(String uid, String jurisdictionId, String caseTypeId, String eventTriggerId, CaseDataContent caseDataContent) {
        return aCreateCaseDraftBuilder()
            .withDocument(aCaseDraft()
                              .withUserId(uid)
                              .withJurisdictionId(jurisdictionId)
                              .withCaseTypeId(caseTypeId)
                              .withEventTriggerId(eventTriggerId)
                              .withCaseDataContent(caseDataContent)
                              .build())
            .withType(CASE_DATA_CONTENT)
            .withMaxStaleDays(applicationParams.getDraftMaxStaleDays())
            .build();
    }

    private UpdateCaseDataContentDraft buildUpdateCaseDraft(String uid, String jurisdictionId, String caseTypeId, String eventTriggerId, CaseDataContent caseDataContent) {
        return anUpdateCaseDraftBuilder()
            .withDocument(aCaseDraft()
                              .withUserId(uid)
                              .withJurisdictionId(jurisdictionId)
                              .withCaseTypeId(caseTypeId)
                              .withEventTriggerId(eventTriggerId)
                              .withCaseDataContent(caseDataContent)
                              .build())
            .withType(CASE_DATA_CONTENT)
            .build();
    }

}
