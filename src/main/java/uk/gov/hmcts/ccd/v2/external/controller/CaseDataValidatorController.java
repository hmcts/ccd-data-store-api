package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import net.minidev.json.JSONObject;
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
import uk.gov.hmcts.ccd.domain.service.common.JcLogger;
import uk.gov.hmcts.ccd.domain.service.createevent.MidEventCallback;
import uk.gov.hmcts.ccd.domain.service.validate.ValidateCaseFieldsOperation;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.CaseDataResource;

import java.util.Map;

@RestController
@RequestMapping(path = "/case-types")
public class CaseDataValidatorController {

    private final JcLogger jclogger = new JcLogger("CaseDataValidatorController", true);

    private static final ObjectMapper MAPPER = JacksonUtils.MAPPER;
    private final ValidateCaseFieldsOperation validateCaseFieldsOperation;
    private final MidEventCallback midEventCallback;

    @Autowired
    public CaseDataValidatorController(
        ValidateCaseFieldsOperation validateCaseFieldsOperation,
        MidEventCallback midEventCallback) {
        this.validateCaseFieldsOperation = validateCaseFieldsOperation;
        this.midEventCallback = midEventCallback;
        jclogger.jclog("Constructor");
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
        validateDebug(caseTypeId, pageId, content);
        validateCaseFieldsOperation.validateCaseDetails(caseTypeId, content);

        final JsonNode data = midEventCallback.invoke(caseTypeId, content, pageId);

        content.setData(JacksonUtils.convertValue(data));
        return ResponseEntity.ok(new CaseDataResource(content, caseTypeId, pageId));
    }

    private void validateDebug(final String caseTypeId, final String pageId, final CaseDataContent content) {
        final String contentAsString = jclogger.printObjectToString(content);
        final Map<String, JsonNode> eventData = content.getEventData();
        jclogger.jclog("validateDebug() ----------");
        jclogger.jclog("validateDebug() caseTypeId               = " + caseTypeId);
        jclogger.jclog("validateDebug() pageId                   = " + pageId);
        jclogger.jclog("validateDebug() contentAsString.length   = " + contentAsString.length());
        jclogger.jclog("validateDebug() contentAsString.hashCode = " + contentAsString.hashCode());
        jclogger.jclog("validateDebug() contentAsString          = " + contentAsString);
        if (eventData.containsKey("adjournCasePanelMember3")) {
            JsonNode adjournCasePanelMember3 = eventData.get("adjournCasePanelMember3");
            jclogger.jclog("validateDebug() adjournCasePanelMember3  = "
                + jclogger.printObjectToString(adjournCasePanelMember3));
        } else {
            jclogger.jclog("validateDebug() adjournCasePanelMember3 NOT PRESENT");
        }
    }
}
