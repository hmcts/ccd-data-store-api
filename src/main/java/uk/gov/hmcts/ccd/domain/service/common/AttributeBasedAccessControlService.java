package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
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
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.DefaultCaseDataAccessControl;


@Service
@ConditionalOnProperty(name = "enable-attribute-based-access-control", havingValue = "true")
public class AttributeBasedAccessControlService extends AccessControlServiceImpl implements AccessControl {

    private DefaultCaseDataAccessControl defaultCaseDataAccessControl;
    private final CaseDefinitionRepository caseDefinitionRepository;

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

    private void applyAccessProfileRules(List<CaseFieldDefinition> caseFieldDefinitions) {
        Optional<CaseTypeDefinition> caseTypeDefinition = getCaseTypeDefinition(caseFieldDefinitions);
        if (caseTypeDefinition.isPresent()) {
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

    private Optional<CaseTypeDefinition> getCaseTypeDefinition(List<CaseFieldDefinition> caseFieldDefinitions) {
        if (caseFieldDefinitions != null && caseFieldDefinitions.size() > 0) {
            return Optional.ofNullable(caseDefinitionRepository
                .getCaseType(caseFieldDefinitions.get(0).getCaseTypeId()));
        }
        return Optional.empty();
    }

    @Override
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
