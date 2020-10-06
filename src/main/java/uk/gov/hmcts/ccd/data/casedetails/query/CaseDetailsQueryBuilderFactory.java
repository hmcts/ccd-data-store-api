package uk.gov.hmcts.ccd.data.casedetails.query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;

import javax.persistence.EntityManager;
import java.util.List;

@Component
public class CaseDetailsQueryBuilderFactory {

    private final List<CaseDetailsAuthorisationSecurity> caseDetailsAuthorisationSecurities;

    @Autowired
    public CaseDetailsQueryBuilderFactory(List<CaseDetailsAuthorisationSecurity> caseDetailsAuthorisationSecurities) {
        this.caseDetailsAuthorisationSecurities = caseDetailsAuthorisationSecurities;
    }

    public CaseDetailsQueryBuilder<CaseDetailsEntity> selectUnsecured(EntityManager em) {
        return new SelectCaseDetailsQueryBuilder(em);
    }

    public CaseDetailsQueryBuilder<CaseDetailsEntity> selectSecured(EntityManager em, MetaData metaData) {
        return secure(new SelectCaseDetailsQueryBuilder(em), metaData);
    }

    public CaseDetailsQueryBuilder<CaseDetailsEntity> selectSecured(EntityManager em) {
        return selectSecured(em, null);
    }

    public CaseDetailsQueryBuilder<Long> count(EntityManager em, MetaData metaData) {
        return secure(new CountCaseDetailsQueryBuilder(em), metaData);
    }

    private <T> CaseDetailsQueryBuilder<T> secure(CaseDetailsQueryBuilder<T> builder, MetaData metadata) {
        caseDetailsAuthorisationSecurities.forEach(security -> security.secure(builder, metadata));
        return builder;
    }
}
