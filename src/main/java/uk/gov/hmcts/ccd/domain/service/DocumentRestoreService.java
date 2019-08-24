package uk.gov.hmcts.ccd.domain.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventEntity;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Component
public class DocumentRestoreService {
    private static final Logger LOG = LoggerFactory.getLogger(DocumentRestoreService.class);

    public static final String CASE_QUERY = "SELECT * FROM case_data WHERE last_modified BETWEEN  '2019-08-20 15:50:00.000' AND '2019-08-21 13:30:00.000'";

    public static final String EVENT_QUERY = "SELECT * FROM case_event WHERE id IN " +
        "(SELECT max(id) FROM case_event WHERE case_data_id IN :caseIds AND created_date < '2019-08-20 15:50:00.000' GROUP BY case_data_id)";

    public static final String DOCUMENT_URL = "document_url";

    @PersistenceContext
    private EntityManager em;

    public List<CaseDetailsEntity> findDocumentMissingCases() {
        List<CaseDetailsEntity> caseDetailsEntities = em.createNativeQuery(CASE_QUERY, CaseDetailsEntity.class).getResultList();
        return findMissingDocCasesBasedOnEventHistory(caseDetailsEntities);
    }

    private List<CaseDetailsEntity> findMissingDocCasesBasedOnEventHistory(List<CaseDetailsEntity> caseDetailsEntities) {
        Map<Long, CaseDetailsEntity> caseMap = caseDetailsEntities.stream()
            .collect(Collectors.toMap(CaseDetailsEntity::getId, Function.identity()));

        Query query = em.createNativeQuery(EVENT_QUERY, CaseAuditEventEntity.class);
        query.setParameter("caseIds", caseMap.keySet());

        List<CaseAuditEventEntity> lastWellKnownEvents = query.getResultList();

        Map<CaseDetailsEntity, CaseAuditEventEntity> caseToEventsMap = lastWellKnownEvents.stream()
            .collect(Collectors.toMap(event -> caseMap.get(event.getCaseDataId()), Function.identity()));

        List<CaseDetailsEntity> missingDocumentCases = caseToEventsMap.keySet().stream()
            .filter(key -> hasMissingDocuments(key, caseToEventsMap.get(key)))
            .collect(Collectors.toList());

        // log stats
        logStats(missingDocumentCases);

        return missingDocumentCases;
    }

    private boolean hasMissingDocuments(CaseDetailsEntity caseDetails, CaseAuditEventEntity caseEvent) {
        List<String> eventDocValues = caseEvent.getData().findValuesAsText(DOCUMENT_URL);
        List<String> caseDocValues = caseDetails.getData().findValuesAsText(DOCUMENT_URL);
        return !caseDocValues.containsAll(eventDocValues);
    }

    private void logStats(List<CaseDetailsEntity> missingDocumentCases) {
        LOG.info("Total number of impacted cases :{}", missingDocumentCases.size());
        Map<String, Set<Long>> jurisdictionCaseMap = missingDocumentCases.stream()
            .collect(groupingBy(CaseDetailsEntity::getJurisdiction, mapping(CaseDetailsEntity::getReference, toSet())));
        jurisdictionCaseMap.forEach((key,value) -> {
            LOG.info("Number of impacted cases in {} is :{}", key, value.size());
            LOG.info("Case references for {} are :{}", key, value);
        });
    }
}
