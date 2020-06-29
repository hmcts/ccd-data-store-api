package uk.gov.hmcts.ccd.data.casedetails.query;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.collections.CollectionUtils;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserEntity;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;

public abstract class CaseDetailsQueryBuilder<T> {

    private static final String CREATED_DATE = "createdDate";
    protected final EntityManager em;
    protected final CriteriaBuilder cb;
    protected final CriteriaQuery<T> query;
    protected final Root<CaseDetailsEntity> root;
    protected final List<Predicate> predicates;
    protected final List<Order> orders;

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

    public CaseDetailsQueryBuilder whereStates(List<String> states) {
        if (CollectionUtils.isNotEmpty(states)) {
            predicates.add(cb.in(root.get("state")).value(states));
        }
        return this;
    }

    public CaseDetailsQueryBuilder whereCreatedDate(String createdDate) {
        predicates.add(whereDate(root.get(CREATED_DATE), createdDate));

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

        metadata.getState().ifPresent(this::whereState);
        metadata.getCaseReference().ifPresent(this::whereReference);
        metadata.getCreatedDate().ifPresent(this::whereCreatedDate);
        metadata.getLastModifiedDate().ifPresent(this::whereLastModified);
        metadata.getSecurityClassification().ifPresent(this::whereSecurityClassification);

        return this;
    }

    public CaseDetailsQueryBuilder orderBy(MetaData metadata) {
        String sortDirection = metadata.getSortDirection().orElse("asc");
        String sortField = CREATED_DATE;
        if (sortDirection.equalsIgnoreCase("asc")) {
            orders.add(cb.asc(root.get(sortField)));
        } else {
            orders.add(cb.desc(root.get(sortField)));
        }

        return this;
    }

    public abstract TypedQuery<T> build();

    public Optional<T> getSingleResult() {
        return build().getResultList()
                      .stream()
                      .findFirst();
    }

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
