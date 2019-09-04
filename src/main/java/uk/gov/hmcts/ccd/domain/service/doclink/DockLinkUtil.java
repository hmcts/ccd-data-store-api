package uk.gov.hmcts.ccd.domain.service.doclink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.data.casedetails.CaseAuditEventEntity;
import uk.gov.hmcts.ccd.data.casedetails.CaseDetailsEntity;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class DockLinkUtil {

    private static final Logger LOG = LoggerFactory.getLogger(DockLinkUtil.class);

    public static final String DOCUMENT_FILENAME = "document_filename";
    public static final String DOT_VALUE = "/value/";
    public static final String SLASH = "/";

    public static Pattern BRACKET_PATTERN = Pattern.compile("\\[(.*?)\\]");

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
        JsonNode dockLinkNode = caseData.at(jsonPath);
        boolean isMissed = false;
        if (dockLinkNode.isMissingNode() || dockLinkNode.isNull()) {
            isMissed = true;
        } else if (jsonPath.contains(DOT_VALUE)) { // collection field match value
            String eventFileName = eventData.at(jsonPath).textValue();
            List<String> allFileNamesInTheCaseCollection = caseData.at(getCollectionRootPath(jsonPath)).findValuesAsText(DOCUMENT_FILENAME);
            isMissed = !allFileNamesInTheCaseCollection.contains(eventFileName);
            if (isMissed) {
                LOG.info("Probably manually corrected link jsonPath:{}, eventId:{}", jsonPath, caseEvent.getId());
            }
        }
        if (isMissed) {
            LOG.info("Document link is missing for case :{} from event :{} with link path :{} and fileName: {}",
                caseDetails.getReference(), caseEvent.getId(), jsonPath, eventData.at(jsonPath).textValue());
        }
        return isMissed;
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

    public static JsonNode merge(JsonNode mainNode, JsonNode updateNode) {

        Iterator<String> fieldNames = updateNode.fieldNames();

        while (fieldNames.hasNext()) {
            String updatedFieldName = fieldNames.next();
            JsonNode valueToBeUpdated = mainNode.get(updatedFieldName);
            JsonNode updatedValue = updateNode.get(updatedFieldName);

            // If the node is an @ArrayNode
            if (valueToBeUpdated != null && valueToBeUpdated.isArray() &&
                updatedValue.isArray()) {
                // running a loop for all elements of the updated ArrayNode
                for (int i = 0; i < updatedValue.size(); i++) {
                    JsonNode updatedChildNode = updatedValue.get(i);
                    // Create a new Node in the node that should be updated, if there was no corresponding node in it
                    // Use-case - where the updateNode will have a new element in its Array
                    if (valueToBeUpdated.size() <= i) {
                        ((ArrayNode) valueToBeUpdated).add(updatedChildNode);
                    }
                    // getting reference for the node to be updated
                    JsonNode childNodeToBeUpdated = valueToBeUpdated.get(i);
                    merge(childNodeToBeUpdated, updatedChildNode);
                }
                // if the Node is an @ObjectNode
            } else if (valueToBeUpdated != null && valueToBeUpdated.isObject()) {
                merge(valueToBeUpdated, updatedValue);
            } else {
                if (mainNode instanceof ObjectNode) {
                    ((ObjectNode) mainNode).replace(updatedFieldName, updatedValue);
                }
            }
        }
        return mainNode;
    }
}
