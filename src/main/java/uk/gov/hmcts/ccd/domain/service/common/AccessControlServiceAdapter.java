package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewActionableEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignmentFilteringResult;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.DefaultCaseDataAccessControl;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentService;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentsFilteringService;

import static uk.gov.hmcts.ccd.domain.service.common.AccessControlServiceAdapter.QUALIFIER;


@Service
@Qualifier(QUALIFIER)
@Primary
public class AccessControlServiceAdapter implements AccessControlService {


    public static final String QUALIFIER = "ACCESS_CONTROL_ADAPTER";
    private RoleAssignmentService roleAssignmentService;
    private DefaultCaseDataAccessControl defaultCaseDataAccessControl;
    private AccessControlService accessControlService;
    private final RoleAssignmentsFilteringService roleAssignmentsFilteringService;

    @Autowired
    public AccessControlServiceAdapter(final RoleAssignmentService roleAssignmentService,
                                       final DefaultCaseDataAccessControl defaultCaseDataAccessControl,
                                       final @Qualifier(AccessControlServiceImpl.QUALIFIER)
                                           AccessControlService accessControlService,
                                       final RoleAssignmentsFilteringService roleAssignmentsFilteringService) {
        this.roleAssignmentService = roleAssignmentService;
        this.defaultCaseDataAccessControl = defaultCaseDataAccessControl;
        this.accessControlService = accessControlService;
        this.roleAssignmentsFilteringService = roleAssignmentsFilteringService;
    }

    @Override
    public boolean canAccessCaseTypeWithCriteria(CaseTypeDefinition caseType,
                                                 Set<String> userRoles,
                                                 Predicate<AccessControlList> criteria) {
        RoleAssignments roleAssignments = roleAssignmentService.getRoleAssignments("userid");
                RoleAssignmentFilteringResult filteringResults = roleAssignmentsFilteringService
            .filter(roleAssignments, caseType);
        List<AccessProfile> accessProfiles = defaultCaseDataAccessControl.applyAccessControl(filteringResults, caseType);

        return accessControlService.canAccessCaseTypeWithCriteria(caseType,
            userRoles,
            criteria);
    }

    @Override
    public boolean canAccessCaseStateWithCriteria(String caseState,
                                                  CaseTypeDefinition caseType,
                                                  Set<String> userRoles,
                                                  Predicate<AccessControlList> criteria) {
        return accessControlService.canAccessCaseStateWithCriteria(caseState,
            caseType,
            userRoles,
            criteria);
    }

    @Override
    public boolean canAccessCaseEventWithCriteria(String eventId,
                                                  List<CaseEventDefinition> caseEventDefinitions,
                                                  Set<String> userRoles,
                                                  Predicate<AccessControlList> criteria) {
        return accessControlService.canAccessCaseEventWithCriteria(eventId,
            caseEventDefinitions,
            userRoles,
            criteria);
    }

    @Override
    public boolean canAccessCaseFieldsWithCriteria(JsonNode caseFields,
                                                   List<CaseFieldDefinition> caseFieldDefinitions,
                                                   Set<String> userRoles,
                                                   Predicate<AccessControlList> criteria) {
        return accessControlService.canAccessCaseFieldsWithCriteria(caseFields,
            caseFieldDefinitions,
            userRoles,
            criteria);
    }

    @Override
    public boolean canAccessCaseViewFieldWithCriteria(CommonField caseViewField,
                                                      Set<String> userRoles,
                                                      Predicate<AccessControlList> criteria) {
        return accessControlService.canAccessCaseViewFieldWithCriteria(caseViewField,
            userRoles,
            criteria);
    }

    @Override
    public boolean canAccessCaseFieldsForUpsert(JsonNode newData,
                                                JsonNode existingData,
                                                List<CaseFieldDefinition> caseFieldDefinitions,
                                                Set<String> userRoles) {
        return accessControlService.canAccessCaseFieldsForUpsert(newData,
            existingData,
            caseFieldDefinitions,
            userRoles);
    }

    @Override
    public CaseUpdateViewEvent setReadOnlyOnCaseViewFieldsIfNoAccess(CaseUpdateViewEvent caseEventTrigger,
                                                                     List<CaseFieldDefinition> caseFieldDefinitions,
                                                                     Set<String> userRoles,
                                                                     Predicate<AccessControlList> access) {
        return accessControlService.setReadOnlyOnCaseViewFieldsIfNoAccess(caseEventTrigger,
            caseFieldDefinitions,
            userRoles,
            access);
    }

    @Override
    public CaseUpdateViewEvent updateCollectionDisplayContextParameterByAccess(CaseUpdateViewEvent caseEventTrigger,
                                                                               Set<String> userRoles) {
        return accessControlService.updateCollectionDisplayContextParameterByAccess(caseEventTrigger,
            userRoles);
    }

    @Override
    public JsonNode filterCaseFieldsByAccess(JsonNode caseFields,
                                             List<CaseFieldDefinition> caseFieldDefinitions,
                                             Set<String> userRoles,
                                             Predicate<AccessControlList> access,
                                             boolean isClassification) {
        return accessControlService.filterCaseFieldsByAccess(caseFields,
            caseFieldDefinitions,
            userRoles,
            access,
            isClassification);
    }

    @Override
    public List<CaseFieldDefinition> filterCaseFieldsByAccess(List<CaseFieldDefinition> caseFieldDefinitions,
                                                              Set<String> userRoles,
                                                              Predicate<AccessControlList> access) {
        return accessControlService.filterCaseFieldsByAccess(caseFieldDefinitions,
            userRoles,
            access);
    }

    @Override
    public CaseUpdateViewEvent filterCaseViewFieldsByAccess(CaseUpdateViewEvent caseEventTrigger,
                                                            List<CaseFieldDefinition> caseFieldDefinitions,
                                                            Set<String> userRoles,
                                                            Predicate<AccessControlList> access) {
        return accessControlService.filterCaseViewFieldsByAccess(caseEventTrigger,
            caseFieldDefinitions,
            userRoles,
            access);
    }

    @Override
    public List<AuditEvent> filterCaseAuditEventsByReadAccess(List<AuditEvent> auditEvents,
                                                              List<CaseEventDefinition> caseEventDefinitions,
                                                              Set<String> userRoles) {
        return accessControlService.filterCaseAuditEventsByReadAccess(auditEvents,
            caseEventDefinitions,
            userRoles);
    }

    @Override
    public List<CaseStateDefinition> filterCaseStatesByAccess(List<CaseStateDefinition> caseStateDefinitions,
                                                              Set<String> userRoles,
                                                              Predicate<AccessControlList> access) {
        return accessControlService.filterCaseStatesByAccess(caseStateDefinitions,
            userRoles,
            access);
    }

    @Override
    public List<CaseEventDefinition> filterCaseEventsByAccess(List<CaseEventDefinition> caseEventDefinitions,
                                                              Set<String> userRoles,
                                                              Predicate<AccessControlList> access) {
        return accessControlService.filterCaseEventsByAccess(caseEventDefinitions,
            userRoles,
            access);
    }

    @Override
    public CaseViewActionableEvent[] filterCaseViewTriggersByCreateAccess(CaseViewActionableEvent[] caseViewTriggers,
                                                                          List<CaseEventDefinition> caseEventDefinitions,
                                                                          Set<String> userRoles) {
        return accessControlService.filterCaseViewTriggersByCreateAccess(caseViewTriggers,
            caseEventDefinitions,
            userRoles);
    }

}
