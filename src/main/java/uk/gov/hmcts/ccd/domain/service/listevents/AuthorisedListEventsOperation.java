package uk.gov.hmcts.ccd.domain.service.listevents;

import com.google.common.collect.Lists;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.common.AccessControlService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.List;
import java.util.Set;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlService.CAN_READ;

@Service
@Qualifier("authorised")
public class AuthorisedListEventsOperation implements ListEventsOperation {
    private final ListEventsOperation listEventsOperation;
    private final AccessControlService accessControlService;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final UserRepository userRepository;

    public AuthorisedListEventsOperation(@Qualifier("classified") final ListEventsOperation listEventsOperation,
                                         @Qualifier(CachedCaseDefinitionRepository.QUALIFIER) final CaseDefinitionRepository caseDefinitionRepository,
                                         final AccessControlService accessControlService,
                                         @Qualifier(CachedUserRepository.QUALIFIER) final UserRepository userRepository) {

        this.listEventsOperation = listEventsOperation;
        this.accessControlService = accessControlService;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<AuditEvent> execute(CaseDetails caseDetails) {


        final List<AuditEvent> events = listEventsOperation.execute(caseDetails);

        if (null == events) {
            return Lists.newArrayList();
        }

        final CaseType caseType = caseDefinitionRepository.getCaseType(caseDetails.getCaseTypeId());
        if (caseType == null) {
            throw new ValidationException("Cannot find case type definition for  " + caseDetails.getCaseTypeId());
        }

        Set<String> userRoles = userRepository.getUserRoles();
        if (userRoles == null || userRoles.isEmpty()) {
            throw new ValidationException("Cannot find user roles for the user");
        }

        return verifyReadAccess(events, userRoles, caseType);
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
