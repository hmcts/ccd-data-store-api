package uk.gov.hmcts.ccd.domain.service.validate;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.callbacks.EventTokenProperties;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;
import uk.gov.hmcts.ccd.domain.service.callbacks.EventTokenService;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.ConditionalFieldRestorer;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
import uk.gov.hmcts.ccd.domain.service.createevent.MidEventCallback;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Maps.newHashMap;
import static java.lang.String.format;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_STATE_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_EVENT_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_FIELD_FOUND;

@Service
@Slf4j
@Qualifier(AuthorisedValidateCaseFieldsOperation.QUALIFIER)
public class AuthorisedValidateCaseFieldsOperation implements ValidateCaseFieldsOperation {
    public static final String QUALIFIER = "authorised";

    private final AccessControlService accessControlService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseAccessService caseAccessService;
    private final ValidateCaseFieldsOperation validateCaseFieldsOperation;
    private final ConditionalFieldRestorer conditionalFieldRestorer;
    private final ApplicationParams applicationParams;
    private final MidEventCallback midEventCallback;
    private final GetCaseOperation getCaseOperation;
    private final EventTokenService eventTokenService;
    private final CaseDetailsRepository caseDetailsRepository;
    private final EventTriggerService eventTriggerService;
    private final UserRepository userRepository;
    private final PersistenceStrategyResolver persistenceStrategyResolver;

    public AuthorisedValidateCaseFieldsOperation(AccessControlService accessControlService,
                                                 @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                                 CaseDefinitionRepository caseDefinitionRepository,
                                                 CaseAccessService caseAccessService,
                                                 @Qualifier(DefaultValidateCaseFieldsOperation.QUALIFIER)
                                                 ValidateCaseFieldsOperation validateCaseFieldsOperation,
                                                 ConditionalFieldRestorer conditionalFieldRestorer,
                                                 ApplicationParams applicationParams,
                                                 MidEventCallback midEventCallback,
                                                 @Qualifier("default") GetCaseOperation getCaseOperation,
                                                 EventTokenService eventTokenService,
                                                 @Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                                                 CaseDetailsRepository caseDetailsRepository,
                                                 EventTriggerService eventTriggerService,
                                                 @Qualifier(CachedUserRepository.QUALIFIER)
                                                 UserRepository userRepository,
                                                 PersistenceStrategyResolver persistenceStrategyResolver) {
        this.accessControlService = accessControlService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseAccessService = caseAccessService;
        this.validateCaseFieldsOperation = validateCaseFieldsOperation;
        this.conditionalFieldRestorer = conditionalFieldRestorer;
        this.applicationParams = applicationParams;
        this.midEventCallback = midEventCallback;
        this.getCaseOperation = getCaseOperation;
        this.eventTokenService = eventTokenService;
        this.caseDetailsRepository = caseDetailsRepository;
        this.eventTriggerService = eventTriggerService;
        this.userRepository = userRepository;
        this.persistenceStrategyResolver = persistenceStrategyResolver;
    }

    @Override
    public Map<String, JsonNode> validateCaseDetails(OperationContext operationContext) {
        validateCaseFieldsOperation.validateCaseDetails(operationContext);

        CaseDataContent content = operationContext.content();

        resolveCaseReferenceFromEventToken(content);

        final String pageId = operationContext.pageId();
        if (StringUtils.isNotBlank(pageId)) {
            verifyEventIsPresent(content);
        }

        final String effectiveCaseTypeId = StringUtils.isNotBlank(pageId)
            ? verifyEventAccessBeforeMidEvent(operationContext, content)
            : operationContext.caseTypeId();

        callMidEventCallback(effectiveCaseTypeId, content, pageId);

        if (applicationParams.getExcludeVerifyAccessCaseTypesForValidate()
            .stream()
            .anyMatch(c -> c.equalsIgnoreCase(effectiveCaseTypeId))) {
            content.setData(JacksonUtils.convertValueInDataField(content.getData()));
            return content.getData();
        }

        Set<AccessProfile> accessProfiles = determineAccessProfiles(effectiveCaseTypeId, content.getCaseReference());
        CaseTypeDefinition caseTypeDefinition = getCaseDefinitionType(effectiveCaseTypeId);
        Map<String, JsonNode> validatedData = captureValidatedData(content);

        verifyReadAccess(caseTypeDefinition, content, accessProfiles);

        Map<String, JsonNode> restoredData = restoreConditionalFieldsData(
            caseTypeDefinition,
            content.getData(),
            validatedData,
            accessProfiles
        );

        content.setData(JacksonUtils.convertValueInDataField(restoredData));
        return content.getData();
    }

