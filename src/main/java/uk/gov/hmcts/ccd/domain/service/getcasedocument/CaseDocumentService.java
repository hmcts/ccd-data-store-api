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

        return documentNodes.isEmpty() ? caseDetails : removeHashes(caseDetails);
    }

    public List<DocumentHashToken> extractDocumentHashToken(final Map<String, JsonNode> preCallbackCaseData,
                                                            final Map<String, JsonNode> postCallbackCaseData) {

        final List<Tuple2<String, String>> preCallbackHashes = caseDocumentUtils.findDocumentsHashes(
            preCallbackCaseData
        );

        final List<Tuple2<String, String>> postCallbackHashes = caseDocumentUtils.findDocumentsHashes(
            postCallbackCaseData
        );

        verifyNoTamper(preCallbackHashes, postCallbackHashes);

        final List<DocumentHashToken> documentHashTokens = caseDocumentUtils.buildDocumentHashToken(
            preCallbackHashes,
            postCallbackHashes
        );

        validate(documentHashTokens);

        return documentHashTokens;
    }

    public void attachCaseDocuments(final String caseId,
                                    final String caseTypeId,
                                    final String jurisdictionId,
                                    final List<DocumentHashToken> documentHashes) {
        if (documentHashes.isEmpty()) {
            return;
        }

        final CaseDocumentsMetadata documentMetadata = CaseDocumentsMetadata.builder()
            .caseId(caseId)
            .caseTypeId(caseTypeId)
            .jurisdictionId(jurisdictionId)
            .documentHashToken(documentHashes)
            .build();

        caseDocumentAmApiClient.applyPatch(documentMetadata);
    }

    void validate(final List<DocumentHashToken> documentHashes) {
        if (!applicationParams.isDocumentHashCheckingEnabled()) {
            return;
        }

        final List<DocumentHashToken> violatingDocuments = caseDocumentUtils.getViolatingDocuments(
            documentHashes
        );

        if (!violatingDocuments.isEmpty()) {
            throw new ValidationException("Some message");  // TODO: suitable error message
        }
    }

    private CaseDetails removeHashes(final CaseDetails caseDetails) {
        final CaseDetails clonedCaseDetails = caseService.clone(caseDetails);

        final List<JsonNode> documentNodes = caseDocumentUtils.findDocumentNodes(clonedCaseDetails.getData());
        documentNodes.forEach(x -> ((ObjectNode) x).remove(DOCUMENT_HASH));

        return clonedCaseDetails;
    }

    private void verifyNoTamper(final List<Tuple2<String, String>> preCallbackHashes,
                                final List<Tuple2<String, String>> postCallbackHashes) {
        final Set<String> tamperedHashes = caseDocumentUtils.getTamperedHashes(preCallbackHashes, postCallbackHashes);

        if (!tamperedHashes.isEmpty()) {
            throw new ServiceException("call back attempted to change the hashToken of the following documents:"
                + tamperedHashes);
        }
    }

}
