package uk.gov.hmcts.ccd.endpoint.std;

import io.swagger.annotations.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.getdraft.GetDraftOperation;
import uk.gov.hmcts.ccd.domain.service.getdraft.GetDraftsOperation;
import uk.gov.hmcts.ccd.domain.service.upsertdraft.UpsertDraftOperation;

import java.util.List;

@RestController
@RequestMapping(path = "/",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "/", description = "Drafts API")
public class DraftsEndpoint {
    private final UpsertDraftOperation upsertDraftOperation;
    private final GetDraftOperation getDraftOperation;
    private final GetDraftsOperation getDraftsOperation;

    @Autowired
    public DraftsEndpoint(@Qualifier("default") final UpsertDraftOperation upsertDraftOperation,
                          @Qualifier("default") final GetDraftOperation getDraftOperation,
                          @Qualifier("default") final GetDraftsOperation getDraftsOperation) {
        this.upsertDraftOperation = upsertDraftOperation;
        this.getDraftOperation = getDraftOperation;
        this.getDraftsOperation = getDraftsOperation;
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
    public DraftResponse saveDraftForCaseWorker(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Event Trigger ID", required = true)
        @PathVariable("etid") final String eventTriggerId,
        @RequestBody final CaseDataContent caseDataContent) {

        return upsertDraftOperation.executeSave(uid, jurisdictionId, caseTypeId, eventTriggerId, caseDataContent);
    }

    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/event-trigger/{etid}/drafts/{did}", method = RequestMethod.PUT)
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(
        value = "Update draft as a caseworker."
    )
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Draft updated"),
        @ApiResponse(code = 400, message = "Bad request")
    })
    public DraftResponse updateDraftForCaseWorker(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Event Trigger ID", required = true)
        @PathVariable("etid") final String eventTriggerId,
        @ApiParam(value = "Event Trigger ID", required = true)
        @PathVariable("did") final String draftId,
        @RequestBody final CaseDataContent caseDataContent) {

        return upsertDraftOperation.executeUpdate(uid, jurisdictionId, caseTypeId, eventTriggerId, draftId, caseDataContent);
    }

    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/drafts/{did}", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(
        value = "Update draft as a caseworker."
    )
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Case draft found for the given ID"),
        @ApiResponse(code = 400, message = "Invalid draft ID"),
        @ApiResponse(code = 404, message = "No case draft found for the given ID")
    })
    public DraftResponse getDraftForCaseWorker(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @ApiParam(value = "Draft ID", required = true)
        @PathVariable("did") final String draftId) {

        return getDraftOperation.execute(draftId);
    }

    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/drafts", method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    @ApiOperation(
        value = "Update draft as a caseworker."
    )
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Case draft found for the given ID"),
        @ApiResponse(code = 400, message = "Invalid draft ID"),
        @ApiResponse(code = 404, message = "No case draft found for the given ID")
    })
    public List<DraftResponse> getDraftsForCaseWorker(
        @ApiParam(value = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @ApiParam(value = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @ApiParam(value = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId) {

        return getDraftsOperation.execute();
    }

}
