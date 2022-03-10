package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jooq.lambda.tuple.Tuple2;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static uk.gov.hmcts.ccd.domain.service.getcasedocument.CaseDocumentUtils.DOCUMENT_HASH;

@Named
public class CaseDocumentService {
    private final CaseService caseService;
    private final CaseDocumentUtils caseDocumentUtils;
    private final ApplicationParams applicationParams;
    private final CaseDocumentAmApiClient caseDocumentAmApiClient;

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
