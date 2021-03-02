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
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
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
    public AuthorisedGetEventTriggerOperation(@Qualifier("default")
                                                  final GetEventTriggerOperation getEventTriggerOperation,
                                              @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                                  final CaseDefinitionRepository caseDefinitionRepository,
                                              @Qualifier(CachedCaseDetailsRepository.QUALIFIER)
                                                  final CaseDetailsRepository caseDetailsRepository,
                                              CaseAccessService caseAccessService,
                                              final AccessControlService accessControlService,
                                              final EventTriggerService eventTriggerService,
                                              @Qualifier(CachedDraftGateway.QUALIFIER)
                                                  final DraftGateway draftGateway) {
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseDetailsRepository = caseDetailsRepository;
        this.caseAccessService = caseAccessService;
        this.getEventTriggerOperation = getEventTriggerOperation;
        this.accessControlService = accessControlService;
        this.eventTriggerService = eventTriggerService;
        this.draftGateway = draftGateway;
    }

    @Override
    public CaseUpdateViewEvent executeForCaseType(String caseTypeId, String eventId, Boolean ignoreWarning) {
        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);

        Set<String> userRoles = caseAccessService.getCaseCreationRoles();

        verifyRequiredAccessExistsForCaseType(eventId, caseTypeDefinition, userRoles);

        CaseUpdateViewEvent caseUpdateViewEvent = filterCaseFieldsByCreateAccess(caseTypeDefinition,
            userRoles,
            getEventTriggerOperation.executeForCaseType(caseTypeId,
                eventId,
                ignoreWarning));

        return accessControlService.updateCollectionDisplayContextParameterByAccess(caseUpdateViewEvent, userRoles);
    }

    @Override
    public CaseUpdateViewEvent executeForCase(String caseReference,
                                              String eventId,
                                              Boolean ignoreWarning) {
        final CaseDetails caseDetails = getCaseDetails(caseReference);
        final CaseTypeDefinition caseTypeDefinition =
            caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId());
        final CaseEventDefinition caseEventDefinition = getCaseEventDefinition(eventId, caseTypeDefinition);

        validateEventDefinition(() -> !eventTriggerService.isPreStateValid(
            caseDetails.getState(), caseEventDefinition));

        Set<String> userRoles = Sets.union(caseAccessService.getUserRoles(),
            caseAccessService.getCaseRoles(caseDetails.getId()));

        verifyMandatoryAccessForCase(eventId, caseDetails, caseTypeDefinition, userRoles);

        CaseUpdateViewEvent caseUpdateViewEvent = filterUpsertAccessForCase(caseTypeDefinition, userRoles,
            getEventTriggerOperation.executeForCase(caseReference, eventId, ignoreWarning));
        return accessControlService.updateCollectionDisplayContextParameterByAccess(caseUpdateViewEvent, userRoles);
    }

    @Override
    public CaseUpdateViewEvent executeForDraft(String draftReference, Boolean ignoreWarning) {
        final DraftResponse draftResponse = draftGateway.get(Draft.stripId(draftReference));
        final CaseDetails caseDetails = draftGateway.getCaseDetails(Draft.stripId(draftReference));
        final CaseTypeDefinition caseTypeDefinition =
            caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId());

        Set<String> userRoles = caseAccessService.getUserRoles();

        verifyRequiredAccessExistsForCaseType(draftResponse.getDocument().getEventId(), caseTypeDefinition, userRoles);

        CaseUpdateViewEvent caseUpdateViewEvent = filterCaseFieldsByCreateAccess(caseTypeDefinition,
            userRoles, getEventTriggerOperation.executeForDraft(draftReference, ignoreWarning));
        return accessControlService.updateCollectionDisplayContextParameterByAccess(caseUpdateViewEvent, userRoles);
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

    private void verifyRequiredAccessExistsForCaseType(String eventId,
                                                       CaseTypeDefinition caseTypeDefinition,
                                                       Set<String> userRoles) {
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
        if (!accessControlService.canAccessCaseEventWithCriteria(eventId,
                                                                 caseTypeDefinition.getEvents(),
                                                                 userRoles,
                                                                 CAN_CREATE)) {
            throw new ResourceNotFoundException(NO_EVENT_FOUND);
        }
    }

    private void verifyMandatoryAccessForCase(String eventId, CaseDetails caseDetails,
                                              CaseTypeDefinition caseTypeDefinition,
                                              Set<String> userRoles) {
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
        if (!accessControlService.canAccessCaseEventWithCriteria(eventId,
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

    private CaseUpdateViewEvent filterCaseFieldsByCreateAccess(CaseTypeDefinition caseTypeDefinition,
                                                               Set<String> userRoles,
                                                               CaseUpdateViewEvent caseUpdateViewEvent) {
        return accessControlService.filterCaseViewFieldsByAccess(
            caseUpdateViewEvent,
            caseTypeDefinition.getCaseFieldDefinitions(),
            userRoles,
            CAN_CREATE);
    }

    private CaseUpdateViewEvent filterUpsertAccessForCase(CaseTypeDefinition caseTypeDefinition,
                                                          Set<String> userRoles,
                                                          CaseUpdateViewEvent caseUpdateViewEvent) {
        return accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(
            caseUpdateViewEvent,
            caseTypeDefinition.getCaseFieldDefinitions(),
            userRoles,
            CAN_UPDATE);
    }

    private CaseEventDefinition getCaseEventDefinition(String eventId, CaseTypeDefinition caseTypeDefinition) {
        final CaseEventDefinition caseEventDefinition = eventTriggerService.findCaseEvent(caseTypeDefinition, eventId);
        if (caseEventDefinition == null) {
            throw new ResourceNotFoundException(
                "Cannot find event " + eventId + " for case type " + caseTypeDefinition.getId());
        }
        return caseEventDefinition;
    }

    private void validateEventDefinition(Supplier<Boolean> validationOperation) {
        if (validationOperation.get()) {
            throw new ValidationException("The case status did not qualify for the event");
        }
    }
}
