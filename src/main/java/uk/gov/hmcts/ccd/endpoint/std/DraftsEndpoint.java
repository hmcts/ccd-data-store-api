package uk.gov.hmcts.ccd.endpoint.std;

import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.service.createdraft.SaveDraftOperation;

@RestController
@RequestMapping(path = "/",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "/", description = "Drafts API")
public class DraftsEndpoint {
    private final SaveDraftOperation saveDraftOperation;

    @Autowired
    public DraftsEndpoint(@Qualifier("default") final SaveDraftOperation saveDraftOperation) {
        this.saveDraftOperation = saveDraftOperation;
    }

    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/event-trigger/{etid}/drafts", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation(
        value = "Save draft as a caseworker."
    )
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Draft created"),
        @ApiResponse(code = 400, message = "Bad request")
    })
    public Draft saveDraftForCaseWorker(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Event Trigger ID", required = true)
        @PathVariable("etid") final String eventTriggerId,
        @RequestBody final CaseDataContent caseDataContent) {

        return saveDraftOperation.saveDraft(uid, jurisdictionId, caseTypeId, eventTriggerId, caseDataContent);
    }

}
