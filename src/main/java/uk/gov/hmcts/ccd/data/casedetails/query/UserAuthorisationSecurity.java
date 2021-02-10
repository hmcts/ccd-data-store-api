package uk.gov.hmcts.ccd.data.casedetails.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation;
import uk.gov.hmcts.ccd.infrastructure.user.UserAuthorisation.AccessLevel;

@Component
class UserAuthorisationSecurity implements CaseDetailsAuthorisationSecurity {

    private final UserAuthorisation userAuthorisation;

    @Autowired
    UserAuthorisationSecurity(UserAuthorisation userAuthorisation) {
        this.userAuthorisation = userAuthorisation;
    }

    @Override
    public <T> void secure(CaseDetailsQueryBuilder<T> builder, MetaData metadata) {
        if (AccessLevel.GRANTED.equals(userAuthorisation.getAccessLevel())) {
            builder.whereGrantedAccessOnly(userAuthorisation.getUserId());
        }
    }
}
