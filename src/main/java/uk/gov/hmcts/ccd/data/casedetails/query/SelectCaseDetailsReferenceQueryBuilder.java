package uk.gov.hmcts.ccd.data.casedetails.query;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;

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
