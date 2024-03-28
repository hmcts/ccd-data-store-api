package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Qualifier;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.DOCUMENT_URL;
import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.UPLOAD_TIMESTAMP;

@Named
public class CaseDocumentTimestampService {
    private final Clock clock;
    private final ApplicationParams applicationParams;
    private static final String UPLOAD_TIMESTAMP_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSS";

    @Inject
    public CaseDocumentTimestampService(@Qualifier("utcClock") Clock clock,
                                        final ApplicationParams applicationParams
    ) {
        this.clock = clock;
        this.applicationParams = applicationParams;
    }

    public void addUploadTimestamps(CaseDetails caseDetailsModified, CaseDetails caseDetailsInDb) {

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
        addUploadTimestampToDocument(jsonNodes, documentUrlsNew, uploadTimestamp);
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

    protected void addUploadTimestampToDocument(Collection<JsonNode> nodes,
                                                List<String> documentUrlsNew,
                                                String uploadTimestamp) {
        List<JsonNode> jsonNodes = findNodes(nodes);
        jsonNodes.forEach(jsonNode -> {
            if (documentUrlsNew.contains(jsonNode.get(DOCUMENT_URL).asText()) && isToBeUpdatedWithTimestamp(jsonNode)) {
                insertUploadTimestamp(jsonNode, uploadTimestamp);
            }
        });
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
