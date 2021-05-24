package uk.gov.hmcts.ccd.domain.service.common;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.checkerframework.checker.nullness.Opt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.definition.AccessControlList;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseStateDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.DefaultCaseDataAccessControl;

import static com.google.common.collect.Lists.newArrayList;


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

    public List<AccessControlList> getAccessControlList(CaseTypeDefinition caseTypeDefinition) {
        return getAttributeBasedAccessControls(caseTypeDefinition);
    }

    public List<AccessControlList> getAccessControlList(CaseFieldDefinition caseFieldDefinition) {
        Optional<CaseTypeDefinition> caseTypeDefinition = getCaseTypeDefinition(caseFieldDefinition.getCaseTypeId());
        if (caseTypeDefinition.isPresent()) {
            return getAttributeBasedAccessControls(caseTypeDefinition.get());
        }
        return newArrayList();
    }

    private Optional<CaseTypeDefinition> getCaseTypeDefinition(String caseTypeId) {
        return Optional.ofNullable(caseDefinitionRepository.getCaseType(caseTypeId));
    }

    private List<AccessControlList> getAttributeBasedAccessControls(CaseTypeDefinition caseTypeDefinition) {
        List<AccessProfile> accessProfiles = defaultCaseDataAccessControl
            .generateAccessProfilesByCaseTypeId(caseTypeDefinition.getId());
        List<AccessControlList> accessControlLists = caseTypeDefinition.getAccessControlLists();
        if (accessControlLists != null) {
            return accessProfiles
                .stream()
                .map(accessProfile -> updateAccessControlCRUD(accessProfile, accessControlLists).orElse(null))
                .filter(accessControl -> accessControl != null)
                .collect(Collectors.toList());
        }
        return newArrayList();
    }

    private Optional<AccessControlList> updateAccessControlCRUD(AccessProfile accessProfile,
                                                                List<AccessControlList> accessControlLists) {
        return accessControlLists
            .stream()
            .filter(acls -> accessProfile.getAccessProfile().equals(acls.getAccessProfile()))
            .map(acls -> {
                AccessControlList accessControl = acls.duplicate();
                if (accessProfile.getReadOnly()) {
                    accessControl.setCreate(false);
                    accessControl.setDelete(false);
                    accessControl.setUpdate(false);
                    accessControl.setRead(true);
                }
                return accessControl;
            }).findAny();
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
