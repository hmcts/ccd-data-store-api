package uk.gov.hmcts.ccd.domain.service.doclink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import liquibase.util.StringUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventEntity;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;
import uk.gov.hmcts.ccd.data.user.CachedUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamUser;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.jayway.jsonpath.JsonPath.using;
import static uk.gov.hmcts.ccd.domain.service.doclink.DockLinkUtil.*;

@Service
public class DocLinksRestoreService {

    private static final Logger LOG = LoggerFactory.getLogger(DocLinksRestoreService.class);

    public static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static final String START_TIME = "'2019-08-20 15:35:00.000'";
    public static final String END_TIME = "'2019-08-21 12:41:00.000'";
    public static final LocalDateTime BUG_START_TIME = LocalDateTime.parse("2019-08-20 15:35:00", formatter);

    public static final String BETWEEN_CLAUSE = "BETWEEN  " + START_TIME + " AND " + END_TIME;

    public static final String LAST_EVENT_WITH_VALID_DATA = "SELECT * FROM case_event WHERE id IN "
        + "(SELECT max(id) FROM case_event WHERE case_data_id IN :caseIds AND created_date < " + START_TIME + " GROUP BY case_data_id)";

    public static final String EVENTS_DURING_BUG_PERIOD = "SELECT * FROM case_event WHERE case_data_id IN :caseIds "
        + "AND created_date " + BETWEEN_CLAUSE + " order by id DESC ";

    private static final String CASE_QUERY = "SELECT * FROM case_data where reference IN :caseReferences";

    public static final String EVENTS_TO_MARK = "SELECT * FROM case_event WHERE case_data_id = :caseId AND id > :eventId";

    public static final String LATEST_EVENT = "SELECT * FROM case_event WHERE case_data_id = :caseId ORDER BY id DESC LIMIT 1";

    public static final String DOCUMENT_FILENAME = "document_filename";
    public static final String SLASH = "/";

    @PersistenceContext
    private EntityManager em;

    private final UserRepository userRepository;
    private final DataClassificationRestoreService dataClassificationRestoreService;

    private final Configuration jsonPathConfig;

    @Autowired
    public DocLinksRestoreService(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository, DataClassificationRestoreService dataClassificationRestoreService) {
        this.userRepository = userRepository;
        this.dataClassificationRestoreService = dataClassificationRestoreService;
        this.jsonPathConfig = Configuration.builder().jsonProvider(new JacksonJsonProvider())
            .options(Option.AS_PATH_LIST, Option.SUPPRESS_EXCEPTIONS).build();
    }

    @Transactional(readOnly = true)
    public void restoreWithDryRun(List<Long> caseReferences) {
        restoreByCaseReferences(caseReferences, true);
    }

    @Transactional
    public void restoreWithPersist(List<Long> caseReferences) {
        restoreByCaseReferences(caseReferences, false);
    }

    private void restoreByCaseReferences(List<Long> caseReferences, boolean dryRun) {
        Query query = em.createNativeQuery(CASE_QUERY, CaseDetailsEntity.class).setParameter("caseReferences", caseReferences);
        List<CaseDetailsEntity> caseDetailsEntities = query.getResultList();

        List<CaseDetailsEntity> oldCases = caseDetailsEntities.stream().filter(c -> BUG_START_TIME.isAfter(c.getCreatedDate())).collect(Collectors.toList());
        List<CaseDetailsEntity> newCases = caseDetailsEntities.stream().filter(c -> BUG_START_TIME.isBefore(c.getCreatedDate())).collect(Collectors.toList());

        if (CollectionUtils.isNotEmpty(oldCases)) {
            restoreOldCasesBasedOnEventHistory(oldCases, dryRun);
        }
        if (CollectionUtils.isNotEmpty(newCases)) {
            restoreNewCasesBasedOnEventHistory(newCases, dryRun);
        }
    }

    private void restoreOldCasesBasedOnEventHistory(List<CaseDetailsEntity> caseDetailsEntities, boolean dryRun) {
        List<Long> caseIds = caseDetailsEntities.stream().map(CaseDetailsEntity::getId).collect(Collectors.toList());

        Query query = em.createNativeQuery(LAST_EVENT_WITH_VALID_DATA, CaseAuditEventEntity.class).setParameter("caseIds", caseIds);
        List<CaseAuditEventEntity> mostRecentEventsBeforeBug = query.getResultList();

        query = em.createNativeQuery(EVENTS_DURING_BUG_PERIOD, CaseAuditEventEntity.class).setParameter("caseIds", caseIds);
        List<CaseAuditEventEntity> eventsDuringBug = query.getResultList();

        List<CaseAuditEventEntity> allEvents = ListUtils.union(eventsDuringBug, mostRecentEventsBeforeBug);

        findAndRestoreMissedLinks(caseDetailsEntities, allEvents, dryRun);
    }

