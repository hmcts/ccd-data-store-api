package uk.gov.hmcts.ccd.domain.service;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.jayway.jsonpath.JsonPath.using;
import static java.util.stream.Collectors.*;

@Component
public class DocLinksRestoreService {
    private static final Logger LOG = LoggerFactory.getLogger(DocLinksRestoreService.class);

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

    public static final String NEW_CASE_EVENTS = "SELECT * FROM case_event WHERE case_data_id IN :caseIds";

    public Pattern BRACKET_PATTERN = Pattern.compile("\\[(.*?)\\]");

    private final ObjectMapper mapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager em;

    private final Configuration jsonPathConfig;

    public DocLinksRestoreService() {
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
        return findInOldCasesBasedOnEventHistory(query.getResultList());
    }

    private List<CaseDetailsEntity> findDocLinksMissedNewCases(List<String> jurisdictionList) {
        Query query = jurisdictionList.isEmpty() ? em.createNativeQuery(NEW_CASE_QUERY, CaseDetailsEntity.class) :
            em.createNativeQuery(NEW_CASE_QUERY_WITH_JUIDS, CaseDetailsEntity.class).setParameter("jids", jurisdictionList);
        return findInNewCasesBasedOnEventHistory(query.getResultList());
    }

    private List<CaseDetailsEntity> findInOldCasesBasedOnEventHistory(List<CaseDetailsEntity> caseDetailsEntities) {
        Map<Long, CaseDetailsEntity> caseMap = getCaseIdMap(caseDetailsEntities);

        Query query = em.createNativeQuery(LAST_EVENT_WITH_VALID_DATA, CaseAuditEventEntity.class).setParameter("caseIds", caseMap.keySet());
        List<CaseAuditEventEntity> lastWellKnownEvents = query.getResultList();

        Map<CaseDetailsEntity, CaseAuditEventEntity> caseToEventsMap = lastWellKnownEvents.stream()
            .collect(Collectors.toMap(event -> caseMap.get(event.getCaseDataId()), Function.identity()));

        return caseToEventsMap.keySet().stream()
            .filter(key -> hasMissingDocuments(key, caseToEventsMap.get(key)))
            .collect(Collectors.toList());
    }

    private List<CaseDetailsEntity> findInNewCasesBasedOnEventHistory(List<CaseDetailsEntity> caseDetailsEntities) {
        Map<Long, CaseDetailsEntity> caseMap = getCaseIdMap(caseDetailsEntities);

        Query query = em.createNativeQuery(NEW_CASE_EVENTS, CaseAuditEventEntity.class).setParameter("caseIds", caseMap.keySet());
        List<CaseAuditEventEntity> caseEvents = query.getResultList();

        Map<Long, List<CaseAuditEventEntity>> eventsByCaseIdMap = caseEvents.stream().collect(groupingBy(CaseAuditEventEntity::getCaseDataId));

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
        String eventDataString;
        try {
            eventDataString = mapper.writeValueAsString(caseEvent.getData());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        List<String> dockUrlPaths = using(jsonPathConfig).parse(eventDataString).read("$..document_url");
        return dockUrlPaths.stream()
            .anyMatch(jsonPath -> isDockLinkMissingInTheCase(jsonPath, caseDetails.getData()));
    }

    private boolean isDockLinkMissingInTheCase(String jsonPath, JsonNode caseData) {
        JsonNode dockLinkNode = findByPath(jsonPath, caseData);
        return dockLinkNode.isMissingNode() || dockLinkNode.isNull();
    }

    private boolean hasMissingDocuments(CaseDetailsEntity caseDetails, List<CaseAuditEventEntity> caseEvents) {
        return caseEvents.stream()
            .anyMatch(event -> hasMissingDocuments(caseDetails, event));
    }

    private void logStats(List<CaseDetailsEntity> oldCases, List<CaseDetailsEntity> newCases) {
        LOG.info("Total number of cases impacted during bug period :{}", oldCases.size() + newCases.size());
        logStats(oldCases, true);
        logStats(newCases, false);
    }

    private void logStats(List<CaseDetailsEntity> cases, boolean isOld) {
        String type = isOld ? "Old" : "New";
        LOG.info("** {} cases stats begin **", type);
        LOG.info("Number of impacted cases :{}", cases.size());
        Map<String, Set<Long>> jurisdictionCaseMap = cases.stream()
            .collect(groupingBy(CaseDetailsEntity::getJurisdiction, mapping(CaseDetailsEntity::getReference, toSet())));
        jurisdictionCaseMap.forEach((key,value) -> {
            LOG.info("Number of impacted cases in {} is :{}", key, value.size());
            LOG.info("Case references for {} are :{}", key, value);
        });
        LOG.info("** {} cases stats end **", type);
    }

    // simplify this if possible
    private JsonNode findByPath(String bracketPath, JsonNode jsonNode) {
        String path = bracketPath.substring(1); // ignore $
        Matcher matcher = BRACKET_PATTERN.matcher(path);
        JsonNode childNode = jsonNode;
        while(matcher.find()) {
            if (childNode.isMissingNode()) {
                return childNode;
            }
            String group = matcher.group(1);
            if (group.startsWith("\'")) {
                childNode = childNode.path(StringUtils.unwrap(group, '\''));
            } else {
                childNode = childNode.path(Integer.valueOf(group));
            }
        }
        return childNode;
    }
}
