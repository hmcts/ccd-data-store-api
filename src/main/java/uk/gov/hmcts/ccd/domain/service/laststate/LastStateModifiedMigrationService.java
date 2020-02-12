package uk.gov.hmcts.ccd.domain.service.laststate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventEntity;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Service
public class LastStateModifiedMigrationService {

    private static final Logger LOG = LoggerFactory.getLogger(LastStateModifiedMigrationService.class);

    public static final String CASE_QUERY = "SELECT * FROM case_data WHERE last_state_modified_date IS NULL "
        + " AND jurisdiction = :jid" + " ORDER BY created_date " + " LIMIT :size";

    public static final String EVENTS_QUERY = "SELECT * FROM case_event WHERE case_data_id IN :caseIds";

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void migrate(String jurisdiction, int batchSize, boolean dryRun) {

        Query query = em.createNativeQuery(CASE_QUERY, CaseDetailsEntity.class)
            .setParameter("jid", jurisdiction)
            .setParameter("size", batchSize);
        List<CaseDetailsEntity> caseEntities = query.getResultList();

        if (!caseEntities.isEmpty()) {
            query = em.createNativeQuery(EVENTS_QUERY, CaseAuditEventEntity.class)
                .setParameter("caseIds", caseEntities.stream().map(CaseDetailsEntity::getId).collect(toList()));
            List<CaseAuditEventEntity> eventEntities = query.getResultList();
            findAndUpdateLastStateModifiedDate(caseEntities, eventEntities, dryRun);
        }
    }

    private void findAndUpdateLastStateModifiedDate(List<CaseDetailsEntity> caseEntities, List<CaseAuditEventEntity> eventEntities, boolean dryRun) {

        // sort events group by caseId
        Map<Long, List<CaseAuditEventEntity>> eventsByCase = eventEntities.stream()
            .sorted(Comparator.comparing(CaseAuditEventEntity::getCreatedDate).reversed())
            .collect(groupingBy(CaseAuditEventEntity::getCaseDataId));

        caseEntities.stream().forEach(caseDetailsEntity -> {
            List<CaseAuditEventEntity> sortedCaseEvents = eventsByCase.get(caseDetailsEntity.getId());
            CaseAuditEventEntity latestEvent = sortedCaseEvents.get(0);

            // find event where last state transition happened
            CaseAuditEventEntity stateChangeEvent = null;
            for (CaseAuditEventEntity event : sortedCaseEvents) {
                if (latestEvent.getStateId().equalsIgnoreCase(event.getStateId())) {
                    stateChangeEvent = event;
                } else {
                    break;
                }
            }

            LOG.info("Derived LastStateModifiedDate:{} for case:{} from event:{}",
            stateChangeEvent.getCreatedDate(), caseDetailsEntity.getId(), stateChangeEvent.getId());
            // save
            if (!dryRun) {
                caseDetailsEntity.setLastStateModifiedDate(stateChangeEvent.getCreatedDate());
                em.merge(caseDetailsEntity);
            }
        });
    }
}
