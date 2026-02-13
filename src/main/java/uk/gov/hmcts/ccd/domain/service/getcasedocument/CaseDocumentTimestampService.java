package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.std.CaseFieldValidationError;
import uk.gov.hmcts.ccd.endpoint.exceptions.CaseValidationException;
import org.apache.commons.lang3.StringUtils;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.DOCUMENT_URL;
import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.UPLOAD_TIMESTAMP;
import static uk.gov.hmcts.ccd.domain.types.CollectionValidator.VALUE;

@Named
public class CaseDocumentTimestampService {
    private final Clock clock;
    private final ApplicationParams applicationParams;
    private static final String UPLOAD_TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS";
    private static final String DOCUMENT_FILENAME = "document_filename";
    private static final String HTML_NOT_ALLOWED_MSG = "HTML documents are not permitted for this field";
    private static final String EMPTY_PATH = "";

    @Inject
    public CaseDocumentTimestampService(@Qualifier("utcClock") Clock clock,
                                        final ApplicationParams applicationParams
    ) {
        this.clock = clock;
        this.applicationParams = applicationParams;
    }

    public void addUploadTimestamps(CaseDetails caseDetailsModified, CaseDetails caseDetailsInDb) {
        addUploadTimestamps(caseDetailsModified, caseDetailsInDb, null);
    }

    public void addUploadTimestamps(CaseDetails caseDetailsModified,
                                    CaseDetails caseDetailsInDb,
                                    CaseTypeDefinition caseTypeDefinition) {

        if (!isCaseTypeUploadTimestampFeatureEnabled(caseDetailsModified.getCaseTypeId())) {
            return;
        }

        List<JsonNode> jsonNodes = new ArrayList<>();
        if (null != caseDetailsModified && null != caseDetailsModified.getData()) {
            jsonNodes = findNodes(caseDetailsModified.getData().values());
        }
        List<String> documentUrlsNew = findUrlsNotInOriginal(caseDetailsModified, caseDetailsInDb);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(UPLOAD_TIMESTAMP_PATTERN);
        final String uploadTimestamp = LocalDateTime.now(clock).format(formatter);
        if (caseTypeDefinition == null
            || caseTypeDefinition.getCaseFieldDefinitions() == null
            || caseTypeDefinition.getCaseFieldDefinitions().isEmpty()) {
            addUploadTimestampToDocument(jsonNodes, documentUrlsNew, uploadTimestamp);
            return;
        }
        addUploadTimestampToDocument(caseDetailsModified.getData(),
            caseTypeDefinition.getCaseFieldDefinitions(),
            EMPTY_PATH,
            documentUrlsNew,
            uploadTimestamp);
    }

    protected List<String> findUrlsNotInOriginal(CaseDetails caseDetailsModified, CaseDetails caseDetailsInDb) {
        List<String> documentUrlsFromDb = getDocumentUrls(caseDetailsInDb);

        List<JsonNode> jsonNodes = new ArrayList<>();
        if (null != caseDetailsModified && null != caseDetailsModified.getData()) {
            jsonNodes = findNodes(caseDetailsModified.getData().values());
        }
        List<String> documentUrlsFromRequest = getDocumentUrls(jsonNodes);

        return findUrlsNotInOriginal(documentUrlsFromDb, documentUrlsFromRequest);
    }

    protected List<String> findUrlsNotInOriginal(List<String> dbUrls, List<String> requestUrls) {
        List<String> urlsNotInOriginal = new ArrayList<>();

        for (String url : requestUrls) {
            if (!dbUrls.contains(url)) {
                urlsNotInOriginal.add(url);
            }
        }
        return urlsNotInOriginal;
    }

    protected List<String> getDocumentUrls(CaseDetails caseDetails) {
        List<JsonNode> jsonNodes = new ArrayList<>();
        if (null != caseDetails && null != caseDetails.getData()) {
            jsonNodes = findNodes(caseDetails.getData().values());
        }
        return findDocumentUrls(jsonNodes);
    }

    protected List<String> getDocumentUrls(List<JsonNode> jsonNodes) {
        return findDocumentUrls(jsonNodes);
    }

