package uk.gov.hmcts.ccd.data.casedetails.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;

public class CountCaseDetailsQueryBuilder extends CaseDetailsQueryBuilder<String> {

    CountCaseDetailsQueryBuilder(EntityManager em) {
        super(em);
    }

    @Override
    public TypedQuery<String> build() {
        return em.createQuery(query.select(cb.count(root).as(String.class))
                                   .orderBy(orders)
                                   .where(predicates.toArray(new Predicate[]{})));
    }

    @Override
    protected CriteriaQuery<String> createQuery() {
        return cb.createQuery(String.class);
    }
}
