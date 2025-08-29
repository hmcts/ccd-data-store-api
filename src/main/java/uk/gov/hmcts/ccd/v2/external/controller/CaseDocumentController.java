package uk.gov.hmcts.ccd.v2.external.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
    @ApiOperation(
        value = "Retrieve a case document metadata by case and document Id"
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "OK",
            response = CaseDocumentResource.class
            ),
        @ApiResponse(
            code = 400,
            message = V2.Error.CASE_ID_INVALID
            ),
        @ApiResponse(
            code = 400,
            message = V2.Error.CASE_DOCUMENT_ID_INVALID
            ),
        @ApiResponse(
            code = 404,
            message = V2.Error.CASE_NOT_FOUND
            ),
        @ApiResponse(
            code = 404,
            message = V2.Error.CASE_DOCUMENT_NOT_FOUND
            )
    })
    public ResponseEntity<CaseDocumentResource> getCaseDocumentMetadata(@PathVariable("caseId") String caseId,
                                                                        @PathVariable("documentId") String documentId) {

        final CaseDocumentMetadata documentMetadata =
            this.getCaseDocumentOperation.getCaseDocumentMetadata(caseId, documentId);
        return ResponseEntity.ok(new CaseDocumentResource(caseId, documentId, documentMetadata));
    }
}