    private void verifyEventIsPresent(CaseDataContent content) {
        Event event = content.getEvent();
        if (event == null || StringUtils.isEmpty(event.getEventId())) {
            throw new ResourceNotFoundException(NO_EVENT_FOUND);
        }
    }

    private String verifyEventAccessBeforeMidEvent(OperationContext operationContext, CaseDataContent content) {
        String urlCaseTypeId = operationContext.caseTypeId();

        if (StringUtils.isEmpty(content.getCaseReference())) {
            if (hasUnresolvedCaseIdInEventToken(content)) {
                throw new ResourceNotFoundException("Cannot find matching start trigger");
            }
            CaseTypeDefinition caseTypeDefinition = getCaseDefinitionType(urlCaseTypeId);
            verifyCreateCaseEventAccess(content, caseTypeDefinition);
            return urlCaseTypeId;
        }

        return verifyUpdateCaseEventAccess(operationContext, content);
    }

    private boolean hasUnresolvedCaseIdInEventToken(CaseDataContent content) {
        if (StringUtils.isEmpty(content.getToken()) || StringUtils.isNotEmpty(content.getCaseReference())) {
            return false;
        }
        try {
            EventTokenProperties eventTokenProperties = eventTokenService.parseToken(content.getToken());
            return StringUtils.isNotEmpty(eventTokenProperties.getCaseId());
        } catch (RuntimeException e) {
            log.debug("Unable to determine case id from event token: {}", e.getMessage());
            return false;
        }
    }

    private void resolveCaseReferenceFromEventToken(CaseDataContent content) {
        if (StringUtils.isNotEmpty(content.getCaseReference()) || StringUtils.isEmpty(content.getToken())) {
            return;
        }
        try {
            EventTokenProperties eventTokenProperties = eventTokenService.parseToken(content.getToken());
            if (StringUtils.isNotEmpty(eventTokenProperties.getCaseId())) {
                content.setCaseReference(toCaseReference(eventTokenProperties.getCaseId()));
            }
        } catch (RuntimeException e) {
            log.debug("Unable to resolve case reference from event token: {}", e.getMessage());
        }
    }

    private String toCaseReference(String caseIdFromToken) {
        if (StringUtils.isEmpty(caseIdFromToken)) {
            return caseIdFromToken;
        }
        try {
            if (getCaseOperation.execute(caseIdFromToken).isPresent()) {
                return caseIdFromToken;
            }
        } catch (RuntimeException e) {
            log.debug("Unable to load case by reference from event token: {}", e.getMessage());
        }
        try {
            CaseDetails caseDetails = caseDetailsRepository.findById(Long.valueOf(caseIdFromToken));
            if (caseDetails != null && StringUtils.isNotEmpty(caseDetails.getReferenceAsString())) {
                return caseDetails.getReferenceAsString();
            }
        } catch (NumberFormatException e) {
            log.debug("Case id from event token is not a numeric entity id: {}", caseIdFromToken);
        } catch (RuntimeException e) {
            log.debug("Unable to load case by entity id from event token: {}", e.getMessage());
        }
        return caseIdFromToken;
    }

