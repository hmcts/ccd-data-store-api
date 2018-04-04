package uk.gov.hmcts.ccd.data.casedetails.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;

@Component
public class CaseDetailsQueryBuilderFactory {

    private final UserAuthorisationSecurity userAuthorisationSecurity;

    @Autowired
    public CaseDetailsQueryBuilderFactory(UserAuthorisationSecurity userAuthorisationSecurity) {
        this.userAuthorisationSecurity = userAuthorisationSecurity;
    }

    public CaseDetailsQueryBuilder create(EntityManager em) {
        return userAuthorisationSecurity.secure(new CaseDetailsQueryBuilder(em));
    }
}
