package uk.gov.hmcts.ccd.domain.service.getcasedocument;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;
import uk.gov.hmcts.ccd.v2.external.domain.DocumentPermissions;
import uk.gov.hmcts.ccd.v2.external.domain.Permission;

import java.util.List;
import java.util.Map;

@Service
public class GetCaseDocumentOperation {

    private static final String DOCUMENT_BINARY_URL = "document_url";
    private static final String BINARY_SUFFIX = "/binary";
    private static final int DOC_UUID_LENGTH = 36;
    private static final String BAD_REQUEST_EXCEPTION_DOCUMENT_INVALID = "DocumentId is not valid";

    private final GetCaseOperation getCaseOperation;
    private final DocumentIdValidationService documentIdValidationService;

    @Autowired
    public GetCaseDocumentOperation(
        @Qualifier(CreatorGetCaseOperation.QUALIFIER) final GetCaseOperation getCaseOperation,
        DocumentIdValidationService documentIdValidationService) {
        this.getCaseOperation = getCaseOperation;
        this.documentIdValidationService = documentIdValidationService;
    }

    public CaseDocumentMetadata getCaseDocumentMetadata(String caseReference, String documentId) {

        if (!documentIdValidationService.validateDocumentUUID(documentId)) {
            throw new BadRequestException(BAD_REQUEST_EXCEPTION_DOCUMENT_INVALID);
        }

        // at this stage case data is filtered by user access permissions already.
        final CaseDetails caseDetails = this.getCaseOperation.execute(caseReference)
            .orElseThrow(() -> new CaseNotFoundException(caseReference));

        boolean documentAccessible = isDocumentPresentInTheCaseData(documentId, caseDetails.getData());

        if (documentAccessible) {
            return CaseDocumentMetadata.builder()
                .caseId(caseReference)
                .documentPermissions(DocumentPermissions.builder()
                    .id(documentId)
                    .permissions(List.of(Permission.READ))
                    .build())
                .build();
        }
        throw new CaseDocumentNotFoundException(
            String.format("Document %s is not found in the case : %s", documentId, caseReference));
    }

    private boolean isDocumentPresentInTheCaseData(String documentId, Map<String, JsonNode> data) {
        return data.values().stream()
            .map(node -> node.findValuesAsText(DOCUMENT_BINARY_URL))
            .flatMap(List::stream)
            .anyMatch(binaryLink -> isDocumentIdMatches(binaryLink, documentId));
    }

    private boolean isDocumentIdMatches(String binaryLink, String documentId) {
        String selfHref = binaryLink.replace(BINARY_SUFFIX, "");
        return documentId.equalsIgnoreCase(selfHref.substring(selfHref.length() - DOC_UUID_LENGTH));
    }

}
