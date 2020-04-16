package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.casedetails.CachedCaseDetailsRepository;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.domain.service.common.EventTriggerService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Set;
import java.util.function.Supplier;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_CREATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_UPDATE;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_STATE_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_CASE_TYPE_FOUND;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.NO_EVENT_FOUND;

@Service
@Qualifier(AuthorisedGetEventTriggerOperation.QUALIFIER)
public class AuthorisedGetEventTriggerOperation implements GetEventTriggerOperation {

    public static final String QUALIFIER = "authorised";
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseDetailsRepository caseDetailsRepository;
    private final CaseAccessService caseAccessService;
    private final GetEventTriggerOperation getEventTriggerOperation;
    private final AccessControlService accessControlService;
    private final EventTriggerService eventTriggerService;
    private final DraftGateway draftGateway;

    @Autowired
    public AuthorisedGetEventTriggerOperation(@Qualifier("default") final GetEventTriggerOperation getEventTriggerOperation,
                                              @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                              @Qualifier(CachedCaseDetailsRepository.QUALIFIER) final CaseDetailsRepository caseDetailsRepository,
                                              CaseAccessService caseAccessService,
                                              final AccessControlService accessControlService,
                                              final EventTriggerService eventTriggerService,
                                              @Qualifier(CachedDraftGateway.QUALIFIER) final DraftGateway draftGateway) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseAccessService = caseAccessService;
        this.getEventTriggerOperation = getEventTriggerOperation;
        this.accessControlService = accessControlService;
        this.eventTriggerService = eventTriggerService;
        this.draftGateway = draftGateway;
    }

    @Override
    public CaseEventTrigger executeForCaseType(String caseTypeId, String eventTriggerId, Boolean ignoreWarning) {
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);

        Set<String> userRoles = caseAccessService.getCaseCreationRoles();

        verifyRequiredAccessExistsForCaseType(eventTriggerId, caseTypeDefinition, userRoles);

        CaseEventTrigger caseEventTrigger = filterCaseFieldsByCreateAccess(caseTypeDefinition,
            userRoles,
            getEventTriggerOperation.executeForCaseType(caseTypeId,
                eventTriggerId,
                ignoreWarning));

        return accessControlService.updateCollectionDisplayContextParameterByAccess(caseEventTrigger, userRoles);
    }

    @Override
    public CaseEventTrigger executeForCase(String caseReference,
                                           String eventTriggerId,
                                           Boolean ignoreWarning) {
        final CaseDetails caseDetails = getCaseDetails(caseReference);
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId());
        final CaseEvent eventTrigger = getEventTrigger(eventTriggerId, caseTypeDefinition);

        validateEventTrigger(() -> !eventTriggerService.isPreStateValid(caseDetails.getState(), eventTrigger));

        Set<String> userRoles = Sets.union(caseAccessService.getUserRoles(), caseAccessService.getCaseRoles(caseDetails.getId()));

        verifyMandatoryAccessForCase(eventTriggerId, caseDetails, caseTypeDefinition, userRoles);

        CaseEventTrigger caseEventTrigger = filterUpsertAccessForCase(caseTypeDefinition, userRoles, getEventTriggerOperation.executeForCase(caseReference,
                                                                                                      eventTriggerId,
                                                                                                      ignoreWarning));
        return accessControlService.updateCollectionDisplayContextParameterByAccess(caseEventTrigger, userRoles);
    }

    @Override
    public CaseEventTrigger executeForDraft(String draftReference, Boolean ignoreWarning) {
        final DraftResponse draftResponse = draftGateway.get(Draft.stripId(draftReference));
        final CaseDetails caseDetails = draftGateway.getCaseDetails(Draft.stripId(draftReference));
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId());

        Set<String> userRoles = caseAccessService.getUserRoles();

        verifyRequiredAccessExistsForCaseType(draftResponse.getDocument().getEventTriggerId(), caseTypeDefinition, userRoles);

        CaseEventTrigger caseEventTrigger = filterCaseFieldsByCreateAccess(caseTypeDefinition, userRoles, getEventTriggerOperation.executeForDraft(draftReference,
                                                                                                            ignoreWarning));
        return accessControlService.updateCollectionDisplayContextParameterByAccess(caseEventTrigger, userRoles);
    }

    private CaseDetails getCaseDetails(String caseReference) {
        CaseDetails caseDetails = null;
        try {
            caseDetails = caseDetailsRepository.findByReference(caseReference)
                .orElseThrow(() -> new ResourceNotFoundException("No case exist with id=" + caseReference));
        } catch (NumberFormatException nfe) {
            throw new BadRequestException("Case reference is not valid");
        }
        return caseDetails;
    }

    private void verifyRequiredAccessExistsForCaseType(String eventTriggerId, CaseTypeDefinition caseTypeDefinition, Set<String> userRoles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition,
                                                                userRoles,
                                                                CAN_READ)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition,
                                                                userRoles,
                                                                CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }
        if (!accessControlService.canAccessCaseEventWithCriteria(eventTriggerId,
                                                                 caseTypeDefinition.getEvents(),
                                                                 userRoles,
                                                                 CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_EVENT_FOUND);
        }
    }

    private void verifyMandatoryAccessForCase(String eventTriggerId, CaseDetails caseDetails, CaseTypeDefinition caseTypeDefinition, Set<String> userRoles) {
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition,
                                                                userRoles,
                                                                CAN_READ)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }
        if (!accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition,
                                                                userRoles,
                                                                CAN_UPDATE)) {
            throw new ResourceNotFoundException(NO_CASE_TYPE_FOUND);
        }
        if (!accessControlService.canAccessCaseEventWithCriteria(eventTriggerId,
                                                                 caseTypeDefinition.getEvents(),
                                                                 userRoles,
                                                                 CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_EVENT_FOUND);
        }
        if (!accessControlService.canAccessCaseStateWithCriteria(caseDetails.getState(),
            caseTypeDefinition,
                                                                 userRoles,
                                                                 CAN_UPDATE)) {
            throw new ResourceNotFoundException(NO_CASE_STATE_FOUND);
        }
    }

    private CaseEventTrigger filterCaseFieldsByCreateAccess(CaseTypeDefinition caseTypeDefinition, Set<String> userRoles, CaseEventTrigger caseEventTrigger) {
        return accessControlService.filterCaseViewFieldsByAccess(
            caseEventTrigger,
            caseTypeDefinition.getCaseFields(),
            userRoles,
            CAN_CREATE);
    }

    private CaseEventTrigger filterUpsertAccessForCase(CaseTypeDefinition caseTypeDefinition, Set<String> userRoles, CaseEventTrigger caseEventTrigger) {
        return accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
            caseEventTrigger,
            caseTypeDefinition.getCaseFields(),
            userRoles,
            CAN_UPDATE);
    }

    private CaseEvent getEventTrigger(String eventTriggerId, CaseTypeDefinition caseTypeDefinition) {
        final CaseEvent eventTrigger = eventTriggerService.findCaseEvent(caseTypeDefinition, eventTriggerId);
        if (eventTrigger == null) {
            throw new ResourceNotFoundException("Cannot find event " + eventTriggerId + " for case type " + caseTypeDefinition.getId());
        }
        return eventTrigger;
    }

    private void validateEventTrigger(Supplier<Boolean> validationOperation) {
        if (validationOperation.get()) {
            throw new ValidationException("The case status did not qualify for the event");
        }
    }
}