    protected List<String> findDocumentUrls(Collection<JsonNode> nodes) {
        List<JsonNode> lstJsonNodes = findNodes(nodes);
        List<String> lstDocumentUrls = new ArrayList<>();
        lstJsonNodes.forEach(node -> lstDocumentUrls.add(node.get(DOCUMENT_URL).asText()));
        return lstDocumentUrls;
    }

    protected List<String> findDocumentUrls(List<JsonNode> jsonNodes) {
        List<String> documentUrls = new ArrayList<>();
        jsonNodes.forEach(node -> documentUrls.add(node.get(DOCUMENT_URL).asText()));
        return documentUrls;
    }

    public List<JsonNode> findNodes(Collection<JsonNode> nodes) {
        return nodes.stream()
            .map(node -> node.findParents(DOCUMENT_URL))
            .flatMap(List::stream)
            .toList();
    }

    private void addUploadTimestampToDocument(Map<String, JsonNode> data,
                                              List<CaseFieldDefinition> caseFieldDefinitions,
                                              String fieldPathPrefix,
                                              List<String> documentUrlsNew,
                                              String uploadTimestamp) {
        if (data == null) {
            return;
        }
        ObjectNode rootNode = JsonNodeFactory.instance.objectNode();
        data.forEach(rootNode::set);
        addUploadTimestampToDocument(rootNode, caseFieldDefinitions, fieldPathPrefix, documentUrlsNew, uploadTimestamp);
    }

    private void addUploadTimestampToDocument(JsonNode dataNode,
                                              List<CaseFieldDefinition> caseFieldDefinitions,
                                              String fieldPathPrefix,
                                              List<String> documentUrlsNew,
                                              String uploadTimestamp) {
        if (dataNode == null || !dataNode.isObject()) {
            return;
        }
        for (CaseFieldDefinition caseFieldDefinition : caseFieldDefinitions) {
            JsonNode fieldValue = dataNode.get(caseFieldDefinition.getId());
            if (fieldValue == null || fieldValue.isNull()) {
                continue;
            }
            FieldTypeDefinition fieldTypeDefinition = caseFieldDefinition.getFieldTypeDefinition();
            if (fieldTypeDefinition == null) {
                continue;
            }
            String fieldPath = fieldPathPrefix + caseFieldDefinition.getId();

            if (FieldTypeDefinition.DOCUMENT.equalsIgnoreCase(fieldTypeDefinition.getType())) {
                processDocumentNode(fieldValue, fieldTypeDefinition, fieldPath, documentUrlsNew, uploadTimestamp);
            } else if (fieldTypeDefinition.isComplexFieldType()) {
                addUploadTimestampToDocument(fieldValue, fieldTypeDefinition.getComplexFields(),
                    fieldPath + ".", documentUrlsNew, uploadTimestamp);
            } else if (fieldTypeDefinition.isCollectionFieldType()) {
                processCollection(fieldValue, fieldTypeDefinition, fieldPath, documentUrlsNew, uploadTimestamp);
            }
        }
    }

    protected void addUploadTimestampToDocument(Collection<JsonNode> nodes,
                                                List<String> documentUrlsNew,
                                                String uploadTimestamp) {
        List<JsonNode> jsonNodes = findNodes(nodes);
        jsonNodes.forEach(jsonNode -> {
            JsonNode documentUrl = jsonNode.get(DOCUMENT_URL);
            if (documentUrl != null
                && documentUrlsNew.contains(documentUrl.asText())
                && isToBeUpdatedWithTimestamp(jsonNode)) {
                insertUploadTimestamp(jsonNode, uploadTimestamp);
            }
        });
    }

