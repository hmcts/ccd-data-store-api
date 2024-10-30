package uk.gov.hmcts.ccd.data.casedetails;

import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.clients.PocApiClient;
import uk.gov.hmcts.ccd.data.casedetails.query.CaseDetailsQueryBuilder;
import uk.gov.hmcts.ccd.data.casedetails.query.CaseDetailsQueryBuilderFactory;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.data.casedetails.search.SearchQueryFactoryOperation;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationParameters;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseConcurrencyException;
import uk.gov.hmcts.ccd.endpoint.exceptions.CasePersistenceException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ReferenceKeyUniqueConstraintException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Named
@Qualifier(DefaultCaseDetailsRepository.QUALIFIER)
@Singleton
@SuppressWarnings("checkstyle:SummaryJavadoc")
// partial javadoc attributes added prior to checkstyle implementation in module
public class DefaultCaseDetailsRepository implements CaseDetailsRepository {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultCaseDetailsRepository.class);

    public static final String QUALIFIER = "default";
    private static final String UNIQUE_REFERENCE_KEY_CONSTRAINT = "case_data_reference_key";
    private final CaseDetailsMapper caseDetailsMapper;

    @PersistenceContext
    private EntityManager em;

    private final SearchQueryFactoryOperation queryBuilder;
    private final CaseDetailsQueryBuilderFactory queryBuilderFactory;
    private final ApplicationParams applicationParams;
    private final PocApiClient pocApiClient;

    @Inject
    public DefaultCaseDetailsRepository(
            final CaseDetailsMapper caseDetailsMapper,
            final SearchQueryFactoryOperation queryBuilder,
            final CaseDetailsQueryBuilderFactory queryBuilderFactory,
            final ApplicationParams applicationParams,
            final PocApiClient pocApiClient) {
        this.caseDetailsMapper = caseDetailsMapper;
        this.queryBuilder = queryBuilder;
        this.queryBuilderFactory = queryBuilderFactory;
        this.applicationParams = applicationParams;
        this.pocApiClient = pocApiClient;
    }

    @Override
    public CaseDetails set(final CaseDetails caseDetails) {
        final CaseDetailsEntity newCaseDetailsEntity = caseDetailsMapper.modelToEntity(caseDetails);
        newCaseDetailsEntity.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
//        CaseDetailsEntity aCase = pocApiClient.createCase(newCaseDetailsEntity);
        CaseDetailsEntity mergedEntity;
        try {
            mergedEntity = em.merge(newCaseDetailsEntity);
            em.flush();
        } catch (StaleObjectStateException | OptimisticLockException e) {
            LOG.info("Optimistic Lock Exception: Case data has been altered, UUID={}", caseDetails.getReference(), e);
            throw new CaseConcurrencyException("""
                Unfortunately we were unable to save your work to the case as \
                another action happened at the same time.
                Please review the case and try again.""");
        } catch (PersistenceException e) {
            if (e.getCause() instanceof ConstraintViolationException && isDuplicateReference(e)) {
                LOG.warn("ConstraintViolationException happen for UUID={}. ConstraintName: {}",
                    caseDetails.getReference(), UNIQUE_REFERENCE_KEY_CONSTRAINT);
                throw new ReferenceKeyUniqueConstraintException(e.getMessage());
            } else {
                LOG.warn("Failed to store case details, UUID={}", caseDetails.getReference(), e);
                throw new CasePersistenceException(e.getMessage());
            }
        }
//        if (this.applicationParams.getPocCaseTypes().contains(caseDetails.getCaseTypeId())) {
//            return caseDetailsMapper.entityToModel(aCase);
//        } else {
            return caseDetailsMapper.entityToModel(mergedEntity);
//        }
    }

    @Override
    public Optional<CaseDetails> findById(String jurisdiction, Long id) {
        return find(jurisdiction, id, null).map(this.caseDetailsMapper::entityToModel);
    }

    /**
     * @param id Internal case ID
     * @return Case details if found; null otherwise
     * @deprecated Use {@link DefaultCaseDetailsRepository#findByReference(String, Long)} instead
     */
    @Override
    @Deprecated
    public CaseDetails findById(final Long id) {
        return findById(null, id).orElse(null);
    }

    @Override
    public List<Long> findCaseReferencesByIds(final List<Long> ids) {
        return findReferencesByIds(ids);
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
    public Optional<CaseDetails> findByReference(String caseReference) {
        return findByReference(null, caseReference);
    }

    /**
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
     * @param jurisdiction Jurisdiction's ID
     * @param caseTypeId   Case's type ID
     * @param reference    Case unique 16-digit reference
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

    @Override
    public List<CaseDetails> findByMetaDataAndFieldData(final MetaData metadata,
                                                        final Map<String, String> dataSearchParams) {
        return getQuery(metadata, dataSearchParams, false)
            .map(query -> {
                paginate(query, metadata.getPage());
                return caseDetailsMapper.entityToModel((List<CaseDetailsEntity>) query.getResultList());
            })
            .orElse(Collections.emptyList());
    }

    @Override
    public List<CaseDetails> findByParamsWithLimit(final MigrationParameters migrationParameters) {
        final Query query = em.createNamedQuery("CaseDataEntity_FIND_CASE_WITH_LIMITS");
        query.setParameter("jurisdictionId", migrationParameters.getJurisdictionId());
        query.setParameter("caseTypeId", migrationParameters.getCaseTypeId());
        query.setParameter("caseDataId", migrationParameters.getCaseDataId());

        paginateSetLimit(query, migrationParameters.getNumRecords());
        return caseDetailsMapper.entityToModel(query.getResultList());
    }

    @Override
    public PaginatedSearchMetadata getPaginatedSearchMetadata(MetaData metaData,
                                                              Map<String, String> dataSearchParams) {

        return getQuery(metaData, dataSearchParams, true)
            .map(query -> {
                Integer totalResults = ((Number) query.getSingleResult()).intValue();
                int pageSize = applicationParams.getPaginationPageSize();
                PaginatedSearchMetadata sr = new PaginatedSearchMetadata();
                sr.setTotalResultsCount(totalResults);
                sr.setTotalPagesCount((int) Math.ceil((double) sr.getTotalResultsCount() / pageSize));
                return sr;
            })
            .orElse(PaginatedSearchMetadata.EMPTY);
    }

    // TODO This accepts null values for backward compatibility. Once deprecated methods are removed, parameters should
    // be annotated with @NotNull
    private Optional<CaseDetailsEntity> find(String jurisdiction, Long id, String reference) {
        final CaseDetailsQueryBuilder<CaseDetailsEntity> qb = queryBuilderFactory.selectSecured(em);

        if (null != jurisdiction) {
            qb.whereJurisdiction(jurisdiction);
        }

        return getCaseDetailsEntity(id, reference, qb);
    }

    private List<Long> findReferencesByIds(List<Long> ids) {
        final CaseDetailsQueryBuilder<Long> qb = queryBuilderFactory.selectByReferenceSecured(em);
        qb.whereIdsAreIn(ids);

        return qb.build().getResultList();
    }

    /**
     * Finds a case using a query builder that doesn't secure the query.
     * Required in some corner cases but {@link CaseDetailsRepository#findByReference(String, Long)}
     * should be used most of the times
     */
    @Override
    public Optional<CaseDetails> findByReferenceWithNoAccessControl(String reference) {
        CaseDetailsQueryBuilder<CaseDetailsEntity> qb = queryBuilderFactory.selectUnsecured(em);
        qb.whereReference(String.valueOf(reference));
        return qb.getSingleResult().map(this.caseDetailsMapper::entityToModel);
    }

    private Optional<CaseDetailsEntity> getCaseDetailsEntity(Long id,
                                                             String reference,
                                                             CaseDetailsQueryBuilder<CaseDetailsEntity> qb) {
        if (null != reference) {
            qb.whereReference(reference);
        } else {
            qb.whereId(id);
        }

        return qb.getSingleResult();
    }

    private Optional<Query> getQuery(MetaData metadata, Map<String, String> dataSearchParams, boolean isCountQuery) {
        return getQueryByParameters(metadata, dataSearchParams, isCountQuery);
    }

    private Optional<Query> getQueryByParameters(MetaData metadata, final Map<String, String> requestData,
                                                 boolean isCountQuery) {
        Map<String, String> fieldData = new HashMap<>(requestData);
        return this.queryBuilder.build(metadata, fieldData, isCountQuery);
    }

    private void paginate(Query query, Optional<String> pageOpt) {
        int page = pageOpt.map(Integer::valueOf).orElse(1);
        int pageSize = applicationParams.getPaginationPageSize();
        int firstResult = (page - 1) * pageSize;
        query.setFirstResult(firstResult);
        query.setMaxResults(pageSize);
    }

    private void paginateSetLimit(Query query, int limit) {
        query.setMaxResults(limit);
    }

    private boolean isDuplicateReference(PersistenceException e) {
        return ((ConstraintViolationException) e.getCause()).getConstraintName()
            .equals(UNIQUE_REFERENCE_KEY_CONSTRAINT);
    }
}
