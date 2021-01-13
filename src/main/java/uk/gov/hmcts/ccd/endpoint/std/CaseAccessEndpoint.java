package uk.gov.hmcts.ccd.endpoint.std;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.domain.model.std.UserId;
import uk.gov.hmcts.ccd.domain.service.caseaccess.CaseAccessOperation;

import java.util.List;

@RestController
@RequestMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "/", description = "Case access API")
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
    @ApiOperation(value = "Get case ids", notes = "Retrieve case ids for given users ids")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "List of cases ids found"),
        @ApiResponse(code = 400, message = "Invalid case ID")
    })
    public List<String> findCaseIdsGivenUserIdHasAccessTo(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "User id searching for", required = true)
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
    @ApiOperation(value = "Grant access to case")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Grant successful"),
        @ApiResponse(code = 400, message = "Invalid case ID")
    })
    @ResponseStatus(value = HttpStatus.CREATED)
    @LogAudit(operationType = AuditOperationType.GRANT_CASE_ACCESS, jurisdiction = "#jurisdictionId",
        caseId = "#caseId", targetIdamId = "#userId.id")
    public void grantAccessToCase(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Case ID", required = true)
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
    @ApiOperation(value = "Revoke access to case")
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "Access revoked"),
        @ApiResponse(code = 400, message = "Invalid case ID")
    })
    @ResponseStatus(value = HttpStatus.NO_CONTENT)
    @LogAudit(operationType = AuditOperationType.REVOKE_CASE_ACCESS, jurisdiction = "#jurisdictionId",
        caseId = "#caseId", targetIdamId = "#idToDelete")
    public void revokeAccessToCase(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Case ID", required = true)
        @PathVariable("cid") final String caseId,
        @ApiParam(value = "Id to delete", required = true)
        @PathVariable("idToDelete") final String idToDelete
    ) {
        LOG.debug("Revoking access to case: {}, for user: {}", caseId, idToDelete);
        caseAccessOperation.revokeAccess(jurisdictionId, caseId, idToDelete);
    }
}
