package uk.gov.hmcts.ccd.v2.external.controller;

import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.validate.AuthorisedValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.domain.service.validate.OperationContext;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.CaseDataResource;

@RestController
@RequestMapping(path = "/case-types")
public class CaseDataValidatorController {
    private final ValidateCaseFieldsOperation validateCaseFieldsOperation;

    @Autowired
    public CaseDataValidatorController(
        @Qualifier(AuthorisedValidateCaseFieldsOperation.QUALIFIER)
        ValidateCaseFieldsOperation validateCaseFieldsOperation) {
        this.validateCaseFieldsOperation = validateCaseFieldsOperation;
    }

    @PostMapping(
        path = "/{caseTypeId}/validate",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.CASE_DATA_VALIDATE
        }
    )
    @ApiOperation(
        value = "Validate case data",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiImplicitParams({
        @ApiImplicitParam(name = V2.EXPERIMENTAL_HEADER, value = "'true' to use this endpoint", paramType = "header")
    })
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = CaseDataResource.class
            ),
        @ApiResponse(
            code = 404,
            message = "Case type not found"
            ),
        @ApiResponse(
            code = 422,
            message = "One of: Event trigger not provided, case type does not exist or case data validation failed"
            )
    })
    public ResponseEntity<CaseDataResource> validate(@PathVariable("caseTypeId") String caseTypeId,
                                                     @RequestParam(required = false) final String pageId,
                                                     @RequestBody final CaseDataContent content) {
        validateCaseFieldsOperation.validateCaseDetails(new OperationContext(caseTypeId, content, pageId));
        return ResponseEntity.ok(new CaseDataResource(content, caseTypeId, pageId));
    }
}
