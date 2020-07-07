package uk.gov.hmcts.ccd.data.casedetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Named
@Singleton
public class CaseAuditEventRepository {
    private static final Logger LOG = LoggerFactory.getLogger(CaseAuditEventRepository.class);
    private static final String EVENT_NOT_FOUND = "Event not found";

    private final CaseAuditEventMapper caseAuditEventMapper;

    @PersistenceContext
    private EntityManager em;

    @Inject
    public CaseAuditEventRepository(final CaseAuditEventMapper caseAuditEventMapper) {
        this.caseAuditEventMapper = caseAuditEventMapper;
    }

    public AuditEvent set(final AuditEvent auditEvent) {
        final CaseAuditEventEntity newCaseAuditEventEntity = caseAuditEventMapper.modelToEntity(auditEvent);
        em.persist(newCaseAuditEventEntity);
        return caseAuditEventMapper.entityToModel(newCaseAuditEventEntity);
    }

    public List<AuditEvent> findByCase(final CaseDetails caseDetails) {
        final Query query = em.createNamedQuery(CaseAuditEventEntity.FIND_BY_CASE);
        query.setParameter(CaseAuditEventEntity.CASE_DATA_ID, Long.valueOf(caseDetails.getId()));

        return caseAuditEventMapper.entityToModel(query.getResultList());
    }

    public Optional<AuditEvent> getCreateEvent(CaseDetails caseDetails) {
        final Query query = em.createNamedQuery(CaseAuditEventEntity.FIND_CREATE_EVENT);

        query.setParameter(CaseAuditEventEntity.CASE_DATA_ID, Long.valueOf(caseDetails.getId()));
        List<CaseAuditEventEntity> auditEvents = query.getResultList();

        return (auditEvents == null || auditEvents.size() == 0)
              ? Optional.empty()
              : Optional.of(caseAuditEventMapper.entityToModel(auditEvents.get(0)));
    }

    public Optional<AuditEvent> findByEventId(Long eventId) {
        Query query = em.createNamedQuery(CaseAuditEventEntity.FIND_BY_ID);

        query.setParameter(CaseAuditEventEntity.EVENT_ID, eventId);
        CaseAuditEventEntity caseAuditEvent;
        try {
            caseAuditEvent = (CaseAuditEventEntity) query.getSingleResult();
        } catch (NoResultException e) {
            LOG.warn(EVENT_NOT_FOUND, e);
            throw new ResourceNotFoundException(EVENT_NOT_FOUND);
        }
        return ofNullable(caseAuditEvent).map(caseAuditEventMapper::entityToModel);
    }
}
