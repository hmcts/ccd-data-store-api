package uk.gov.hmcts.ccd.v2.internal.controller;

import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.CASE_ID_SEPARATOR;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.MAX_CASE_IDS_LIST;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Strings;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import lombok.extern.slf4j.Slf4j;
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
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.AuthorisedCaseSearchOperation;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseSearchResultViewResource;

@RestController
@RequestMapping(path = "/internal/searchCases", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
@Api(tags = {"Elastic Based Search API"})
@SwaggerDefinition(tags = {
    @Tag(name = "Elastic Based Search API", description = "Internal ElasticSearch based case search API, "
        + "returning extra information required by the UI for display purposes on a UI.")
})
@Slf4j
public class UICaseSearchController {

    private final CaseSearchOperation caseSearchOperation;
    private final ElasticsearchQueryHelper elasticsearchQueryHelper;
    private final CaseSearchResultViewGenerator caseSearchResultViewGenerator;
    private final ElasticsearchSortService elasticsearchSortService;

    @Autowired
    public UICaseSearchController(
        @Qualifier(AuthorisedCaseSearchOperation.QUALIFIER) CaseSearchOperation caseSearchOperation,
        ElasticsearchQueryHelper elasticsearchQueryHelper,
        CaseSearchResultViewGenerator caseSearchResultViewGenerator,
        ElasticsearchSortService elasticsearchSortService) {
        this.caseSearchOperation = caseSearchOperation;
        this.elasticsearchQueryHelper = elasticsearchQueryHelper;
        this.caseSearchResultViewGenerator = caseSearchResultViewGenerator;
        this.elasticsearchSortService = elasticsearchSortService;
    }

    @PostMapping(path = "")
    @ApiOperation(
        value = "Search cases according to the provided ElasticSearch query. Supports searching a single case type and a use case."
    )
    @ApiResponses({
        @ApiResponse(
            code = 200,
            message = "Success.",
            response = CaseSearchResultViewResource.class
        ),
        @ApiResponse(
            code = 400,
            message = "Request is invalid. For some other types HTTP code 500 is returned instead.\n"
                      + "Examples include:\n"
                      + "- Unsupported use case specified in `usecase` query parameter.\n"
                      + "- No case type query parameter `ctid` provided.\n"
                      + "- Query is missing required `query` field.\n"
                      + "- Query includes blacklisted type.\n"
                      + "- Query has failed in ElasticSearch - for example, a sort is attempted on an unknown/unmapped field.\n"
                      + "- Query includes supplementary_data which is NOT an array of text values.\n"
        ),
        @ApiResponse(
            code = 401,
            message = "Request doesn't include a valid `Authorization` header. "
                      + "This applies to all missing, malformed & expired tokens."
        ),
        @ApiResponse(
            code = 403,
            message = "Request doesn't include a valid `ServiceAuthorization` header. "
                      + "This applies to all missing, malformed & expired tokens.\n"
                      + "A valid S2S token issued to the name of a non-permitted API Client will also return the same."
        ),
        @ApiResponse(
            code = 404,
            message = "Case type specified in `ctid` query parameter could not be found."
        ),
        @ApiResponse(
            code = 500,
            message = "An unexpected situation that is not attributable to the user or API Client; or request is invalid. "
                      + "For some other types HTTP code 400 is returned instead.\n"
                      + "Invalid request examples include:\n"
                      + "- Malformed JSON request."
        )
    })
    @ApiImplicitParams(
        @ApiImplicitParam(
            name = "jsonSearchRequest",
            value = "A wrapped native ElasticSearch Search API request as a JSON string. "
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
                                     @ApiParam(value = "Case type ID for search.", required = true)
                                     @RequestParam(value = "ctid") String caseTypeId,
                                     @ApiParam(value = "Use case for search. Examples include `WORKBASKET`, `SEARCH` or `orgCases`. "
                                         + "Used when the list of fields to return is configured in the CCD definition.\n"
                                         + "If omitted, all case fields are returned.")
                                     @RequestParam(value = "use_case", required = false) final String useCase,
                                     @RequestBody String jsonSearchRequest) {
        Instant start = Instant.now();

        ElasticsearchRequest searchRequest = elasticsearchQueryHelper.validateAndConvertRequest(jsonSearchRequest);
        String useCaseUppercase = Strings.isNullOrEmpty(useCase) || searchRequest.hasSourceFields() ? null : useCase.toUpperCase();
        elasticsearchSortService.applyConfiguredSort(searchRequest, caseTypeId, useCaseUppercase);
        List<String> requestedFields = searchRequest.getRequestedFields();

        if (useCaseUppercase == null && !searchRequest.hasRequestedSupplementaryData()) {
            searchRequest.setRequestedSupplementaryData(ElasticsearchRequest.WILDCARD);
        }

        CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
            .withCaseTypes(Collections.singletonList(caseTypeId))
            .withSearchRequest(searchRequest)
            .build();

        CaseSearchResult caseSearchResult = caseSearchOperation.execute(request);
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
