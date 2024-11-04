package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.DefaultCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.casedeletion.TimeToLiveService;
import uk.gov.hmcts.ccd.domain.service.caselinking.CaseLinkService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessGroupUtils;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CasePostStateService;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentService;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentTimestampService;
import uk.gov.hmcts.ccd.domain.service.jsonpath.CaseDetailsJsonParser;
import uk.gov.hmcts.ccd.domain.service.message.MessageContext;
import uk.gov.hmcts.ccd.domain.service.message.MessageService;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;
import uk.gov.hmcts.ccd.domain.service.processor.GlobalSearchProcessorService;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.domain.service.validate.CaseDataIssueLogger;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import javax.inject.Inject;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Collections.emptyMap;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

@Service
public class CreateCaseEventService {

    private final UserRepository userRepository;
    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseDetailsRepository defaultCaseDetailsRepository;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseAuditEventRepository caseAuditEventRepository;
    private final EventTriggerService eventTriggerService;
    private final EventTokenService eventTokenService;
    private final CaseService caseService;
    private final CaseDataService caseDataService;
    private final CaseTypeService caseTypeService;
    private final CaseSanitiser caseSanitiser;
    private final CallbackInvoker callbackInvoker;
    private final UIDService uidService;
    private final SecurityClassificationServiceImpl securityClassificationService;
    private final ValidateCaseFieldsOperation validateCaseFieldsOperation;
    private final UserAuthorisation userAuthorisation;
    private final FieldProcessorService fieldProcessorService;
    private final Clock clock;
    private final CasePostStateService casePostStateService;
    private final MessageService messageService;
    private final CaseDocumentService caseDocumentService;
    private final CaseDataIssueLogger caseDataIssueLogger;
    private final GlobalSearchProcessorService globalSearchProcessorService;
    private final CaseDetailsJsonParser caseDetailsJsonParser;
    private final TimeToLiveService timeToLiveService;
    private final CaseLinkService caseLinkService;
    private final CaseDocumentTimestampService caseDocumentTimestampService;
    private final GetCaseOperation getCaseOperation;
    private final POCCreateCaseEventService pocCreateCaseEventService;
    private final ApplicationParams applicationParams;
    private final CaseAccessGroupUtils caseAccessGroupUtils;

