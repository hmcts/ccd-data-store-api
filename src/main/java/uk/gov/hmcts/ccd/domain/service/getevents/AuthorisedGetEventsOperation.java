package uk.gov.hmcts.ccd.domain.service.getevents;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.domain.service.common.CaseAccessService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import static java.util.Collections.singletonList;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
@Qualifier("authorised")
public class AuthorisedGetEventsOperation implements GetEventsOperation {

    private final GetEventsOperation getEventsOperation;
    private final AccessControlService accessControlService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final CaseAccessService caseAccessService;

    public AuthorisedGetEventsOperation(@Qualifier("classified") GetEventsOperation getEventsOperation,
                                        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                            CaseDefinitionRepository caseDefinitionRepository,
                                        AccessControlService accessControlService,
                                        CaseAccessService caseAccessService) {

        this.getEventsOperation = getEventsOperation;
        this.accessControlService = accessControlService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.caseAccessService = caseAccessService;
    }

    @Override
    public List<AuditEvent> getEvents(CaseDetails caseDetails) {
        final List<AuditEvent> events = getEventsOperation.getEvents(caseDetails);
        return secureEvents(caseDetails.getCaseTypeId(), caseDetails.getId(), events);
    }

    @Override
    public List<AuditEvent> getEvents(String jurisdiction, String caseTypeId, String caseReference) {
        List<AuditEvent> auditEvents = getEventsOperation.getEvents(jurisdiction, caseTypeId, caseReference);
        return secureEvents(caseTypeId, auditEvents.get(0).getCaseDataId(),  auditEvents);
    }

    @Override
    public List<AuditEvent> getEvents(String caseReference) {
        return secureEvents(getEventsOperation.getEvents(caseReference));
    }

    @Override
    public Optional<AuditEvent> getEvent(String jurisdiction, String caseTypeId, Long eventId) {
        return getEventsOperation.getEvent(jurisdiction, caseTypeId, eventId).flatMap(
            event -> secureEvent(caseTypeId, event));
    }

    private Optional<AuditEvent> secureEvent(String caseTypeId, AuditEvent event) {
        return secureEvents(caseTypeId, event.getCaseDataId(), singletonList(event)).stream().findFirst();
    }

    private List<AuditEvent> secureEvents(List<AuditEvent> events) {
        if (events == null || events.size() == 0) {
            return Lists.newArrayList();
        }
        return secureEvents(events.get(0).getCaseTypeId(), events.get(0).getCaseDataId(),  events);
    }

    private List<AuditEvent> secureEvents(String caseTypeId, String caseId, List<AuditEvent> events) {
        if (null == events) {
            return Lists.newArrayList();
        }

        final CaseTypeDefinition caseTypeDefinition = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseTypeDefinition == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }

        Set<String> accessRoles = caseAccessService.getAccessRoles(caseId);
        if (accessRoles == null || accessRoles.isEmpty()) {
            throw new ValidationException("Cannot find user roles or case roles for the case ID " + caseId);
        }

        return verifyReadAccess(events, accessRoles, caseTypeDefinition);
    }

    private List<AuditEvent> verifyReadAccess(List<AuditEvent> events, Set<String> userRoles, CaseTypeDefinition caseTypeDefinition) {

        if (!accessControlService.canAccessCaseTypeWithCriteria(caseTypeDefinition,
            userRoles,
            CAN_READ)) {
            return Lists.newArrayList();
        }

        return accessControlService.filterCaseAuditEventsByReadAccess(events,
            caseTypeDefinition.getEvents(),
            userRoles);
    }

}
