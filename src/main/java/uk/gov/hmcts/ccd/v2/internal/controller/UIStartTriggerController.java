package uk.gov.hmcts.ccd.v2.internal.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetEventTriggerOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetEventTriggerOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.domain.CaseAssignedUserRolesResponse;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseUpdateViewEventResource;

import static uk.gov.hmcts.ccd.v2.internal.resource.CaseUpdateViewEventResource.forCase;
import static uk.gov.hmcts.ccd.v2.internal.resource.CaseUpdateViewEventResource.forCaseType;
import static uk.gov.hmcts.ccd.v2.internal.resource.CaseUpdateViewEventResource.forDraft;

@RestController
@RequestMapping(path = "/internal")
public class UIStartTriggerController {
    private static final String ERROR_CASE_ID_INVALID = "Case ID is not valid";

    private final GetEventTriggerOperation getEventTriggerOperation;
    private final UIDService caseReferenceService;

    @Autowired
    public UIStartTriggerController(
        @Qualifier(AuthorisedGetEventTriggerOperation.QUALIFIER) GetEventTriggerOperation getEventTriggerOperation,
        UIDService caseReferenceService
    ) {
        this.getEventTriggerOperation = getEventTriggerOperation;
        this.caseReferenceService = caseReferenceService;
    }

    @GetMapping(
        path = "/case-types/{caseTypeId}/event-triggers/{triggerId}",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.CASE_TYPE_UPDATE_VIEW_EVENT
        }
    )
    @Operation(
        summary = "Retrieve a start case trigger by ID for dynamic display",
        description = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = CaseUpdateViewEventResource.class))
    )
    @ApiResponse(
        responseCode = "422",
        description = "One of: Case event has no pre states, callback validation errors, unable to sanitize document "
            + "for case field or missing user roles"
    )
    @ApiResponse(
        responseCode = "404",
        description = "Trigger not found"
    )
    public ResponseEntity<CaseUpdateViewEventResource> getCaseUpdateViewEventByCaseType(@PathVariable("caseTypeId")
                                                                                                String caseTypeId,
                                                                                        @PathVariable("triggerId")
                                                                                            String triggerId,
                                                               @RequestParam(value = "ignore-warning", required = false)
                                                                                          final Boolean ignoreWarning) {

        final CaseUpdateViewEvent caseUpdateViewEvent = this.getEventTriggerOperation.executeForCaseType(caseTypeId,
                                                                                                   triggerId,
                                                                                                   ignoreWarning);

        return ResponseEntity.ok(forCaseType(caseUpdateViewEvent, caseTypeId, ignoreWarning));
    }

    @GetMapping(
        path = "/cases/{caseId}/event-triggers/{triggerId}",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.CASE_UPDATE_VIEW_EVENT
        }
    )
    @Operation(
        summary = "Retrieve a start event trigger by ID for dynamic display",
        description = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success",
        content = @Content(schema = @Schema(implementation = CaseUpdateViewEventResource.class))
    )
    @ApiResponse(
        responseCode = "422",
        description = "One of: Case event has no pre states, callback validation errors, unable to sanitize document "
            + "for case field or missing user roles"
    )
    @ApiResponse(
        responseCode = "400",
        description = ERROR_CASE_ID_INVALID
    )
    @ApiResponse(
        responseCode = "404",
        description = "Trigger not found"
    )
    public ResponseEntity<CaseUpdateViewEventResource> getCaseUpdateViewEvent(@PathVariable("caseId") String caseId,
                                                                            @PathVariable("triggerId") String triggerId,
                                                                            @RequestParam(value = "ignore-warning",
                                                                                required = false)
                                                                                  final Boolean ignoreWarning) {
        if (!caseReferenceService.validateUID(caseId)) {
            throw new BadRequestException(ERROR_CASE_ID_INVALID);
        }

        final CaseUpdateViewEvent caseUpdateViewEvent = this.getEventTriggerOperation.executeForCase(caseId,
                                                                                               triggerId,
                                                                                               ignoreWarning);

        return ResponseEntity.ok(forCase(caseUpdateViewEvent, caseId, ignoreWarning));
    }

    @GetMapping(
        path = "/drafts/{draftId}/event-trigger",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.UI_START_DRAFT_TRIGGER
        }
    )
    @Operation(
        summary = "Retrieve a start draft trigger by ID for dynamic display",
        description = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Success",
            content = @Content(schema = @Schema(implementation = CaseUpdateViewEventResource.class))
            ),
        @ApiResponse(
            responseCode = "422",
            description = "One of: Case event has no pre states, callback validation errors, unable to sanitize document "
                + "for case field or missing user roles"
            ),
        @ApiResponse(
            responseCode = "404",
            description = "Trigger not found"
            )
    })
    public ResponseEntity<CaseUpdateViewEventResource> getStartDraftTrigger(@PathVariable("draftId") String draftId,
                                                                            @RequestParam(value = "ignore-warning",
                                                                                required = false)
                                                                            final Boolean ignoreWarning) {

        final CaseUpdateViewEvent caseUpdateViewEvent = getEventTriggerOperation.executeForDraft(draftId,
                                                                                           ignoreWarning);

        return ResponseEntity.ok(forDraft(caseUpdateViewEvent, draftId, ignoreWarning));
    }
}
