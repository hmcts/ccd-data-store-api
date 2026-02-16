package uk.gov.hmcts.ccd.v2.internal.controller;

import com.google.common.base.Strings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.CaseSearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.ccd.domain.service.search.CaseSearchResultViewGenerator;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchQueryHelper;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchSortService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.SearchIndex;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.AuthorisedCaseSearchOperation;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseSearchResultViewResource;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.CASE_ID_SEPARATOR;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.MAX_CASE_IDS_LIST;

@RestController
@RequestMapping(path = "/internal/searchCases", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Elastic Based Search API", description = "Internal ElasticSearch based case search API, "
        + "returning extra information required by the UI for display purposes on a UI.")
@Slf4j
public class UICaseSearchController {

    private final CaseSearchOperation caseSearchOperation;
    private final ElasticsearchQueryHelper elasticsearchQueryHelper;
    private final CaseSearchResultViewGenerator caseSearchResultViewGenerator;
    private final ElasticsearchSortService elasticsearchSortService;
    private final ApplicationParams applicationParams;

    @Autowired
    @SuppressWarnings("checkstyle:LineLength") //don't want to break message

    public UICaseSearchController(
        @Qualifier(AuthorisedCaseSearchOperation.QUALIFIER) CaseSearchOperation caseSearchOperation,
        ElasticsearchQueryHelper elasticsearchQueryHelper,
        CaseSearchResultViewGenerator caseSearchResultViewGenerator,
        ElasticsearchSortService elasticsearchSortService,
        ApplicationParams applicationParams) {
        this.caseSearchOperation = caseSearchOperation;
        this.elasticsearchQueryHelper = elasticsearchQueryHelper;
        this.caseSearchResultViewGenerator = caseSearchResultViewGenerator;
        this.elasticsearchSortService = elasticsearchSortService;
        this.applicationParams = applicationParams;
    }

    public ResponseEntity<CaseSearchResultViewResource> searchCases(
        @Parameter(name = "Case type ID for search.", required = true)
        @RequestParam(value = "ctid") String caseTypeId,
        @Parameter(name = "Use case for search. Examples include `WORKBASKET`, `SEARCH` "
            + "or `orgCases`. Used when the list of fields to return is configured in the "
            + "CCD definition.\nIf omitted, all case fields are returned.")
        @RequestParam(value = "use_case", required = false) final String useCase,
        @RequestBody String jsonSearchRequest) {

        return searchCases(caseTypeId, useCase, false, jsonSearchRequest);
    }

