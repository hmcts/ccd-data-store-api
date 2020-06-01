package uk.gov.hmcts.ccd.v2.internal.controller;

import com.google.common.base.Strings;
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
@Api(tags = {"Elastic Based Search API"})
@SwaggerDefinition(tags = {
    @Tag(name = "Elastic Based Search API", description = "Internal ElasticSearch based search API")
})
@Slf4j
public class UICaseSearchController {

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
        value = "Search cases according to the provided ElasticSearch query. Supports searching across multiple case types and a use case.",
        notes = V2.EXPERIMENTAL_WARNING
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
                      + "- Query is missing required `query` field.\n"
                      + "- Query includes blacklisted type.\n"
                      + "- Query has failed in ElasticSearch - for example, a sort is attempted on an unknown/unmapped field."
        ),
        @ApiResponse(
            code = 401,
            message = "Request doesn't include a valid `Authorization` header. "
                      + "This applies to all missing, malformed & expired tokens."
        ),
        @ApiResponse(
            code = 403,
            message = "Request doesn't include a valid `ServiceAuthorization` header. "
                      + "This applies to all missing, malformed & expired tokens.\n\n"
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
    @LogAudit(operationType = AuditOperationType.SEARCH_CASE, caseTypeIds = "#caseTypeIds",
        caseId = "T(uk.gov.hmcts.ccd.v2.internal.controller.UICaseSearchController).buildCaseIds(#result)")
    public ResponseEntity<CaseSearchResultViewResource> searchCases(
                                     @ApiParam(value = "Comma-separated list of case type ID(s).")
                                     @RequestParam(value = "ctid", required = false) List<String> caseTypeIds,
                                     @ApiParam(value = "Use case for search. Examples include `WORKBASKET`, `SEARCH` or `ORGCASES`.")
                                     @RequestParam(value = "usecase", required = false) final String useCase,
                                     @ApiParam(value = "Native ElasticSearch Search API request. Please refer to the ElasticSearch official "
                                         + "documentation.",
                                     example = "{\"_source\":[\"data.TextField\"],\"query\":{\"match_all\":{}},\"size\":20,\"from\":1}")
                                     @RequestBody String jsonSearchRequest) {
        Instant start = Instant.now();

        String useCaseTransformed = Strings.isNullOrEmpty(useCase) ? useCase : useCase.toUpperCase();
        CrossCaseTypeSearchRequest request = elasticsearchQueryHelper.prepareRequest(caseTypeIds, useCaseTransformed, jsonSearchRequest);
        CaseSearchResult caseSearchResult = caseSearchOperation.executeExternal(request);
        UICaseSearchResult uiCaseSearchResult = caseSearchOperation.executeInternal(caseSearchResult, caseTypeIds, useCaseTransformed);

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
