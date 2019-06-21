package uk.gov.hmcts.ccd.domain.service.createevent;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.model.std.validator.EventValidator;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityClassificationService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.stdapi.AboutToSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.domain.service.util.TransactionHelper;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;

@Service
@Qualifier("default")
public class DefaultCreateEventOperation implements CreateEventOperation {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultCreateEventOperation.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    private final UserRepository userRepository;
    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseAuditEventRepository caseAuditEventRepository;
    private final EventTriggerService eventTriggerService;
    private final EventValidator eventValidator;
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
    private final TransactionHelper transactionHelper;

    @Inject
    public DefaultCreateEventOperation(@Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                       @Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                                       @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                       final CaseAuditEventRepository caseAuditEventRepository,
                                       final EventTriggerService eventTriggerService,
                                       final EventTokenService eventTokenService,
                                       final CaseService caseService,
                                       final CaseDataService caseDataService,
                                       final CaseTypeService caseTypeService,
                                       final EventValidator eventValidator,
                                       final CaseSanitiser caseSanitiser,
                                       final CallbackInvoker callbackInvoker,
                                       final UIDService uidService,
                                       final SecurityClassificationService securityClassificationService,
                                       final ValidateCaseFieldsOperation validateCaseFieldsOperation,
                                       final UserAuthorisation userAuthorisation,
                                       final TransactionHelper transactionHelper) {
        this.userRepository = userRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseAuditEventRepository = caseAuditEventRepository;
        this.eventTriggerService = eventTriggerService;
        this.caseService = caseService;
        this.caseDataService = caseDataService;
        this.caseTypeService = caseTypeService;
        this.eventValidator = eventValidator;
        this.eventTokenService = eventTokenService;
        this.caseSanitiser = caseSanitiser;
        this.callbackInvoker = callbackInvoker;
        this.uidService = uidService;
        this.securityClassificationService = securityClassificationService;
        this.validateCaseFieldsOperation = validateCaseFieldsOperation;
        this.userAuthorisation = userAuthorisation;
        this.transactionHelper = transactionHelper;
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public CaseDetails createCaseEvent(final String caseReference,
                                       final CaseDataContent content) {
        LOG.info("createCaseEvent with caseReference:{}, and data :{}", caseReference);
        eventValidator.validate(content.getEvent());

        final CaseDetails caseDetails = lockCaseDetails(caseReference);
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId());
        final CaseEvent eventTrigger = findAndValidateCaseEvent(caseType, content.getEvent());
        final CaseDetails caseDetailsBefore = caseService.clone(caseDetails);
        String uid = userAuthorisation.getUserId();

        eventTokenService.validateToken(content.getToken(), uid, caseDetails, eventTrigger, caseType.getJurisdiction(), caseType);

        validatePreState(caseDetails, eventTrigger);
        mergeUpdatedFieldsToCaseDetails(content.getData(), caseDetails, eventTrigger, caseType);
        AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = callbackInvoker.invokeAboutToSubmitCallback(eventTrigger,
            caseDetailsBefore,
            caseDetails,
            caseType,
            content.getIgnoreWarning());

        final Optional<String>
            newState = aboutToSubmitCallbackResponse.getState();

        validateCaseFieldsOperation.validateData(caseDetails.getData(), caseType);
        LOG.info("before save of createCaseEvent with caseReference:{}, and data :{}", caseReference);
        final CaseDetails savedCaseDetails = saveCaseDetails(caseDetails, eventTrigger, newState);
        LOG.info("after save of createCaseEvent with caseReference:{}, and data :{}", caseReference);
        saveAuditEventForCaseDetails(aboutToSubmitCallbackResponse, content.getEvent(), eventTrigger, savedCaseDetails, caseType);

        if (!isBlank(eventTrigger.getCallBackURLSubmittedEvent())) {
            try { // make a call back
                final ResponseEntity<AfterSubmitCallbackResponse> callBackResponse = callbackInvoker
                    .invokeSubmittedCallback(eventTrigger, caseDetailsBefore, savedCaseDetails);
                caseDetails.setAfterSubmitCallbackResponseEntity(callBackResponse);
            } catch (CallbackException ex) {
                LOG.warn("Submitted callback failed", ex);
                // Exception occurred, e.g. call back service is unavailable,
                caseDetails.setIncompleteCallbackResponse();
            }
        }
        return caseDetails;
    }

    private CaseEvent findAndValidateCaseEvent(final CaseType caseType,
                                               final Event event) {
        final CaseEvent eventTrigger = eventTriggerService.findCaseEvent(caseType, event.getEventId());
        if (eventTrigger == null) {
            throw new ValidationException(String.format("%s is not a known event ID for the specified case type %s", event.getEventId(), caseType.getId()));
        }
        return eventTrigger;
    }

    private void validatePreState(final CaseDetails caseDetails,
                                  final CaseEvent caseEvent) {
        if (!eventTriggerService.isPreStateValid(caseDetails.getState(), caseEvent)) {
            throw new ValidationException(
                String.format(
                    "Pre-state condition is not valid for case with state: %s; and event trigger: %s",
                    caseDetails.getState(),
                    caseEvent.getId()
                )
            );
        }
    }

    private CaseDetails lockCaseDetails(final String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        final CaseDetails caseDetails;
        try {
            caseDetails = caseDetailsRepository.lockCase(Long.valueOf(caseReference));
        } catch (NumberFormatException exception) {
            throw new ResourceNotFoundException(
                String.format("Case with reference %s could not be found", caseReference));
        }
        if (null == caseDetails) {
            throw new ResourceNotFoundException(
                String.format("Case with reference %s could not be found", caseReference));
        }
        return caseDetails;
    }

    private CaseDetails saveCaseDetails(final CaseDetails caseDetails,
                                        final CaseEvent eventTrigger,
                                        final Optional<String> state) {
        if (!state.isPresent() && !equalsIgnoreCase(CaseState.ANY, eventTrigger.getPostState())) {
            caseDetails.setState(eventTrigger.getPostState());
        }
        return transactionHelper.withNewTransaction(() -> caseDetailsRepository.set(caseDetails));
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
        caseDetails.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        if (!StringUtils.equalsAnyIgnoreCase(CaseState.ANY, caseEvent.getPostState())) {
            caseDetails.setState(caseEvent.getPostState());
        }
    }

    private void saveAuditEventForCaseDetails(final AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse,
                                              final Event event,
                                              final CaseEvent eventTrigger,
                                              final CaseDetails caseDetails,
                                              final CaseType caseType) {
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
        auditEvent.setCreatedDate(LocalDateTime.now(ZoneOffset.UTC));
        auditEvent.setSecurityClassification(securityClassificationService.getClassificationForEvent(caseType, eventTrigger));
        auditEvent.setDataClassification(caseDetails.getDataClassification());
        auditEvent.setSignificantItem(aboutToSubmitCallbackResponse.getSignificantItem());

        caseAuditEventRepository.set(auditEvent);
    }

}
