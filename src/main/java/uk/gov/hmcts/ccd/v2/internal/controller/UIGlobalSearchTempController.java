package uk.gov.hmcts.ccd.v2.internal.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.SearchResultViewItem;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteriaResponse;
import uk.gov.hmcts.ccd.domain.model.search.global.SearchCriteriaResponseTEMP;
import uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchParser;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseSearchResultViewResource;

import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.CASE_ID_SEPARATOR;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.MAX_CASE_IDS_LIST;

@RestController
@RequestMapping(path = "/internal/searchCases/global", consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Api(tags = {"Elastic Based Search API"})
@SwaggerDefinition(tags = {
    @Tag(name = "Elastic Based Search API", description = "Internal ElasticSearch based case search API, "
        + "returning extra information required by the UI for display purposes on a UI.")
})
@Slf4j
public class UIGlobalSearchTempController {

    private final GlobalSearchParser globalSearchParser;

    @Autowired
    @SuppressWarnings("checkstyle:LineLength") //don't want to break message

    public UIGlobalSearchTempController(
        GlobalSearchParser globalSearchParser) {
        this.globalSearchParser = globalSearchParser;
    }

    @Transactional
    @PostMapping(path = "")
    @ApiOperation(
        value = "Search cases according to the provided ElasticSearch query. Supports searching a single case type and"
            + " a use case."
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
            message = "An unexpected situation that is not attributable to the user or API Client; "
                      + "or request is invalid. For some other types HTTP code 400 is returned instead.\n"
                      + "Invalid request examples include:\n"
                      + "- Malformed JSON request."
        )
    })
    @SuppressWarnings("checkstyle:LineLength") // don't want to break message
    @LogAudit(operationType = AuditOperationType.SEARCH_CASE, caseTypeIds = "#caseTypeIds",
        caseId = "T(uk.gov.hmcts.ccd.v2.internal.controller.UICaseSearchController).buildCaseIds(#result)")
    public ResponseEntity<List<SearchCriteriaResponse>> searchCases(@RequestBody SearchCriteriaResponseTEMP values) {
        values.getRequestValues();
        List<SearchCriteriaResponse> response = globalSearchParser.filterCases(values.getResponse(), values.getRequestValues());

        return ResponseEntity.ok(response);
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
