package uk.gov.hmcts.ccd.data.casedetails;

import lombok.RequiredArgsConstructor;
import org.hibernate.StaleObjectStateException;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.data.casedetails.query.CaseDetailsQueryBuilder;
import uk.gov.hmcts.ccd.data.casedetails.query.CaseDetailsQueryBuilderFactory;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.data.casedetails.search.SearchQueryFactoryOperation;
import uk.gov.hmcts.ccd.domain.model.migration.MigrationParameters;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
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


@RequiredArgsConstructor
@Qualifier(DelegatingCaseDetailsRepository.QUALIFIER)
@Service
public class DelegatingCaseDetailsRepository implements CaseDetailsRepository {

    private final PersistenceStrategyResolver resolver;
    private final DecentralisedCaseDetailsRepository decentralisedCaseDetailsRepository;
    private final DefaultCaseDetailsRepository defaultRepository;
    public static final String QUALIFIER = "delegating";

    @Override
    public CaseDetails set(final CaseDetails caseDetails) {
            return defaultRepository.set(caseDetails);
    }

    @Override
    public Optional<CaseDetails> findById(String jurisdiction, Long id) {
        if (resolver.isDecentralised(id)) {
            return decentralisedCaseDetailsRepository.findById(jurisdiction, id);
        } else {
            return defaultRepository.findById(jurisdiction, id);
        }
    }

    @Override
    @Deprecated
    public CaseDetails findById(final Long id) {
        if (resolver.isDecentralised(id)) {
            return decentralisedCaseDetailsRepository.findById(id);
        } else {
            return defaultRepository.findById(id);
        }
    }

    @Override
    public List<Long> findCaseReferencesByIds(final List<Long> ids) {
        // TODO
        return defaultRepository.findCaseReferencesByIds(ids);
    }

    @Override
    public Optional<CaseDetails> findByReference(String jurisdiction, Long caseReference) {
        if (resolver.isDecentralised(caseReference)) {
            return decentralisedCaseDetailsRepository.findByReference(jurisdiction, caseReference);
        } else {
            return defaultRepository.findByReference(jurisdiction, caseReference);
        }
    }

    @Override
    public Optional<CaseDetails> findByReference(String jurisdiction, String reference) {
        if (resolver.isDecentralised(reference)) {
            return decentralisedCaseDetailsRepository.findByReference(jurisdiction, reference);
        } else {
            return defaultRepository.findByReference(jurisdiction, reference);
        }
    }

    @Override
    public Optional<CaseDetails> findByReference(String caseReference) {
        if (resolver.isDecentralised(caseReference)) {
            return decentralisedCaseDetailsRepository.findByReference(caseReference);
        } else {
            return defaultRepository.findByReference(caseReference);
        }
    }

    /**
     * @param caseReference Public case reference
     * @return Case details if found; null otherwise.
     * @deprecated Use {@link DefaultCaseDetailsRepository#findByReference(String, Long)} instead
     */
    @Override
    @Deprecated
    public CaseDetails findByReference(final Long caseReference) {
        if (resolver.isDecentralised(caseReference)) {
            return decentralisedCaseDetailsRepository.findByReference(caseReference);
        } else {
            return defaultRepository.findByReference(caseReference);
        }
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
       if (resolver.isDecentralised(reference)) {
            return decentralisedCaseDetailsRepository.findUniqueCase(jurisdiction, caseTypeId, reference);
        } else {
            return defaultRepository.findUniqueCase(jurisdiction, caseTypeId, reference);
        }
    }

    @Override
    public List<CaseDetails> findByMetaDataAndFieldData(final MetaData metadata,
                                                        final Map<String, String> dataSearchParams) {
        return defaultRepository.findByMetaDataAndFieldData(metadata, dataSearchParams);
    }

    @Override
    public List<CaseDetails> findByParamsWithLimit(final MigrationParameters migrationParameters) {
        return defaultRepository.findByParamsWithLimit(migrationParameters);
    }

    @Override
    public PaginatedSearchMetadata getPaginatedSearchMetadata(MetaData metaData,
                                                              Map<String, String> dataSearchParams) {
        return defaultRepository.getPaginatedSearchMetadata(metaData, dataSearchParams);
    }

    @Override
    public Optional<CaseDetails> findByReferenceWithNoAccessControl(String reference) {
        if (resolver.isDecentralised(reference)) {
            return decentralisedCaseDetailsRepository.findByReferenceWithNoAccessControl(reference);
        } else {
            return defaultRepository.findByReferenceWithNoAccessControl(reference);
        }
    }
}
