package uk.gov.hmcts.ccd.data.casedetails.query;

import uk.gov.hmcts.ccd.data.caseaccess.CaseUserEntity;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;

public class CaseDetailsQueryBuilder {

    private final EntityManager em;
    private final CriteriaBuilder cb;
    private final CriteriaQuery<CaseDetailsEntity> query;
    private final Root<CaseDetailsEntity> root;
    private final ArrayList<Predicate> predicates;

    CaseDetailsQueryBuilder(EntityManager em) {
        this.em = em;
        cb = em.getCriteriaBuilder();
        query = cb.createQuery(CaseDetailsEntity.class);
        root = query.from(CaseDetailsEntity.class);
        predicates = new ArrayList<>();
    }

    public CaseDetailsQueryBuilder whereGrantedAccessOnly(String userId) {
        final Subquery<CaseUserEntity> cuQuery = query.subquery(CaseUserEntity.class);
        final Root<CaseUserEntity> cu = cuQuery.from(CaseUserEntity.class);
        cuQuery.select(cu.get("casePrimaryKey").get("caseDataId"))
               .where(cb.equal(cu.get("casePrimaryKey").get("userId"), userId));

        predicates.add(cb.in(root.get("id")).value(cuQuery));

        return this;
    }

    public CaseDetailsQueryBuilder whereJurisdiction(String jurisdiction) {
        predicates.add(cb.equal(root.get("jurisdiction"), jurisdiction));

        return this;
    }

    public CaseDetailsQueryBuilder whereReference(String reference) {
        predicates.add(cb.equal(root.get("reference"), reference));

        return this;
    }

    public CaseDetailsQueryBuilder whereId(Long id) {
        predicates.add(cb.equal(root.get("id"), id));

        return this;
    }

    public TypedQuery<CaseDetailsEntity> build() {
        return em.createQuery(query.select(root)
                                   .where(predicates.toArray(new Predicate[]{})));
    }
}
