package uk.gov.hmcts.ccd.data.casedetails.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;

public class CountCaseDetailsQueryBuilder extends CaseDetailsQueryBuilder<Long> {

    CountCaseDetailsQueryBuilder(EntityManager em) {
        super(em);
    }

    @Override
    public TypedQuery<Long> build() {
        return em.createQuery(query.select(cb.count(root))
                                   .orderBy(orders)
                                   .where(predicates.toArray(new Predicate[]{})));
    }

    @Override
    protected CriteriaQuery<Long> createQuery() {
        return cb.createQuery(Long.class);
    }
}
