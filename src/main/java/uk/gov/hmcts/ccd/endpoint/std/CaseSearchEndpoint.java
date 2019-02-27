package uk.gov.hmcts.ccd.endpoint.std;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.AuthorisedCaseSearchOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

@RestController
@RequestMapping(path = "/",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "/", description = "New ElasticSearch based search API")
@Slf4j
public class CaseSearchEndpoint {

    private final CaseSearchOperation caseSearchOperation;
    private final ApplicationParams applicationParams;
    private final ObjectMapperService objectMapperService;

    @Autowired
    public CaseSearchEndpoint(@Qualifier(AuthorisedCaseSearchOperation.QUALIFIER) CaseSearchOperation caseSearchOperation,
                              ApplicationParams applicationParams,
                              ObjectMapperService objectMapperService) {
        this.caseSearchOperation = caseSearchOperation;
        this.applicationParams = applicationParams;
        this.objectMapperService = objectMapperService;
    }

    @RequestMapping(value = "/searchCases", method = RequestMethod.POST)
    @ApiOperation("Search cases according to the provided ElasticSearch query. Supports searching across multiple case types.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "List of case data for the given search request")
    })
    public CaseSearchResult searchCases(
        @ApiParam(value = "Case type ID(s)", required = true)
        @RequestParam("ctid") List<String> caseTypeIds,
        @ApiParam(value = "Native ElasticSearch Search API request. Please refer to the ElasticSearch official "
            + "documentation. For cross case type search, "
            + "the search results will contain only metadata by default (no case field data). To get case data in the "
            + "search results, please state the alias fields to be returned in the _source property for e.g."
            + " \"_source\":[\"alias.customer\",\"alias.postcode\"]",
                  required = true)
        @RequestBody String jsonSearchRequest) {

        Instant start = Instant.now();

        rejectBlackListedQuery(jsonSearchRequest);

        CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
            .withCaseTypes(caseTypeIds)
            .withSearchRequest(stringToJsonNode(jsonSearchRequest))
            .build();

        CaseSearchResult result = caseSearchOperation.execute(request);

        Duration between = Duration.between(start, Instant.now());
        log.info("searchCases execution completed in {} millisecs...", between.toMillis());

        return result;
    }

    private JsonNode stringToJsonNode(String jsonSearchRequest) {
        return objectMapperService.convertStringToObject(jsonSearchRequest, JsonNode.class);
    }

    private void rejectBlackListedQuery(String jsonSearchRequest) {
        List<String> blackListedQueries = applicationParams.getSearchBlackList();
        Optional<String> blackListedQueryOpt = blackListedQueries
            .stream()
            .filter(blacklisted -> {
                Pattern p = Pattern.compile("\\b" + blacklisted + "\\b");
                Matcher m = p.matcher(jsonSearchRequest);
                return m.find();
            })
            .findFirst();
        blackListedQueryOpt.ifPresent(blacklisted -> {
            throw new BadSearchRequest(String.format("Query of type '%s' is not allowed", blacklisted));
        });
    }
}
