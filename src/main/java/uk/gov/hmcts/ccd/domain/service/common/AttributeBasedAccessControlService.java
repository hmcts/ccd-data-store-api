package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(name = "enable-attribute-based-access-control", havingValue = "true")
public class AttributeBasedAccessControlService extends AccessControlServiceImpl implements AccessControl {

    private DefaultCaseDataAccessControl defaultCaseDataAccessControl;
    private final CaseDefinitionRepository caseDefinitionRepository;
    protected static final String IDAM_PREFIX = "idam:";

    @Autowired
    public AttributeBasedAccessControlService(final CompoundAccessControlService compoundAccessControlService,
                                              final DefaultCaseDataAccessControl defaultCaseDataAccessControl,
                                              final CaseDefinitionRepository caseDefinitionRepository) {
        super(compoundAccessControlService);
        this.defaultCaseDataAccessControl = defaultCaseDataAccessControl;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    @Override
    public boolean canAccessCaseTypeWithCriteria(CaseTypeDefinition caseType,
                                                 Set<String> userRoles,
                                                 Predicate<AccessControlList> criteria) {
        applyAccessProfileRules(caseType);


        return super.canAccessCaseTypeWithCriteria(caseType,
            userRoles,
            criteria);
    }

    @Override
    public boolean canAccessCaseStateWithCriteria(String caseState,
                                                  CaseTypeDefinition caseType,
                                                  Set<String> userRoles,
                                                  Predicate<AccessControlList> criteria) {
        applyAccessProfileRules(caseType);

        return super.canAccessCaseStateWithCriteria(caseState,
            caseType,
            userRoles,
            criteria);
    }

    @Override
    public boolean canAccessCaseEventWithCriteria(String eventId,
                                                  List<CaseEventDefinition> caseEventDefinitions,
                                                  Set<String> userRoles,
                                                  Predicate<AccessControlList> criteria) {
        return super.canAccessCaseEventWithCriteria(eventId,
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

        return super.canAccessCaseFieldsWithCriteria(caseFields,
            caseFieldDefinitions,
            userRoles,
            criteria);
    }

    @Override
    public boolean canAccessCaseViewFieldWithCriteria(CommonField caseViewField,
                                                      Set<String> userRoles,
                                                      Predicate<AccessControlList> criteria) {
        return super.canAccessCaseViewFieldWithCriteria(caseViewField,
            userRoles,
            criteria);
    }

    @Override
    public boolean canAccessCaseFieldsForUpsert(JsonNode newData,
                                                JsonNode existingData,
                                                List<CaseFieldDefinition> caseFieldDefinitions,
                                                Set<String> userRoles) {
        applyAccessProfileRules(caseFieldDefinitions);
        return super.canAccessCaseFieldsForUpsert(newData,
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

        return super.setReadOnlyOnCaseViewFieldsIfNoAccess(caseEventTrigger,
            caseFieldDefinitions,
            userRoles,
            access);
    }

    @Override
    public CaseUpdateViewEvent updateCollectionDisplayContextParameterByAccess(CaseUpdateViewEvent caseEventTrigger,
                                                                               Set<String> userRoles) {
        return super.updateCollectionDisplayContextParameterByAccess(caseEventTrigger,
            userRoles);
    }

    @Override
    public JsonNode filterCaseFieldsByAccess(JsonNode caseFields,
                                             List<CaseFieldDefinition> caseFieldDefinitions,
                                             Set<String> userRoles,
                                             Predicate<AccessControlList> access,
                                             boolean isClassification) {
        applyAccessProfileRules(caseFieldDefinitions);

        return super.filterCaseFieldsByAccess(caseFields,
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

        return super.filterCaseFieldsByAccess(caseFieldDefinitions,
            userRoles,
            access);
    }

    @Override
    public CaseUpdateViewEvent filterCaseViewFieldsByAccess(CaseUpdateViewEvent caseEventTrigger,
                                                            List<CaseFieldDefinition> caseFieldDefinitions,
                                                            Set<String> userRoles,
                                                            Predicate<AccessControlList> access) {
        applyAccessProfileRules(caseFieldDefinitions);

        return super.filterCaseViewFieldsByAccess(caseEventTrigger,
            caseFieldDefinitions,
            userRoles,
            access);
    }

    @Override
    public List<AuditEvent> filterCaseAuditEventsByReadAccess(List<AuditEvent> auditEvents,
                                                              List<CaseEventDefinition> caseEventDefinitions,
                                                              Set<String> userRoles) {
        return super.filterCaseAuditEventsByReadAccess(auditEvents,
            caseEventDefinitions,
            userRoles);
    }

    @Override
    public List<CaseStateDefinition> filterCaseStatesByAccess(List<CaseStateDefinition> caseStateDefinitions,
                                                              Set<String> userRoles,
                                                              Predicate<AccessControlList> access) {
        return super.filterCaseStatesByAccess(caseStateDefinitions,
            userRoles,
            access);
    }

    @Override
    public List<CaseEventDefinition> filterCaseEventsByAccess(List<CaseEventDefinition> caseEventDefinitions,
                                                              Set<String> userRoles,
                                                              Predicate<AccessControlList> access) {
        return super.filterCaseEventsByAccess(caseEventDefinitions,
            userRoles,
            access);
    }

    @Override
    public CaseViewActionableEvent[] filterCaseViewTriggersByCreateAccess(
        CaseViewActionableEvent[] caseViewTriggers,
        List<CaseEventDefinition> caseEventDefinitions,
        Set<String> userRoles) {
        return super.filterCaseViewTriggersByCreateAccess(caseViewTriggers,
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
        List<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesByCaseTypeId(caseTypeDefinition.getId());
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

    public boolean hasAccessControlList(Set<String> userRoles,
                                        Predicate<AccessControlList> criteria,
                                        List<AccessControlList> accessControlLists) {
        return accessControlLists != null && accessControlLists
            .stream()
            .filter(acls -> {
                String accessProfile = acls.getAccessProfile();
                return userRoles.contains(accessProfile)
                    || userRoles.contains(IDAM_PREFIX + accessProfile);
            })
            .anyMatch(criteria);
    }
}
