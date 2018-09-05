package uk.gov.hmcts.ccd.data.casedetails.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;

import javax.persistence.EntityManager;
import java.util.List;

@Component
public class CaseDetailsQueryBuilderFactory {

    private final UserAuthorisationSecurity userAuthorisationSecurity;
    private final List<CaseDetailsAuthorisationSecurity> caseDetailsAuthorisationSecurities;

    @Autowired
    public CaseDetailsQueryBuilderFactory(UserAuthorisationSecurity userAuthorisationSecurity,
                                          List<CaseDetailsAuthorisationSecurity> caseDetailsAuthorisationSecurities) {
        this.userAuthorisationSecurity = userAuthorisationSecurity;
        this.caseDetailsAuthorisationSecurities = caseDetailsAuthorisationSecurities;
    }

    public CaseDetailsQueryBuilder<CaseDetailsEntity> select(EntityManager em, MetaData metaData) {
        return secure(select(em), metaData);
    }

    public CaseDetailsQueryBuilder<CaseDetailsEntity> select(EntityManager em) {
        return userAuthorisationSecurity.secure(new SelectCaseDetailsQueryBuilder(em));
    }

    public CaseDetailsQueryBuilder<Long> count(EntityManager em, MetaData metaData) {
        CaseDetailsQueryBuilder<Long> builder = userAuthorisationSecurity.secure(new CountCaseDetailsQueryBuilder(em));
        return secure(builder, metaData);
    }

    private <T> CaseDetailsQueryBuilder<T> secure(CaseDetailsQueryBuilder<T> builder, MetaData metadata) {
        caseDetailsAuthorisationSecurities.forEach(security -> security.secure(builder, metadata));
        return builder;
    }
}
