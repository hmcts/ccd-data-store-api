package uk.gov.hmcts.ccd.endpoint.std;

import javax.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;

import io.swagger.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.aggregated.DefaultGetCaseViewFromDraftOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseViewOperation;
import uk.gov.hmcts.ccd.domain.service.upsertdraft.UpsertDraftOperation;

@RestController
@RequestMapping(path = "/",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "/", description = "Drafts API")
public class DraftsEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(DraftsEndpoint.class);

    private final UpsertDraftOperation upsertDraftOperation;
    private final GetCaseViewOperation getDraftViewOperation;
    private final DraftGateway draftGateway;

    @Autowired
    public DraftsEndpoint(@Qualifier("default") final UpsertDraftOperation upsertDraftOperation,
                          @Qualifier(DefaultGetCaseViewFromDraftOperation.QUALIFIER) GetCaseViewOperation getDraftViewOperation,
                          @Qualifier(CachedDraftGateway.QUALIFIER) DraftGateway draftGateway) {
        this.upsertDraftOperation = upsertDraftOperation;
        this.getDraftViewOperation = getDraftViewOperation;
        this.draftGateway = draftGateway;
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

        return upsertDraftOperation.executeSave(caseTypeId, caseDataContent);
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

        return upsertDraftOperation.executeUpdate(caseTypeId, draftId, caseDataContent);
    }

    @Transactional
    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/drafts/{did}",
        method = RequestMethod.GET)
    @ApiOperation(value = "Fetch a draft for display")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "A displayable draft")
    })
    public CaseView findDraft(@PathVariable("jid") final String jurisdictionId,
                              @PathVariable("ctid") final String caseTypeId,
                              @PathVariable("did") final String did) {
        Instant start = Instant.now();
        CaseView caseView = getDraftViewOperation.execute(did);
        final Duration between = Duration.between(start, Instant.now());
        LOG.info("findDraft has been completed in {} millisecs...", between.toMillis());
        return caseView;
    }

    @Transactional
    @RequestMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/drafts/{did}",
        method = RequestMethod.DELETE)
    @ApiOperation(value = "Delete a given draft")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "A draft deleted successfully")
    })
    public void deleteDraft(@PathVariable("jid") final String jurisdictionId,
                            @PathVariable("ctid") final String caseTypeId,
                            @PathVariable("did") final String did) {
        Instant start = Instant.now();
        draftGateway.delete(did);
        final Duration between = Duration.between(start, Instant.now());
        LOG.info("deleteDraft has been completed in {} millisecs...", between.toMillis());
    }

}
