package uk.gov.hmcts.ccd.domain.service.aggregated;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Set;
import java.util.function.Supplier;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.*;

@Service
@Qualifier(AuthorisedGetEventTriggerOperation.QUALIFIER)
public class AuthorisedGetEventTriggerOperation implements GetEventTriggerOperation {

    public static final String QUALIFIER = "authorised";
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDetailsRepository caseDetailsRepository;
    private final UserRepository userRepository;
    private final GetEventTriggerOperation getEventTriggerOperation;
    private final AccessControlService accessControlService;
    private final EventTriggerService eventTriggerService;
    private final UIDService uidService;

    @Autowired
    public AuthorisedGetEventTriggerOperation(@Qualifier("default") final GetEventTriggerOperation getEventTriggerOperation,
                                              @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                              @Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                                              @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository,
                                              final AccessControlService accessControlService,
                                              final EventTriggerService eventTriggerService,
                                              final UIDService uidService) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.userRepository = userRepository;
        this.getEventTriggerOperation = getEventTriggerOperation;
        this.accessControlService = accessControlService;
        this.eventTriggerService = eventTriggerService;
        this.uidService = uidService;
    }

    public CaseEventTrigger executeForCaseType(String uid,
                                               String jurisdictionId,
                                               String caseTypeId,
                                               String eventTriggerId,
                                               Boolean ignoreWarning) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);

        Set<String> userRoles = getUserRoles();

        verifyRequiredAccessExistsForCaseType(eventTriggerId, caseType, userRoles);

        return filterCaseFieldsByCreateAccess(caseType, userRoles, getEventTriggerOperation.executeForCaseType(uid,
                                                           jurisdictionId,
                                                           caseTypeId,
                                                           eventTriggerId,
                                                           ignoreWarning));
    }

    public CaseEventTrigger executeForCase(String uid,
                                           String jurisdictionId,
                                           String caseTypeId,
                                           String caseReference,
                                           String eventTriggerId,
                                           Boolean ignoreWarning) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        final CaseEvent eventTrigger = getEventTrigger(caseTypeId, eventTriggerId, caseType);

        final CaseDetails caseDetails = getCaseDetails(caseReference);

        validateEventTrigger(() -> !eventTriggerService.isPreStateValid(caseDetails.getState(), eventTrigger));

        Set<String> userRoles = getUserRoles();

        verifyMandatoryAccessForCase(eventTriggerId, caseDetails, caseType, userRoles);

        return filterUpsertAccessForCase(caseType, userRoles, getEventTriggerOperation.executeForCase(uid,
                                                       jurisdictionId,
                                                       caseTypeId,
                                                       caseReference,
                                                       eventTriggerId,
                                                       ignoreWarning));
    }

    @Override
    public CaseEventTrigger executeForDraft(String uid, String jurisdictionId, String caseTypeId,
                                            String draftReference, String eventTriggerId, Boolean ignoreWarning) {
        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);

        Set<String> userRoles = getUserRoles();

        verifyRequiredAccessExistsForCaseType(eventTriggerId, caseType, userRoles);

        return filterCaseFieldsByCreateAccess(caseType, userRoles, getEventTriggerOperation.executeForDraft(uid,
            jurisdictionId,
            caseTypeId,
            draftReference,
            eventTriggerId,
            ignoreWarning));
    }

    private CaseDetails getCaseDetails(String caseReference) {
        if (!uidService.validateUID(caseReference)) {
            throw new BadRequestException("Case reference is not valid");
        }
        final CaseDetails caseDetails = caseDetailsRepository.findByReference(Long.valueOf(caseReference));
        if (caseDetails == null) {
            throw new ResourceNotFoundException("No case exist with id=" + caseReference);
        }
        return caseDetails;
    }

    private Set<String> getUserRoles() {
        Set<String> userRoles = userRepository.getUserRoles();
        if (userRoles == null) {
            throw new ValidationException("Cannot find user roles for the user");
        }
        return userRoles;
    }

    private void verifyRequiredAccessExistsForCaseType(String eventTriggerId, CaseType caseType, Set<String> userRoles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseType,
                                                                userRoles,
                                                                CAN_READ)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseType,
                                                                userRoles,
                                                                CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }
        if (!accessControlService.canAccessCaseEventWithCriteria(eventTriggerId,
                                                                 caseType.getEvents(),
                                                                 userRoles,
                                                                 CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_EVENT_FOUND);
        }
    }

    private void verifyMandatoryAccessForCase(String eventTriggerId, CaseDetails caseDetails, CaseType caseType, Set<String> userRoles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseType,
                                                                userRoles,
                                                                CAN_READ)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseType,
                                                                userRoles,
                                                                CAN_UPDATE)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }
        if (!accessControlService.canAccessCaseEventWithCriteria(eventTriggerId,
                                                                 caseType.getEvents(),
                                                                 userRoles,
                                                                 CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_EVENT_FOUND);
        }
        if (!accessControlService.canAccessCaseStateWithCriteria(caseDetails.getState(),
                                                                 caseType,
                                                                 userRoles,
                                                                 CAN_UPDATE)) {
            throw new ResourceNotFoundException(NO_CASE_STATE_FOUND);
        }
    }

    private CaseEventTrigger filterCaseFieldsByCreateAccess(CaseType caseType, Set<String> userRoles, CaseEventTrigger caseEventTrigger) {
        return accessControlService.filterCaseViewFieldsByAccess(
            caseEventTrigger,
            caseType.getCaseFields(),
            userRoles,
            CAN_CREATE);
    }

    private CaseEventTrigger filterUpsertAccessForCase(CaseType caseType, Set<String> userRoles, CaseEventTrigger caseEventTrigger) {
        return accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
            caseEventTrigger,
            caseType.getCaseFields(),
            userRoles,
            CAN_UPDATE);
    }

    private CaseEvent getEventTrigger(String caseTypeId, String eventTriggerId, CaseType caseType) {
        final CaseEvent eventTrigger = eventTriggerService.findCaseEvent(caseType, eventTriggerId);
        if (eventTrigger == null) {
            throw new ResourceNotFoundException("Cannot findCaseEvent event " + eventTriggerId + " for case type " + caseTypeId);
        }
        return eventTrigger;
    }

    private void validateEventTrigger(Supplier<Boolean> validationOperation) {
        if (validationOperation.get()) {
            throw new ValidationException("The case status did not qualify for the event");
        }
    }

}
