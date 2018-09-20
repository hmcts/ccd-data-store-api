package uk.gov.hmcts.ccd.domain.service.upsertdraft;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.DefaultDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import javax.inject.Inject;

@Service
@Qualifier("default")
public class DefaultUpsertDraftOperation implements UpsertDraftOperation {

    private final DraftGateway draftGateway;
    private final ApplicationParams applicationParams;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseSanitiser caseSanitiser;

    protected static final String CASE_DATA_CONTENT = "CaseDataContent";

    @Inject
    public DefaultUpsertDraftOperation(@Qualifier(DefaultDraftGateway.QUALIFIER) final DraftGateway draftGateway,
                                       @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                       final CaseSanitiser caseSanitiser,

                                       ApplicationParams applicationParams) {
        this.draftGateway = draftGateway;
        this.applicationParams = applicationParams;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseSanitiser = caseSanitiser;
    }

    @Override
    public DraftResponse executeSave(final String uid,
                                     final String jurisdictionId,
                                     final String caseTypeId,
                                     final String eventTriggerId,
                                     final CaseDataContent caseDataContent) {
        final DraftResponse draftResponse = new DraftResponse();
        draftResponse.setId(draftGateway.create(buildCreateCaseDraft(uid, jurisdictionId, caseTypeId, eventTriggerId, caseDataContent)).toString());
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

    private CreateCaseDraftRequest buildCreateCaseDraft(String uid,
                                                        String jurisdictionId,
                                                        String caseTypeId,
                                                        String eventTriggerId,
                                                        CaseDataContent caseDataContent) {
        sanitiseData(caseTypeId, caseDataContent);

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

    private UpdateCaseDraftRequest buildUpdateCaseDraft(String uid,
                                                        String jurisdictionId,
                                                        String caseTypeId,
                                                        String eventTriggerId,
                                                        CaseDataContent caseDataContent) {
        sanitiseData(caseTypeId, caseDataContent);

        final CaseDraft caseDraft = new CaseDraft();
        caseDraft.setUserId(uid);
        caseDraft.setJurisdictionId(jurisdictionId);
        caseDraft.setCaseTypeId(caseTypeId);
        caseDraft.setEventTriggerId(eventTriggerId);
        caseDraft.setCaseDataContent(caseDataContent);

        final UpdateCaseDraftRequest updateCaseDraftRequest = new UpdateCaseDraftRequest();
        updateCaseDraftRequest.setType(CASE_DATA_CONTENT);
        updateCaseDraftRequest.setDocument(caseDraft);
        return updateCaseDraftRequest;
    }

    private void sanitiseData(String caseTypeId, CaseDataContent caseDataContent) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for " + caseTypeId);
        }
        caseDataContent.setData(caseSanitiser.sanitise(caseType, caseDataContent.getData()));
    }

}
