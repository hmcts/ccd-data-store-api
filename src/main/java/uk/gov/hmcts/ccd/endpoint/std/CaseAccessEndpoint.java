package uk.gov.hmcts.ccd.endpoint.std;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.domain.model.std.UserId;
import uk.gov.hmcts.ccd.domain.service.caseaccess.CaseAccessOperation;

import java.util.List;

@RestController
@RequestMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "/", description = "Case access API")
public class CaseAccessEndpoint {

    private static final Logger LOG = LoggerFactory.getLogger(CaseAccessEndpoint.class);
    private final CaseAccessOperation caseAccessOperation;

    public CaseAccessEndpoint(CaseAccessOperation caseAccessOperation) {
        this.caseAccessOperation = caseAccessOperation;
    }

    @RequestMapping(
        value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/ids",
        method = RequestMethod.GET,
        consumes = MediaType.ALL_VALUE
    )
    @Operation(summary = "Get case ids", description = "Retrieve case ids for given users ids")
    @ApiResponse(responseCode = "200", description = "List of cases ids found")
    @ApiResponse(responseCode = "400", description = "Invalid case ID")
    public List<String> findCaseIdsGivenUserIdHasAccessTo(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "User id searching for", required = true)
        @RequestParam(value = "userId") final String idSearchingFor
    ) {
        LOG.debug("Finding cases user: {} has access to", idSearchingFor);
        return caseAccessOperation.findCasesUserIdHasAccessTo(idSearchingFor);
    }

    @RequestMapping(
        value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/users",
        method = RequestMethod.POST,
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(summary = "Grant access to case")
    @ApiResponse(responseCode = "201", description = "Grant successful")
    @ApiResponse(responseCode = "400", description = "Invalid case ID")
    @ResponseStatus(value = HttpStatus.CREATED)
    @LogAudit(operationType = AuditOperationType.GRANT_CASE_ACCESS, jurisdiction = "#jurisdictionId",
        caseId = "#caseId", targetIdamId = "#userId.id")
    public void grantAccessToCase(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Case ID", required = true)
        @PathVariable("cid") final String caseId,
        @RequestBody final UserId userId
    ) {
        LOG.debug("Granting access to case: {}, for user: {}", caseId, userId);
        caseAccessOperation.grantAccess(jurisdictionId, caseId, userId.getId());
    }

    @RequestMapping(
        value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/cases/{cid}/users/{idToDelete}",
        method = RequestMethod.DELETE,
        consumes = MediaType.ALL_VALUE
    )
    @Operation(summary = "Revoke access to case")
    @ApiResponse(responseCode = "204", description = "Access revoked")
    @ApiResponse(responseCode = "400", description = "Invalid case ID")
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @LogAudit(operationType = AuditOperationType.REVOKE_CASE_ACCESS, jurisdiction = "#jurisdictionId",
        caseId = "#caseId", targetIdamId = "#idToDelete")
    public void revokeAccessToCase(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Case ID", required = true)
        @PathVariable("cid") final String caseId,
        @Parameter(name = "Id to delete", required = true)
        @PathVariable("idToDelete") final String idToDelete
    ) {
        LOG.debug("Revoking access to case: {}, for user: {}", caseId, idToDelete);
        caseAccessOperation.revokeAccess(jurisdictionId, caseId, idToDelete);
    }
}
