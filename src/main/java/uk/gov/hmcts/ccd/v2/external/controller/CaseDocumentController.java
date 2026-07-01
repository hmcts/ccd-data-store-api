package uk.gov.hmcts.ccd.v2.external.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import uk.gov.hmcts.ccd.domain.service.getcasedocument.GetCaseDocumentOperation;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;
import uk.gov.hmcts.ccd.v2.external.resource.CaseDocumentResource;


@RestController
@RequestMapping(path = "/cases")
public class CaseDocumentController {

    private final GetCaseDocumentOperation getCaseDocumentOperation;

    @Autowired
    public CaseDocumentController(GetCaseDocumentOperation getCaseDocumentOperation) {
        this.getCaseDocumentOperation = getCaseDocumentOperation;
    }

    @GetMapping(
        path = "/{caseId}/documents/{documentId}",
        produces = {
            V2.MediaType.CASE_DOCUMENT
        }
    )
    @Operation(summary = "Retrieve a case document metadata by case and document Id")
    @ApiResponse(
        responseCode = "200",
        description = "OK",
        content = @Content(schema = @Schema(implementation = CaseDocumentResource.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = V2.Error.CASE_ID_INVALID
    )
    @ApiResponse(
        responseCode = "400",
        description = V2.Error.CASE_DOCUMENT_ID_INVALID
    )
    @ApiResponse(
        responseCode = "404",
        description = V2.Error.CASE_NOT_FOUND
    )
    @ApiResponse(
        responseCode = "404",
        description = V2.Error.CASE_DOCUMENT_NOT_FOUND
    )
    public ResponseEntity<CaseDocumentResource> getCaseDocumentMetadata(@PathVariable("caseId") String caseId,
                                                                        @PathVariable("documentId") String documentId) {

        final CaseDocumentMetadata documentMetadata =
            this.getCaseDocumentOperation.getCaseDocumentMetadata(caseId, documentId);
        return ResponseEntity.ok(new CaseDocumentResource(caseId, documentId, documentMetadata));
    }
}
