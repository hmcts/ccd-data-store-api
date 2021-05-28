package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseDocumentsMetadata;
import uk.gov.hmcts.ccd.domain.service.common.CaseService;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentHashToken;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Named
public class CaseDocumentService {
    private static final String DOCUMENT_HASH = "hashToken";//"document_hash";

    private final CaseService caseService;
    private final CaseDocumentUtils caseDocumentUtils;
    private final CaseDocumentAmApiClient caseDocumentAmApiClient;

    @Inject
    public CaseDocumentService(final CaseService caseService,
                               final CaseDocumentUtils caseDocumentUtils,
                               final CaseDocumentAmApiClient caseDocumentAmApiClient) {
        this.caseService = caseService;
        this.caseDocumentUtils = caseDocumentUtils;
        this.caseDocumentAmApiClient = caseDocumentAmApiClient;
    }

    public CaseDetails cloneCaseDetailsWithoutHashes(final CaseDetails caseDetails) {
        final CaseDetails clonedCaseDetails = caseService.clone(caseDetails);
        removeHashes(clonedCaseDetails.getData());

        return clonedCaseDetails;
    }

    public void attachCaseDocuments(final CaseDetails beforeCallbackCaseDetails,
                                    final CaseDetails afterCallbackCaseDetails) {

        final Map<String, String> preCallbackHashes = caseDocumentUtils.extractDocumentsHashes(
            beforeCallbackCaseDetails.getData()
        );

        final Map<String, String> postCallbackHashes = caseDocumentUtils.extractDocumentsHashes(
            afterCallbackCaseDetails.getData()
        );

        ensureNoTemper(preCallbackHashes, postCallbackHashes);

        final List<DocumentHashToken> documentHashes = caseDocumentUtils.buildDocumentHashToken(
            preCallbackHashes,
            postCallbackHashes
        );

        final CaseDocumentsMetadata documentMetadata = CaseDocumentsMetadata.builder()
            .caseId(afterCallbackCaseDetails.getReferenceAsString())
            .caseTypeId(afterCallbackCaseDetails.getCaseTypeId())
            .jurisdictionId(afterCallbackCaseDetails.getJurisdiction())
            .documentHashToken(documentHashes)
            .build();

        caseDocumentAmApiClient.applyPatch(documentMetadata);
    }

    private void removeHashes(final Map<String, JsonNode> data) {
        final List<JsonNode> documentNodes = caseDocumentUtils.findDocumentNodes(data);
        documentNodes.forEach(x -> ((ObjectNode) x).remove(DOCUMENT_HASH));
    }

    private void ensureNoTemper(final Map<String, String> preCallbackHashes,
                                final Map<String, String> postCallbackHashes) {
        final Set<String> tamperedHashes = caseDocumentUtils.getTamperedHashes(preCallbackHashes, postCallbackHashes);

        if (!tamperedHashes.isEmpty()) {
            throw new ServiceException("call back attempted to change the hashToken of the following documents:"
                + tamperedHashes);
        }
    }

}
