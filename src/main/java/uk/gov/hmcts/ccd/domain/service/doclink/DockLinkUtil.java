package uk.gov.hmcts.ccd.domain.service.doclink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static java.util.stream.Collectors.groupingBy;

public class DockLinkUtil {

    private static final Logger LOG = LoggerFactory.getLogger(DockLinkUtil.class);

    public static final String DOCUMENT_FILENAME = "document_filename";
    public static final String DOT_VALUE = "/value/";
    public static final String SLASH = "/";

    public static Pattern BRACKET_PATTERN = Pattern.compile("\\[(.*?)\\]");

    private static final ObjectMapper mapper = new ObjectMapper();

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
            LOG.info("Inside DOT_VALUE block jsonPath:{}, eventId:{}", jsonPath, caseEvent.getId());
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

    // simplify this if possible
    // eg: $['D8DocumentsUploaded'][0]['value']['DocumentLink']['document_filename'] to /D8DocumentsUploaded/0/value/DocumentLink/document_filename
    public static String toJsonPtrExpression(String bracketPath) {
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

    public static Map<CaseDetailsEntity, List<CaseAuditEventEntity>> getCaseToEventsMap(List<CaseDetailsEntity> caseDetailsEntities, List<CaseAuditEventEntity> allEvents) {
        Map<Long, List<CaseAuditEventEntity>> eventsByCaseIdMap = allEvents.stream().collect(groupingBy(CaseAuditEventEntity::getCaseDataId));

        return caseDetailsEntities.stream()
            .collect(Collectors.toMap(Function.identity(), caseData -> eventsByCaseIdMap.get(caseData.getId())));
    }

    public static String getFileNameParentNodePath(String jsonPath) {
        return jsonPath.substring(0, jsonPath.lastIndexOf("/document_filename"));
    }
}
