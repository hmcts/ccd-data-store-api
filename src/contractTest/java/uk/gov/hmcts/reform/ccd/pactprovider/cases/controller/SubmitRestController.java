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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.createcase.CreateCaseOperation;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

// Submit Rest Controller version 1.   The request handler method calls the injected service-layer operation.

@RestController
public class SubmitRestController {

    private final CreateCaseOperation createCaseOperation;

    public SubmitRestController(final CreateCaseOperation createCaseOperation) {
        this.createCaseOperation = createCaseOperation;
    }

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
