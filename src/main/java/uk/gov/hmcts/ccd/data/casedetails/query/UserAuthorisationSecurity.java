package uk.gov.hmcts.ccd.data.casedetails.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.service.casedataaccesscontrol.RoleAssignmentService;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;

@Component
public class UserAuthorisationSecurity implements CaseDetailsAuthorisationSecurity {

    private final UserAuthorisation userAuthorisation;
    private final ApplicationParams applicationParams;
    private final RoleAssignmentService roleAssignmentService;

    @Autowired
    UserAuthorisationSecurity(UserAuthorisation userAuthorisation,
                              ApplicationParams applicationParams, RoleAssignmentService roleAssignmentService) {
        this.userAuthorisation = userAuthorisation;
        this.applicationParams = applicationParams;
        this.roleAssignmentService = roleAssignmentService;
    }


    @Override
    public <T> void secure(CaseDetailsQueryBuilder<T> builder, MetaData metadata) {
        if (AccessLevel.GRANTED.equals(userAuthorisation.getAccessLevel())) {
            if (!applicationParams.getEnableAttributeBasedAccessControl()) {
                builder.whereGrantedAccessOnly(userAuthorisation.getUserId());
            }
        }
    }
}
