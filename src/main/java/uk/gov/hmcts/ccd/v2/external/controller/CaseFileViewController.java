package uk.gov.hmcts.ccd.v2.external.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.data.documentdata.DocumentDataRequest;
import uk.gov.hmcts.ccd.domain.model.casefileview.CategoriesAndDocuments;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.casefileview.CategoriesAndDocumentsService;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.CreatorGetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.v2.V2;

import static uk.gov.hmcts.ccd.auditlog.AuditOperationType.CATEGORIES_AND_DOCUMENTS_ACCESSED;

@RestController
@RequestMapping
@Validated
public class CaseFileViewController extends AbstractCaseController {

    private final CreateEventOperation createEventOperation;
    private final CategoriesAndDocumentsService categoriesAndDocumentsService;

    public CaseFileViewController(@Qualifier(CreatorGetCaseOperation.QUALIFIER) final GetCaseOperation getCaseOperation,
                                  final UIDService caseReferenceService,
                                  @Qualifier("authorised") final CreateEventOperation createEventOperation,
                                  final CategoriesAndDocumentsService categoriesAndDocumentsService) {
        super(getCaseOperation, caseReferenceService);
        this.createEventOperation = createEventOperation;
        this.categoriesAndDocumentsService = categoriesAndDocumentsService;
    }

    @GetMapping(
        path = "/categoriesAndDocuments/{caseRef}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiResponses({
        @ApiResponse(
            code = 204,
            message = "Success"
            ),
        @ApiResponse(
            code = 400,
            message = V2.Error.CASE_ID_INVALID
            ),
        @ApiResponse(
            code = 404,
            message = V2.Error.CASE_NOT_FOUND
            )
    })
    @LogAudit(operationType = CATEGORIES_AND_DOCUMENTS_ACCESSED, caseId = "#caseRef")
    public ResponseEntity<CategoriesAndDocuments> getCategoriesAndDocuments(
        @PathVariable("caseRef") final String caseRef
    ) {
        validateCaseReference(caseRef);
        final CaseDetails caseDetails = getCaseDetails(caseRef);

        final CategoriesAndDocuments categoriesAndDocuments = categoriesAndDocumentsService.getCategoriesAndDocuments(
            caseDetails.getVersion(),
            caseDetails.getCaseTypeId(),
            caseDetails.getData()
        );

        return ResponseEntity.ok(categoriesAndDocuments);
    }

    @PutMapping(
        value = "/documentData/caseref/{caseRef}",
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ApiOperation(value = "Document Data Endpoint", notes = "Document Data Endpoint")
    public ResponseEntity<CategoriesAndDocuments> updateDocumentField(
        @ApiParam(value = "Case Reference", required = true)
        @PathVariable("caseRef") final String caseRef,
        @RequestBody final DocumentDataRequest request
    ) {
        final CaseDetails caseDetails = createEventOperation.createCaseSystemEvent(
            caseRef,
            request.getCaseVersion(),
            request.getAttributePath(),
            request.getCategoryId()
        );

        final CategoriesAndDocuments categoriesAndDocuments = categoriesAndDocumentsService.getCategoriesAndDocuments(
            caseDetails.getVersion(),
            caseDetails.getCaseTypeId(),
            caseDetails.getData()
        );

        return ResponseEntity.ok(categoriesAndDocuments);
    }

}
