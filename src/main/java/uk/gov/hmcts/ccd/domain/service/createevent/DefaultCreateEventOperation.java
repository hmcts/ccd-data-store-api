package uk.gov.hmcts.ccd.domain.service.createevent;

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
import uk.gov.hmcts.ccd.domain.model.aggregated.IDAMProperties;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.model.std.validator.EventValidator;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.*;
import uk.gov.hmcts.ccd.domain.service.stdapi.CallbackInvoker;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CallbackException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;

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
                                       final ValidateCaseFieldsOperation validateCaseFieldsOperation) {
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
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public CaseDetails createCaseEvent(final String uid,
                                       final String jurisdictionId,
                                       final String caseTypeId,
                                       final String caseReference,
                                       final Event event,
                                       final Map<String, JsonNode> data,
                                       final String token,
                                       final Boolean ignoreWarning) {
        eventValidator.validate(event);

        final CaseType caseType = findAndValidateCaseType(caseTypeId, jurisdictionId);
        final CaseEvent eventTrigger = findAndValidateCaseEvent(caseType, event);
        final CaseDetails caseDetails = lockCaseDetails(caseType, caseReference);
        final CaseDetails caseDetailsBefore = caseService.clone(caseDetails);

        eventTokenService.validateToken(token, uid, caseDetails, eventTrigger, caseType.getJurisdiction(), caseType);

        validatePreState(caseDetails, eventTrigger);
        mergeUpdatedFieldsToCaseDetails(data, caseDetails, eventTrigger, caseType);
        final Optional<String>
            newState =
            callbackInvoker.invokeAboutToSubmitCallback(eventTrigger,
                                                        caseDetailsBefore,
                                                        caseDetails,
                                                        caseType,
                                                        ignoreWarning);
        this.validateCaseFieldsOperation.validateCaseDetails(jurisdictionId, caseTypeId, event, caseDetails.getData());
        final CaseDetails savedCaseDetails = saveCaseDetails(caseDetails, eventTrigger, newState);
        saveAuditEventForCaseDetails(event, eventTrigger, savedCaseDetails, caseType);

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

    private CaseType findAndValidateCaseType(final String caseTypeId,
                                             final String jurisdictionId) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (!caseTypeService.isJurisdictionValid(jurisdictionId, caseType)) {
            throw new ResourceNotFoundException(
                String.format("Case type with id %s could not be found for jurisdiction %s", caseTypeId, jurisdictionId));
        }
        return caseType;
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

    private CaseDetails lockCaseDetails(final CaseType caseType,
                                        final String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }

        final CaseDetails caseDetails;
        try {
            caseDetails = caseDetailsRepository.lockCase(Long.valueOf(caseReference));
        } catch (NumberFormatException exception) {
            throw new ResourceNotFoundException(
                String.format("Case with reference %s could not be found for case type %s", caseReference, caseType.getId()));
        }
        if (null == caseDetails || !caseType.getId().equalsIgnoreCase(caseDetails.getCaseTypeId())) {
            throw new ResourceNotFoundException(
                String.format("Case with reference %s could not be found for case type %s", caseReference, caseType.getId()));
        }
        return caseDetails;
    }

    private CaseDetails saveCaseDetails(final CaseDetails caseDetails,
                                        final CaseEvent eventTrigger,
                                        final Optional<String> state) {
        if (!state.isPresent() && !equalsIgnoreCase(CaseState.ANY, eventTrigger.getPostState())) {
            caseDetails.setState(eventTrigger.getPostState());
        }
        return caseDetailsRepository.set(caseDetails);
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

    private void saveAuditEventForCaseDetails(final Event event,
                                              final CaseEvent eventTrigger,
                                              final CaseDetails caseDetails,
                                              final CaseType caseType) {
        final IDAMProperties user = userRepository.getUserDetails();
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

        caseAuditEventRepository.set(auditEvent);
    }

}
