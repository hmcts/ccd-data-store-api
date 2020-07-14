package uk.gov.hmcts.ccd.v2.internal.controller;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCaseHistoryViewOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.AuthorisedGetCaseViewOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseHistoryViewOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseViewOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseViewResource;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseHistoryViewResource;

import static uk.gov.hmcts.ccd.auditlog.AuditOperationType.CASE_ACCESSED;
import static uk.gov.hmcts.ccd.auditlog.AuditOperationType.VIEW_CASE_HISTORY;

@RestController
@RequestMapping(path = "/internal/cases")
public class UICaseController {
    private static final String ERROR_CASE_ID_INVALID = "Case ID is not valid";

    private final GetCaseViewOperation getCaseViewOperation;
    private final GetCaseHistoryViewOperation getCaseHistoryViewOperation;
    private final UIDService caseReferenceService;

    @Autowired
    public UICaseController(
        @Qualifier(AuthorisedGetCaseViewOperation.QUALIFIER) GetCaseViewOperation getCaseViewOperation,
        @Qualifier(AuthorisedGetCaseHistoryViewOperation.QUALIFIER) GetCaseHistoryViewOperation getCaseHistoryOperation,
        UIDService caseReferenceService
    ) {
        this.getCaseViewOperation = getCaseViewOperation;
        this.getCaseHistoryViewOperation = getCaseHistoryOperation;
        this.caseReferenceService = caseReferenceService;
    }

    @GetMapping(
        path = "/{caseId}",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.UI_CASE_VIEW
        }
    )
    @ApiOperation(
        value = "Retrieve a case by ID for dynamic display",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = CaseViewResource.class
        ),
        @ApiResponse(
            code = 400,
            message = ERROR_CASE_ID_INVALID
        ),
        @ApiResponse(
            code = 404,
            message = "Case not found"
        )
    })
    @LogAudit(operationType = CASE_ACCESSED, caseId = "#caseId", jurisdiction = "#result.body.caseType.jurisdiction.id",
        caseType = "#result.body.caseType.id")
    public ResponseEntity<CaseViewResource> getCaseView(@PathVariable("caseId") String caseId) {
        if (!caseReferenceService.validateUID(caseId)) {
            throw new BadRequestException(ERROR_CASE_ID_INVALID);
        }

        final CaseView caseView = getCaseViewOperation.execute(caseId);

        return ResponseEntity.ok(new CaseViewResource(caseView));
    }

    @GetMapping(
        path = "/{caseId}/events/{eventId}",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.UI_EVENT_VIEW
        }
    )
    @ApiOperation(
        value = "Retrieve an event by case and event IDs for dynamic display",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = CaseHistoryViewResource.class
        ),
        @ApiResponse(
            code = 400,
            message = ERROR_CASE_ID_INVALID
        ),
        @ApiResponse(
            code = 404,
            message = "Case event not found"
        )
    })
    @LogAudit(operationType = VIEW_CASE_HISTORY, caseId = "#caseId", eventName = "#result.body.event.eventId",
        jurisdiction = "#result.body.caseType.jurisdiction.id", caseType = "#result.body.caseType.id")
    public ResponseEntity<CaseHistoryViewResource> getCaseHistoryView(@PathVariable("caseId") String caseId,
                                                                      @PathVariable("eventId") String eventId) {
        if (!caseReferenceService.validateUID(caseId)) {
            throw new BadRequestException(ERROR_CASE_ID_INVALID);
        }

        final CaseHistoryView caseHistoryView = getCaseHistoryViewOperation.execute(caseId, Long.valueOf(eventId));

        return ResponseEntity.ok(new CaseHistoryViewResource(caseHistoryView, caseId));
    }
}
