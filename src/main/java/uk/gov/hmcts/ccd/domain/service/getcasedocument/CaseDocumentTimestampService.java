package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;

import javax.inject.Named;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.DOCUMENT_URL;
import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.UPLOAD_TIMESTAMP;

@Named
public class CaseDocumentTimestampService {

    public void addUploadTimestamps(CaseDetails caseDetailsModified, CaseDetails caseDetailsInDb) {

        List<JsonNode> jsonNodes = findNodes(caseDetailsModified.getData().values());
        List<String> documentUrlsNew = findUrlsNotInOriginal(caseDetailsModified, caseDetailsInDb);

        final String uploadTimestamp = Instant.now().toString();
        addUploadTimestampToDocument(jsonNodes, documentUrlsNew, uploadTimestamp);
    }

    protected List<String> findUrlsNotInOriginal(CaseDetails caseDetailsModified, CaseDetails caseDetailsInDb) {
        List<String> documentUrlsFromDb = getDocumentUrls(caseDetailsInDb);

        List<JsonNode> jsonNodes = findNodes(caseDetailsModified.getData().values());
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
        if (null == caseDetails || null == caseDetails.getData()) {
            return new ArrayList<>();
        }
        List<JsonNode> jsonNodes = findNodes(caseDetails.getData().values());
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

    protected List<JsonNode> findNodes(Collection<JsonNode> nodes) {
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
            if (documentUrlsNew.contains(jsonNode.get(DOCUMENT_URL).asText()) && !jsonNode.has(UPLOAD_TIMESTAMP)) {
                insertUploadTimestamp(jsonNode, uploadTimestamp);
            }
        });
    }

    protected void insertUploadTimestamp(JsonNode node, String uploadTimestamp) {
        if (!node.has(UPLOAD_TIMESTAMP)) {
            ((ObjectNode) node).put(UPLOAD_TIMESTAMP, uploadTimestamp);
        }
    }

}
