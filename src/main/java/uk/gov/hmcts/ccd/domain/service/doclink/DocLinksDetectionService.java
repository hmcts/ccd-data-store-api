package uk.gov.hmcts.ccd.domain.service.doclink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventEntity;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.jayway.jsonpath.JsonPath.using;
import static java.util.Collections.EMPTY_LIST;
import static java.util.stream.Collectors.*;

@Component
public class DocLinksDetectionService {
    private static final Logger LOG = LoggerFactory.getLogger(DocLinksDetectionService.class);

    public static final String START_TIME = "'2019-08-20 15:50:00.000'";
    public static final String END_TIME = "'2019-08-21 13:30:00.000'";
    public static final String BETWEEN_CLAUSE = "BETWEEN  " + START_TIME + " AND " + END_TIME;

    public static final String OLD_CASE_QUERY = "SELECT * FROM case_data WHERE created_date < " + START_TIME + " AND id IN " +
        "(SELECT distinct(case_data_id) FROM case_event WHERE created_date " + BETWEEN_CLAUSE + ")";
    public static final String OLD_CASE_QUERY_WITH_JUIDS = OLD_CASE_QUERY + " AND jurisdiction IN :jids";

    public static final String LAST_EVENT_WITH_VALID_DATA = "SELECT * FROM case_event WHERE id IN " +
        "(SELECT max(id) FROM case_event WHERE case_data_id IN :caseIds AND created_date < " + START_TIME + " GROUP BY case_data_id)";

    public static final String NEW_CASE_QUERY = "SELECT * FROM case_data WHERE created_date " + BETWEEN_CLAUSE;
    public static final String NEW_CASE_QUERY_WITH_JUIDS = NEW_CASE_QUERY +  " AND jurisdiction IN :jids";

    public static final String EVENTS_DURING_BUG_PERIOD = "SELECT * FROM case_event WHERE case_data_id IN :caseIds AND created_date " + BETWEEN_CLAUSE;

    public static final String DOCUMENT_FILENAME = "document_filename";
    public static final String DOT_VALUE = "/value/";
    public static final String SLASH = "/";

    public Pattern BRACKET_PATTERN = Pattern.compile("\\[(.*?)\\]");

    private final ObjectMapper mapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager em;

    private final Configuration jsonPathConfig;

    public DocLinksDetectionService() {
        this.jsonPathConfig = Configuration.builder().jsonProvider(new JacksonJsonProvider())
            .options(Option.AS_PATH_LIST, Option.SUPPRESS_EXCEPTIONS).build();
    }

    public List<CaseDetailsEntity> findDocLinksMissedCases(List<String> jurisdictionList) {
        // missing links in old cases due to an edit during bug window
        List<CaseDetailsEntity> oldCases = findDocLinksMissedOldCases(jurisdictionList);
        // missing links in newly created cases during bug window
        List<CaseDetailsEntity> newCases = findDocLinksMissedNewCases(jurisdictionList);
        logStats(oldCases, newCases);
        return ListUtils.union(oldCases, newCases);
    }

    private List<CaseDetailsEntity> findDocLinksMissedOldCases(List<String> jurisdictionList) {
        Query query = jurisdictionList.isEmpty() ? em.createNativeQuery(OLD_CASE_QUERY, CaseDetailsEntity.class) :
            em.createNativeQuery(OLD_CASE_QUERY_WITH_JUIDS, CaseDetailsEntity.class).setParameter("jids", jurisdictionList);
        List<CaseDetailsEntity> resultList = query.getResultList();
        return resultList.isEmpty() ? EMPTY_LIST : findInOldCasesBasedOnEventHistory(query.getResultList());
    }

    private List<CaseDetailsEntity> findDocLinksMissedNewCases(List<String> jurisdictionList) {
        Query query = jurisdictionList.isEmpty() ? em.createNativeQuery(NEW_CASE_QUERY, CaseDetailsEntity.class) :
            em.createNativeQuery(NEW_CASE_QUERY_WITH_JUIDS, CaseDetailsEntity.class).setParameter("jids", jurisdictionList);
        List<CaseDetailsEntity> resultList = query.getResultList();
        return resultList.isEmpty() ? EMPTY_LIST : findInNewCasesBasedOnEventHistory(query.getResultList());
    }

    private List<CaseDetailsEntity> findInOldCasesBasedOnEventHistory(List<CaseDetailsEntity> caseDetailsEntities) {
        Map<Long, CaseDetailsEntity> caseMap = getCaseIdMap(caseDetailsEntities);

        Query query = em.createNativeQuery(LAST_EVENT_WITH_VALID_DATA, CaseAuditEventEntity.class).setParameter("caseIds", caseMap.keySet());

        List<CaseAuditEventEntity> mostRecentEventsBeforeBug = query.getResultList();

        query = em.createNativeQuery(EVENTS_DURING_BUG_PERIOD, CaseAuditEventEntity.class).setParameter("caseIds", caseMap.keySet());

        List<CaseAuditEventEntity> eventsDuringBug = query.getResultList();

        List<CaseAuditEventEntity> allEvents = ListUtils.union(mostRecentEventsBeforeBug, eventsDuringBug);

        return findMissedCases(caseDetailsEntities, allEvents);
    }

    private List<CaseDetailsEntity> findInNewCasesBasedOnEventHistory(List<CaseDetailsEntity> caseDetailsEntities) {
        Map<Long, CaseDetailsEntity> caseMap = getCaseIdMap(caseDetailsEntities);

        Query query = em.createNativeQuery(EVENTS_DURING_BUG_PERIOD, CaseAuditEventEntity.class).setParameter("caseIds", caseMap.keySet());
        List<CaseAuditEventEntity> caseEvents = query.getResultList();

        return findMissedCases(caseDetailsEntities, caseEvents);
    }