    @PostMapping(path = "")
    @Operation(
        description = "Search cases according to the provided ElasticSearch query. Supports searching a single"
        + " case type and a use case."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Success.",
        content = @Content(schema = @Schema(implementation = CaseSearchResultViewResource.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Request is invalid. For some other types HTTP code 500 is returned instead.\n"
                  + "Examples include:\n"
                  + "- Unsupported use case specified in `usecase` query parameter.\n"
                  + "- No case type query parameter `ctid` provided.\n"
                  + "- Query is missing required `query` field.\n"
                  + "- Query includes blacklisted type.\n"
                  + "- Query has failed in ElasticSearch - for example, a sort is attempted on an unknown/unmapped field.\n"
                  + "- Query includes supplementary_data which is NOT an array of text values.\n"
    )
    @ApiResponse(
        responseCode = "401",
        description = "Request doesn't include a valid `Authorization` header. "
                  + "This applies to all missing, malformed & expired tokens."
    )
    @ApiResponse(
        responseCode = "403",
        description = "Request doesn't include a valid `ServiceAuthorization` header. "
                  + "This applies to all missing, malformed & expired tokens.\n"
                  + "A valid S2S token issued to the name of a non-permitted API Client will also return the same."
    )
    @ApiResponse(
        responseCode = "404",
        description = "Case type specified in `ctid` query parameter could not be found."
    )
    @ApiResponse(
        responseCode = "500",
        description = "An unexpected situation that is not attributable to the user or API Client; "
                  + "or request is invalid. For some other types HTTP code 400 is returned instead.\n"
                  + "Invalid request examples include:\n"
                  + "- Malformed JSON request."
    )
    @SuppressWarnings("checkstyle:LineLength") // don't want to break message
    @Parameters(
        @Parameter(
            name = "jsonSearchRequest",
            description = "A wrapped native ElasticSearch Search API request as a JSON string. "
                    + "Please refer to the following for further information:\n"
                    + "- [Official ElasticSearch Documentation - Search APIs]"
                    + "(https://www.elastic.co/guide/en/elasticsearch/reference/current/search.html)\n"
                    + "- [Official ElasticSearch Documentation - Query DSL]"
                    + "(https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html)\n"
                    + "- [CCD ElasticSearch API LLD]"
                    + "(https://tools.hmcts.net/confluence/pages/viewpage.action?pageId=843514186)\n\n"
                    + "Note that for backward compatibility this API also supports unwrapped native ElasticSearch requests "
                    + "(i.e. requests consisting of a native query instead of being wrapped in a `native_es_query` object).",
            example = "{\n\t\"native_es_query\": {\n\t\t\"query\": { \n\t\t\t\"match_all\": {} \n\t\t},\n\t\t\"sort\": "
                    + "[\n\t\t\t{ \"reference.keyword\": \"asc\" }\n\t\t],\n\t\t\"size\": 20,\n\t\t\"from\": 1\n\t},"
                    + "\n\t\"supplementary_data\": [\n\t\t\"orgs_assigned_users\"\n\t]\n}",
            required = true
        )
    )
    @LogAudit(operationType = AuditOperationType.SEARCH_CASE, caseTypeIds = "#caseTypeIds",
        caseId = "T(uk.gov.hmcts.ccd.v2.internal.controller.UICaseSearchController).buildCaseIds(#result)")
    public ResponseEntity<CaseSearchResultViewResource> searchCases(
                                     @Parameter(name = "Case type ID for search.", required = true)
                                     @RequestParam(value = "ctid") String caseTypeId,
                                     @Parameter(name = "Use case for search. Examples include `WORKBASKET`, `SEARCH` "
                                         + "or `orgCases`. Used when the list of fields to return is configured in the "
                                         + "CCD definition.\nIf omitted, all case fields are returned.")
                                     @RequestParam(value = "use_case", required = false) final String useCase,
                                     @RequestParam(value = "global", required = false, defaultValue = "false") boolean global,
                                     @RequestBody String jsonSearchRequest) {
        final Instant start = Instant.now();

        ElasticsearchRequest searchRequest = elasticsearchQueryHelper.validateAndConvertRequest(jsonSearchRequest);
        String useCaseUppercase = (Strings.isNullOrEmpty(useCase) || searchRequest.hasSourceFields())
                ? null : useCase.toUpperCase(Locale.ENGLISH);
        elasticsearchSortService.applyConfiguredSort(searchRequest, caseTypeId, useCaseUppercase);
        List<String> requestedFields = searchRequest.getRequestedFields();

        if (useCaseUppercase == null && !searchRequest.hasRequestedSupplementaryData()) {
            searchRequest.setRequestedSupplementaryData(ElasticsearchRequest.WILDCARD);
        }

        CrossCaseTypeSearchRequest.Builder builder = new CrossCaseTypeSearchRequest.Builder()
            .withCaseTypes(Collections.singletonList(caseTypeId))
            .withSearchRequest(searchRequest);

        if (global) {
            SearchIndex searchIndex = new SearchIndex(
                applicationParams.getGlobalSearchIndexName(),
                applicationParams.getGlobalSearchIndexType()
            );
            builder.withSearchIndex(searchIndex);
            log.info("pointing to global search index...");
        }

        CrossCaseTypeSearchRequest request = builder.build();

        CaseSearchResult caseSearchResult = caseSearchOperation.execute(request, false);
        CaseSearchResultView caseSearchResultView = caseSearchResultViewGenerator
            .execute(caseTypeId, caseSearchResult, useCaseUppercase, requestedFields);

        Duration between = Duration.between(start, Instant.now());
        log.debug("Internal searchCases execution completed in {} millisecs...", between.toMillis());

        return ResponseEntity.ok(new CaseSearchResultViewResource(caseSearchResultView));
    }

    public static String buildCaseIds(ResponseEntity<CaseSearchResultViewResource> response) {
        CaseSearchResultViewResource body = response.getBody();
        return body == null ? null
                : body.getCases().stream().limit(
                        MAX_CASE_IDS_LIST)
                .map(SearchResultViewItem::getCaseId)
                .collect(Collectors.joining(CASE_ID_SEPARATOR));
    }
}
