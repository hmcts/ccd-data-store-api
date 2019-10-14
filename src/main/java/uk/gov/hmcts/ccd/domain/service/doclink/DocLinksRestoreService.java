package uk.gov.hmcts.ccd.domain.service.doclink;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
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
    public static final String END_TIME = "'2019-08-21 13:41:00.000'";
    public static final LocalDateTime BUG_START_TIME = LocalDateTime.parse("2019-08-20 15:35:00", formatter);

    public static final String BETWEEN_CLAUSE = "BETWEEN  " + START_TIME + " AND " + END_TIME;

    public static final String LAST_EVENT_WITH_VALID_DATA = "SELECT * FROM case_event WHERE id IN "
        + "(SELECT max(id) FROM case_event WHERE case_data_id IN :caseIds AND created_date < " + START_TIME + " GROUP BY case_data_id)";

    public static final String EVENTS_DURING_BUG_PERIOD = "SELECT * FROM case_event WHERE case_data_id IN :caseIds "
        + "AND created_date " + BETWEEN_CLAUSE + " order by id DESC ";

    private static final String CASE_QUERY = "SELECT * FROM case_data where reference IN :caseReferences";

    public static final String EVENTS_TO_MARK = "SELECT * FROM case_event WHERE case_data_id = :caseId AND id > :eventId AND created_date < " + END_TIME;

    public static final String LATEST_EVENT = "SELECT * FROM case_event WHERE case_data_id = :caseId ORDER BY id DESC LIMIT 1";

    public static final String DOCUMENT_FILENAME = "document_filename";
    public static final String SLASH = "/";

    @PersistenceContext
    private EntityManager em;

    private final UserRepository userRepository;
    private final DataClassificationRestoreService dataClassificationRestoreService;

    private final Configuration jsonPathConfig;

    @Autowired
    public DocLinksRestoreService(@Qualifier(CachedUserRepository.QUALIFIER) UserRepository userRepository,
                                  DataClassificationRestoreService dataClassificationRestoreService) {
        this.userRepository = userRepository;
        this.dataClassificationRestoreService = dataClassificationRestoreService;
        this.jsonPathConfig = Configuration.builder().jsonProvider(new JacksonJsonProvider())
            .options(Option.AS_PATH_LIST, Option.SUPPRESS_EXCEPTIONS).build();
    }

    @Transactional(readOnly = true)
    public List<CaseDetailsEntity> restoreWithDryRun(List<Long> caseReferences) {
        return restoreByCaseReferences(caseReferences, true);
    }

    @Transactional
    public List<CaseDetailsEntity> restoreWithPersist(List<Long> caseReferences) {
        return restoreByCaseReferences(caseReferences, false);
    }

    private List<CaseDetailsEntity> restoreByCaseReferences(List<Long> caseReferences, boolean dryRun) {
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
        return ListUtils.union(oldCases, newCases);
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
        JsonNode detailsData = caseDetails.getData();
        linksToRecoverFromEvent.keySet().forEach(jsonPath -> {
            CaseAuditEventEntity event = linksToRecoverFromEvent.get(jsonPath);

            // Eg: jsonPath : /D8DocumentsUploaded/0/value/DocumentLink/document_filename
            String docNodePath = StringUtils.substringBeforeLast(jsonPath,SLASH + DOCUMENT_FILENAME); //Eg : /D8DocumentsUploaded/0/value/DocumentLink
            String docNodeParent = StringUtils.substringBeforeLast(docNodePath, SLASH); //Eg : /D8DocumentsUploaded/0/value
            String docNodeName = docNodePath.substring(docNodePath.lastIndexOf(SLASH) + 1);
            JsonNode eventDocLinkNode = event.getData().at(docNodePath);

            // restore in case data
            JsonNode docNodeParentInCase = detailsData.at(docNodeParent);

            if (jsonPath.contains(DOT_VALUE)) { // collection field match value
                String arrayElementPath = StringUtils.substringBeforeLast(jsonPath, DOT_VALUE);
                String collectionRootPath = StringUtils.substringBeforeLast(arrayElementPath, SLASH);
                String idValue = event.getData().at(arrayElementPath + "/id").textValue();

                JsonNode matchingCaseElementNode = findInCaseCollection(detailsData, collectionRootPath, idValue);

                if (!matchingCaseElementNode.isMissingNode() && matchingCaseElementNode.findValue(docNodeName) == null)  {
                    JsonNode parentNode = matchingCaseElementNode.findValue(StringUtils.substringAfterLast(docNodeParent, SLASH));
                    ((ObjectNode)parentNode).set(docNodeName, eventDocLinkNode);
                }

            } else if (!docNodeParentInCase.isMissingNode() && (docNodeParentInCase.path(docNodeName).isMissingNode())) {
                ((ObjectNode)docNodeParentInCase).set(docNodeName, eventDocLinkNode);
            }

            recoveredFiles.put(eventDocLinkNode.findValuesAsText(DOCUMENT_FILENAME).get(0), eventDocLinkNode.findValuesAsText("document_url").get(0));

            LOG.info("Restored a doc link for case:{} from event:{} with path:{} and value :{}",
                caseDetails.getReference(), event.getId(), docNodePath, detailsData.at(docNodePath).findValuesAsText(DOCUMENT_FILENAME));
        });

        if (!recoveredFiles.isEmpty()) {
            CaseAuditEventEntity eventBeforeDocsLost = linksToRecoverFromEvent.values().stream()
                .min(Comparator.comparing(CaseAuditEventEntity::getId)).get();
            saveToDatabase(caseDetails, dryRun, eventBeforeDocsLost, recoveredFiles);
        } else {
            LOG.info("No Missing links identified for case :{}", caseDetails.getReference());
        }
    }

    private void saveToDatabase(CaseDetailsEntity caseDetails, boolean dryRun, CaseAuditEventEntity eventBeforeDocsLost, Map<String, String> recoveredFiles) {
        // 1. persist case data
        dataClassificationRestoreService.deduceAndUpdateDataClassification(caseDetails);  // Data classification
        caseDetails.setLastModified(LocalDateTime.now(ZoneOffset.UTC));
        em.merge(caseDetails);

        // 2. mark events
        markImpactedEvents(eventBeforeDocsLost);

        // 3. create admin event
        CaseAuditEventEntity adminEvent = createAdminEvent(caseDetails, recoveredFiles);
        if (!dryRun) {
            em.persist(adminEvent);
        }
        LOG.info("Created adminEvent with id:{}, summary:{} and description:{}", adminEvent.getId(), adminEvent.getSummary(), adminEvent.getDescription());
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

    private void markImpactedEvents(CaseAuditEventEntity eventBeforeDocsLost) {
        List<CaseAuditEventEntity> eventsToMark = getEventsToMark(eventBeforeDocsLost);
        eventsToMark.forEach(e -> {
            String summary =  StringUtils.defaultIfEmpty(e.getSummary(), "") + " ***Event contains erroneously removed documents.***";
            e.setSummary(summary);

            String description =  StringUtils.defaultIfEmpty(e.getDescription(), "")
                + " ***This event had documents erroneously removed during system maintenance. They have been reattached in the above 'System Maintenance' event."
                + " Please check the documents currently attached to the case are as expected.***";
            e.setDescription(description);
            em.merge(e);
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

        newCaseAuditEventEntity.setEventId("SYSTEM_MAINTENANCE");
        newCaseAuditEventEntity.setEventName("System Maintenance");
        newCaseAuditEventEntity.setSummary("Checks required for reattached documents");
        newCaseAuditEventEntity.setDescription("Documents removed from this case in error during maintenance between" +
            " 20-08-2019 16:35 and 21-08-2019 13:41 have been reattached. Please check the documents attached to this case are as expected");
        return newCaseAuditEventEntity;
    }

    private IdamUser getIdamUser() {
        return userRepository.getUser();
    }

}
