package uk.gov.hmcts.ccd.data.casedetails;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.data.casedetails.search.SearchQueryFactoryOperation;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseConcurrencyException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.*;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;
import static java.lang.Long.valueOf;

@Named
@Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
@Singleton
public class DefaultCaseDetailsRepository implements CaseDetailsRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCaseDetailsRepository.class);
    private static final String CREATED_DATE = "createdDate";
    private static final String LAST_MODIFIED = "lastModified";

    public static final String QUALIFIER = "default";

    private final CaseDetailsMapper caseDetailsMapper;

    @PersistenceContext
    private EntityManager em;

    private final SearchQueryFactoryOperation queryBuilder;
    private final ApplicationParams applicationParams;

    @Inject
    public DefaultCaseDetailsRepository(
            final CaseDetailsMapper caseDetailsMapper,
            final SearchQueryFactoryOperation queryBuilder,
            final ApplicationParams applicationParams) {
        this.caseDetailsMapper = caseDetailsMapper;
        this.queryBuilder = queryBuilder;
        this.applicationParams = applicationParams;
    }

    @Override
    public CaseDetails set(final CaseDetails caseDetails) {
        final CaseDetailsEntity newCaseDetailsEntity = caseDetailsMapper.modelToEntity(caseDetails);
        newCaseDetailsEntity.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        CaseDetailsEntity mergedEntity = null;
        try {
            mergedEntity = em.merge(newCaseDetailsEntity);
        } catch (PersistenceException e) {
            LOG.warn("Failed to store case details", e);
            if (e.getCause() instanceof ConstraintViolationException) {
                throw new CaseConcurrencyException(e.getMessage());
            }
        }
        return caseDetailsMapper.entityToModel(mergedEntity);
    }

    @Override
    public CaseDetails findById(final Long id) {
        return caseDetailsMapper.entityToModel(em.find(CaseDetailsEntity.class, id));
    }

    @Override
    public CaseDetails findByReference(final Long caseReference) {
        final CaseDetailsEntity caseDetailsEntity = findByReference(caseReference, Optional.empty());
        return caseDetailsMapper.entityToModel(caseDetailsEntity);
    }

    @Override
    public CaseDetails lockCase(final Long caseReference) {
        final CaseDetailsEntity caseDetailsEntity = findByReference(caseReference, Optional.of(LockModeType.PESSIMISTIC_WRITE));
        return caseDetailsMapper.entityToModel(caseDetailsEntity);
    }

    private CaseDetailsEntity findByReference(final Long caseReference, Optional<LockModeType> lockModeType) {
        final TypedQuery<CaseDetailsEntity> query = em.createNamedQuery(CaseDetailsEntity.FIND_BY_REFERENCE, CaseDetailsEntity.class);
        query.setParameter(CaseDetailsEntity.CASE_REFERENCE_PARAM, valueOf(caseReference));
        CaseDetailsEntity caseDetailsEntity = null;
        try {
            caseDetailsEntity = query.getSingleResult();
        } catch (NoResultException e) {
            throw new ResourceNotFoundException("No case found");
        }
        if (caseDetailsEntity == null || caseDetailsEntity.getCaseType() == null)
            throw new ResourceNotFoundException("No case found");
        if (lockModeType.isPresent()) {
            em.lock(caseDetailsEntity, lockModeType.get());
        }
        return caseDetailsEntity;
    }

    public Optional<CaseDetails> findByReference(String jurisdictionId, Long caseReference) {
        final TypedQuery<CaseDetailsEntity> query = em.createNamedQuery(CaseDetailsEntity.FIND_BY_REF_AND_JURISDICTION,
                                                                        CaseDetailsEntity.class);
        query.setParameter(CaseDetailsEntity.JURISDICTION_ID_PARAM, jurisdictionId);
        query.setParameter(CaseDetailsEntity.CASE_REFERENCE_PARAM, caseReference);

        try {
            final CaseDetailsEntity caseEntity = query.getSingleResult();
            return Optional.of(caseDetailsMapper.entityToModel(caseEntity));
        } catch (NoResultException ex) {
            LOG.debug("Case not found for jurisdiction '{}' and reference '{}", jurisdictionId, caseReference);
        }

        return Optional.empty();
    }

    @Override
    public CaseDetails findUniqueCase(final String jurisdictionId,
                                      final String caseTypeId,
                                      final String caseReference) {
        final TypedQuery<CaseDetailsEntity> query = em.createNamedQuery(CaseDetailsEntity.FIND_CASE, CaseDetailsEntity.class);
        query.setParameter(CaseDetailsEntity.JURISDICTION_ID_PARAM, jurisdictionId);
        query.setParameter(CaseDetailsEntity.CASE_TYPE_PARAM, caseTypeId);
        query.setParameter(CaseDetailsEntity.CASE_REFERENCE_PARAM, valueOf(caseReference));
        final List<CaseDetailsEntity> result = query.getResultList();
        return result.isEmpty() ? null : caseDetailsMapper.entityToModel(result.get(0));
    }

    @Override
    public List<CaseDetails> findByMetaDataAndFieldData(final MetaData metadata, final Map<String, String> dataSearchParams) {
        final Query query = getQuery(metadata, dataSearchParams, false);
        paginate(query, metadata.getPage());
        return caseDetailsMapper.entityToModel(query.getResultList());
    }

    @Override
    public PaginatedSearchMetadata getPaginatedSearchMetadata(MetaData metaData, Map<String, String> dataSearchParams) {
        final Query query = getQuery(metaData, dataSearchParams, true);
        Integer totalResults = ((Number) query.getSingleResult()).intValue();
        int pageSize = applicationParams.getPaginationPageSize();
        PaginatedSearchMetadata sr = new PaginatedSearchMetadata();
        sr.setTotalResultsCount(totalResults);
        sr.setTotalPagesCount((int) Math.ceil((double) sr.getTotalResultsCount()/pageSize));
        return sr;
    }

    private Query getQuery(MetaData metadata, Map<String, String> dataSearchParams, boolean isCountQuery) {
        Query query;
        if (dataSearchParams.isEmpty()) {
            query = isCountQuery? getCountQueryByMetaData(metadata) : getQueryByMetaData(metadata);
        } else {
            query = getQueryByParameters(metadata, dataSearchParams, isCountQuery);
        }
        return query;
    }

    private Query getQueryByParameters(MetaData metadata, final Map<String, String> requestData, boolean isCountQuery) {
        Map<String, String> fieldData = new HashMap<>(requestData);
        return this.queryBuilder.build(metadata, fieldData, isCountQuery);
    }

    private Query getCountQueryByMetaData(MetaData metadata) {
        CriteriaBuilder qb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = qb.createQuery(Long.class);
        Root<CaseDetailsEntity> cd = cq.from(CaseDetailsEntity.class);
        List<Optional<Predicate>> predicatesOpt = getPredicates(metadata, qb, cd);

        cq.select(qb.count(cd)).where(qb.and(toArray(predicatesOpt)));

        return em.createQuery(cq);
    }

    private Query getQueryByMetaData(MetaData metadata) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<CaseDetailsEntity> q = cb.createQuery(CaseDetailsEntity.class);
        Root<CaseDetailsEntity> cd = q.from(CaseDetailsEntity.class);
        List<Optional<Predicate>> predicatesOpt = getPredicates(metadata, cb, cd);

        q.select(cd)
                .orderBy(cb.asc(cd.get(CREATED_DATE)))
                .where(cb.and(toArray(predicatesOpt)));

        return em.createQuery(q);
    }

    private List<Optional<Predicate>> getPredicates(MetaData metadata, CriteriaBuilder cb, Root<CaseDetailsEntity> cd) {
        Optional<Predicate> eqJurisdiction = Optional.of(cb.equal(cd.get("jurisdiction"), metadata.getJurisdiction()));
        Optional<Predicate> eqCaseType = Optional.of(cb.equal(cd.get("caseType"), metadata.getCaseTypeId()));
        Optional<Predicate> eqState = metadata.getState().map(s -> cb.equal(cd.get("state"), s));
        Optional<Predicate> eqReference = metadata.getCaseReference().map(cr -> cb.equal(cd.get("reference"), cr));
        Optional<Predicate> eqCreatedDate = metadata.getCreatedDate().map(crDt -> cb.and(
                cb.greaterThanOrEqualTo((cd.get(CREATED_DATE)), atStartOfDay(crDt)),
                cb.lessThan((cd.get(CREATED_DATE)), atBeginningOfNextDay(crDt)))
        );
        Optional<Predicate> eqLastModified = metadata.getLastModified().map(crDt -> cb.and(
                cb.greaterThanOrEqualTo((cd.get(LAST_MODIFIED)), atStartOfDay(crDt)),
                cb.lessThan((cd.get(LAST_MODIFIED)), atBeginningOfNextDay(crDt)))
        );
        Optional<Predicate> eqSecurityClassification = metadata.getSecurityClassification().map(sc -> cb.equal(cd.get("securityClassification"), SecurityClassification.valueOf(sc.toUpperCase())));

        return newArrayList(eqJurisdiction, eqCaseType, eqState, eqReference,
                eqCreatedDate, eqLastModified, eqSecurityClassification);
    }

    private void paginate(Query query, Optional<String> pageOpt) {
        int page = pageOpt.map(Integer::valueOf).orElse(1);
        int pageSize = applicationParams.getPaginationPageSize();
        int firstResult = (page - 1) * pageSize;
        query.setFirstResult(firstResult);
        query.setMaxResults(pageSize);
    }

    private LocalDateTime atBeginningOfNextDay(String date) {
        return LocalDate.parse(date).plusDays(1).atStartOfDay();
    }

    private LocalDateTime atStartOfDay(String date) {
        return LocalDate.parse(date).atStartOfDay();
    }

    private Predicate[] toArray(List<Optional<Predicate>> predicatesOpt) {
        List<Predicate> predicates = predicatesOpt.stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
        return predicates.toArray(new Predicate[predicates.size()]);
    }
}