    @Inject
    public CreateCaseEventService(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                  @Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                                  final CaseDetailsRepository caseDetailsRepository,
                                  @Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
                                  final CaseDetailsRepository defaultCaseDetailsRepository,
                                  @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                  final CaseDefinitionRepository caseDefinitionRepository,
                                  final CaseAuditEventRepository caseAuditEventRepository,
                                  final EventTriggerService eventTriggerService,
                                  final EventTokenService eventTokenService,
                                  final CaseService caseService,
                                  final CaseDataService caseDataService,
                                  final CaseTypeService caseTypeService,
                                  final CaseSanitiser caseSanitiser,
                                  final CallbackInvoker callbackInvoker,
                                  final UIDService uidService,
                                  final SecurityClassificationServiceImpl securityClassificationService,
                                  final ValidateCaseFieldsOperation validateCaseFieldsOperation,
                                  final UserAuthorisation userAuthorisation,
                                  final FieldProcessorService fieldProcessorService,
                                  final CasePostStateService casePostStateService,
                                  @Qualifier("utcClock") final Clock clock,
                                  @Qualifier("caseEventMessageService") final MessageService messageService,
                                  final CaseDocumentService caseDocumentService,
                                  final CaseDataIssueLogger caseDataIssueLogger,
                                  final GlobalSearchProcessorService globalSearchProcessorService,
                                  final CaseDetailsJsonParser jsonPathParser,
                                  final TimeToLiveService timeToLiveService,
                                  final CaseLinkService caseLinkService,
                                  final ApplicationParams applicationParams,
                                  final CaseAccessGroupUtils caseAccessGroupUtils,
                                  final CaseDocumentTimestampService caseDocumentTimestampService,
                                  @Qualifier("default") final GetCaseOperation getCaseOperation,
                                  final POCCreateCaseEventService pocCreateCaseEventService) {

        this.userRepository = userRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseAuditEventRepository = caseAuditEventRepository;
        this.eventTriggerService = eventTriggerService;
        this.caseService = caseService;
        this.caseDataService = caseDataService;
        this.caseTypeService = caseTypeService;
        this.eventTokenService = eventTokenService;
        this.caseSanitiser = caseSanitiser;
        this.callbackInvoker = callbackInvoker;
        this.uidService = uidService;
        this.securityClassificationService = securityClassificationService;
        this.validateCaseFieldsOperation = validateCaseFieldsOperation;
        this.userAuthorisation = userAuthorisation;
        this.fieldProcessorService = fieldProcessorService;
        this.casePostStateService = casePostStateService;
        this.clock = clock;
        this.messageService = messageService;
        this.caseDocumentService = caseDocumentService;
        this.caseDataIssueLogger = caseDataIssueLogger;
        this.globalSearchProcessorService = globalSearchProcessorService;
        this.caseDetailsJsonParser = jsonPathParser;
        this.timeToLiveService = timeToLiveService;
        this.caseLinkService = caseLinkService;
        this.defaultCaseDetailsRepository = defaultCaseDetailsRepository;
        this.applicationParams = applicationParams;
        this.caseAccessGroupUtils = caseAccessGroupUtils;
        this.caseDocumentTimestampService = caseDocumentTimestampService;
        this.getCaseOperation = getCaseOperation;
        this.pocCreateCaseEventService = pocCreateCaseEventService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CreateCaseEventResult createCaseEvent(final String caseReference, final CaseDataContent content) {

        //TODO get case details from service
        final CaseDetails caseDetails = getCaseDetails(caseReference);
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId());
        final CaseEventDefinition caseEventDefinition = findAndValidateCaseEvent(
            caseTypeDefinition,
            content.getEvent()
        );

        updateCaseDetailsWithTtlIncrement(caseDetails, caseTypeDefinition, caseEventDefinition);

        final CaseDetails caseDetailsInDatabase = caseService.clone(caseDetails);
        final String uid = userAuthorisation.getUserId();

        eventTokenService.validateToken(content.getToken(),
            uid,
            caseDetails,
            caseEventDefinition,
            caseTypeDefinition.getJurisdictionDefinition(),
            caseTypeDefinition);

        validatePreState(caseDetails, caseEventDefinition);

        content.setData(fieldProcessorService.processData(content.getData(), caseTypeDefinition, caseEventDefinition));
        final String oldState = caseDetails.getState();

        // Logic start from here to attach document with case ID
        final CaseDetails updatedCaseDetails = mergeUpdatedFieldsToCaseDetails(
            content.getData(),
            caseDetails,
            caseEventDefinition,
            caseTypeDefinition
        );

        timeToLiveService.validateTTLChangeAgainstTTLGuard(content.getData(), caseDetailsInDatabase.getData());

        final CaseDetails updatedCaseDetailsWithoutHashes = caseDocumentService.stripDocumentHashes(updatedCaseDetails);

        final AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = callbackInvoker.invokeAboutToSubmitCallback(
            caseEventDefinition,
            caseDetailsInDatabase,
            updatedCaseDetailsWithoutHashes,
            caseTypeDefinition,
            content.getIgnoreWarning()
        );

        final Optional<String> newState = aboutToSubmitCallbackResponse.getState();

        // add upload timestamp
        caseDocumentTimestampService.addUploadTimestamps(updatedCaseDetailsWithoutHashes, caseDetailsInDatabase);

        @SuppressWarnings("UnnecessaryLocalVariable")
        final CaseDetails caseDetailsAfterCallback = updatedCaseDetailsWithoutHashes;

        validateCaseFieldsOperation.validateData(caseDetailsAfterCallback.getData(), caseTypeDefinition, content);
        final LocalDateTime timeNow = now();

        final List<DocumentHashToken> documentHashes = caseDocumentService.extractDocumentHashToken(
            caseDetailsInDatabase.getData(),
            Optional.ofNullable(content.getData()).orElse(emptyMap()),
            Optional.ofNullable(caseDetailsAfterCallback.getData()).orElse(emptyMap())
        );

        final CaseDetails caseDetailsAfterCallbackWithoutHashes = caseDocumentService.stripDocumentHashes(
            caseDetailsAfterCallback
        );

        if (this.applicationParams.getCaseGroupAccessFilteringEnabled()) {
            caseAccessGroupUtils.updateCaseAccessGroupsInCaseDetails(caseDetailsAfterCallbackWithoutHashes,
                caseTypeDefinition);
        }

        caseDetailsAfterCallbackWithoutHashes
            .setResolvedTTL(timeToLiveService.getUpdatedResolvedTTL(caseDetailsAfterCallback.getData()));

        boolean isPocCaseType = this.applicationParams.getPocCaseTypes().contains(caseDetailsInDatabase.getCaseTypeId());

        CaseDetails finalCaseDetails = isPocCaseType
                ? pocCreateCaseEventService.saveAuditEventForCaseDetails(content.getEvent(), caseEventDefinition,
                caseDetailsAfterCallbackWithoutHashes, caseTypeDefinition, caseDetailsInDatabase)
                : saveCaseDetails(caseDetailsInDatabase, caseDetailsAfterCallbackWithoutHashes, caseEventDefinition,
                newState, timeNow);

        caseLinkService.updateCaseLinks(finalCaseDetails, caseTypeDefinition.getCaseFieldDefinitions());

        if (!isPocCaseType) {
            saveAuditEventForCaseDetails(
                    aboutToSubmitCallbackResponse,
                    content.getEvent(),
                    caseEventDefinition,
                    finalCaseDetails,
                    caseTypeDefinition,
                    timeNow,
                    oldState,
                    content.getOnBehalfOfUserToken(),
                    content.getOnBehalfOfId(),
                    securityClassificationService.getClassificationForEvent(caseTypeDefinition, caseEventDefinition)
            );
        }

        caseDocumentService.attachCaseDocuments(
            caseDetails.getReferenceAsString(),
            caseDetails.getCaseTypeId(),
            caseDetails.getJurisdiction(),
            documentHashes
        );

        return CreateCaseEventResult.caseEventWith()
            .caseDetailsBefore(caseDetailsInDatabase)
            .savedCaseDetails(finalCaseDetails)
            .eventTrigger(caseEventDefinition)
            .build();
    }

