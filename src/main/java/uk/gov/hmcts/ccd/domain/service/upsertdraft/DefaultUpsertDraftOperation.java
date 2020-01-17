package uk.gov.hmcts.ccd.domain.service.upsertdraft;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.draft.CaseDraft;
import uk.gov.hmcts.ccd.domain.model.draft.CreateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.draft.UpdateCaseDraftRequest;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

@Service
@Qualifier("default")
public class DefaultUpsertDraftOperation implements UpsertDraftOperation {

    private final DraftGateway draftGateway;
    private final ApplicationParams applicationParams;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseSanitiser caseSanitiser;
    private final UserAuthorisation userAuthorisation;

    protected static final String CASE_DATA_CONTENT = "CaseDataContent";

    @Inject
    public DefaultUpsertDraftOperation(@Qualifier(CachedDraftGateway.QUALIFIER) final DraftGateway draftGateway,
                                       @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                       final CaseSanitiser caseSanitiser,
                                       final UserAuthorisation userAuthorisation,
                                       ApplicationParams applicationParams) {
        this.draftGateway = draftGateway;
        this.applicationParams = applicationParams;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseSanitiser = caseSanitiser;
        this.userAuthorisation = userAuthorisation;
    }

    @Override
    public DraftResponse executeSave(final String caseTypeId, final CaseDataContent caseDataContent) {
        final DraftResponse draftResponse = new DraftResponse();
        CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        final String eventId = caseDataContent.getEventId();
        if (!caseType.hasEventId(eventId)) {
            throw new ValidationException("Validation error. Event id " + eventId + " is not found in case type definition");
        }
        draftResponse.setId(createDraft(caseDataContent, caseType, eventId));
        return draftResponse;
    }

    @Override
    public DraftResponse executeUpdate(final String caseTypeId, final String draftId, final CaseDataContent caseDataContent) {
        CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        final String eventId = caseDataContent.getEventId();
        if (!caseType.hasEventId(eventId)) {
            throw new ValidationException("Validation error. Event id " + eventId + " is not found in case type definition");
        }
        return draftGateway.update(buildUpdateCaseDraft(userAuthorisation.getUserId(),
                                                        caseType,
                                                        eventId,
                                                        caseDataContent),
                                   draftId);
    }

    private String createDraft(CaseDataContent caseDataContent, CaseType caseType, String eventId) {
        return draftGateway.create(buildCreateCaseDraft(userAuthorisation.getUserId(),
                                                        caseType,
                                                        eventId,
                                                        caseDataContent)).toString();
    }

    private CreateCaseDraftRequest buildCreateCaseDraft(String uid,
                                                        CaseType caseType,
                                                        String eventTriggerId,
                                                        CaseDataContent caseDataContent) {
        caseDataContent.setData(caseSanitiser.sanitise(caseType, caseDataContent.getData()));

        final CaseDraft caseDraft = new CaseDraft();
        caseDraft.setUserId(uid);
        caseDraft.setJurisdictionId(caseType.getJurisdictionId());
        caseDraft.setCaseTypeId(caseType.getId());
        caseDraft.setEventTriggerId(eventTriggerId);
        caseDraft.setCaseDataContent(caseDataContent);

        final CreateCaseDraftRequest createCaseDraftRequest = new CreateCaseDraftRequest();
        createCaseDraftRequest.setDocument(caseDraft);
        createCaseDraftRequest.setType(CASE_DATA_CONTENT);
        createCaseDraftRequest.setMaxTTLDays(applicationParams.getDraftMaxTTLDays());
        return createCaseDraftRequest;
    }

    private UpdateCaseDraftRequest buildUpdateCaseDraft(String uid,
                                                        CaseType caseType,
                                                        String eventTriggerId,
                                                        CaseDataContent caseDataContent) {
        caseDataContent.setData(caseSanitiser.sanitise(caseType, caseDataContent.getData()));

        final CaseDraft caseDraft = new CaseDraft();
        caseDraft.setUserId(uid);
        caseDraft.setJurisdictionId(caseType.getJurisdictionId());
        caseDraft.setCaseTypeId(caseType.getId());
        caseDraft.setEventTriggerId(eventTriggerId);
        caseDraft.setCaseDataContent(caseDataContent);

        final UpdateCaseDraftRequest updateCaseDraftRequest = new UpdateCaseDraftRequest();
        updateCaseDraftRequest.setType(CASE_DATA_CONTENT);
        updateCaseDraftRequest.setDocument(caseDraft);
        return updateCaseDraftRequest;
    }

}
