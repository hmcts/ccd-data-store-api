package uk.gov.hmcts.ccd.domain.service.doclink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventEntity;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.groupingBy;

public class DockLinkUtil {

    private static final Logger LOG = LoggerFactory.getLogger(DockLinkUtil.class);

    public static final String DOCUMENT_FILENAME = "document_filename";
    public static final String DOT_VALUE = "/value/";
    public static final String SLASH = "/";

    public static final Pattern BRACKET_PATTERN = Pattern.compile("\\[(.*?)\\]");

    private static final ObjectMapper mapper = new ObjectMapper();

    private DockLinkUtil() {
    }

    public static String getJsonString(JsonNode eventData) {
        try {
            return mapper.writeValueAsString(eventData);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getCollectionRootPath(String jsonPath) {
        String collectionArray = jsonPath.substring(0, jsonPath.lastIndexOf(DOT_VALUE));
        return collectionArray.substring(0, collectionArray.lastIndexOf(SLASH));
    }

    public static boolean isDockLinkMissingInTheCase(String bracketPath, CaseDetailsEntity caseDetails, CaseAuditEventEntity caseEvent) {
        JsonNode eventData = caseEvent.getData();
        JsonNode caseData = caseDetails.getData();
        String jsonPath = toJsonPtrExpression(bracketPath);
        boolean isMissed = false;
        if (jsonPath.contains(DOT_VALUE)) { // collection field match value
            // Eg: jsonPath : /D8DocumentsUploaded/0/value/DocumentLink/document_filename
            String docNodePath = StringUtils.substringBeforeLast(jsonPath,SLASH + DOCUMENT_FILENAME);
            String docNodeName = docNodePath.substring(docNodePath.lastIndexOf(SLASH) + 1);
            String arrayElementPath = StringUtils.substringBeforeLast(jsonPath, DOT_VALUE);
            String collectionRootPath = StringUtils.substringBeforeLast(arrayElementPath, SLASH);
            String idValue = eventData.at(arrayElementPath + "/id").textValue();

            JsonNode matchingCaseElementNode = findInCaseCollection(caseData, collectionRootPath, idValue);

            // 1. matching id not exists means - manually removed
            // 2. exists but no links node - lost because of bug
            // 3. exists but different link names means - manually corrected / replaced with new link

            if (matchingCaseElementNode.isMissingNode()) {
                LOG.warn("Ignoring manually removed link for case :{} from event :{} with link path :{} and fileName: {}",
                    caseDetails.getReference(), caseEvent.getId(), jsonPath, eventData.at(jsonPath).textValue());
            } else if (matchingCaseElementNode.findValue(docNodeName) == null)  {
                isMissed = true;
            } else if (!matchingCaseElementNode.findValue(docNodeName).findValue(DOCUMENT_FILENAME).textValue()
                .equalsIgnoreCase(eventData.at(jsonPath).textValue())) {
                LOG.warn("Found manually corrected / replaced a new link for case :{} from event :{} with link path :{}",
                    caseDetails.getReference(), caseEvent.getId(), jsonPath);
            }

        } else { // Simple field
            JsonNode dockLinkNode = caseData.at(jsonPath);
            if (dockLinkNode.isMissingNode()) {
                isMissed = true;
            }
        }

        if (isMissed) {
            LOG.info("Document link is missing for case :{} from event :{} with link path :{} and fileName: {}",
                caseDetails.getReference(), caseEvent.getId(), jsonPath, eventData.at(jsonPath).textValue());
        }
        return isMissed;
    }

    public static JsonNode findInCaseCollection(JsonNode caseData, String collectionRootPath, String idValue) {
        JsonNode collectionNode = caseData.at(collectionRootPath);
        if (!collectionNode.isMissingNode()) {
            Iterable<JsonNode> iterable = collectionNode::iterator;
            return StreamSupport.stream(iterable.spliterator(), false)
                .filter(node -> idValue.equalsIgnoreCase(node.findValue("id").textValue()))
                .findFirst()
                .orElse(MissingNode.getInstance());
        }
        return MissingNode.getInstance();
    }

    // simplify this if possible
    // eg: $['D8DocumentsUploaded'][0]['value']['DocumentLink']['document_filename'] to /D8DocumentsUploaded/0/value/DocumentLink/document_filename
    public static String toJsonPtrExpression(String bracketPath) {
        String path = bracketPath.substring(1); // ignore $
        Matcher matcher = BRACKET_PATTERN.matcher(path);
        StringBuilder stringBuilder = new StringBuilder();
        while (matcher.find()) {
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

    public static Map<CaseDetailsEntity, List<CaseAuditEventEntity>> getCaseToEventsMap(
        List<CaseDetailsEntity> caseDetailsEntities, List<CaseAuditEventEntity> allEvents) {
        Map<Long, List<CaseAuditEventEntity>> eventsByCaseIdMap = allEvents.stream().collect(groupingBy(CaseAuditEventEntity::getCaseDataId));

        return caseDetailsEntities.stream()
            .collect(Collectors.toMap(Function.identity(), caseData -> eventsByCaseIdMap.get(caseData.getId())));
    }
}