    private List<CaseDetailsEntity> findMissedCases(List<CaseDetailsEntity> caseDetailsEntities, List<CaseAuditEventEntity> allEvents) {
        Map<Long, List<CaseAuditEventEntity>> eventsByCaseIdMap = allEvents.stream().collect(groupingBy(CaseAuditEventEntity::getCaseDataId));

        Map<CaseDetailsEntity, List<CaseAuditEventEntity>> caseToEventsMap = caseDetailsEntities.stream()
            .collect(Collectors.toMap(Function.identity(), caseData -> eventsByCaseIdMap.get(caseData.getId())));

        return caseToEventsMap.keySet().stream()
            .filter(key -> hasMissingDocuments(key, caseToEventsMap.get(key)))
            .collect(Collectors.toList());
    }

    private Map<Long, CaseDetailsEntity> getCaseIdMap(List<CaseDetailsEntity> caseDetailsEntities) {
        return caseDetailsEntities.stream()
                .collect(Collectors.toMap(CaseDetailsEntity::getId, Function.identity()));
    }

    private boolean hasMissingDocuments(CaseDetailsEntity caseDetails, CaseAuditEventEntity caseEvent) {
        JsonNode eventData = caseEvent.getData();
        String eventDataString = getJsonString(eventData);
        List<String> dockUrlPaths = using(jsonPathConfig).parse(eventDataString).read("$..document_filename");
        List<Boolean> missedLinks = dockUrlPaths.stream()
            .map(jsonPath -> isDockLinkMissingInTheCase(jsonPath, caseDetails, caseEvent))
            .collect(toList()); // checking all instead of anyMatch to find all missing Links
        return missedLinks.contains(true);
    }

    private boolean isDockLinkMissingInTheCase(String bracketPath, CaseDetailsEntity caseDetails, CaseAuditEventEntity caseEvent) {
        JsonNode eventData = caseEvent.getData();
        JsonNode caseData = caseDetails.getData();
        String jsonPath = toJsonPtrExpression(bracketPath);
        JsonNode dockLinkNode = caseData.at(jsonPath);
        boolean isMissed = false;
        if (dockLinkNode.isMissingNode() || dockLinkNode.isNull()) {
            isMissed = true;
        } else if (jsonPath.contains(DOT_VALUE)) { // collection field match value
            String eventFileName = eventData.at(jsonPath).textValue();
            List<String> allFileNamesInTheCaseCollection = caseData.at(getCollectionRootPath(jsonPath)).findValuesAsText(DOCUMENT_FILENAME);
            isMissed = !allFileNamesInTheCaseCollection.contains(eventFileName);
        }
        if (isMissed) {
            LOG.info("Document link is missing for case :{} from event :{} with link path :{} and fileName: {}",
                caseDetails.getReference(), caseEvent.getId(), jsonPath, eventData.at(jsonPath).textValue());
        }
        return isMissed;
    }

    private boolean hasMissingDocuments(CaseDetailsEntity caseDetails, List<CaseAuditEventEntity> caseEvents) {
        List<Boolean> missedLinks = caseEvents.stream()
            .map(event -> hasMissingDocuments(caseDetails, event))
            .collect(toList()); // checking all instead of anyMatch to find all missing Links
        return missedLinks.contains(true);
    }

    // simplify this if possible
    // eg: $['D8DocumentsUploaded'][0]['value']['DocumentLink']['document_filename'] to /D8DocumentsUploaded/0/value/DocumentLink/document_ur
    private String toJsonPtrExpression(String bracketPath) {
        String path = bracketPath.substring(1); // ignore $
        Matcher matcher = BRACKET_PATTERN.matcher(path);
        StringBuilder stringBuilder = new StringBuilder();
        while(matcher.find()) {
            String group = matcher.group(1);
            stringBuilder.append(SLASH);
            if (group.startsWith("\'")) {
                stringBuilder.append(StringUtils.unwrap(group, '\''));
            } else {
                stringBuilder.append(group);
            }
        }
        return stringBuilder.toString();
    }

    private String getCollectionRootPath(String jsonPath) {
        String collectionArray = jsonPath.substring(0, jsonPath.lastIndexOf(DOT_VALUE));
        return collectionArray.substring(0, collectionArray.lastIndexOf(SLASH));
    }

    private String getJsonString(JsonNode eventData) {
        try {
            return mapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private void logStats(List<CaseDetailsEntity> oldCases, List<CaseDetailsEntity> newCases) {
        LOG.info("Total number of cases impacted during bug period :{}", oldCases.size() + newCases.size());
        logStatsByJurisdiction(ListUtils.union(oldCases, newCases));
    }

    private void logStatsByJurisdiction(List<CaseDetailsEntity> cases) {

        Map<String, List<CaseDetailsEntity>> byJurisdictionMap = cases.stream()
            .collect(groupingBy(CaseDetailsEntity::getJurisdiction));

        byJurisdictionMap.forEach((key,jCases) -> {
            LOG.info("Number of impacted cases in Jurisdiction: {} is :{}", key, jCases.size());

            Map<String, Set<Long>> byCaseTypeMap = byJurisdictionMap.get(key).stream()
                .collect(groupingBy(CaseDetailsEntity::getCaseType, mapping(CaseDetailsEntity::getReference, toSet())));

            byCaseTypeMap.forEach((caseType, caseTypeCases) -> {
                LOG.info("Number of impacted cases in CaseType: {} is :{}", caseType, caseTypeCases.size());
                LOG.info("Case references for {} are :{}", caseType, caseTypeCases);
                });
        });
    }
}
