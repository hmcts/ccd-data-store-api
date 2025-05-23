package uk.gov.hmcts.ccd.domain.service.startevent;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import uk.gov.hmcts.ccd.domain.service.casedeletion.TimeToLiveService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
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
import java.util.function.BooleanSupplier;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Optional.ofNullable;


@Slf4j
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
    private final CaseDataService caseDataService;
    private final TimeToLiveService timeToLiveService;

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
                                      final UIDService uidService,
                                      final CaseDataService caseDataService,
                                      final TimeToLiveService timeToLiveService) {

        this.eventTokenService = eventTokenService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.draftGateway = draftGateway;
        this.eventTriggerService = eventTriggerService;
        this.caseService = caseService;
        this.userAuthorisation = userAuthorisation;
        this.callbackInvoker = callbackInvoker;
        this.uidService = uidService;
        this.caseDataService = caseDataService;
        this.timeToLiveService = timeToLiveService;
    }

    @Transactional
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

    @Transactional
    @Override
    public StartEventResult triggerStartForCase(final String caseReference,
                                                final String eventId,
                                                final Boolean ignoreWarning) {

        final CaseDetails caseDetails = getCaseDetails(caseReference);

        final String uid = userAuthorisation.getUserId();

        final CaseTypeDefinition caseTypeDefinition = getCaseType(caseDetails.getCaseTypeId());

        final CaseEventDefinition caseEventDefinition = getCaseEventDefinition(eventId, caseTypeDefinition);

        validateEventTrigger(() ->
                !eventTriggerService.isPreStateValid(caseDetails.getState(), caseEventDefinition),
                caseReference, eventId, caseDetails.getState());

        mergeDefaultValueAndNullifyByDefault(caseEventDefinition, caseDetails, caseTypeDefinition);

        // update TTL in data
        Map<String, JsonNode> caseDataWithTtl = timeToLiveService.updateCaseDetailsWithTTL(
            caseDetails.getData(), caseEventDefinition, caseTypeDefinition
        );
        caseDetails.setData(caseDataWithTtl);

        // update TTL in data classification
        Map<String, JsonNode> caseDataClassificationWithTtl = timeToLiveService.updateCaseDataClassificationWithTTL(
            caseDetails.getData(), caseDetails.getDataClassification(), caseEventDefinition, caseTypeDefinition
        );
        caseDetails.setDataClassification(caseDataClassificationWithTtl);

        final String eventToken = eventTokenService.generateToken(uid,
            caseDetails,
            caseEventDefinition,
            caseTypeDefinition.getJurisdictionDefinition(),
            caseTypeDefinition);

        callbackInvoker.invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition, caseDetails, ignoreWarning);

        return buildStartEventTrigger(eventId, eventToken, caseDetails);
    }

    private void mergeDefaultValueAndNullifyByDefault(CaseEventDefinition caseEventDefinition,
                                                      CaseDetails caseDetails,
                                                      CaseTypeDefinition caseTypeDefinition) {
        Map<String, JsonNode> defaultValueData = caseService
            .buildJsonFromCaseFieldsWithDefaultValue(caseEventDefinition.getCaseFields());
        if (!defaultValueData.isEmpty()) {
            mergeDataAndClassificationForNewFields(defaultValueData, caseDetails, caseTypeDefinition);
        }

        Map<String, JsonNode> nullifyByDefaultData = caseService
            .buildJsonFromCaseFieldsWithNullifyByDefault(caseTypeDefinition, caseEventDefinition.getCaseFields());
        if (!nullifyByDefaultData.isEmpty()) {
            mergeDataAndClassificationForNewFields(nullifyByDefaultData, caseDetails, caseTypeDefinition);
        }
    }

    @Transactional
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

        mergeDefaultValueAndNullifyByDefault(caseEventDefinition, caseDetails, caseTypeDefinition);

        validateEventTrigger(() ->
                !eventTriggerService.isPreStateEmpty(caseEventDefinition),
                caseDetails.getReferenceAsString(), eventId, caseDetails.getState());

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

    private void validateEventTrigger(BooleanSupplier validationOperation, String reference,
                                                                            String eventId, String preStateId) {
        if (validationOperation.getAsBoolean()) {
            log.error("eventId={} cannot be triggered on case={} with currentStatus={}",
                        reference, eventId, preStateId);
            throw new ValidationException("The case status did not qualify for the event");
        }
    }

    private void mergeDataAndClassificationForNewFields(Map<String, JsonNode> defaultValueData,
                                                        CaseDetails caseDetails,
                                                        CaseTypeDefinition caseTypeDefinition) {
        JacksonUtils.merge(defaultValueData, caseDetails.getData());
        deduceDataClassificationForNewFields(caseTypeDefinition, caseDetails);
    }

    private void deduceDataClassificationForNewFields(CaseTypeDefinition caseTypeDefinition, CaseDetails caseDetails) {
        Map<String, JsonNode> defaultSecurityClassifications = caseDataService.getDefaultSecurityClassifications(
            caseTypeDefinition,
            caseDetails.getData(),
            ofNullable(caseDetails.getDataClassification()).orElse(
                newHashMap()));
        caseDetails.setDataClassification(defaultSecurityClassifications);
    }

}
