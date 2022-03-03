package uk.gov.hmcts.ccd.domain.service.createevent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
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
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CasePostStateService;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationServiceImpl;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
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

    private static final ObjectMapper MAPPER = JacksonUtils.MAPPER;

    @Inject
    public CreateCaseEventService(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                  @Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                                  final CaseDetailsRepository caseDetailsRepository,
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
                                  final GlobalSearchProcessorService globalSearchProcessorService) {
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
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CreateCaseEventResult createCaseEvent(final String caseReference, final CaseDataContent content) {

        final CaseDetails caseDetails = getCaseDetails(caseReference);
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId());
        final CaseEventDefinition caseEventDefinition = findAndValidateCaseEvent(
            caseTypeDefinition,
            content.getEvent()
        );
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
        final CaseDetails updatedCaseDetailsWithoutHashes = caseDocumentService.stripDocumentHashes(updatedCaseDetails);

        final AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = callbackInvoker.invokeAboutToSubmitCallback(
            caseEventDefinition,
            caseDetailsInDatabase,
            updatedCaseDetailsWithoutHashes,
            caseTypeDefinition,
            content.getIgnoreWarning()
        );

        final Optional<String> newState = aboutToSubmitCallbackResponse.getState();

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

        final CaseDetails savedCaseDetails = saveCaseDetails(
            caseDetailsInDatabase,
            caseDetailsAfterCallbackWithoutHashes,
            caseEventDefinition,
            newState,
            timeNow
        );
        saveAuditEventForCaseDetails(
            aboutToSubmitCallbackResponse,
            content.getEvent(),
            caseEventDefinition,
            savedCaseDetails,
            caseTypeDefinition,
            timeNow,
            oldState,
            content.getOnBehalfOfUserToken(),
            securityClassificationService.getClassificationForEvent(caseTypeDefinition,
                caseEventDefinition)
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

    public CreateCaseEventResult createCaseSystemEvent(final String caseReference, final CaseDataContent content,
                                                       final String attributePath, final String categoryId) {
        final CaseDetails caseDetails = getCaseDetails(caseReference);
        final CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("DocumentUpdated");
        caseEventDefinition.setName("Update Document Category Id");
        caseDetails.setLastModified(now());
        Map<String, JsonNode> currentData = caseDetails.getData();

        //merge new categoryId field with existing data
        if (attributePath.contains(".")) {
            List<String> paths = Arrays.asList(attributePath.split("\\."));
            List<Map<String, JsonNode>> listOfMappedData = new ArrayList<>();
            listOfMappedData.add(currentData);
            for (int i = 0; i < paths.size(); i++) {
                if (paths.get(i).contains("[")) {
                    String path = paths.get(i);
                    int idStart = path.indexOf("[");
                    String key = path.substring(0, idStart);
                    int idEnd = path.indexOf("]");
                    String id = path.substring(idStart + 1, idEnd);
                    JsonNode data = listOfMappedData.get(i).get(key);
                    Iterator<JsonNode> dataElements = data.elements();
                    while (dataElements.hasNext()) {
                        JsonNode json = dataElements.next();
                        if (json.findValue("id").asText().equals(id)) {
                            JsonNode value = json.findValue("value");
                            Iterator<Map.Entry<String, JsonNode>> map = value.fields();
                            Map<String, JsonNode> mappedData = new HashMap<>();
                            map.forEachRemaining(x -> mappedData.put(x.getKey(), x.getValue()));
                            listOfMappedData.add(mappedData);
                        }

                    }
                } else {
                    JsonNode data = listOfMappedData.get(i).get(paths.get(i));
                    Iterator<Map.Entry<String, JsonNode>> map = data.fields();
                    Map<String, JsonNode> mappedData = new HashMap<>();
                    map.forEachRemaining(x -> mappedData.put(x.getKey(), x.getValue()));
                    listOfMappedData.add(mappedData);

                }

            }
            Map<String, JsonNode> map = listOfMappedData.get(listOfMappedData.size() - 1);
            map.put("categoryId", MAPPER.convertValue(categoryId, JsonNode.class));

            for (int i = listOfMappedData.size() - 1; i > 0; i--) {
                if (paths.get(i - 1).contains("[")) {
                    String path = paths.get(i - 1);
                    int idStart = path.indexOf("[");
                    String key = path.substring(0, idStart);
                    int idEnd = path.indexOf("]");
                    String id = path.substring(idStart + 1, idEnd);
                    Map<String, JsonNode> previousData = listOfMappedData.get(i - 1);
                    Iterator<JsonNode> previousDataElements = previousData.get(key).elements();
                    List<JsonNode> updatedData = new ArrayList<>();
                    while (previousDataElements.hasNext()) {
                        JsonNode elementData = previousDataElements.next();
                        if (!elementData.get("id").asText().equals(id)) {
                            updatedData.add(elementData);
                        }
                    }
                    Map<String, JsonNode> collectionData = new HashMap<>();
                    collectionData.put("id", MAPPER.convertValue(id, JsonNode.class));
                    collectionData.put("value", MAPPER.convertValue(listOfMappedData.get(i), JsonNode.class));
                    updatedData.add(MAPPER.convertValue(collectionData, JsonNode.class));
                    listOfMappedData.get(i - 1).put(key, MAPPER.convertValue(updatedData, JsonNode.class));
                } else {
                    listOfMappedData.get(i - 1).put(paths.get(i - 1), MAPPER.convertValue(listOfMappedData.get(i),
                        JsonNode.class));
                }
            }
            currentData = listOfMappedData.get(0);
        } else if (attributePath.contains("[")) {
            int idStart = attributePath.indexOf("[");
            String key = attributePath.substring(0, idStart);
            JsonNode topLevelData = currentData.get(key);
            Iterator<JsonNode> collectionFields = topLevelData.elements();
            int idEnd = attributePath.indexOf("]");
            String id = attributePath.substring(idStart + 1, idEnd);

            List<JsonNode> updatedData = new ArrayList<>();
            while (collectionFields.hasNext()) {
                JsonNode json = collectionFields.next();
                JsonNode value = json.findValue("value");
                Iterator<Map.Entry<String, JsonNode>> dataPair = value.fields();
                Map<String, String> mappedData = new HashMap<>();
                dataPair.forEachRemaining(x -> mappedData.put(x.getKey(), x.getValue().textValue()));

                if (json.findValue("id").asText().equals(id)) {
                    mappedData.put("categoryId", categoryId);
                }
                Map<String, JsonNode> collectionData = new HashMap<>();
                collectionData.put("id", json.findValue("id"));
                collectionData.put("value", MAPPER.convertValue(mappedData, JsonNode.class));
                updatedData.add(MAPPER.convertValue(collectionData, JsonNode.class));
            }
            currentData.put(key, MAPPER.convertValue(updatedData, JsonNode.class));

        } else {
            JsonNode topLevelData = currentData.get(attributePath);
            Iterator<Map.Entry<String, JsonNode>> topLevelDataFields = topLevelData.fields();
            Map<String, String> mappedData = new HashMap<>();
            topLevelDataFields.forEachRemaining(x -> mappedData.put(x.getKey(), x.getValue().textValue()));
            mappedData.put("categoryId", categoryId);
            currentData.put(attributePath, MAPPER.convertValue(mappedData, JsonNode.class));
        }
        caseDetails.setData(currentData);

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
            content.getIgnoreWarning()
        );

        final Optional<String> newState = Optional.ofNullable(oldState);

        @SuppressWarnings("UnnecessaryLocalVariable")
        final CaseDetails caseDetailsAfterCallback = updatedCaseDetailsWithoutHashes;

        final LocalDateTime timeNow = now();

        final List<DocumentHashToken> documentHashes = caseDocumentService.extractDocumentHashToken(
            caseDetailsInDatabase.getData(),
            Optional.ofNullable(content.getData()).orElse(emptyMap()),
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
            content.getEvent(),
            caseEventDefinition,
            savedCaseDetails,
            caseTypeDefinition,
            timeNow,
            oldState,
            content.getOnBehalfOfUserToken(),
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
        return caseDetailsRepository.findByReference(caseReference)
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
                                              final SecurityClassification securityClassification) {
        final CaseStateDefinition caseStateDefinition =
            caseTypeService.findState(caseTypeDefinition, caseDetails.getState());
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
        saveUserDetails(onBehalfOfUserToken, auditEvent);

        caseAuditEventRepository.set(auditEvent);
        messageService.handleMessage(MessageContext.builder()
            .caseDetails(caseDetails)
            .caseTypeDefinition(caseTypeDefinition)
            .caseEventDefinition(caseEventDefinition)
            .oldState(oldState)
            .build());
    }

    private void saveUserDetails(String onBehalfOfUserToken, AuditEvent auditEvent) {
        boolean onBehalfOfUserTokenExists = !StringUtils.isEmpty(onBehalfOfUserToken);
        IdamUser user = onBehalfOfUserTokenExists
            ? userRepository.getUser(onBehalfOfUserToken)
            : userRepository.getUser();
        auditEvent.setUserId(user.getId());
        auditEvent.setUserLastName(user.getSurname());
        auditEvent.setUserFirstName(user.getForename());
        if (onBehalfOfUserTokenExists) {
            user = userRepository.getUser();
            auditEvent.setProxiedBy(user.getId());
            auditEvent.setProxiedByLastName(user.getSurname());
            auditEvent.setProxiedByFirstName(user.getForename());
        }
    }
}
