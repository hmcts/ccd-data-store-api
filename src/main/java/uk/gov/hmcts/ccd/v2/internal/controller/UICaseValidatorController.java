package uk.gov.hmcts.ccd.v2.internal.controller;

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
import uk.gov.hmcts.ccd.v2.internal.resource.UICaseDataResource;
import uk.gov.hmcts.ccd.v2.internal.resource.UICaseViewResource;

import java.util.HashMap;

@RestController
@RequestMapping(path = "/internal")
public class UICaseValidatorController {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference STRING_JSON_MAP = new TypeReference<HashMap<String, JsonNode>>() {
    };
    private final ValidateCaseFieldsOperation validateCaseFieldsOperation;
    private final MidEventCallback midEventCallback;

    @Autowired
    public UICaseValidatorController(
        ValidateCaseFieldsOperation validateCaseFieldsOperation,
        MidEventCallback midEventCallback) {
        this.validateCaseFieldsOperation = validateCaseFieldsOperation;
        this.midEventCallback = midEventCallback;
    }

    @RequestMapping(
        method = RequestMethod.POST,
        path = "/case-types/{caseTypeId}/validate",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.UI_CASE_DATA_VALIDATE
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
            response = UICaseViewResource.class
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
    public ResponseEntity<UICaseDataResource> validate(@PathVariable("caseTypeId") String caseTypeId,
                                                       @RequestParam(required = false) final String pageId,
                                                       @RequestBody final CaseDataContent content) {
        validateCaseFieldsOperation.validateCaseDetails(caseTypeId,
                                                        content);

        final JsonNode data = midEventCallback.invoke(caseTypeId,
                                                      content,
                                                      pageId);

        content.setData(MAPPER.convertValue(data, STRING_JSON_MAP));
        return ResponseEntity.ok(new UICaseDataResource(content, caseTypeId, pageId));
    }
}
