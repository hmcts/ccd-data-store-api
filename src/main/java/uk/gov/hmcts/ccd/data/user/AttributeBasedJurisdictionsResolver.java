package uk.gov.hmcts.ccd.data.user;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.hmcts.ccd.domain.model.casedataaccesscontrol.RoleAssignments;
import uk.gov.hmcts.ccd.domain.service.AccessControl;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentService;

import java.util.List;

@Service
@Qualifier(AttributeBasedJurisdictionsResolver.QUALIFIER)
@RequestScope
public class AttributeBasedJurisdictionsResolver implements JurisdictionsResolver, AccessControl {
    public static final String QUALIFIER = "access-control";

    private final UserRepository userRepository;
    private final RoleAssignmentService roleAssignmentService;

    public AttributeBasedJurisdictionsResolver(@Qualifier(CachedUserRepository.QUALIFIER)
                                                     UserRepository userRepository,
                                               RoleAssignmentService roleAssignmentService) {
        this.userRepository = userRepository;
        this.roleAssignmentService = roleAssignmentService;
    }

    @Override
    public List<String> getJurisdictions() {
        String userId = this.userRepository.getUser().getId();
        RoleAssignments roleAssignments = roleAssignmentService.getRoleAssignments(userId);

        return roleAssignments.getJurisdictions();
    }
}
