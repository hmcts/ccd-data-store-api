package uk.gov.hmcts.ccd.v2.internal.controller;

import io.swagger.annotations.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.domain.model.search.*;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.UICaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.*;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.*;
import uk.gov.hmcts.ccd.v2.*;
import uk.gov.hmcts.ccd.v2.internal.resource.*;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.CASE_ID_SEPARATOR;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.MAX_CASE_IDS_LIST;

@RestController
@RequestMapping(path = "/internal/searchCases")
@Slf4j
public class UICaseSearchController {
    private static final String ERROR_CASE_ID_INVALID = "Case ID is not valid";

    private final CaseSearchOperation caseSearchOperation;
    private final ElasticsearchQueryHelper elasticsearchQueryHelper;

    @Autowired
    public UICaseSearchController(
        @Qualifier(AuthorisedCaseSearchOperation.QUALIFIER) CaseSearchOperation caseSearchOperation,
        ElasticsearchQueryHelper elasticsearchQueryHelper) {
        this.caseSearchOperation = caseSearchOperation;
        this.elasticsearchQueryHelper = elasticsearchQueryHelper;
    }

    @PostMapping(
        path = "",
        headers = {
            V2.EXPERIMENTAL_HEADER
        },
        produces = {
            V2.MediaType.UI_CASE_SEARCH
        }
    )
    @ApiOperation(
        value = "Elastic search for cases returning paginated data",
        notes = V2.EXPERIMENTAL_WARNING
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success",
            response = CaseSearchResultViewResource.class
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
    // TODO: Docs
    @LogAudit(operationType = AuditOperationType.SEARCH_CASE, caseTypeIds = "#caseTypeIds",
        caseId = "T(uk.gov.hmcts.ccd.v2.internal.controller.UICaseSearchController).buildCaseIds(#result)")
    public ResponseEntity<CaseSearchResultViewResource> searchCases(@ApiParam(value = "Case type ID(s)")
                                     @RequestParam(value = "ctid", required = false) List<String> caseTypeIds,
                                     @ApiParam(value = "Case type ID(s)")
                                     @RequestParam(value = "usecase", required = false) final String useCase,
                                     @ApiParam(value = "Native ElasticSearch Search API request. Please refer to the ElasticSearch official "
                                         + "documentation. For cross case type search, "
                                         + "the search results will contain only metadata by default (no case field data). To get case data in the "
                                         + "search results, please state the alias fields to be returned in the _source property for e.g."
                                         + " \"_source\":[\"alias.customer\",\"alias.postcode\"]",
                                         required = true)
                                     @RequestBody String jsonSearchRequest) {
        Instant start = Instant.now();

        CrossCaseTypeSearchRequest request = elasticsearchQueryHelper.prepareRequest(caseTypeIds, useCase, jsonSearchRequest);
        CaseSearchResult caseSearchResult = caseSearchOperation.executeExternal(request);
        UICaseSearchResult uiCaseSearchResult = caseSearchOperation.executeInternal(caseSearchResult, caseTypeIds, UseCase.valueOfReference(useCase));

        Duration between = Duration.between(start, Instant.now());
        log.debug("Internal searchCases execution completed in {} millisecs...", between.toMillis());

        return ResponseEntity.ok(new CaseSearchResultViewResource(uiCaseSearchResult));
    }

    public static String buildCaseIds(ResponseEntity<CaseSearchResultViewResource> response) {
        return response.getBody().getCases().stream().limit(MAX_CASE_IDS_LIST)
            .map(SearchResultViewItem::getCaseId)
            .collect(Collectors.joining(CASE_ID_SEPARATOR));
    }
}
