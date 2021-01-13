package uk.gov.hmcts.ccd.domain.service.startevent;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import java.util.Map;
import java.util.function.Supplier;


@Service
@Qualifier("default")
public class DefaultStartEventOperation implements StartEventOperation {

    private final EventTokenService eventTokenService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final DraftGateway draftGateway;
    private final CaseDetailsRepository caseDetailsRepository;
    private final EventTriggerService eventTriggerService;
    private final CaseService caseService;
    private final UserAuthorisation userAuthorisation;
    private final CallbackInvoker callbackInvoker;
    private final UIDService uidService;

    @Autowired
    public DefaultStartEventOperation(final EventTokenService eventTokenService,
                                      @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                          final CaseDefinitionRepository caseDefinitionRepository,
                                      @Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                                          final CaseDetailsRepository caseDetailsRepository,
                                      @Qualifier(CachedDraftGateway.QUALIFIER) final DraftGateway draftGateway,
                                      final EventTriggerService eventTriggerService,
                                      final CaseService caseService,
                                      final UserAuthorisation userAuthorisation,
                                      final CallbackInvoker callbackInvoker,
                                      final UIDService uidService) {

        this.eventTokenService = eventTokenService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.draftGateway = draftGateway;
        this.eventTriggerService = eventTriggerService;
        this.caseService = caseService;
        this.userAuthorisation = userAuthorisation;
        this.callbackInvoker = callbackInvoker;
        this.uidService = uidService;
    }

    @Override
    public StartEventResult triggerStartForCaseType(final String caseTypeId,
                                                    final String eventId,
                                                    final Boolean ignoreWarning) {

        String uid = userAuthorisation.getUserId();

        final CaseTypeDefinition caseTypeDefinition = getCaseType(caseTypeId);

        final Map<String, JsonNode> data = Maps.newHashMap();

        CaseDetails newCaseDetails = caseService
            .createNewCaseDetails(caseTypeId, caseTypeDefinition.getJurisdictionId(), data);
        return buildStartEventTrigger(uid,
                                      caseTypeDefinition,
                                      eventId,
                                      ignoreWarning,
                                      newCaseDetails);
    }

    @Override
    public StartEventResult triggerStartForCase(final String caseReference,
                                                final String eventId,
                                                final Boolean ignoreWarning) {

        final CaseDetails caseDetails = getCaseDetails(caseReference);

        final String uid = userAuthorisation.getUserId();

        final CaseTypeDefinition caseTypeDefinition = getCaseType(caseDetails.getCaseTypeId());

        final CaseEventDefinition caseEventDefinition = getCaseEventDefinition(eventId, caseTypeDefinition);

        validateEventTrigger(() -> !eventTriggerService.isPreStateValid(caseDetails.getState(), caseEventDefinition));

        Map<String, JsonNode> defaultValueData = caseService
            .buildJsonFromCaseFieldsWithDefaultValue(caseEventDefinition.getCaseFields());
        JacksonUtils.merge(defaultValueData, caseDetails.getData());

        final String eventToken = eventTokenService.generateToken(uid,
            caseDetails,
            caseEventDefinition,
            caseTypeDefinition.getJurisdictionDefinition(),
            caseTypeDefinition);

        callbackInvoker.invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition, caseDetails, ignoreWarning);

        return buildStartEventTrigger(eventId, eventToken, caseDetails);

    }

    @Override
    public StartEventResult triggerStartForDraft(final String draftReference,
                                                 final Boolean ignoreWarning) {
        final DraftResponse draftResponse = draftGateway.get(Draft.stripId(draftReference));
        final CaseDetails caseDetails = draftGateway.getCaseDetails(Draft.stripId(draftReference));

        final String uid = userAuthorisation.getUserId();

        final CaseTypeDefinition caseTypeDefinition = getCaseType(caseDetails.getCaseTypeId());

        return buildStartEventTrigger(uid,
                                      caseTypeDefinition,
                                      draftResponse.getDocument().getEventId(),
                                      ignoreWarning,
                                      caseDetails);
    }

    private StartEventResult buildStartEventTrigger(final String uid,
                                                    final CaseTypeDefinition caseTypeDefinition,
                                                    final String eventId,
                                                    final Boolean ignoreWarning,
                                                    final CaseDetails caseDetails) {
        final CaseEventDefinition caseEventDefinition = getCaseEventDefinition(eventId, caseTypeDefinition);

        Map<String, JsonNode> defaultValueData = caseService
            .buildJsonFromCaseFieldsWithDefaultValue(caseEventDefinition.getCaseFields());
        JacksonUtils.merge(defaultValueData, caseDetails.getData());

        validateEventTrigger(() -> !eventTriggerService.isPreStateEmpty(caseEventDefinition));

        // TODO: we may need to take care of drafts that are saved for existing case so token needs to include the
        //  relevant draft payload
        final String eventToken = eventTokenService.generateToken(uid, caseEventDefinition,
            caseTypeDefinition.getJurisdictionDefinition(), caseTypeDefinition);

        callbackInvoker.invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition, caseDetails, ignoreWarning);

        return buildStartEventTrigger(eventId, eventToken, caseDetails);
    }

    private StartEventResult buildStartEventTrigger(String eventId, String eventToken, CaseDetails caseDetails) {
        final StartEventResult startEventResult = new StartEventResult();
        startEventResult.setCaseDetails(caseDetails);
        startEventResult.setToken(eventToken);
        startEventResult.setEventId(eventId);
        return startEventResult;
    }

    private CaseDetails getCaseDetails(String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        return caseDetailsRepository.findByReference(caseReference).orElseThrow(
            () -> new CaseNotFoundException(caseReference));
    }

    private CaseEventDefinition getCaseEventDefinition(String eventId, CaseTypeDefinition caseTypeDefinition) {
        final CaseEventDefinition caseEventDefinition = eventTriggerService.findCaseEvent(caseTypeDefinition, eventId);
        if (caseEventDefinition == null) {
            throw new ResourceNotFoundException("Cannot find event " + eventId + " for case type " + caseTypeDefinition
                .getId());
        }
        return caseEventDefinition;
    }

    private CaseTypeDefinition getCaseType(String caseTypeId) {
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            throw new ResourceNotFoundException("Cannot find case type definition for " + caseTypeId);
        }
        return caseTypeDefinition;
    }

    private void validateEventTrigger(Supplier<Boolean> validationOperation) {
        if (validationOperation.get()) {
            throw new ValidationException("The case status did not qualify for the event");
        }
    }

}