    private void restoreNewCasesBasedOnEventHistory(List<CaseDetailsEntity> caseDetailsEntities, boolean dryRun) {
        List<Long> caseIds = caseDetailsEntities.stream().map(CaseDetailsEntity::getId).collect(Collectors.toList());
        Query query = em.createNativeQuery(EVENTS_DURING_BUG_PERIOD, CaseAuditEventEntity.class).setParameter("caseIds", caseIds);
        findAndRestoreMissedLinks(caseDetailsEntities, query.getResultList(), dryRun);
    }

    private void findAndRestoreMissedLinks(List<CaseDetailsEntity> caseDetailsEntities, List<CaseAuditEventEntity> allEvents, boolean dryRun) {
        Map<CaseDetailsEntity, List<CaseAuditEventEntity>> caseToEventsMap = getCaseToEventsMap(caseDetailsEntities, allEvents);
        caseToEventsMap.keySet().forEach(key -> restoreMissedLinks(key, caseToEventsMap.get(key), dryRun));
    }

    private void restoreMissedLinks(CaseDetailsEntity caseDetails, List<CaseAuditEventEntity> events, boolean dryRun) {
        Map<String, CaseAuditEventEntity> linksToRecoverFromEvent = findLinksToRecoverForACase(caseDetails, events);
        Map<String, String> recoveredFiles = new HashMap<>();
        linksToRecoverFromEvent.keySet().forEach(jsonPath -> {
            CaseAuditEventEntity event = linksToRecoverFromEvent.get(jsonPath);

            String docNodePath = jsonPath.substring(0, jsonPath.lastIndexOf(SLASH + DOCUMENT_FILENAME)); //Eg : /D8DocumentsUploaded/0/value/DocumentLink
            String docNodeParent = docNodePath.substring(0, docNodePath.lastIndexOf(SLASH)); //Eg : /D8DocumentsUploaded/0/value
            JsonNode eventDocLinkNode = event.getData().at(docNodePath);

            // restore in case data
            JsonNode detailsData = caseDetails.getData();
            JsonNode docNodeParentInCase = detailsData.at(docNodeParent);
            String docNodeName = docNodePath.substring(docNodePath.lastIndexOf(SLASH) + 1);
            JsonNode docNodeInCase = docNodeParentInCase.path(docNodeName);

            // don't touch manually corrected doc-link nodes both in collection and simple field
            if (!docNodeParentInCase.isMissingNode() && (docNodeInCase.isMissingNode() || docNodeInCase.isNull())) {
                ((ObjectNode)docNodeParentInCase).set(docNodeName, eventDocLinkNode);
            } else if (jsonPath.contains(DOT_VALUE)) { // add as a new element
                LOG.info("Found manually removed links in the case which are lost from eventId:{} and jsonPath:{}", event.getId(), jsonPath);
                // TODO : add whole element to end of collection if required
            }

            recoveredFiles.put(eventDocLinkNode.findValuesAsText(DOCUMENT_FILENAME).get(0), eventDocLinkNode.findValuesAsText("document_url").get(0));

            LOG.info("Restored a doc link for case:{} from event:{} with path:{} and value :{}",
                caseDetails.getReference(), event.getId(), docNodePath, detailsData.at(docNodePath).findValuesAsText(DOCUMENT_FILENAME));
        });

        // 1. persist case data
        dataClassificationRestoreService.deduceAndUpdateDataClassification(caseDetails);  // Data classification
        caseDetails.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        em.merge(caseDetails);
        LOG.info("Restored missing links for case:{} with data:{}", caseDetails.getReference(), getJsonString(caseDetails.getData()));
        LOG.info("Restored missing link field info for case:{} with classification:{}", caseDetails.getReference(), getJsonString(caseDetails.getDataClassification()));

        // 2. mark events
        markImpactedEvents(linksToRecoverFromEvent);

        // 3. create admin event
        CaseAuditEventEntity adminEvent = createAdminEvent(caseDetails, recoveredFiles);
        if (!dryRun) {
            em.persist(adminEvent);
        }
        LOG.info("Created adminEvent with id:{}, summary:{} and data:{}", adminEvent.getId(), adminEvent.getSummary(), getJsonString(adminEvent.getData()));
    }

