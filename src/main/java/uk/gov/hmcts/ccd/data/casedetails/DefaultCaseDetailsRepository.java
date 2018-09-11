package uk.gov.hmcts.ccd.data.casedetails;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.query.CaseDetailsQueryBuilder;
import uk.gov.hmcts.ccd.data.casedetails.query.CaseDetailsQueryBuilderFactory;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.data.casedetails.search.SearchQueryFactoryOperation;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseConcurrencyException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Named
@Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
@Singleton
public class DefaultCaseDetailsRepository implements CaseDetailsRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCaseDetailsRepository.class);
    public static final String QUALIFIER = "default";

    private final CaseDetailsMapper caseDetailsMapper;

    @PersistenceContext
    private EntityManager em;

    private final SearchQueryFactoryOperation queryBuilder;
    private final CaseDetailsQueryBuilderFactory queryBuilderFactory;
    private final ApplicationParams applicationParams;

    @Inject
    public DefaultCaseDetailsRepository(
        final CaseDetailsMapper caseDetailsMapper,
        final SearchQueryFactoryOperation queryBuilder,
        final CaseDetailsQueryBuilderFactory queryBuilderFactory,
        final ApplicationParams applicationParams) {
        this.caseDetailsMapper = caseDetailsMapper;
        this.queryBuilder = queryBuilder;
        this.queryBuilderFactory = queryBuilderFactory;
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
    public Optional<CaseDetails> findById(String jurisdiction, Long id) {
        return find(jurisdiction, id, null).map(this.caseDetailsMapper::entityToModel);
    }

    @Override
    public Optional<CaseDetails> findByReference(String jurisdiction, Long caseReference) {
        return findByReference(jurisdiction, caseReference.toString());
    }

    @Override
    public Optional<CaseDetails> findByReference(String jurisdiction, String reference) {
        return find(jurisdiction, null, reference).map(this.caseDetailsMapper::entityToModel);
    }

    @Override
    public Optional<CaseDetails> lockByReference(String jurisdiction, Long reference) {
        return lockByReference(jurisdiction, reference.toString());
    }

    @Override
    public Optional<CaseDetails> lockByReference(String jurisdiction, String reference) {
        return find(jurisdiction, null, reference).map(entity -> {
            em.lock(entity, LockModeType.PESSIMISTIC_WRITE);
            return this.caseDetailsMapper.entityToModel(entity);
        });
    }

    /**
     *
     * @param id Internal case ID
     * @return Case details if found; null otherwise
     * @deprecated Use {@link DefaultCaseDetailsRepository#findByReference(String, Long)} instead
     */
    @Override
    @Deprecated
    public CaseDetails findById(final Long id) {
        return findById(null, id).orElse(null);
    }

    /**
     *
     * @param caseReference Public case reference
     * @return Case details if found; null otherwise.
     * @deprecated Use {@link DefaultCaseDetailsRepository#findByReference(String, Long)} instead
     */
    @Override
    @Deprecated
    public CaseDetails findByReference(final Long caseReference) {
        return findByReference(null, caseReference).orElseThrow(() -> new ResourceNotFoundException("No case found"));
    }

    /**
     *
     * @param jurisdiction Jurisdiction's ID
     * @param caseTypeId Case's type ID
     * @param reference Case unique 16-digit reference
     * @return Case details if found; null otherwise
     * @deprecated Use {@link DefaultCaseDetailsRepository#findByReference(String, String)} instead
     */
    @Override
    @Deprecated
    public CaseDetails findUniqueCase(final String jurisdiction,
                                      final String caseTypeId,
                                      final String reference) {
        return findByReference(jurisdiction, reference).orElse(null);
    }

    /**
     *
     * @param reference Case unique 16-digit reference
     * @return Case details if found; throw NotFound exception otherwise
     * @deprecated Use {@link DefaultCaseDetailsRepository#lockByReference(String, Long)} instead
     */
    @Override
    @Deprecated
    public CaseDetails lockCase(final Long reference) {
        return lockByReference(null, reference).orElseThrow(() -> new ResourceNotFoundException("No case found"));
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

    // TODO This accepts null values for backward compatibility. Once deprecated methods are removed, parameters should
    // be annotated with @NotNull
    private Optional<CaseDetailsEntity> find(String jurisdiction, Long id, String reference) {
        final CaseDetailsQueryBuilder<CaseDetailsEntity> qb = queryBuilderFactory.select(em);

        if (null != jurisdiction) {
            qb.whereJurisdiction(jurisdiction);
        }

        if (null != reference) {
            qb.whereReference(reference);
        } else {
            qb.whereId(id);
        }

        return qb.getSingleResult();
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
        return queryBuilderFactory.count(em, metadata)
                                  .whereMetadata(metadata)
                                  .build();
    }

    private Query getQueryByMetaData(MetaData metadata) {
        return queryBuilderFactory.select(em, metadata)
                                  .whereMetadata(metadata)
                                  .orderByCreatedDate(metadata.getSortDirection().orElse("asc"))
                                  .build();
    }

    private void paginate(Query query, Optional<String> pageOpt) {
        int page = pageOpt.map(Integer::valueOf).orElse(1);
        int pageSize = applicationParams.getPaginationPageSize();
        int firstResult = (page - 1) * pageSize;
        query.setFirstResult(firstResult);
        query.setMaxResults(pageSize);
    }
}
