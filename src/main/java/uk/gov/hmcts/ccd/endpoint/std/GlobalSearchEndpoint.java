package uk.gov.hmcts.ccd.endpoint.std;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchResponsePayload;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;
import uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchParser;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.AuthorisedCaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.global.GlobalSearchService;

import javax.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.CASE_ID_SEPARATOR;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.MAX_CASE_IDS_LIST;

@Slf4j
@RestController
@Validated
public class GlobalSearchEndpoint {

    private final CaseSearchOperation caseSearchOperation;
    private final GlobalSearchService globalSearchService;
    private final GlobalSearchParser globalSearchParser;

    @SuppressWarnings({"squid:S1075"})
    public static final String GLOBAL_SEARCH_PATH = "/globalSearch";

    @Autowired
    public GlobalSearchEndpoint(@Qualifier(AuthorisedCaseSearchOperation.QUALIFIER)
                                    CaseSearchOperation caseSearchOperation,
                                GlobalSearchService globalSearchService, GlobalSearchParser globalSearchParser) {
        this.caseSearchOperation = caseSearchOperation;
        this.globalSearchService = globalSearchService;
        this.globalSearchParser = globalSearchParser;
    }

    @PostMapping(path = GLOBAL_SEARCH_PATH, consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Global Search Request", notes = "Global Search Request")
    @ApiResponses(value = {
        @ApiResponse(
            code = 200,
            message = "Search Request Valid"
            ),
        @ApiResponse(
            code = 400,
            message = "One or more of the following reasons:"
                + "\n1) " + ValidationError.DATE_OF_DEATH_INVALID
                + "\n2) " + ValidationError.SORT_BY_INVALID
                + "\n3) " + ValidationError.SORT_DIRECTION_INVALID
                + "\n4) " + ValidationError.MAX_RECORD_COUNT_INVALID
                + "\n5) " + ValidationError.GLOBAL_SEARCH_CRITERIA_INVALID
                + "\n6) " + ValidationError.JURISDICTION_ID_LENGTH_INVALID
                + "\n7) " + ValidationError.STATE_ID_LENGTH_INVALID
                + "\n8) " + ValidationError.CASE_TYPE_ID_LENGTH_INVALID
                + "\n9) " + ValidationError.CASE_REFERENCE_INVALID
                + "\n10) " + ValidationError.START_RECORD_NUMBER_INVALID
                + "\n11) " + ValidationError.DATE_OF_BIRTH_INVALID,

            examples = @Example({
                @ExampleProperty(
                    value = "{\n"
                        + "   \"status\": \"400\",\n"
                        + "   \"error\": \"Bad Request\",\n"
                        + "   \"message\": \"" + ValidationError.ARGUMENT_INVALID + "\",\n"
                        + "   \"path\": \"" + GLOBAL_SEARCH_PATH + "\",\n"
                        + "   \"details\": [ \"" + ValidationError.STATE_ID_LENGTH_INVALID + "\" ]\n"
                        + "}",
                    mediaType = APPLICATION_JSON_VALUE)
            })
            )
    })
    @LogAudit(
        operationType = AuditOperationType.GLOBAL_SEARCH,
        caseTypeIds = "T(uk.gov.hmcts.ccd.endpoint.std.GlobalSearchEndpoint).buildCaseTypeIds(#requestPayload)",
        caseId = "T(uk.gov.hmcts.ccd.endpoint.std.GlobalSearchEndpoint).buildCaseIds(#result)"
    )
    public GlobalSearchResponsePayload searchForCases(@RequestBody @Valid GlobalSearchRequestPayload requestPayload) {

        Instant start = Instant.now();

        requestPayload.setDefaults();

        CrossCaseTypeSearchRequest searchRequest = globalSearchService.assembleSearchQuery(requestPayload);

        CaseSearchResult caseSearchResult = caseSearchOperation.execute(searchRequest, true);

        List<CaseDetails> filteredCaseList = globalSearchParser.filterCases(caseSearchResult.getCases(),
                                                                            requestPayload.getSearchCriteria());

        GlobalSearchResponsePayload result = globalSearchService.transformResponse(requestPayload,
            caseSearchResult.getTotal(), filteredCaseList);

        Duration between = Duration.between(start, Instant.now());
        log.debug("GlobalSearchEndpoint.searchForCases execution completed in {} milliseconds...", between.toMillis());

        return result;
    }

    public static String buildCaseIds(GlobalSearchResponsePayload globalSearchResult) {
        return globalSearchResult.getResults().stream().limit(MAX_CASE_IDS_LIST)
            .map(c -> String.valueOf(c.getCaseReference()))
            .collect(Collectors.joining(CASE_ID_SEPARATOR));
    }

    public static String buildCaseTypeIds(GlobalSearchRequestPayload globalSearchRequest) {
        // NB: match Case ID list size and separator configuration
        return getCaseTypeIds(globalSearchRequest).limit(MAX_CASE_IDS_LIST)
            .collect(Collectors.joining(CASE_ID_SEPARATOR));
    }

    private static Stream<String> getCaseTypeIds(GlobalSearchRequestPayload globalSearchRequest) {
        return globalSearchRequest != null && globalSearchRequest.getSearchCriteria() != null
            ? Optional.ofNullable(globalSearchRequest.getSearchCriteria().getCcdCaseTypeIds())
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            : Stream.empty();
    }
}
