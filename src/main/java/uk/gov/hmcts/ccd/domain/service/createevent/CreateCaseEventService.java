package uk.gov.hmcts.ccd.domain.service.createevent;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.processor.FieldProcessorService;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateCaseEventService {

    private static final ObjectMapper mapper = new ObjectMapper();

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
    private final SecurityClassificationService securityClassificationService;
    private final ValidateCaseFieldsOperation validateCaseFieldsOperation;
    private final UserAuthorisation userAuthorisation;
    private final FieldProcessorService fieldProcessorService;
    private final Clock clock;

    @Inject
    public CreateCaseEventService(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                  @Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                                  @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                  final CaseAuditEventRepository caseAuditEventRepository,
                                  final EventTriggerService eventTriggerService,
                                  final EventTokenService eventTokenService,
                                  final CaseService caseService,
                                  final CaseDataService caseDataService,
                                  final CaseTypeService caseTypeService,
                                  final CaseSanitiser caseSanitiser,
                                  final CallbackInvoker callbackInvoker,
                                  final UIDService uidService,
                                  final SecurityClassificationService securityClassificationService,
                                  final ValidateCaseFieldsOperation validateCaseFieldsOperation,
                                  final UserAuthorisation userAuthorisation,
                                  final FieldProcessorService fieldProcessorService,
                                  @Qualifier("utcClock") final Clock clock) {
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
        this.clock = clock;
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CreateCaseEventResult createCaseEvent(String caseReference, CaseDataContent content) {

        final CaseDetails caseDetails = getCaseDetails(caseReference);
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId());
        final CaseEvent eventTrigger = findAndValidateCaseEvent(caseType, content.getEvent());
        final CaseDetails caseDetailsBefore = caseService.clone(caseDetails);
        String uid = userAuthorisation.getUserId();

        eventTokenService.validateToken(content.getToken(), uid, caseDetails, eventTrigger, caseType.getJurisdiction(), caseType);

        validatePreState(caseDetails, eventTrigger);
        content.setData(fieldProcessorService.processData(content.getData(), caseType, eventTrigger));
        mergeUpdatedFieldsToCaseDetails(content.getData(), caseDetails, eventTrigger, caseType);
        AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = callbackInvoker.invokeAboutToSubmitCallback(eventTrigger,
            caseDetailsBefore,
            caseDetails,
            caseType,
            content.getIgnoreWarning());

        final Optional<String>
            newState = aboutToSubmitCallbackResponse.getState();

        validateCaseFieldsOperation.validateData(caseDetails.getData(), caseType);
        LocalDateTime timeNow = now();
        final CaseDetails savedCaseDetails = saveCaseDetails(caseDetailsBefore, caseDetails, eventTrigger, newState, timeNow);
        saveAuditEventForCaseDetails(aboutToSubmitCallbackResponse, content.getEvent(), eventTrigger, savedCaseDetails, caseType, timeNow);

        return CreateCaseEventResult.caseEventWith()
            .caseDetailsBefore(caseDetailsBefore)
            .savedCaseDetails(savedCaseDetails)
            .eventTrigger(eventTrigger)
            .build();
    }

    private CaseEvent findAndValidateCaseEvent(final CaseType caseType,
                                               final Event event) {
        final CaseEvent eventTrigger = eventTriggerService.findCaseEvent(caseType, event.getEventId());
        if (eventTrigger == null) {
            throw new ValidationException(format("%s is not a known event ID for the specified case type %s", event.getEventId(), caseType.getId()));
        }
        return eventTrigger;
    }

    private void validatePreState(final CaseDetails caseDetails,
                                  final CaseEvent caseEvent) {
        if (!eventTriggerService.isPreStateValid(caseDetails.getState(), caseEvent)) {
            throw new ValidationException(
                format(
                    "Pre-state condition is not valid for case with state: %s; and event trigger: %s",
                    caseDetails.getState(),
                    caseEvent.getId()
                )
            );
        }
    }

    private CaseDetails getCaseDetails(final String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }
        return caseDetailsRepository.findByReference(caseReference)
            .orElseThrow(() -> new ResourceNotFoundException(format("Case with reference %s could not be found", caseReference)));
    }

    private CaseDetails saveCaseDetails(CaseDetails caseDetailsBefore, final CaseDetails caseDetails,
                                        final CaseEvent eventTrigger,
                                        final Optional<String> state, LocalDateTime timeNow) {
        if (!state.isPresent() && !equalsIgnoreCase(CaseState.ANY, eventTrigger.getPostState())) {
            caseDetails.setState(eventTrigger.getPostState());
        }
        if (!caseDetails.getState().equalsIgnoreCase(caseDetailsBefore.getState())) {
            caseDetails.setLastStateModifiedDate(timeNow);
        }
        return caseDetailsRepository.set(caseDetails);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private void mergeUpdatedFieldsToCaseDetails(final Map<String, JsonNode> data,
                                                 final CaseDetails caseDetails,
                                                 final CaseEvent caseEvent,
                                                 final CaseType caseType) {
        if (null != data) {
            final Map<String, JsonNode> sanitisedData = caseSanitiser.sanitise(caseType, data);
            for (Map.Entry<String, JsonNode> field : sanitisedData.entrySet()) {
                caseDetails.getData().put(field.getKey(), field.getValue());
            }
            caseDetails.setDataClassification(caseDataService.getDefaultSecurityClassifications(caseType, caseDetails.getData(), caseDetails.getDataClassification()));
        }
        caseDetails.setLastModified(now());
        if (!StringUtils.equalsAnyIgnoreCase(CaseState.ANY, caseEvent.getPostState())) {
            caseDetails.setState(caseEvent.getPostState());
        }
    }

    private void saveAuditEventForCaseDetails(final AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse,
                                              final Event event,
                                              final CaseEvent eventTrigger,
                                              final CaseDetails caseDetails,
                                              final CaseType caseType, LocalDateTime timeNow) {
        final IdamUser user = userRepository.getUser();
        final CaseState caseState = caseTypeService.findState(caseType, caseDetails.getState());
        final AuditEvent auditEvent = new AuditEvent();

        auditEvent.setEventId(event.getEventId());
        auditEvent.setEventName(eventTrigger.getName());
        auditEvent.setSummary(event.getSummary());
        auditEvent.setDescription(event.getDescription());
        auditEvent.setCaseDataId(caseDetails.getId());
        auditEvent.setData(caseDetails.getData());
        auditEvent.setStateId(caseDetails.getState());
        auditEvent.setStateName(caseState.getName());
        auditEvent.setCaseTypeId(caseType.getId());
        auditEvent.setCaseTypeVersion(caseType.getVersion().getNumber());
        auditEvent.setUserId(user.getId());
        auditEvent.setUserLastName(user.getSurname());
        auditEvent.setUserFirstName(user.getForename());
        auditEvent.setCreatedDate(timeNow);
        auditEvent.setSecurityClassification(securityClassificationService.getClassificationForEvent(caseType, eventTrigger));
        auditEvent.setDataClassification(caseDetails.getDataClassification());
        auditEvent.setSignificantItem(aboutToSubmitCallbackResponse.getSignificantItem());

        caseAuditEventRepository.set(auditEvent);
    }
}
