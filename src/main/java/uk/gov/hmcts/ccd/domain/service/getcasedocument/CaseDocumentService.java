package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    public void attachCaseDocuments(final CaseDetails beforeCallbackCaseDetails,
                                    final CaseDetails afterCallbackCaseDetails) {

        final Map<String, String> preCallbackHashes = caseDocumentUtils.extractDocumentsHashes(
            beforeCallbackCaseDetails.getData()
        );

        final Map<String, String> postCallbackHashes = caseDocumentUtils.extractDocumentsHashes(
            afterCallbackCaseDetails.getData()
        );

        verifyNoTamper(preCallbackHashes, postCallbackHashes);

        final List<DocumentHashToken> documentHashes = caseDocumentUtils.buildDocumentHashToken(
            preCallbackHashes,
            postCallbackHashes
        );

        if (!documentHashes.isEmpty()) {
            final CaseDocumentsMetadata documentMetadata = CaseDocumentsMetadata.builder()
                .caseId(afterCallbackCaseDetails.getReferenceAsString())
                .caseTypeId(afterCallbackCaseDetails.getCaseTypeId())
                .jurisdictionId(afterCallbackCaseDetails.getJurisdiction())
                .documentHashToken(documentHashes)
                .build();

            caseDocumentAmApiClient.applyPatch(documentMetadata);
        }
    }

    public void validate(final Map<String, JsonNode> originalCaseData,
                         final Map<String, JsonNode> caseDataAfterCallback) {
        if (!applicationParams.isDocumentHashCheckingEnabled()) {
            return;
        }

        final List<JsonNode> preCallbackDocumentNodes = caseDocumentUtils.findDocumentNodes(originalCaseData);

        final List<JsonNode> postCallbackDocumentNodes = caseDocumentUtils.findDocumentNodes(caseDataAfterCallback);

        final List<JsonNode> violatingDocuments = caseDocumentUtils.getViolatingDocuments(
            preCallbackDocumentNodes,
            postCallbackDocumentNodes
        );

        if (!violatingDocuments.isEmpty()) {
            throw new ValidationException("Some message");
        }
    }

    private CaseDetails removeHashes(final CaseDetails caseDetails) {
        final CaseDetails clonedCaseDetails = caseService.clone(caseDetails);

        final List<JsonNode> documentNodes = caseDocumentUtils.findDocumentNodes(clonedCaseDetails.getData());
        documentNodes.forEach(x -> ((ObjectNode) x).remove(DOCUMENT_HASH));

        return clonedCaseDetails;
    }

    private void verifyNoTamper(final Map<String, String> preCallbackHashes,
                                final Map<String, String> postCallbackHashes) {
        final Set<String> tamperedHashes = caseDocumentUtils.getTamperedHashes(preCallbackHashes, postCallbackHashes);

        if (!tamperedHashes.isEmpty()) {
            throw new ServiceException("call back attempted to change the hashToken of the following documents:"
                + tamperedHashes);
        }
    }

}
