package uk.gov.hmcts.ccd.data.user;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.GrantType;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Qualifier(AttributeBasedJurisdictionsResolver.QUALIFIER)
@RequestScope
public class AttributeBasedJurisdictionsResolver implements JurisdictionsResolver, AccessControl {
    public static final String QUALIFIER = "access-control";
    public static final String ORGANISATION = "organisation";

    private final UserRepository userRepository;
    private final RoleAssignmentService roleAssignmentService;
    private final CaseDefinitionRepository caseDefinitionRepository;

    public AttributeBasedJurisdictionsResolver(@Qualifier(CachedUserRepository.QUALIFIER)
                                                   UserRepository userRepository,
                                               RoleAssignmentService roleAssignmentService,
                                               @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                                   CaseDefinitionRepository caseDefinitionRepository) {
        this.userRepository = userRepository;
        this.roleAssignmentService = roleAssignmentService;
        this.caseDefinitionRepository = caseDefinitionRepository;
    }

    @Override
    public List<String> getJurisdictions() {
        String userId = this.userRepository.getUser().getId();
        RoleAssignments roleAssignments = roleAssignmentService.getRoleAssignments(userId);
        return filterJurisdictions(roleAssignments);
    }

    private List<String> filterJurisdictions(RoleAssignments roleAssignmentsList) {
        List<String> jurisdictions = new ArrayList<>();
        roleAssignmentsList.getRoleAssignments().stream()
            .filter(roleAssignment -> !roleAssignment.getGrantType().equals(GrantType.BASIC.name()))
            .forEach(roleAssignments -> {
                if (Objects.nonNull(roleAssignments.getAttributes().getJurisdiction())) {
                    doesJurisdictionExist(roleAssignments
                        .getAttributes().getJurisdiction().get(), jurisdictions);
                } else if (Objects.nonNull(roleAssignments.getAttributes().getCaseType())) {
                    doesJurisdictionExist(caseDefinitionRepository
                        .getCaseType(roleAssignments
                            .getAttributes().getCaseType().get()).getJurisdictionId(), jurisdictions);
                } else if (Objects.nonNull(roleAssignments.getRoleType())
                    && roleAssignments.getRoleType().equalsIgnoreCase(ORGANISATION)) {
                    caseDefinitionRepository.getAllJurisdictionsFromDefinitionStore()
                        .forEach(jurisdictionDefinition ->
                            doesJurisdictionExist(jurisdictionDefinition.getId(), jurisdictions));
                }
            });
        return jurisdictions;
    }

    private List<String> doesJurisdictionExist(String jurisdiction, List<String> jurisdictions) {
        if (!jurisdictions.contains(jurisdiction.toLowerCase())) {
            jurisdictions.add(jurisdiction.toLowerCase());
        }
        return jurisdictions;
    }
}
