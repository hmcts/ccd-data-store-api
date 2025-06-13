package uk.gov.hmcts.ccd.data.casedetails.query;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;

public class SelectCaseDetailsReferenceQueryBuilder extends CaseDetailsQueryBuilder<String> {

    SelectCaseDetailsReferenceQueryBuilder(EntityManager em) {
        super(em);
    }

    @Override
    public TypedQuery<String> build() {
        return em.createQuery(query.select(root.get("reference"))
            .orderBy(orders)
            .where(predicates.toArray(new Predicate[]{})));
    }

    @Override
    protected CriteriaQuery<String> createQuery() {
        return cb.createQuery(String.class);
    }
}
