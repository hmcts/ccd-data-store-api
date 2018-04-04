package uk.gov.hmcts.ccd.data.casedetails.query;

import uk.gov.hmcts.ccd.data.caseaccess.CaseUserEntity;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

public abstract class CaseDetailsQueryBuilder<T> {

    protected final EntityManager em;
    protected final CriteriaBuilder cb;
    protected final CriteriaQuery<T> query;
    protected final Root<CaseDetailsEntity> root;
    protected final ArrayList<Predicate> predicates;
    protected final ArrayList<Order> orders;

    CaseDetailsQueryBuilder(EntityManager em) {
        this.em = em;
        cb = em.getCriteriaBuilder();
        query = createQuery();
        root = query.from(CaseDetailsEntity.class);
        predicates = new ArrayList<>();
        orders = new ArrayList<>();

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

    public CaseDetailsQueryBuilder whereCaseType(String caseType) {
        predicates.add(cb.equal(root.get("caseType"), caseType));

        return this;
    }

    public CaseDetailsQueryBuilder whereState(String state) {
        predicates.add(cb.equal(root.get("state"), state));

        return this;
    }

    public CaseDetailsQueryBuilder whereCreatedDate(String createdDate) {
        predicates.add(whereDate(root.get("createdDate"), createdDate));

        return this;
    }

    public CaseDetailsQueryBuilder whereLastModified(String lastModified) {
        predicates.add(whereDate(root.get("lastModified"), lastModified));

        return this;
    }

    public CaseDetailsQueryBuilder whereSecurityClassification(String rawClassification) {
        final SecurityClassification securityClassification = SecurityClassification.valueOf(rawClassification.toUpperCase());
        predicates.add(cb.equal(root.get("securityClassification"), securityClassification));

        return this;
    }

    public CaseDetailsQueryBuilder whereMetadata(MetaData metadata) {
        whereJurisdiction(metadata.getJurisdiction());
        whereCaseType(metadata.getCaseTypeId());

        metadata.getState().map(this::whereState);
        metadata.getCaseReference().map(this::whereReference);
        metadata.getCreatedDate().map(this::whereCreatedDate);
        metadata.getLastModified().map(this::whereLastModified);
        metadata.getSecurityClassification().map(this::whereSecurityClassification);

        return this;
    }

    public CaseDetailsQueryBuilder orderByCreatedDate() {
        orders.add(cb.asc(root.get("createdDate")));

        return this;
    }

    public abstract TypedQuery<T> build();

    protected abstract CriteriaQuery<T> createQuery();

    private Predicate whereDate(Path<LocalDateTime> field, String value) {
        return cb.and(
            cb.greaterThanOrEqualTo((field), atStartOfDay(value)),
            cb.lessThan((field), atBeginningOfNextDay(value))
        );
    }

    private LocalDateTime atBeginningOfNextDay(String date) {
        return LocalDate.parse(date).plusDays(1).atStartOfDay();
    }

    private LocalDateTime atStartOfDay(String date) {
        return LocalDate.parse(date).atStartOfDay();
    }
}
