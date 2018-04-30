package uk.gov.hmcts.ccd.data.casedetails.query;

import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;

public class SelectCaseDetailsQueryBuilder extends CaseDetailsQueryBuilder<CaseDetailsEntity> {

    SelectCaseDetailsQueryBuilder(EntityManager em) {
        super(em);
    }

    @Override
    public TypedQuery<CaseDetailsEntity> build() {
        return em.createQuery(query.select(root)
                                   .orderBy(orders)
                                   .where(predicates.toArray(new Predicate[]{})));
    }

    @Override
    protected CriteriaQuery<CaseDetailsEntity> createQuery() {
        return cb.createQuery(CaseDetailsEntity.class);
    }
}