    public CreateCaseEventResult createCaseSystemEvent(final String caseReference,
                                                       final String attributePath,
                                                       final String categoryId,
                                                       Event event) {
        final CaseDetails caseDetails = defaultCaseDetailsRepository.findByReference(caseReference)
            .orElseThrow(() ->
            new ResourceNotFoundException(format("Case with reference %s could not be found", caseReference)));

        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("DocumentUpdated");
        caseEventDefinition.setName("Update Document Category Id");
        caseDetails.setLastModified(now());

        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId());

        final CaseDetails caseDetailsInDatabase = caseService.clone(caseDetails);

        final String oldState = caseDetails.getState();

        // Logic start from here to attach document with case ID

        final CaseDetails updatedCaseDetailsWithoutHashes = caseDocumentService.stripDocumentHashes(caseDetails);

        final AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = callbackInvoker.invokeAboutToSubmitCallback(
            caseEventDefinition,
            caseDetailsInDatabase,
            updatedCaseDetailsWithoutHashes,
            caseTypeDefinition,
            false
        );

        final Optional<String> newState = Optional.ofNullable(oldState);

        caseDocumentTimestampService.addUploadTimestamps(updatedCaseDetailsWithoutHashes, caseDetailsInDatabase);

        @SuppressWarnings("UnnecessaryLocalVariable")
        final CaseDetails caseDetailsAfterCallback = updatedCaseDetailsWithoutHashes;
        caseDetailsJsonParser.updateCaseDocumentData(attributePath, categoryId, caseDetailsAfterCallback);

        final LocalDateTime timeNow = now();

        final List<DocumentHashToken> documentHashes = caseDocumentService.extractDocumentHashToken(
            caseDetailsInDatabase.getData(),
            caseDetailsInDatabase.getData(),
            Optional.ofNullable(caseDetailsAfterCallback.getData()).orElse(emptyMap())
        );

        final CaseDetails caseDetailsAfterCallbackWithoutHashes = caseDocumentService.stripDocumentHashes(
            caseDetailsAfterCallback
        );

        final CaseDetails savedCaseDetails = saveCaseDetails(
            caseDetailsInDatabase,
            caseDetailsAfterCallbackWithoutHashes,
            caseEventDefinition,
            newState,
            timeNow
        );
        saveAuditEventForCaseDetails(
            aboutToSubmitCallbackResponse,
            event,
            caseEventDefinition,
            savedCaseDetails,
            caseTypeDefinition,
            timeNow,
            oldState,
            null,
            null,
            SecurityClassification.PUBLIC
        );

