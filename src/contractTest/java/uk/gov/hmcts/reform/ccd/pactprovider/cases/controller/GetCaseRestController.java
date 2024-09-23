package uk.gov.hmcts.reform.ccd.pactprovider.cases.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.getcase.CaseNotFoundException;
import uk.gov.hmcts.ccd.domain.service.getcase.GetCaseOperation;
import uk.gov.hmcts.ccd.v2.external.resource.CaseResource;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

// Get Case Rest Controller version 1.   The request handler method calls the injected service-layer operation.

@RestController
public class GetCaseRestController {

    private final GetCaseOperation getCaseOperation;

    public GetCaseRestController(final GetCaseOperation getCaseOperation) {
        this.getCaseOperation = getCaseOperation;
    }

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
    public ResponseEntity<CaseResource> getCase(@PathVariable("caseId") final String caseId)  {
        CaseDetails caseDetails = getCaseOperation.execute(caseId)
            .orElseThrow(() -> new CaseNotFoundException(caseId));

        return ResponseEntity.ok(new CaseResource(caseDetails));
    }
}
