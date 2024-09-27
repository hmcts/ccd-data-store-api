package uk.gov.hmcts.ccd.v2.internal.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import uk.gov.hmcts.ccd.data.draft.CachedDraftGateway;
import uk.gov.hmcts.ccd.data.draft.DraftGateway;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.service.aggregated.DefaultGetCaseViewFromDraftOperation;
import uk.gov.hmcts.ccd.domain.service.aggregated.GetCaseViewOperation;
import uk.gov.hmcts.ccd.domain.service.upsertdraft.UpsertDraftOperation;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseViewResource;
import uk.gov.hmcts.ccd.v2.internal.resource.DraftViewResource;

import java.time.Duration;
import java.time.Instant;

import static org.springframework.http.ResponseEntity.status;

@RestController
@RequestMapping(path = "/internal")
public class UIDraftsController {
    private static final Logger LOG = LoggerFactory.getLogger(UIDraftsController.class);

    private final UpsertDraftOperation upsertDraftOperation;
    private final GetCaseViewOperation getDraftViewOperation;
    private final DraftGateway draftGateway;

    @Autowired
    public UIDraftsController(
        @Qualifier("default") final UpsertDraftOperation upsertDraftOperation,
        @Qualifier(DefaultGetCaseViewFromDraftOperation.QUALIFIER) GetCaseViewOperation getDraftViewOperation,
        @Qualifier(CachedDraftGateway.QUALIFIER) DraftGateway draftGateway
    ) {
        this.upsertDraftOperation = upsertDraftOperation;
        this.getDraftViewOperation = getDraftViewOperation;
        this.draftGateway = draftGateway;
    }

    @PostMapping(
        path = "/case-types/{ctid}/drafts",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.UI_DRAFT_CREATE
        }
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Save draft as a caseworker.")
    @ApiResponse(responseCode = "201", description = "Draft created")
    @ApiResponse(responseCode = "422", description = "One of: cannot find event in requested case type or "
        + "unable to sanitize document for case field")
    @ApiResponse(responseCode = "500", description = "Draft store is down.")
    public ResponseEntity<DraftViewResource> saveDraft(
        @Parameter(name = "Case type ID", required = true)
        @PathVariable("ctid") final String caseTypeId,
        @RequestBody final CaseDataContent caseDataContent) {

        ResponseEntity.BodyBuilder builder = status(HttpStatus.CREATED);
        return builder.body(new DraftViewResource(upsertDraftOperation.executeSave(caseTypeId, caseDataContent),
            caseTypeId));
    }

    @PutMapping(
        path = "/case-types/{ctid}/drafts/{did}",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.UI_DRAFT_UPDATE
        }
    )
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Update draft as a caseworker.")
    @ApiResponse(responseCode = "200", description = "Draft updated")
    @ApiResponse(responseCode = "422", description = "One of: cannot find event in requested case type or "
        + "unable to sanitize document for case field")
    @ApiResponse(responseCode = "500", description = "Draft store is down.")
    public ResponseEntity<DraftViewResource> updateDraft(
        @PathVariable("ctid") final String caseTypeId,
        @PathVariable("did") final String draftId,
        @RequestBody final CaseDataContent caseDataContent) {

        return ResponseEntity.ok(new DraftViewResource(upsertDraftOperation.executeUpdate(caseTypeId, draftId,
            caseDataContent), caseTypeId));
    }

    @GetMapping(
        path = "/drafts/{did}",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.UI_DRAFT_READ
        })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Fetch a draft for display")
    @ApiResponse(responseCode = "200", description = "A displayable draft")
    @ApiResponse(responseCode = "500", description = "Draft store is down.")
    public ResponseEntity<CaseViewResource> findDraft(@PathVariable("did") final String did) {
        Instant start = Instant.now();
        CaseView caseView = getDraftViewOperation.execute(did);
        final Duration between = Duration.between(start, Instant.now());
        LOG.info("findDraft has been completed in {} millisecs...", between.toMillis());
        return ResponseEntity.ok(new CaseViewResource(caseView));
    }

    @DeleteMapping(path = "/drafts/{did}",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.UI_DRAFT_DELETE
        })
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Delete a given draft")
    @ApiResponse(responseCode = "200", description = "A draft deleted successfully")
    @ApiResponse(responseCode = "500", description = "Draft store is down.")
    public ResponseEntity<Void> deleteDraft(@PathVariable("did") final String did) {
        Instant start = Instant.now();
        draftGateway.delete(did);
        final Duration between = Duration.between(start, Instant.now());
        LOG.info("deleteDraft has been completed in {} millisecs...", between.toMillis());
        return ResponseEntity.ok().build();
    }
}