        caseDocumentService.attachCaseDocuments(
            caseDetails.getReferenceAsString(),
            caseDetails.getCaseTypeId(),
            caseDetails.getJurisdiction(),
            documentHashes
        );

        return CreateCaseEventResult.caseEventWith()
            .caseDetailsBefore(caseDetailsInDatabase)
            .savedCaseDetails(savedCaseDetails)
            .eventTrigger(caseEventDefinition)
            .build();
    }

    private CaseEventDefinition findAndValidateCaseEvent(final CaseTypeDefinition caseTypeDefinition,
                                                         final Event event) {
        final CaseEventDefinition caseEventDefinition =
            eventTriggerService.findCaseEvent(caseTypeDefinition, event.getEventId());
        if (caseEventDefinition == null) {
            throw new ValidationException(format("%s is not a known event ID for the specified case type %s",
                event.getEventId(), caseTypeDefinition.getId()));
        }
        return caseEventDefinition;
    }

    private void validatePreState(final CaseDetails caseDetails,
                                  final CaseEventDefinition caseEventDefinition) {
        if (!eventTriggerService.isPreStateValid(caseDetails.getState(), caseEventDefinition)) {
            throw new ValidationException(
                format(
                    "Pre-state condition is not valid for case with state: %s; and event trigger: %s",
                    caseDetails.getState(),
                    caseEventDefinition.getId()
                )
            );
        }
    }

    private CaseDetails getCaseDetails(final String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }
        // TODO need to handle this
        // return caseDetailsRepository.findByReference(caseReference)
        //    .orElseThrow(() ->
        //        new ResourceNotFoundException(format("Case with reference %s could not be found", caseReference)));
       return getCaseOperation.execute(caseReference)
                .orElseThrow(() ->
                    new ResourceNotFoundException(format("Case with reference %s could not be found", caseReference)));
    }

    private CaseDetails saveCaseDetails(final CaseDetails caseDetailsBefore,
                                        final CaseDetails caseDetails,
                                        final CaseEventDefinition caseEventDefinition,
                                        final Optional<String> state,
                                        final LocalDateTime timeNow) {

        if (state.isEmpty()) {
            updateCaseState(caseDetails, caseEventDefinition);
        }
        if (!caseDetails.getState().equalsIgnoreCase(caseDetailsBefore.getState())) {
            caseDetails.setLastStateModifiedDate(timeNow);
        }

        caseDataIssueLogger.logAnyDataIssuesIn(caseDetailsBefore, caseDetails);
        return caseDetailsRepository.set(caseDetails);
    }

    private void updateCaseState(CaseDetails caseDetails, CaseEventDefinition caseEventDefinition) {
        final String postState = casePostStateService.evaluateCaseState(caseEventDefinition, caseDetails);
        if (shouldChangeState(postState)) {
            caseDetails.setState(postState);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    CaseDetails mergeUpdatedFieldsToCaseDetails(final Map<String, JsonNode> data,
                                                final CaseDetails caseDetails,
                                                final CaseEventDefinition caseEventDefinition,
                                                final CaseTypeDefinition caseTypeDefinition) {

        return Optional.ofNullable(data)
            .map(nonNullData -> {
                CaseDetails clonedCaseDetails = caseService.clone(caseDetails);

                final Map<String, JsonNode> sanitisedData = caseSanitiser.sanitise(caseTypeDefinition, nonNullData);
                final Map<String, JsonNode> caseData = new HashMap<>(Optional.ofNullable(caseDetails.getData())
                    .orElse(emptyMap()));
                caseData.putAll(sanitisedData);
                clonedCaseDetails.setData(globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition,
                    caseData));

                final Map<String, JsonNode> dataClassifications = caseDataService.getDefaultSecurityClassifications(
                    caseTypeDefinition,
                    clonedCaseDetails.getData(),
                    clonedCaseDetails.getDataClassification()
                );

                clonedCaseDetails.setDataClassification(dataClassifications);
                clonedCaseDetails.setLastModified(now());
                updateCaseState(clonedCaseDetails, caseEventDefinition);

                return clonedCaseDetails;
            })
            .orElseGet(() -> {
                CaseDetails clonedCaseDetails = caseService.clone(caseDetails);
                clonedCaseDetails.setLastModified(now());
                updateCaseState(clonedCaseDetails, caseEventDefinition);

                return clonedCaseDetails;
            });
    }

    private boolean shouldChangeState(final String postState) {
        return !equalsIgnoreCase(CaseStateDefinition.ANY, postState);
    }

    private void saveAuditEventForCaseDetails(final AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse,
                                              final Event event,
                                              final CaseEventDefinition caseEventDefinition,
                                              final CaseDetails caseDetails,
                                              final CaseTypeDefinition caseTypeDefinition,
                                              final LocalDateTime timeNow,
                                              final String oldState,
                                              final String onBehalfOfUserToken,
                                              final String onBehalfOfId,
                                              final SecurityClassification securityClassification) {
        final CaseStateDefinition caseStateDefinition = caseTypeService.findState(caseTypeDefinition,
            caseDetails.getState());
        final AuditEvent auditEvent = new AuditEvent();
        auditEvent.setEventId(event.getEventId());
        auditEvent.setEventName(caseEventDefinition.getName());
        auditEvent.setSummary(event.getSummary());
        auditEvent.setDescription(event.getDescription());
        auditEvent.setCaseDataId(caseDetails.getId());
        auditEvent.setData(caseDetails.getData());
        auditEvent.setStateId(caseDetails.getState());
        auditEvent.setStateName(caseStateDefinition.getName());
        auditEvent.setCaseTypeId(caseTypeDefinition.getId());
        auditEvent.setCaseTypeVersion(caseTypeDefinition.getVersion().getNumber());
        auditEvent.setCreatedDate(timeNow);
        auditEvent.setSecurityClassification(securityClassification);
        auditEvent.setDataClassification(caseDetails.getDataClassification());
        auditEvent.setSignificantItem(aboutToSubmitCallbackResponse.getSignificantItem());
        saveUserDetails(onBehalfOfUserToken, onBehalfOfId, auditEvent);

        caseAuditEventRepository.set(auditEvent);
        messageService.handleMessage(MessageContext.builder()
            .caseDetails(caseDetails)
            .caseTypeDefinition(caseTypeDefinition)
            .caseEventDefinition(caseEventDefinition)
            .oldState(oldState)
            .build());
    }

    private void saveUserDetails(String onBehalfOfUserToken, String onBehalfOfId, AuditEvent auditEvent) {
        boolean onBehalfOfUserTokenExists = !StringUtils.isEmpty(onBehalfOfUserToken);
        boolean onBehalfOfIdExists = !StringUtils.isEmpty(onBehalfOfId);
        IdamUser user = onBehalfOfUserTokenExists
            ? userRepository.getUser(onBehalfOfUserToken)
            : onBehalfOfIdExists ? userRepository.getUserByUserId(onBehalfOfId) : userRepository.getUser();
        auditEvent.setUserId(user.getId());
        auditEvent.setUserLastName(user.getSurname());
        auditEvent.setUserFirstName(user.getForename());
        if (onBehalfOfUserTokenExists || onBehalfOfIdExists) {
            user = userRepository.getUser();
            auditEvent.setProxiedBy(user.getId());
            auditEvent.setProxiedByLastName(user.getSurname());
            auditEvent.setProxiedByFirstName(user.getForename());
        }
    }

    private void updateCaseDetailsWithTtlIncrement(CaseDetails caseDetails,
                                                   CaseTypeDefinition caseTypeDefinition,
                                                   CaseEventDefinition caseEventDefinition) {

        if (timeToLiveService.isCaseTypeUsingTTL(caseTypeDefinition)) {

            // update TTL in data
            var caseDataWithTtl = timeToLiveService.updateCaseDetailsWithTTL(
                caseDetails.getData(), caseEventDefinition, caseTypeDefinition
            );
            caseDetails.setData(caseDataWithTtl);
            // update TTL in data classification
            var caseDataClassificationWithTtl = timeToLiveService.updateCaseDataClassificationWithTTL(
                caseDetails.getData(), caseDetails.getDataClassification(), caseEventDefinition, caseTypeDefinition
            );
            caseDetails.setDataClassification(caseDataClassificationWithTtl);

        }
    }
}