    private void processCollection(JsonNode fieldValue,
                                   FieldTypeDefinition fieldTypeDefinition,
                                   String fieldPath,
                                   List<String> documentUrlsNew,
                                   String uploadTimestamp) {
        if (!fieldValue.isArray()) {
            return;
        }
        int index = 0;
        for (JsonNode item : fieldValue) {
            JsonNode itemValue = item.get(VALUE);
            if (itemValue == null || itemValue.isNull()) {
                index++;
                continue;
            }
            String itemPath = fieldPath + "." + index;
            FieldTypeDefinition collectionType = fieldTypeDefinition.getCollectionFieldTypeDefinition();
            if (collectionType == null) {
                index++;
                continue;
            }
            if (FieldTypeDefinition.DOCUMENT.equalsIgnoreCase(collectionType.getType())) {
                processDocumentNode(itemValue, collectionType, itemPath, documentUrlsNew, uploadTimestamp);
            } else if (collectionType.isComplexFieldType()) {
                addUploadTimestampToDocument(itemValue, collectionType.getComplexFields(),
                    itemPath + ".", documentUrlsNew, uploadTimestamp);
            }
            index++;
        }
    }

    private void processDocumentNode(JsonNode documentNode,
                                     FieldTypeDefinition fieldTypeDefinition,
                                     String fieldPath,
                                     List<String> documentUrlsNew,
                                     String uploadTimestamp) {
        if (documentNode == null || !documentNode.has(DOCUMENT_URL)) {
            return;
        }
        final JsonNode documentUrlNode = documentNode.get(DOCUMENT_URL);
        if (documentUrlNode == null || documentUrlNode.isNull()) {
            return;
        }
        final String documentUrl = documentUrlNode.asText();
        if (!documentUrlsNew.contains(documentUrl)) {
            return;
        }

        final String filename = getDocumentFilename(documentNode);
        if (isHtmlFilename(filename) && !fieldAllowsHtml(fieldTypeDefinition, filename)) {
            throw new CaseValidationException(List.of(new CaseFieldValidationError(fieldPath, HTML_NOT_ALLOWED_MSG)));
        }

        if (isToBeUpdatedWithTimestamp(documentNode)) {
            insertUploadTimestamp(documentNode, uploadTimestamp);
        }
    }

    private String getDocumentFilename(JsonNode documentNode) {
        if (documentNode.has(DOCUMENT_FILENAME) && documentNode.get(DOCUMENT_FILENAME).isTextual()) {
            return documentNode.get(DOCUMENT_FILENAME).asText();
        }
        return null;
    }

    private boolean isHtmlFilename(String filename) {
        if (filename == null) {
            return false;
        }
        String lower = filename.toLowerCase(Locale.ROOT);
        return lower.endsWith(".html") || lower.endsWith(".htm");
    }

    private boolean fieldAllowsHtml(FieldTypeDefinition fieldTypeDefinition, String filename) {
        String regex = fieldTypeDefinition.getRegularExpression();
        if (StringUtils.isBlank(regex) || StringUtils.isBlank(filename)) {
            return false;
        }
        // If the value looks like a real regex, use it directly against the filename.
        if (looksLikeRegex(regex)) {
            try {
                return Pattern.compile(regex, Pattern.CASE_INSENSITIVE).matcher(filename).matches();
            } catch (PatternSyntaxException ex) {
                return false;
            }
        }
        Set<String> extSet = Arrays.stream(regex.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> s.startsWith(".") ? s.toLowerCase(Locale.ROOT) : ("." + s.toLowerCase(Locale.ROOT)))
            .collect(Collectors.toSet());
        return extSet.contains(".html") || extSet.contains(".htm");
    }

    private boolean looksLikeRegex(String value) {
        // Heuristic: if it contains regex metacharacters, treat it as regex.
        return value.matches(".*[\\[\\]\\(\\)\\|\\+\\*\\?].*");
    }

    protected void insertUploadTimestamp(JsonNode node, String uploadTimestamp) {
        if (isToBeUpdatedWithTimestamp(node)) {
            ((ObjectNode) node).put(UPLOAD_TIMESTAMP, uploadTimestamp);
        }
    }

    protected boolean isToBeUpdatedWithTimestamp(JsonNode node) {
        return (!node.has(UPLOAD_TIMESTAMP)
            || (node.has(UPLOAD_TIMESTAMP) && node.get(UPLOAD_TIMESTAMP).isNull()));
    }

    public boolean isCaseTypeUploadTimestampFeatureEnabled(String caseTypeId) {
        return (null != applicationParams.getUploadTimestampFeaturedCaseTypes()
            && applicationParams.getUploadTimestampFeaturedCaseTypes().contains(caseTypeId));

    }

}