    private void verifyCreateCaseEventAccess(CaseDataContent content, CaseTypeDefinition caseTypeDefinition) {
        Set<AccessProfile> userRoles = caseAccessService.getCaseCreationRoles(caseTypeDefinition.getId());
        if (userRoles == null || userRoles.isEmpty()) {
            throw new ValidationException("Cannot find user roles for the user");
        }
        if (!accessControlService.canAccessCaseTypeWithCriteria(
            caseTypeDefinition,
            userRoles,
            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }
        if (!accessControlService.canAccessCaseEventWithCriteria(
            content.getEvent().getEventId(),
            caseTypeDefinition.getEvents(),
            userRoles,
            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_EVENT_FOUND);
        }

        CaseEventDefinition caseEventDefinition = findCaseEvent(caseTypeDefinition, content.getEvent().getEventId());
        validateCreatePreState(caseEventDefinition);
        validateCreateEventToken(content, caseEventDefinition, caseTypeDefinition);
        verifyCreateCaseFieldsAccess(content, caseTypeDefinition, userRoles);
    }

    private String verifyUpdateCaseEventAccess(OperationContext operationContext, CaseDataContent content) {
        String urlCaseTypeId = operationContext.caseTypeId();
        String caseReference = content.getCaseReference();
        CaseDetails existingCaseDetails = getCaseOperation.execute(caseReference)
            .orElseThrow(() -> new ResourceNotFoundException("Case not found"));

        if (!existingCaseDetails.getCaseTypeId().equalsIgnoreCase(urlCaseTypeId)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }

        final CaseTypeDefinition caseTypeDefinition =
            getCaseDefinitionType(existingCaseDetails.getCaseTypeId());

        String caseReferenceForAccess = existingCaseDetails.getReferenceAsString();
        Set<AccessProfile> accessProfiles =
            caseAccessService.getAccessProfilesByCaseReference(caseReferenceForAccess);
        if (accessProfiles == null || accessProfiles.isEmpty()) {
            throw new ValidationException("Cannot find user roles for the user");
        }

        verifyCaseTypeAndStateAccessForUpdate(existingCaseDetails, caseTypeDefinition, accessProfiles);

        CaseEventDefinition caseEventDefinition = findCaseEvent(caseTypeDefinition, content.getEvent().getEventId());
        if (!accessControlService.canAccessCaseEventWithCriteria(
            content.getEvent().getEventId(),
            caseTypeDefinition.getEvents(),
            accessProfiles,
            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_EVENT_FOUND);
        }

        validateUpdatePreState(existingCaseDetails, caseEventDefinition);
        validateUpdateEventToken(content, existingCaseDetails, caseEventDefinition, caseTypeDefinition);

        return existingCaseDetails.getCaseTypeId();
    }

    private CaseEventDefinition findCaseEvent(CaseTypeDefinition caseTypeDefinition, String eventId) {
        CaseEventDefinition caseEventDefinition = eventTriggerService.findCaseEvent(caseTypeDefinition, eventId);
        if (caseEventDefinition == null) {
            throw new ValidationException(format("%s is not a known event ID for the specified case type %s",
                eventId, caseTypeDefinition.getId()));
        }
        return caseEventDefinition;
    }

    private void validateCreatePreState(CaseEventDefinition caseEventDefinition) {
        if (!eventTriggerService.isPreStateValid(null, caseEventDefinition)) {
            throw new ValidationException(format("Cannot create case because of %s has pre-states defined",
                caseEventDefinition.getId()));
        }
    }

    private void validateUpdatePreState(CaseDetails existingCaseDetails, CaseEventDefinition caseEventDefinition) {
        if (!eventTriggerService.isPreStateValid(existingCaseDetails.getState(), caseEventDefinition)) {
            throw new ValidationException(format(
                "Pre-state condition is not valid for case with state: %s; and event trigger: %s",
                existingCaseDetails.getState(),
                caseEventDefinition.getId()));
        }
    }

    private void validateCreateEventToken(CaseDataContent content,
                                          CaseEventDefinition caseEventDefinition,
                                          CaseTypeDefinition caseTypeDefinition) {
        eventTokenService.validateToken(
            content.getToken(),
            userRepository.getUserId(),
            caseEventDefinition,
            caseTypeDefinition.getJurisdictionDefinition(),
            caseTypeDefinition);
    }

    private void validateUpdateEventToken(CaseDataContent content,
                                          CaseDetails existingCaseDetails,
                                          CaseEventDefinition caseEventDefinition,
                                          CaseTypeDefinition caseTypeDefinition) {
        eventTokenService.validateToken(
            content.getToken(),
            userRepository.getUserId(),
            existingCaseDetails,
            caseEventDefinition,
            caseTypeDefinition.getJurisdictionDefinition(),
            caseTypeDefinition,
            persistenceStrategyResolver.isDecentralised(existingCaseDetails));
    }

    private void verifyCreateCaseFieldsAccess(CaseDataContent content,
                                              CaseTypeDefinition caseTypeDefinition,
                                              Set<AccessProfile> accessProfiles) {
        if (content.getData() == null) {
            return;
        }
        if (!accessControlService.canAccessCaseFieldsWithCriteria(
            JacksonUtils.convertValueJsonNode(content.getData()),
            caseTypeDefinition.getCaseFieldDefinitions(),
            accessProfiles,
            CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_FIELD_FOUND);
        }
    }

    private void verifyCaseTypeAndStateAccessForUpdate(CaseDetails existingCaseDetails,
                                                       CaseTypeDefinition caseTypeDefinition,
                                                       Set<AccessProfile> accessProfiles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition, accessProfiles, CAN_UPDATE)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }
        if (!accessControlService.canAccessCaseStateWithCriteria(
            existingCaseDetails.getState(),
            caseTypeDefinition,
            accessProfiles,
            CAN_UPDATE)) {
            throw new ResourceNotFoundException(NO_CASE_STATE_FOUND);
        }
    }

    private void callMidEventCallback(String caseTypeId, CaseDataContent content, String pageId) {
        content.setData(midEventCallback.invoke(caseTypeId, content, pageId));
    }

    private Set<AccessProfile> determineAccessProfiles(String caseTypeId, String caseReference) {
        return StringUtils.isNotEmpty(caseReference)
            ? caseAccessService.getAccessProfilesByCaseReference(caseReference)
            : caseAccessService.getCaseCreationRoles(caseTypeId);
    }

    private Map<String, JsonNode> captureValidatedData(CaseDataContent content) {
        return JacksonUtils.convertValue(
            JacksonUtils.convertValueJsonNode(content.getData())
        );
    }

    private Map<String, JsonNode> restoreConditionalFieldsData(
        CaseTypeDefinition caseTypeDefinition,
        Map<String, JsonNode> filteredData,
        Map<String, JsonNode> validatedData,
        Set<AccessProfile> accessProfiles
    ) {
        return conditionalFieldRestorer.restoreConditionalFields(
            caseTypeDefinition,
            filteredData,
            validatedData,
            accessProfiles
        );
    }

    @Override
    public void validateData(Map<String, JsonNode> data, CaseTypeDefinition caseTypeDefinition,
                             CaseDataContent content) {
        validateCaseFieldsOperation.validateData(data, caseTypeDefinition, content);
    }

    private void verifyReadAccess(CaseTypeDefinition caseTypeDefinition, CaseDataContent content,
                                  Set<AccessProfile> accessProfiles) {
        if (content.getData() == null) {
            content.setData(newHashMap());
            return;
        }

        if (!accessControlService.canAccessCaseTypeWithCriteria(
            caseTypeDefinition,
            accessProfiles,
            CAN_READ)) {
            content.setData(newHashMap());
            return;
        }

        content.setData(JacksonUtils.convertValue(
            accessControlService.filterCaseFieldsByAccess(
                JacksonUtils.convertValueJsonNode(content.getData()),
                caseTypeDefinition.getCaseFieldDefinitions(),
                accessProfiles,
                CAN_READ,
                false)));
    }

    private CaseTypeDefinition getCaseDefinitionType(String caseTypeId) {
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }
        return caseTypeDefinition;
    }
}
