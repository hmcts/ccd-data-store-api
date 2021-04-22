package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewActionableEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.DefaultCaseDataAccessControl;


@Service
@Primary
@ConditionalOnProperty(name = "enable-attribute-based-access-control", havingValue = "true")
public class AttributeBasedAccessControlService implements AccessControlService, AccessControl {

    private DefaultCaseDataAccessControl defaultCaseDataAccessControl;
    private AccessControlService accessControlService;
    private final CaseDefinitionRepository caseDefinitionRepository;

    @Autowired
    public AttributeBasedAccessControlService(final DefaultCaseDataAccessControl defaultCaseDataAccessControl,
                                              final @Qualifier(AccessControlServiceImpl.QUALIFIER)
                                                  AccessControlService accessControlService,
                                              final CaseDefinitionRepository caseDefinitionRepository) {
        this.defaultCaseDataAccessControl = defaultCaseDataAccessControl;
        this.accessControlService = accessControlService;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    @Override
    public boolean canAccessCaseTypeWithCriteria(CaseTypeDefinition caseType,
                                                 Set<String> userRoles,
                                                 Predicate<AccessControlList> criteria) {
        applyAccessProfileRules(caseType);

        return accessControlService.canAccessCaseTypeWithCriteria(caseType,
            userRoles,
            criteria);
    }

    @Override
    public boolean canAccessCaseStateWithCriteria(String caseState,
                                                  CaseTypeDefinition caseType,
                                                  Set<String> userRoles,
                                                  Predicate<AccessControlList> criteria) {
        applyAccessProfileRules(caseType);

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
        applyAccessProfileRules(caseFieldDefinitions);

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
        applyAccessProfileRules(caseFieldDefinitions);
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
        applyAccessProfileRules(caseFieldDefinitions);

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
        applyAccessProfileRules(caseFieldDefinitions);

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
        applyAccessProfileRules(caseFieldDefinitions);

        return accessControlService.filterCaseFieldsByAccess(caseFieldDefinitions,
            userRoles,
            access);
    }

    @Override
    public CaseUpdateViewEvent filterCaseViewFieldsByAccess(CaseUpdateViewEvent caseEventTrigger,
                                                            List<CaseFieldDefinition> caseFieldDefinitions,
                                                            Set<String> userRoles,
                                                            Predicate<AccessControlList> access) {
        applyAccessProfileRules(caseFieldDefinitions);

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
    public CaseViewActionableEvent[] filterCaseViewTriggersByCreateAccess(
        CaseViewActionableEvent[] caseViewTriggers,
        List<CaseEventDefinition> caseEventDefinitions,
        Set<String> userRoles) {
        return accessControlService.filterCaseViewTriggersByCreateAccess(caseViewTriggers,
            caseEventDefinitions,
            userRoles);
    }

    private void applyAccessProfileRules(List<CaseFieldDefinition> caseFieldDefinitions) {
        Optional<CaseTypeDefinition> caseTypeDefinition = getCaseTypeDefinition(caseFieldDefinitions);
        if (!caseFieldDefinitions.isEmpty()) {
            applyAccessProfileRules(caseTypeDefinition.get());
        }
    }

    private void applyAccessProfileRules(CaseTypeDefinition caseTypeDefinition) {
        List<AccessProfile> accessProfiles = defaultCaseDataAccessControl.generateAccessProfiles(caseTypeDefinition);
        // @todo Better to deepCopy caseTypeDefinition
        List<AccessControlList> accessControlLists = caseTypeDefinition.getAccessControlLists();
        if (accessControlLists != null) {
            accessProfiles
                .stream()
                .forEach(accessProfile -> updateAccessControlCRUD(accessProfile, accessControlLists));
        }
    }

    private void updateAccessControlCRUD(AccessProfile accessProfile,
                                         List<AccessControlList> accessControlLists) {
        List<AccessControlList> matchingAccessControls = accessControlLists
            .stream()
            .filter(acls -> accessProfile.getAccessProfile().equals(acls.getAccessProfile()))
            .collect(Collectors.toList());
        matchingAccessControls
            .stream()
            .forEach(accessControlList -> {
                if (accessProfile.getReadOnly()) {
                    accessControlList.setCreate(false);
                    accessControlList.setDelete(false);
                    accessControlList.setUpdate(false);
                    accessControlList.setRead(true);
                }
            });
    }

    private Optional<CaseTypeDefinition> getCaseTypeDefinition(CaseFieldDefinition caseFieldDefinition) {
        return getCaseTypeDefinition(Lists.newArrayList(caseFieldDefinition));
    }

    private Optional<CaseTypeDefinition> getCaseTypeDefinition(List<CaseFieldDefinition> caseFieldDefinitions) {
        if (caseFieldDefinitions != null && caseFieldDefinitions.size() > 0) {
            return Optional.ofNullable(caseDefinitionRepository
                .getCaseType(caseFieldDefinitions.get(0).getCaseTypeId()));
        }
        return Optional.empty();
    }


    private Optional<CaseTypeDefinition> getCaseEventCaseTypeDefinition(List<CaseEventDefinition> caseEventDefinition) {
        return Optional.empty();
    }
}
