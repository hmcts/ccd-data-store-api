package uk.gov.hmcts.ccd.data.casedetails.query;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;

public class SelectCaseDetailsReferenceQueryBuilder extends CaseDetailsQueryBuilder<Long> {

    SelectCaseDetailsReferenceQueryBuilder(EntityManager em) {
        super(em);
    }

    @Override
    public TypedQuery<Long> build() {
        return em.createQuery(query.select(root.get("reference"))
            .orderBy(orders)
            .where(predicates.toArray(new Predicate[]{})));
    }

    @Override
    protected CriteriaQuery<Long> createQuery() {
        return cb.createQuery(Long.class);
    }
}
