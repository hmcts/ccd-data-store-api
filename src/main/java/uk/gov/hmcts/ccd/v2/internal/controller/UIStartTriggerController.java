package uk.gov.hmcts.ccd.v2.internal.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseEventTrigger;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetEventTriggerOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetEventTriggerOperation;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.UIStartTriggerResource;

@RestController
@RequestMapping(path = "/internal")
public class UIStartTriggerController {

    private final GetEventTriggerOperation getEventTriggerOperation;

    @Autowired
    public UIStartTriggerController(
        @Qualifier(AuthorisedGetEventTriggerOperation.QUALIFIER) GetEventTriggerOperation getEventTriggerOperation
    ) {
        this.getEventTriggerOperation = getEventTriggerOperation;
    }

    @GetMapping(
        path = "/case-types/{caseTypeId}/event-triggers/{triggerId}",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.UI_START_TRIGGER
        }
    )
    @ApiOperation(
        value = "Retrieve a trigger by ID for dynamic display",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = UIStartTriggerResource.class
        ),
        @ApiResponse(
            code = 422,
            message = "One of: Case event has no pre states, callback validation errors, unable to sanitize document for case field or missing user roles"
        ),
        @ApiResponse(
            code = 404,
            message = "Trigger not found"
        )
    })
    public ResponseEntity<UIStartTriggerResource> getStartTrigger(@PathVariable("caseTypeId") String caseTypeId,
                                                                  @PathVariable("triggerId") String triggerId,
                                                                  @RequestParam(value = "ignore-warning", required = false) final Boolean ignoreWarning) {

        final CaseEventTrigger caseEventTrigger = this.getEventTriggerOperation.executeForCaseType(caseTypeId,
                                                                                                   triggerId,
                                                                                                   ignoreWarning);

        return ResponseEntity.ok(new UIStartTriggerResource(caseEventTrigger, caseTypeId, ignoreWarning));
    }
}
