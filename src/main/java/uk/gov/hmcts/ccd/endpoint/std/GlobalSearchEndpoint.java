package uk.gov.hmcts.ccd.endpoint.std;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.dto.globalsearch.GlobalSearchResponse;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;
import uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchQueryHelper;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.AuthorisedCaseSearchOperation;

import javax.validation.Valid;
import java.time.Duration;
import java.time.Instant;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@Validated
public class GlobalSearchEndpoint {

    private final CaseSearchOperation caseSearchOperation;
    private final ElasticsearchQueryHelper elasticsearchQueryHelper;
    private final GlobalSearchService globalSearchService;

    @SuppressWarnings({"squid:S1075"})
    public static final String GLOBAL_SEARCH_PATH = "/globalSearch";

    @Autowired
    public GlobalSearchEndpoint(@Qualifier(AuthorisedCaseSearchOperation.QUALIFIER)
                                CaseSearchOperation caseSearchOperation,
                                ElasticsearchQueryHelper elasticsearchQueryHelper,
                                GlobalSearchService globalSearchService) {
        this.caseSearchOperation = caseSearchOperation;
        this.elasticsearchQueryHelper = elasticsearchQueryHelper;
        this.globalSearchService = globalSearchService;
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
                + "\n1) " + ValidationError.PARTIES_INVALID
                + "\n2) " + ValidationError.SORT_BY_INVALID
                + "\n3) " + ValidationError.SORT_DIRECTION_INVALID
                + "\n4) " + ValidationError.MAX_RECORD_COUNT_INVALID
                + "\n5) " + ValidationError.SEARCH_CRITERIA_MISSING
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
    public GlobalSearchResponse searchForCases(@RequestBody @Valid GlobalSearchRequestPayload requestPayload) {

        Instant start = Instant.now();

        requestPayload.setDefaults();

        // if no CaseType filter applied :: load all case types available for user
        if (CollectionUtils.isEmpty(requestPayload.getSearchCriteria().getCcdCaseTypeIds())) {
            requestPayload.getSearchCriteria().setCcdCaseTypeIds(
                elasticsearchQueryHelper.getCaseTypesAvailableToUser()
            );
        }

        CrossCaseTypeSearchRequest searchRequest = globalSearchService.assembleSearchQuery(requestPayload);

        CaseSearchResult caseSearchResult = caseSearchOperation.execute(searchRequest, true);

        GlobalSearchResponse result = globalSearchService.transformResponse(requestPayload, caseSearchResult);

        Duration between = Duration.between(start, Instant.now());
        log.debug("GlobalSearchEndpoint.searchForCases execution completed in {} milliseconds...", between.toMillis());

        return result;

    }

}
