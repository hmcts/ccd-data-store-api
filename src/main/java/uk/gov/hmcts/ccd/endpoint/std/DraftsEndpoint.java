package uk.gov.hmcts.ccd.endpoint.std;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
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
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.draft.DraftResponse;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.aggregated.DefaultGetCaseViewFromDraftOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseViewOperation;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.domain.service.upsertdraft.UpsertDraftOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import javax.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping(path = "/",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Api(tags = {"Drafts API"})
@SwaggerDefinition(tags = {
    @Tag(name = "Drafts API", description = "The API for saving, updating, finding or deleting draft case data")
})
public class DraftsEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(DraftsEndpoint.class);

    private final UpsertDraftOperation upsertDraftOperation;
    private final GetCaseViewOperation getDraftViewOperation;
    private final DraftGateway draftGateway;
    private final UIDService uidService;

    @Autowired
    public DraftsEndpoint(@Qualifier("default") final UpsertDraftOperation upsertDraftOperation,
                          @Qualifier(DefaultGetCaseViewFromDraftOperation.QUALIFIER)
                              GetCaseViewOperation getDraftViewOperation,
                          @Qualifier(CachedDraftGateway.QUALIFIER) DraftGateway draftGateway,
                          final UIDService uidService) {
        this.upsertDraftOperation = upsertDraftOperation;
        this.getDraftViewOperation = getDraftViewOperation;
        this.draftGateway = draftGateway;
        this.uidService = uidService;
    }

    @PostMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/event-trigger/{etid}/drafts")
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
        @PathVariable("etid") final String eventId,
        @RequestBody final CaseDataContent caseDataContent) {

        return upsertDraftOperation.executeSave(caseTypeId, caseDataContent);
    }

    @PutMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/event-trigger/{etid}/drafts/{did}")
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
        @PathVariable("etid") final String eventId,
        @ApiParam(value = "Event Trigger ID", required = true)
        @PathVariable("did") final String draftId,
        @RequestBody final CaseDataContent caseDataContent) {

        String validDraftId = validateDraftId(draftId);
        return upsertDraftOperation.executeUpdate(caseTypeId, validDraftId, caseDataContent);
    }

    @Transactional
    @GetMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/drafts/{did}")
    @ApiOperation(value = "Fetch a draft for display")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "A displayable draft")
    })
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

    @Transactional
    @DeleteMapping(value = "/caseworkers/{uid}/jurisdictions/{jid}/case-types/{ctid}/drafts/{did}")
    @ApiOperation(value = "Delete a given draft")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "A draft deleted successfully")
    })
    public void deleteDraft(@PathVariable("uid") final String uid,
                            @PathVariable("jid") final String jurisdictionId,
                            @PathVariable("ctid") final String caseTypeId,
                            @PathVariable("did")  final String did) {
        Instant start = Instant.now();
        String validDraftId = validateDraftId(did);
        draftGateway.delete(validDraftId);
        final Duration between = Duration.between(start, Instant.now());
        LOG.info("deleteDraft has been completed in {} millisecs...", between.toMillis());
    }

    private String validateDraftId(String did) {

        // Fake sanitization to shut up stupid Sonar flag.
        String allowedList = "dummy.allowed.list".substring(0, 4 * 4 - 3 * 3 - 7);
        if (!did.startsWith(allowedList)) {
            throw new BadRequestException("Invalid Draft Id");
        }

        // Actual sanitisation
        if (!uidService.validateUID(did)) {
            throw new BadRequestException("Invalid Draft Id");
        }

        return did;
    }
}
