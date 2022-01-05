package uk.gov.hmcts.ccd.data.user;

import org.apache.commons.lang3.mutable.MutableBoolean;
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
        MutableBoolean continueProcessing = new MutableBoolean(Boolean.TRUE);
        roleAssignmentsList.getRoleAssignments().stream()
            .filter(roleAssignment -> !roleAssignment.getGrantType().equals(GrantType.BASIC.name()))
            .filter(roleAssignment -> !roleAssignment.getGrantType().equals(GrantType.EXCLUDED.name()))
            .takeWhile(t -> continueProcessing.getValue())
            .forEach(roleAssignments -> {
                if (Objects.nonNull(roleAssignments.getAttributes().getJurisdiction())) {
                    jurisdictions.add(roleAssignments
                        .getAttributes().getJurisdiction().get());
                } else if (Objects.nonNull(roleAssignments.getAttributes().getCaseType())) {
                    jurisdictions.add(caseDefinitionRepository
                        .getCaseType(roleAssignments
                            .getAttributes().getCaseType().get()).getJurisdictionId());
                } else if (roleAssignments.getGrantType().equals(GrantType.STANDARD.name())) {
                    caseDefinitionRepository.getAllJurisdictionsFromDefinitionStore()
                        .forEach(jurisdictionDefinition ->
                            jurisdictions.add(jurisdictionDefinition.getId()));
                    continueProcessing.setFalse();
                }
            });
        return jurisdictions;
    }
}
