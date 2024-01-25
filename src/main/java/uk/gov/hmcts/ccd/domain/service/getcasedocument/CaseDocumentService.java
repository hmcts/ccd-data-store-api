package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jooq.lambda.tuple.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ResourceNotFoundException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import javax.inject.Inject;
import javax.inject.Named;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collection;

import static java.util.Collections.emptyMap;
import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.DOCUMENT_HASH;
import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.DOCUMENT_URL;
import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.UPLOAD_TIMESTAMP;

@Named
public class CaseDocumentService {
    private final CaseService caseService;
    private final CaseDocumentUtils caseDocumentUtils;
    private final ApplicationParams applicationParams;
    private final CaseDocumentAmApiClient caseDocumentAmApiClient;

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseDocumentService.class);

    @Inject
    public CaseDocumentService(final CaseService caseService,
                               final CaseDocumentUtils caseDocumentUtils,
                               final ApplicationParams applicationParams,
                               final CaseDocumentAmApiClient caseDocumentAmApiClient) {
        this.caseService = caseService;
        this.caseDocumentUtils = caseDocumentUtils;
        this.applicationParams = applicationParams;
        this.caseDocumentAmApiClient = caseDocumentAmApiClient;
    }

    public CaseDetails stripDocumentHashes(final CaseDetails caseDetails) {
        final List<JsonNode> documentNodes = caseDocumentUtils.findDocumentNodes(caseDetails.getData());

        // TODO: remove this flag permanently and make this behaviour default.
        if (applicationParams.isDocumentHashCloneEnabled()) {
            return documentNodes.isEmpty() ? caseService.clone(caseDetails) : removeHashes(caseDetails);
        }
        return documentNodes.isEmpty() ? caseDetails : removeHashes(caseDetails);
    }

    public List<DocumentHashToken> extractDocumentHashToken(final Map<String, JsonNode> databaseCaseData,
                                                            final Map<String, JsonNode> preCallbackCaseData,
                                                            final Map<String, JsonNode> postCallbackCaseData) {

        final List<Tuple2<String, String>> dbDocs = caseDocumentUtils.findDocumentsHashes(
            databaseCaseData
        );

        final List<Tuple2<String, String>> eventDocs = caseDocumentUtils.findDocumentsHashes(
            preCallbackCaseData
        );

        final List<Tuple2<String, String>> postCallbackDocs = caseDocumentUtils.findDocumentsHashes(
            postCallbackCaseData
        );

        final List<Tuple2<String, String>> preCallbackDocs = CollectionUtils.listsUnion(dbDocs, eventDocs);

        verifyNoTamper(preCallbackDocs, postCallbackDocs);

        final List<DocumentHashToken> documentHashTokens = caseDocumentUtils.buildDocumentHashToken(
            dbDocs,
            eventDocs,
            postCallbackDocs
        );

        validate(documentHashTokens);

        return documentHashTokens;
    }

    public List<DocumentHashToken> extractDocumentHashToken(final Map<String, JsonNode> preCallbackCaseData,
                                                            final Map<String, JsonNode> postCallbackCaseData) {

        return extractDocumentHashToken(emptyMap(), preCallbackCaseData, postCallbackCaseData);
    }

    public void attachCaseDocuments(final String caseId,
                                    final String caseTypeId,
                                    final String jurisdictionId,
                                    final List<DocumentHashToken> documentHashes) {
        if (applicationParams.isAttachDocumentEnabled()) {
            if (documentHashes.isEmpty()) {
                return;
            }

            final CaseDocumentsMetadata documentMetadata = CaseDocumentsMetadata.builder()
                .caseId(caseId)
                .caseTypeId(caseTypeId)
                .jurisdictionId(jurisdictionId)
                .documentHashTokens(documentHashes)
                .build();

            caseDocumentAmApiClient.applyPatch(documentMetadata);
        }
    }

    void validate(final List<DocumentHashToken> documentHashes) {
        if (!applicationParams.isDocumentHashCheckingEnabled()) {
            return;
        }

        final List<DocumentHashToken> violatingDocuments = caseDocumentUtils.getViolatingDocuments(
            documentHashes
        );

        if (!violatingDocuments.isEmpty()) {
            throw new ValidationException("Document hashTokens are missing for the documents: " + violatingDocuments);
        }
    }

    public void addUploadTimestamps(CaseDetails caseDetails) {
        LOGGER.debug("getDocumentUrlsOnDb() starting ...");
        List<String> documentUrlsFromDb = getDocumentUrlsOnDb(caseDetails.getReferenceAsString());
        LOGGER.debug("getDocumentUrlsOnDb() ended. Size={}", documentUrlsFromDb.size());

        LOGGER.debug("getDocumentUrlsFromRequest() starting ...");
        List<JsonNode> jsonNodes = findNodes(caseDetails.getData().values());
        List<String> documentUrlsFromRequest = getDocumentUrlsFromRequest(jsonNodes);
        LOGGER.debug("getDocumentUrlsFromRequest() ended. Size={}", documentUrlsFromRequest.size());

        LOGGER.debug("findUrlsNotInOriginal() starting ...");
        List<String> documentUrlsNew = findUrlsNotInOriginal(documentUrlsFromDb, documentUrlsFromRequest);
        LOGGER.debug("findUrlsNotInOriginal() ended. Size={}", documentUrlsNew.size());

        final String uploadTimestamp = ZonedDateTime.now().toString();
        addUploadTimestampToDocument(jsonNodes, documentUrlsNew, uploadTimestamp);
    }

    public List<String> getDocumentUrlsOnDb(String reference) {
        CaseDetails caseDetails = null;
        try {
            caseDetails = caseService.getCaseDetailsByCaseReference(reference);
        } catch (ResourceNotFoundException rne) {
            return new ArrayList<>();
        }
        List<JsonNode> jsonNodes = findNodes(caseDetails.getData().values());
        return findDocumentUrls(jsonNodes);
    }

    public List<String> getDocumentUrlsFromRequest(CaseDetails caseDetails) {
        List<JsonNode> jsonNodes = findNodes(caseDetails.getData().values());
        return findDocumentUrls(jsonNodes);
    }

    public List<String> getDocumentUrlsFromRequest(List<JsonNode> jsonNodes) {
        return findDocumentUrls(jsonNodes);
    }

    public List<String> findDocumentUrls(Collection<JsonNode> nodes) {
        List<JsonNode> lstJsonNodes = findNodes(nodes);
        List<String> lstDocumentUrls = new ArrayList<>();
        lstJsonNodes.forEach(node -> lstDocumentUrls.add(node.get(DOCUMENT_URL).asText()));
        return lstDocumentUrls;
    }

    public List<String> findDocumentUrls(List<JsonNode> jsonNodes) {
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

    public List<String> findUrlsNotInOriginal(List<String> dbUrls, List<String> requestUrls) {
        List<String> urlsNotInOriginal = new ArrayList<>();

        for (String url : requestUrls) {
            if (!dbUrls.contains(url)) {
                urlsNotInOriginal.add(url);
            }
        }
        return urlsNotInOriginal;
    }

    public void addUploadTimestampToDocument(Collection<JsonNode> nodes,
                                             List<String> documentUrlsNew,
                                             String uploadTimestamp) {
        List<JsonNode> jsonNodes = findNodes(nodes);
        jsonNodes.forEach(jsonNode -> {
            if (documentUrlsNew.contains(jsonNode.get(DOCUMENT_URL).toString()) && !jsonNode.has(UPLOAD_TIMESTAMP)) {
                insertUploadTimestamp(jsonNode, uploadTimestamp);
            }
        });
    }

    public void insertUploadTimestamp(JsonNode node, String uploadTimestamp) {
        if (!node.has(UPLOAD_TIMESTAMP)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Adding to document_url '{}'", node.findValue(DOCUMENT_URL));
                LOGGER.debug("jsonNode before: '{}'", node);
            }
            ((ObjectNode) node).put(UPLOAD_TIMESTAMP, uploadTimestamp);
            LOGGER.debug("jsonNode after: '{}'", node);
        }
    }

    private CaseDetails removeHashes(final CaseDetails caseDetails) {
        final CaseDetails clonedCaseDetails = caseService.clone(caseDetails);

        final List<JsonNode> documentNodes = caseDocumentUtils.findDocumentNodes(clonedCaseDetails.getData());
        documentNodes.forEach(node -> ((ObjectNode) node).remove(DOCUMENT_HASH));

        return clonedCaseDetails;
    }

    private void verifyNoTamper(final List<Tuple2<String, String>> preCallbackHashes,
                                final List<Tuple2<String, String>> postCallbackHashes) {
        final Set<String> tamperedHashes = caseDocumentUtils.getTamperedHashes(preCallbackHashes, postCallbackHashes);

        if (!tamperedHashes.isEmpty()) {
            throw new ServiceException("Callback attempted to change the hashToken of the following documents:"
                + tamperedHashes);
        }
    }

}
