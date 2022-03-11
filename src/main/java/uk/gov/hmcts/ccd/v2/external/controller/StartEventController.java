package uk.gov.hmcts.ccd.v2.external.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventResult;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.StartEventResource;

import static uk.gov.hmcts.ccd.v2.V2.Error.AUTHENTICATION_TOKEN_INVALID;
import static uk.gov.hmcts.ccd.v2.V2.Error.EVENT_TRIGGER_NOT_FOUND;

@RestController
@RequestMapping(path = "/")
public class StartEventController {
    private static final String ERROR_CASE_ID_INVALID = "Case ID is not valid";

    private final StartEventOperation startEventOperation;
    private final UIDService caseReferenceService;

    @Autowired
    public StartEventController(
        @Qualifier("authorised") final StartEventOperation startEventOperation,
        UIDService caseReferenceService
    ) {
        this.startEventOperation = startEventOperation;
        this.caseReferenceService = caseReferenceService;
    }

    @GetMapping(
        path = "/case-types/{caseTypeId}/event-triggers/{triggerId}",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.START_CASE_EVENT
        }
    )
    @ApiOperation(
        value = "Retrieve a trigger by ID",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = StartEventResource.class
        ),
        @ApiResponse(
            code = 422,
            message = "One of: Case event has no pre states, callback validation errors, unable to sanitize document"
                + " for case field or missing user roles"
        ),
        @ApiResponse(
            code = 404,
            message = EVENT_TRIGGER_NOT_FOUND
        )
    })
    public ResponseEntity<StartEventResource> getStartCaseEvent(@PathVariable("caseTypeId") String caseTypeId,
                                                                @PathVariable("triggerId") String triggerId,
                                                                @RequestParam(value = "ignore-warning",
                                                                    required = false) final Boolean ignoreWarning) {

        final StartEventResult startEventResult = this.startEventOperation.triggerStartForCaseType(caseTypeId,
                                                                                                     triggerId,
                                                                                                     ignoreWarning);

        return ResponseEntity.ok(new StartEventResource(startEventResult, ignoreWarning, false));
    }

    @GetMapping(
        path = "/cases/{caseId}/event-triggers/{eventId}",
        produces = {
            V2.MediaType.START_EVENT
        }
    )
    @ApiOperation(
        value = "Retrieve an Event Trigger for a Case by Event ID",
        notes = "This operation creates an event token for a specific event to be started for a case and returns the "
        + "token, along with the visible case details to the invoking user as per their configured access levels."
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = StartEventResource.class
        ),
        @ApiResponse(
            code = 400,
            message = ERROR_CASE_ID_INVALID
        ),
        @ApiResponse(
            code = 401,
            message = AUTHENTICATION_TOKEN_INVALID
        ),
        @ApiResponse(
            code = 403,
            message = V2.Error.UNAUTHORISED_S2S_SERVICE
        ),
        @ApiResponse(
            code = 404,
            message = EVENT_TRIGGER_NOT_FOUND
        ),
        @ApiResponse(
            code = 422,
            message = "One of the following reasons:\n"
                + "1. Case event has no pre states\n"
                + "2. Callback validation errors\n"
                + "3. Unable to sanitize document for case field\n"
                + "4. Missing user roles"
        ),
    })
    public ResponseEntity<StartEventResource> getStartEventTrigger(@PathVariable("caseId") String caseId,
                                                                   @PathVariable("eventId") String eventId,
                                                                   @RequestParam(value = "ignore-warning",
                                                                       required = false) final Boolean ignoreWarning) {
        if (!caseReferenceService.validateUID(caseId)) {
            throw new BadRequestException(ERROR_CASE_ID_INVALID);
        }

        final StartEventResult startEventResult = this.startEventOperation.triggerStartForCase(caseId,
                                                                                                 eventId,
                                                                                                 ignoreWarning);

        return ResponseEntity.ok(new StartEventResource(startEventResult, ignoreWarning, true));
    }

}
