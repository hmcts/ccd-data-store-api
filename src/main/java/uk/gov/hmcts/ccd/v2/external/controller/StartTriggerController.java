package uk.gov.hmcts.ccd.v2.external.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.auditlog.OperationType;
import uk.gov.hmcts.ccd.domain.model.callbacks.StartEventTrigger;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.startevent.StartEventOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.StartTriggerResource;

import static uk.gov.hmcts.ccd.v2.V2.Error.EVENT_TRIGGER_NOT_FOUND;

@RestController
@RequestMapping(path = "/")
public class StartTriggerController {
    private static final String ERROR_CASE_ID_INVALID = "Case ID is not valid";

    private final StartEventOperation startEventOperation;
    private final UIDService caseReferenceService;

    @Autowired
    public StartTriggerController(
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
            V2.MediaType.START_CASE_TRIGGER
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
            response = StartTriggerResource.class
        ),
        @ApiResponse(
            code = 422,
            message = "One of: Case event has no pre states, callback validation errors, unable to sanitize document for case field or missing user roles"
        ),
        @ApiResponse(
            code = 404,
            message = EVENT_TRIGGER_NOT_FOUND
        )
    })
    @LogAudit(operationType = OperationType.CREATE_CASE, jurisdiction = "#result.body.caseDetails.jurisdiction",
        caseType = "#caseTypeId", eventName = "#triggerId")
    public ResponseEntity<StartTriggerResource> getStartCaseTrigger(@PathVariable("caseTypeId") String caseTypeId,
                                                                    @PathVariable("triggerId") String triggerId,
                                                                    @RequestParam(value = "ignore-warning", required = false) final Boolean ignoreWarning) {

        final StartEventTrigger startEventTrigger = this.startEventOperation.triggerStartForCaseType(caseTypeId,
                                                                                                     triggerId,
                                                                                                     ignoreWarning);

        return ResponseEntity.ok(new StartTriggerResource(startEventTrigger, ignoreWarning, false));
    }

    @GetMapping(
        path = "/cases/{caseId}/event-triggers/{triggerId}",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.START_EVENT_TRIGGER
        }
    )
    @ApiOperation(
        value = "Retrieve a trigger for case by ID",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = StartTriggerResource.class
        ),
        @ApiResponse(
            code = 422,
            message = "One of: Case event has no pre states, callback validation errors, unable to sanitize document for case field or missing user roles"
        ),
        @ApiResponse(
            code = 400,
            message = ERROR_CASE_ID_INVALID
        ),
        @ApiResponse(
            code = 404,
            message = EVENT_TRIGGER_NOT_FOUND
        )
    })
    @LogAudit(operationType = OperationType.UPDATE_CASE, jurisdiction = "#result.body.caseDetails.jurisdiction",
        caseType = "#result.body.caseDetails.caseTypeId", eventName = "#triggerId", caseId = "#caseId")
    public ResponseEntity<StartTriggerResource> getStartEventTrigger(@PathVariable("caseId") String caseId,
                                                                     @PathVariable("triggerId") String triggerId,
                                                                     @RequestParam(value = "ignore-warning", required = false) final Boolean ignoreWarning) {
        if (!caseReferenceService.validateUID(caseId)) {
            throw new BadRequestException(ERROR_CASE_ID_INVALID);
        }

        final StartEventTrigger startEventTrigger = this.startEventOperation.triggerStartForCase(caseId,
                                                                                                 triggerId,
                                                                                                 ignoreWarning);

        return ResponseEntity.ok(new StartTriggerResource(startEventTrigger, ignoreWarning, true));
    }

}
