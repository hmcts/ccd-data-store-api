package uk.gov.hmcts.ccd.data.casedetails.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;

import javax.persistence.EntityManager;

@Component
public class CaseDetailsQueryBuilderFactory {

    private final UserAuthorisationSecurity userAuthorisationSecurity;

    @Autowired
    public CaseDetailsQueryBuilderFactory(UserAuthorisationSecurity userAuthorisationSecurity) {
        this.userAuthorisationSecurity = userAuthorisationSecurity;
    }

    public CaseDetailsQueryBuilder<CaseDetailsEntity> select(EntityManager em) {
        return userAuthorisationSecurity.secure(new SelectCaseDetailsQueryBuilder(em));
    }

    public CaseDetailsQueryBuilder<Long> count(EntityManager em) {
        return userAuthorisationSecurity.secure(new CountCaseDetailsQueryBuilder(em));
    }
}
