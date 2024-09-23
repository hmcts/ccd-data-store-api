package uk.gov.hmcts.reform.ccd.pactprovider.cases.controller;

import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

// Submit Event Rest Controller version 1.   The request handler method calls the injected service-layer operation.

@RestController
public class SubmitEventRestController {

    private final CreateEventOperation createEventOperation;

    public SubmitEventRestController(final CreateEventOperation createEventOperation) {
        this.createEventOperation = createEventOperation;
    }

    @Operation(description = "A Submit event for a Citizen is requested",
        security = {@SecurityRequirement(name = "ServiceAuthorization"), @SecurityRequirement(name = "Authorization")})
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", description = "Created",
            content = {
                @Content(mediaType = "application/json")
            })
    })
    @PostMapping(path = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/events",
        produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<CaseDetails> createCaseEventForCitizen(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Case ID", required = true)
        @PathVariable("cid") final String caseId,
        @RequestBody(required = false) final CaseDataContent content) {

        CaseDetails caseDetails = createEventOperation.createCaseEvent(caseId, content);
        return ResponseEntity.status(HttpStatus.CREATED).body(caseDetails);
    }
}
