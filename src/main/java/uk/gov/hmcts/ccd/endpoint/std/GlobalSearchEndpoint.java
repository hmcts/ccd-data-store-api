package uk.gov.hmcts.ccd.endpoint.std;

import io.swagger.annotations.ApiOperation;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.domain.model.search.global.GlobalSearchRequestPayload;
import uk.gov.hmcts.ccd.domain.model.std.validator.ValidationError;
import uk.gov.hmcts.ccd.domain.service.globalsearch.GlobalSearchService;

import javax.validation.Valid;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Validated
public class GlobalSearchEndpoint {

    private final GlobalSearchService globalSearchService;
    public static final String GLOBAL_SEARCH_PATH = "/globalSearch";

    @Autowired
    public GlobalSearchEndpoint(GlobalSearchService globalSearchService) {
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
                + "\n1) " + ValidationError.DATE_OF_DEATH_INVALID
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
    public void searchForCases(@RequestBody @Valid GlobalSearchRequestPayload requestPayload) {
        requestPayload.setDefaults();
        globalSearchService.assembleSearchQuery(requestPayload);
    }
}
