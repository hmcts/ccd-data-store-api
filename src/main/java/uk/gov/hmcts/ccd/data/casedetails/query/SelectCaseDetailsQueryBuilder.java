package uk.gov.hmcts.ccd.data.casedetails.query;

import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;

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
