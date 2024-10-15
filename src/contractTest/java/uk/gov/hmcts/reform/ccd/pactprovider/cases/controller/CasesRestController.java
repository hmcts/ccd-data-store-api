package uk.gov.hmcts.reform.ccd.pactprovider.cases.controller;

import io.swagger.annotations.ApiParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;
import uk.gov.hmcts.ccd.domain.service.createevent.CreateEventOperation;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;
import uk.gov.hmcts.ccd.v2.external.resource.CaseResource;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class CasesRestController {

    private final GetCaseOperation getCaseOperation;
    private final StartEventOperation startEventOperation;
    private final CreateEventOperation createEventOperation;
    private final CreateCaseOperation createCaseOperation;

    public CasesRestController(final GetCaseOperation getCaseOperation,
                               final StartEventOperation startEventOperation,
                               final CreateEventOperation createEventOperation,
                               final CreateCaseOperation createCaseOperation) {
        this.getCaseOperation = getCaseOperation;
        this.startEventOperation = startEventOperation;
        this.createEventOperation = createEventOperation;
        this.createCaseOperation = createCaseOperation;
    }

    /*
     * Handle Pact State "A Get Case is requested".
     */
    @Operation(description = "A Get Case is requested",
        security = {@SecurityRequirement(name = "ServiceAuthorization"), @SecurityRequirement(name = "Authorization")})
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", description = "OK",
            content = {
                @Content(mediaType = "application/json")
            })
    })
    @GetMapping(path = "/cases/{caseId}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<CaseResource> getCase(@PathVariable("caseId") final String caseId) {
        CaseDetails caseDetails = getCaseOperation.execute(caseId)
            .orElseThrow(() -> new CaseNotFoundException(caseId));

        return ResponseEntity.ok(new CaseResource(caseDetails));
    }

    /*
     * Handle Pact State "A Start event for a Citizen is requested".
     */
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

    /*
     * Handle Pact State "A Start case for a Citizen is requested".
     */
    @Operation(description = "A Start case for a Citizen is requested",
        security = {@SecurityRequirement(name = "ServiceAuthorization"), @SecurityRequirement(name = "Authorization")})
    @ApiResponses({
        @ApiResponse(
            responseCode = "200", description = "OK",
            content = {
                @Content(mediaType = "application/json")
            })
    })
    @GetMapping(path = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/event-triggers/{etid}/token",
        produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<StartEventResult> startCaseForCitizen(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Event ID", required = true)
        @PathVariable("etid") final String eventId,
        @ApiParam(value = "Should `AboutToStart` callback warnings be ignored")
        @RequestParam(value = "ignore-warning", required = false, defaultValue = "false") final Boolean ignoreWarning) {

        StartEventResult startEventResult = startEventOperation.triggerStartForCaseType(caseTypeId, eventId,
            ignoreWarning);
        return ResponseEntity.ok(startEventResult);
    }

    /*
     * Handle Pact State "A Submit event for a Citizen is requested".
     */
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

    /*
     * Handle Pact State "A Submit case for a Citizen is requested".
     */
    @Operation(description = "A Submit case for a Citizen is requested",
        security = {@SecurityRequirement(name = "ServiceAuthorization"), @SecurityRequirement(name = "Authorization")})
    @ApiResponses({
        @ApiResponse(
            responseCode = "201", description = "Created",
            content = {
                @Content(mediaType = "application/json")
            })
    })
    @PostMapping(path = "/citizens/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases",
        produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<CaseDetails> saveCaseDetailsForCitizen(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Should `AboutToSubmit` callback warnings be ignored")
        @RequestParam(value = "ignore-warning", required = false, defaultValue = "false") final Boolean ignoreWarning,
        @RequestBody(required = false) final CaseDataContent content) {

        CaseDetails caseDetails = createCaseOperation.createCaseDetails(caseTypeId, content, ignoreWarning);
        return ResponseEntity.status(HttpStatus.CREATED).body(caseDetails);
    }
}
