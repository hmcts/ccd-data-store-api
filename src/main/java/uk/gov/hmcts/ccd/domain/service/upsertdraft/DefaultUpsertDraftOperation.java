package uk.gov.hmcts.ccd.domain.service.upsertdraft;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.draft.DefaultDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

import javax.inject.Inject;

@Service
@Qualifier("default")
public class DefaultUpsertDraftOperation implements UpsertDraftOperation {

    private final DraftGateway draftGateway;
    private final ApplicationParams applicationParams;

    protected static final String CASE_DATA_CONTENT = "CaseDataContent";

    @Inject
    public DefaultUpsertDraftOperation(@Qualifier(DefaultDraftGateway.QUALIFIER) final DraftGateway draftGateway,
                                       ApplicationParams applicationParams) {
        this.draftGateway = draftGateway;
        this.applicationParams = applicationParams;
    }

    @Override
    public DraftResponse executeSave(final String uid,
                             final String jurisdictionId,
                             final String caseTypeId,
                             final String eventTriggerId,
                             final CaseDataContent caseDataContent) {
        final DraftResponse draftResponse = new DraftResponse();
        draftResponse.setId(draftGateway.save(buildCreateCaseDraft(uid, jurisdictionId, caseTypeId, eventTriggerId, caseDataContent)).toString());
        return draftResponse;
    }

    @Override
    public DraftResponse executeUpdate(final String uid,
                               final String jurisdictionId,
                               final String caseTypeId,
                               final String eventTriggerId,
                               final String draftId,
                               final CaseDataContent caseDataContent) {
        return draftGateway.update(buildUpdateCaseDraft(uid, jurisdictionId, caseTypeId, eventTriggerId, caseDataContent),
                                   draftId);
    }

    private CreateCaseDraftRequest buildCreateCaseDraft(String uid, String jurisdictionId, String caseTypeId, String eventTriggerId, CaseDataContent caseDataContent) {
        final CaseDraft caseDraft = new CaseDraft();
        caseDraft.setUserId(uid);
        caseDraft.setJurisdictionId(jurisdictionId);
        caseDraft.setCaseTypeId(caseTypeId);
        caseDraft.setEventTriggerId(eventTriggerId);
        caseDraft.setCaseDataContent(caseDataContent);

        final CreateCaseDraftRequest createCaseDraftRequest = new CreateCaseDraftRequest();
        createCaseDraftRequest.setDocument(caseDraft);
        createCaseDraftRequest.setType(CASE_DATA_CONTENT);
        createCaseDraftRequest.setMaxTTLDays(applicationParams.getDraftMaxTTLDays());
        return createCaseDraftRequest;
    }

    private UpdateCaseDraftRequest buildUpdateCaseDraft(String uid, String jurisdictionId, String caseTypeId, String eventTriggerId, CaseDataContent caseDataContent) {
        final UpdateCaseDraftRequest updateCaseDraftRequest = new UpdateCaseDraftRequest();
        updateCaseDraftRequest.setType(CASE_DATA_CONTENT);
        final CaseDraft caseDraft = new CaseDraft();
        caseDraft.setUserId(uid);
        caseDraft.setJurisdictionId(jurisdictionId);
        caseDraft.setCaseTypeId(caseTypeId);
        caseDraft.setEventTriggerId(eventTriggerId);
        caseDraft.setCaseDataContent(caseDataContent);
        updateCaseDraftRequest.setDocument(caseDraft);
        return updateCaseDraftRequest;
    }

}
