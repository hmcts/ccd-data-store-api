package uk.gov.hmcts.ccd.domain.service.getevents;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.caseaccess.CachedCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import static java.util.Collections.singletonList;
import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
@Qualifier("authorised")
public class AuthorisedGetEventsOperation implements GetEventsOperation {

    private final GetEventsOperation getEventsOperation;
    private final AccessControlService accessControlService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final UserRepository userRepository;
    private final CaseUserRepository caseUserRepository;

    public AuthorisedGetEventsOperation(@Qualifier("classified") GetEventsOperation getEventsOperation,
                                        @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                            CaseDefinitionRepository caseDefinitionRepository,
                                        AccessControlService accessControlService,
                                        @Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                        @Qualifier(CachedCaseUserRepository.QUALIFIER)  CaseUserRepository caseUserRepository) {

        this.getEventsOperation = getEventsOperation;
        this.accessControlService = accessControlService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.userRepository = userRepository;
        this.caseUserRepository = caseUserRepository;
    }

    @Override
    public List<AuditEvent> getEvents(CaseDetails caseDetails) {

        final List<AuditEvent> events = getEventsOperation.getEvents(caseDetails);


        return secureEvents(caseDetails.getCaseTypeId(), getAccessRoles(caseDetails.getId()), events);
    }

    @Override
    public List<AuditEvent> getEvents(String jurisdiction, String caseTypeId, String caseReference) {
        List<AuditEvent> auditEvents = getEventsOperation.getEvents(jurisdiction, caseTypeId, caseReference);
        Set<String> accessRoles = auditEvents != null && auditEvents.size() > 0
                                    ? getAccessRoles(auditEvents.get(0).getCaseDataId())
                                    : userRepository.getUserRoles();
        return secureEvents(caseTypeId, accessRoles,  auditEvents);
    }

    @Override
    public Optional<AuditEvent> getEvent(String jurisdiction, String caseTypeId, Long eventId) {
        return getEventsOperation.getEvent(jurisdiction, caseTypeId, eventId).flatMap(
            event -> secureEvent(caseTypeId, event));
    }

    private Optional<AuditEvent> secureEvent(String caseTypeId, AuditEvent event) {
        return secureEvents(caseTypeId, getAccessRoles(event.getCaseDataId()), singletonList(event)).stream().findFirst();
    }

    private List<AuditEvent> secureEvents(String caseTypeId, Set<String> accessRoles, List<AuditEvent> events) {
        if (null == events) {
            return Lists.newArrayList();
        }

        final CaseType caseType = caseDefinitionRepository.getCaseType(caseTypeId);
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseTypeId);
        }

        if (accessRoles == null || accessRoles.isEmpty()) {
            throw new ValidationException("Cannot find user roles for the user");
        }

        return verifyReadAccess(events, accessRoles, caseType);
    }

    private Set<String> getAccessRoles(String caseId) {
        return Sets.union(userRepository.getUserRoles(),
            caseUserRepository
                .findCaseRoles(Long.valueOf(caseId), userRepository.getUserId())
                .stream()
                .collect(Collectors.toSet()));
    }

    private List<AuditEvent> verifyReadAccess(List<AuditEvent> events, Set<String> userRoles, CaseType caseType) {

        if (!accessControlService.canAccessCaseTypeWithCriteria(caseType,
            userRoles,
            CAN_READ)) {
            return Lists.newArrayList();
        }

        return accessControlService.filterCaseAuditEventsByReadAccess(events,
            caseType.getEvents(),
            userRoles);
    }

}
