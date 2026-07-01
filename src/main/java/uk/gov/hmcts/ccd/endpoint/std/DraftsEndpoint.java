package uk.gov.hmcts.ccd.endpoint.std;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.aggregated.DefaultGetCaseViewFromDraftOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseViewOperation;
import uk.gov.hmcts.ccd.domain.service.upsertdraft.UpsertDraftOperation;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping(path = "/",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Drafts API", description = "The API for saving, updating, finding or deleting draft case data")
public class DraftsEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(DraftsEndpoint.class);

    private final UpsertDraftOperation upsertDraftOperation;
    private final GetCaseViewOperation getDraftViewOperation;
    private final DraftGateway draftGateway;

    @Autowired
    public DraftsEndpoint(@Qualifier("default") final UpsertDraftOperation upsertDraftOperation,
                          @Qualifier(DefaultGetCaseViewFromDraftOperation.QUALIFIER)
                              GetCaseViewOperation getDraftViewOperation,
                          @Qualifier(CachedDraftGateway.QUALIFIER) DraftGateway draftGateway) {
        this.upsertDraftOperation = upsertDraftOperation;
        this.getDraftViewOperation = getDraftViewOperation;
        this.draftGateway = draftGateway;
    }

    @PostMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/event-trigger/{etid}/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Save draft as a caseworker.")
    @ApiResponse(responseCode = "201", description = "Draft created")
    @ApiResponse(responseCode = "400", description = "Bad request")
    public DraftResponse saveDraftForCaseWorker(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Event Trigger ID", required = true)
        @PathVariable("etid") final String eventId,
        @RequestBody final CaseDataContent caseDataContent) {

        return upsertDraftOperation.executeSave(caseTypeId, caseDataContent);
    }

    @PutMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/event-trigger/{etid}/drafts/{did}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update draft as a caseworker.")
    @ApiResponse(responseCode = "200", description = "Draft updated")
    @ApiResponse(responseCode = "400", description = "Bad request")
    public DraftResponse updateDraftForCaseWorker(
        @Parameter(name = "Idam user ID", required = true)
        @PathVariable("uid") final String uid,
        @Parameter(name = "Jurisdiction ID", required = true)
        @PathVariable("jid") final String jurisdictionId,
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @Parameter(name = "Event Trigger ID", required = true)
        @PathVariable("etid") final String eventId,
        @Parameter(name = "Draft ID", required = true)
        @PathVariable("did") final String draftId,
        @RequestBody final CaseDataContent caseDataContent) {

        return upsertDraftOperation.executeUpdate(caseTypeId, draftId, caseDataContent);
    }

    @GetMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/drafts/{did}")
    @Operation(summary = "Fetch a draft for display")
    @ApiResponse(responseCode = "200", description = "A displayable draft")
    public CaseView findDraft(@PathVariable("uid") final String uid,
                              @PathVariable("jid") final String jurisdictionId,
                              @PathVariable("ctid") final String caseTypeId,
                              @PathVariable("did") final String did) {
        Instant start = Instant.now();
        CaseView caseView = getDraftViewOperation.execute(did);
        final Duration between = Duration.between(start, Instant.now());
        LOG.info("findDraft has been completed in {} millisecs...", between.toMillis());
        return caseView;
    }

    @DeleteMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/drafts/{did}")
    @Operation(summary = "Delete a given draft")
    @ApiResponse(responseCode = "200", description = "A draft deleted successfully")
    public void deleteDraft(@PathVariable("uid") final String uid,
                            @PathVariable("jid") final String jurisdictionId,
                            @PathVariable("ctid") final String caseTypeId,
                            @PathVariable("did")  final String did) {
        Instant start = Instant.now();
        draftGateway.delete(did);
        final Duration between = Duration.between(start, Instant.now());
        LOG.info("deleteDraft has been completed in {} millisecs...", between.toMillis());
    }
}
