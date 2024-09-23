package uk.gov.hmcts.reform.ccd.pactprovider.cases.controller;

import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

// Start Event Rest Controller version 1.   The request handler method calls the injected service-layer operation.

@RestController
public class StartEventRestController {

    private final StartEventOperation startEventOperation;

    public StartEventRestController(final StartEventOperation startEventOperation) {
        this.startEventOperation = startEventOperation;
    }

    @Operation(description = "A Start event for a Citizen is requested",
        security = {@SecurityRequirement(name = "ServiceAuthorization"), @SecurityRequirement(name = "Authorization")})
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", description = "OK",
            content = {
                @Content(mediaType = "application/json")
            })
    })
    @GetMapping(
        path = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/event-triggers/{etid}/token",
        produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<StartEventResult> startEventForCitizen(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Case ID", required = true)
        @PathVariable("cid") final String caseId,
        @ApiParam(value = "Event ID", required = true)
        @PathVariable("etid") final String eventId,
        @ApiParam(value = "Should `AboutToStart` callback warnings be ignored")
        @RequestParam(value = "ignore-warning", required = false, defaultValue = "false") final Boolean ignoreWarning) {

        StartEventResult startEventResult = startEventOperation.triggerStartForCase(caseId, eventId, ignoreWarning);
        return ResponseEntity.ok(startEventResult);
    }
}
