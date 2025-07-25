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
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.stdapi.DocumentsOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.DocumentsResource;

import java.util.List;

@RestController
@RequestMapping(path = "/cases")
public class DocumentController {

    private final UIDService caseReferenceService;
    private final DocumentsOperation documentsOperation;

    @Autowired
    public DocumentController(
        final UIDService caseReferenceService,
        final DocumentsOperation documentsOperation
    ) {
        this.documentsOperation = documentsOperation;
        this.caseReferenceService = caseReferenceService;
    }

    @GetMapping(
        path = "/{caseId}/documents",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.CASE_DOCUMENTS
        }
    )
    @Operation(
        summary = "Retrieve case documents by case ID",
        description = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = DocumentsResource.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = V2.Error.CASE_ID_INVALID
    )
    @ApiResponse(
        responseCode = "404",
        description = V2.Error.CASE_NOT_FOUND
    )
    @ApiResponse(
        responseCode = "500",
        description = V2.Error.PRINTABLE_DOCUMENTS_ENDPOINT_DOWN
    )
    public ResponseEntity<DocumentsResource> getDocuments(@PathVariable("caseId") String caseId) {
        if (!caseReferenceService.validateUID(caseId)) {
            throw new BadRequestException(V2.Error.CASE_ID_INVALID);
        }

        List<Document> printableDocumentList = documentsOperation.getPrintableDocumentsForCase(caseId);

        return ResponseEntity.ok(new DocumentsResource(caseId, printableDocumentList));
    }

}
