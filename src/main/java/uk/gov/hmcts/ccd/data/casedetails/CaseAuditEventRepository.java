package uk.gov.hmcts.ccd.data.casedetails;

import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Named
@Singleton
public class CaseAuditEventRepository {
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
        query.setParameter(CaseAuditEventEntity.CASE_DATA_ID, caseDetails.getId());

        return caseAuditEventMapper.entityToModel(query.getResultList());
    }

    public Optional<AuditEvent> getCreateEvent(CaseDetails caseDetails) {
        final Query query = em.createNamedQuery(CaseAuditEventEntity.FIND_CREATE_EVENT);

        query.setParameter(CaseAuditEventEntity.CASE_DATA_ID, caseDetails.getId());
        List<CaseAuditEventEntity> auditEvents = query.getResultList();

        return (auditEvents == null || auditEvents.size() == 0) ?
                Optional.empty() : Optional.of(caseAuditEventMapper.entityToModel(auditEvents.get(0)));
    }

    public Optional<AuditEvent> findByEventId(Long eventId) {
        Query query = em.createNamedQuery(CaseAuditEventEntity.FIND_BY_ID);

        query.setParameter(CaseAuditEventEntity.EVENT_ID, eventId);
        CaseAuditEventEntity caseAuditEvent = (CaseAuditEventEntity) query.getSingleResult();

        return ofNullable(caseAuditEvent).map(caseAuditEventMapper::entityToModel);
    }
}
