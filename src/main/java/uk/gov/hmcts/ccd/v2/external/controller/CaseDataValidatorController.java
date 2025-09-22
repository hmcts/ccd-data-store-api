package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.createevent.MidEventCallback;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.CaseDataResource;

@RestController
@RequestMapping(path = "/case-types")
public class CaseDataValidatorController {
    private static final ObjectMapper MAPPER = JacksonUtils.MAPPER;
    private final ValidateCaseFieldsOperation validateCaseFieldsOperation;
    private final MidEventCallback midEventCallback;

    @Autowired
    public CaseDataValidatorController(
        ValidateCaseFieldsOperation validateCaseFieldsOperation,
        MidEventCallback midEventCallback) {
        this.validateCaseFieldsOperation = validateCaseFieldsOperation;
        this.midEventCallback = midEventCallback;
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
    @Operation(summary = "Validate case data", description = V2.EXPERIMENTAL_WARNING)
    @Parameters({
        @Parameter(name = V2.EXPERIMENTAL_HEADER, description = "'true' to use this endpoint", in = ParameterIn.HEADER),
    })
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = CaseDataResource.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Case type not found"
    )
    @ApiResponse(
        responseCode = "422",
        description = "One of: Event trigger not provided, case type does not exist or case data validation failed"
    )
    public ResponseEntity<CaseDataResource> validate(@PathVariable("caseTypeId") String caseTypeId,
                                                     @RequestParam(required = false) final String pageId,
                                                     @RequestBody final CaseDataContent content) {
        validateCaseFieldsOperation.validateCaseDetails(caseTypeId,
            content);

        final JsonNode data = midEventCallback.invoke(caseTypeId,
            content,
            pageId);

        content.setData(JacksonUtils.convertValue(data));
        return ResponseEntity.ok(new CaseDataResource(content, caseTypeId, pageId));
    }
}
