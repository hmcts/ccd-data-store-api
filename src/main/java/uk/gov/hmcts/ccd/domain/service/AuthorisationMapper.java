package uk.gov.hmcts.ccd.domain.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.AccessProfile;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignment;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.RoleToAccessProfileDefinition;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;

import static com.google.common.collect.Maps.newConcurrentMap;

@Component
@RequestScope
public class AuthorisationMapper {

    private final CaseTypeService caseTypeService;

    private final Map<String, Map<String, List<RoleToAccessProfileDefinition>>> caseTypeRoleToAccessProfileDefinition
        = newConcurrentMap();

    @Autowired
    public AuthorisationMapper(CaseTypeService caseTypeService) {
        this.caseTypeService = caseTypeService;
    }

    public Map<String, List<RoleToAccessProfileDefinition>> toRoleNameAsKeyMap(CaseTypeDefinition caseTypeDefinition) {
        return caseTypeRoleToAccessProfileDefinition.computeIfAbsent(caseTypeDefinition.getId(),
            id -> toRoleNameAsKeyMap(caseTypeDefinition.getRoleToAccessProfiles()));
    }

    public Map<String, List<RoleToAccessProfileDefinition>> toRoleNameAsKeyMap(String caseTypeId) {
        return caseTypeRoleToAccessProfileDefinition.computeIfAbsent(caseTypeId,
            id -> toRoleNameAsKeyMap(caseTypeService.getCaseType(caseTypeId).getRoleToAccessProfiles()));
    }

    public Map<String, List<RoleToAccessProfileDefinition>> toRoleNameAsKeyMap(
        List<RoleToAccessProfileDefinition> roleToAccessProfiles) {
        return roleToAccessProfiles
            .stream()
            .filter(e -> e.getRoleName() != null)
            .collect(Collectors.groupingBy(RoleToAccessProfileDefinition::getRoleName, Collectors.mapping(Function
                .identity(), Collectors.toList())));
    }

    public boolean authorisationsAllowMappingToAccessProfiles(List<String> authorisations,
                                                               List<String> roleAssignmentAuthorisations) {
        if (roleAssignmentAuthorisations != null
            && !authorisations.isEmpty()) {
            Collection<String> filterAuthorisations = CollectionUtils
                .intersection(roleAssignmentAuthorisations, authorisations);

            return !filterAuthorisations.isEmpty();
        }
        return authorisations.isEmpty();
    }

    public List<AccessProfile> createAccessProfiles(RoleAssignment roleAssignment,
                                                     RoleToAccessProfileDefinition roleToAccessProfileDefinition) {
        List<String> accessProfileList = roleToAccessProfileDefinition.getAccessProfileList();
        return accessProfileList
            .stream()
            .map(accessProfileValue -> AccessProfile.builder()
                .accessProfile(accessProfileValue)
                .caseAccessCategories(roleToAccessProfileDefinition.getCaseAccessCategories())
                .securityClassification(roleAssignment.getClassification())
                .readOnly(readOnly(roleAssignment, roleToAccessProfileDefinition))
                .build()).collect(Collectors.toList());
    }

    private Boolean readOnly(RoleAssignment roleAssignment,
                             RoleToAccessProfileDefinition roleToAccessProfileDefinition) {
        return BooleanUtils.isTrue(roleAssignment.getReadOnly())
            || BooleanUtils.isTrue(roleToAccessProfileDefinition.getReadOnly());
    }
}