    private Map<String, CaseAuditEventEntity> findLinksToRecoverForACase(CaseDetailsEntity caseDetails, List<CaseAuditEventEntity> events) {
        Map<String, CaseAuditEventEntity> linksToRecoverFromEvent = new HashMap<>();
        events.forEach(event -> {
            List<String> dockUrlPaths = using(jsonPathConfig).parse(getJsonString(event.getData())).read("$..document_filename");
            dockUrlPaths.forEach(bracketPath -> {
                boolean isMissed = isDockLinkMissingInTheCase(bracketPath, caseDetails, event);
                if (isMissed) {
                    String jsonPath = toJsonPtrExpression(bracketPath);
                    linksToRecoverFromEvent.putIfAbsent(jsonPath, event);
                }
            });
        });
        return linksToRecoverFromEvent;
    }

    private void markImpactedEvents(Map<String, CaseAuditEventEntity> linksToRecoverFromEvent) {
        Optional<CaseAuditEventEntity> eventBeforeDocsLost = linksToRecoverFromEvent.values().stream()
            .min(Comparator.comparing(CaseAuditEventEntity::getId));
        eventBeforeDocsLost.ifPresent(event -> {
            List<CaseAuditEventEntity> eventsToMark = getEventsToMark(event);
            eventsToMark.forEach(e -> {
                String summary = StringUtils.isNotEmpty(e.getSummary()) ? e.getSummary() + " AND " : e.getSummary();
                summary = summary
                    + " In this event history if you see any missing document links please check history of the 'Document recovery' event";
                e.setSummary(summary);
                em.merge(e);
            });
        });
    }

    private List<CaseAuditEventEntity> getEventsToMark(CaseAuditEventEntity event) {
        Query query = em.createNativeQuery(EVENTS_TO_MARK, CaseAuditEventEntity.class);
        query.setParameter("eventId", event.getId());
        query.setParameter("caseId", event.getCaseDataId());
        return (List<CaseAuditEventEntity>) query.getResultList();
    }

    private CaseAuditEventEntity createAdminEvent(CaseDetailsEntity caseDetails, Map<String, String> recoveredFiles) {
        CaseAuditEventEntity newCaseAuditEventEntity = new CaseAuditEventEntity();

        newCaseAuditEventEntity.setCaseDataId(caseDetails.getId());
        newCaseAuditEventEntity.setCaseTypeId(caseDetails.getCaseType());
        newCaseAuditEventEntity.setCreatedDate(LocalDateTime.now(ZoneOffset.UTC));
        newCaseAuditEventEntity.setSecurityClassification(caseDetails.getSecurityClassification());
        newCaseAuditEventEntity.setData(caseDetails.getData());
        newCaseAuditEventEntity.setDataClassification(caseDetails.getDataClassification());

        Query query = em.createNativeQuery(LATEST_EVENT, CaseAuditEventEntity.class).setParameter("caseId", caseDetails.getId());
        CaseAuditEventEntity latestEvent = (CaseAuditEventEntity) query.getSingleResult();
        newCaseAuditEventEntity.setStateId(latestEvent.getStateId());
        newCaseAuditEventEntity.setStateName(latestEvent.getStateName());
        newCaseAuditEventEntity.setCaseTypeVersion(latestEvent.getCaseTypeVersion());

        IdamUser idamUser = getIdamUser();
        newCaseAuditEventEntity.setUserId(idamUser.getId());
        newCaseAuditEventEntity.setUserLastName(idamUser.getSurname());
        newCaseAuditEventEntity.setUserFirstName(idamUser.getForename());

        newCaseAuditEventEntity.setEventId("CCD_ADMIN");
        newCaseAuditEventEntity.setEventName("CCD Admin");
        newCaseAuditEventEntity.setSummary("Document links recovered because of a bug:" + recoveredFiles);
        newCaseAuditEventEntity.setDescription("Between 20-08-2019 16:35:57 and 21-08-2019 13:40:05 a bug caused document links to disappear from case data");
        return newCaseAuditEventEntity;
    }

    private IdamUser getIdamUser() {
        return userRepository.getUser();
    }

}
