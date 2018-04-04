package uk.gov.hmcts.ccd.data.casedetails.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;

@Component
class UserAuthorisationSecurity {

    private final UserAuthorisation userAuthorisation;

    @Autowired
    UserAuthorisationSecurity(UserAuthorisation userAuthorisation) {
        this.userAuthorisation = userAuthorisation;
    }

    <T> CaseDetailsQueryBuilder<T> secure(CaseDetailsQueryBuilder<T> builder) {
        if (AccessLevel.GRANTED.equals(userAuthorisation.getAccessLevel())) {
            builder.whereGrantedAccessOnly(userAuthorisation.getUserId());
        }
        return builder;
    }
}
