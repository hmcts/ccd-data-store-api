package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.createevent.MidEventCallback;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.CaseDataResource;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseViewResource;

import java.util.HashMap;

@RestController
@RequestMapping(path = "/case-types")
public class CaseDataValidatorController {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<HashMap<String, JsonNode>> STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };
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
    @ApiOperation(
        value = "Validate case data",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = CaseViewResource.class
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
        validateCaseFieldsOperation.validateCaseDetails(caseTypeId,
                                                        content);

        final JsonNode data = midEventCallback.invoke(caseTypeId,
                                                      content,
                                                      pageId);

        content.setData(MAPPER.convertValue(data, STRING_JSON_MAP));
        return ResponseEntity.ok(new CaseDataResource(content, caseTypeId, pageId));
    }
}
