package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.search.CaseDetailsSearchResult;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseDetailsSearchOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping(path = "/",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "/", description = "New ElasticSearch based search API")
public class CaseDetailsSearchEndpoint {

    @Autowired
    private CaseDetailsSearchOperation caseDetailsSearchOperation;

    @Autowired
    private ApplicationParams applicationParams;

    @RequestMapping(value = "/searchCases", method = RequestMethod.POST)
    @ApiOperation("Search case data according to the provided ElasticSearch query")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "List of case data for the given search request")
    })
    public CaseDetailsSearchResult searchCases(
            @ApiParam(value = "Case type ID", required = true)
            @RequestParam("ctid") List<String> caseTypeIds,
            @ApiParam(name="native ElasticSearch Search API request. Please refer to the ElasticSearch official documentation", required = true)
            @RequestBody String jsonSearchRequest) throws IOException {

        validateSearchRequest(jsonSearchRequest);
        return caseDetailsSearchOperation.execute(caseTypeIds, jsonSearchRequest);
    }

    private void validateSearchRequest(String searchRequest) throws IOException {
        Optional<Map> query = getQuery(searchRequest);
        validateSearchRequestContainsQuery(query);
        rejectBlackListedQuery(query);
    }

    private Optional<Map> getQuery(String searchRequest) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        final Map<String, Object> map = mapper.readValue(searchRequest, new TypeReference<Map<String, Object>>(){});
        return Optional.ofNullable((Map) map.get("query"));
    }

    private void validateSearchRequestContainsQuery(Optional<Map> queryOpt) throws IOException {
        if (!queryOpt.isPresent()) {
            throw new BadSearchRequest("missing required field 'query'");
        }
    }

    private void rejectBlackListedQuery(Optional<Map> queryOpt) throws IOException {
        queryOpt.ifPresent(query -> {
            List<String> blackListedQueries = applicationParams.getSearchBlackList();
            Optional<String> blackListedQueryOpt = blackListedQueries.stream().filter(blacklisted ->
                    query.get(blacklisted) != null
            ).findFirst();

            blackListedQueryOpt.ifPresent(blacklisted -> {
                throw new BadSearchRequest(String.format("Query of type '%s' is not allowed", blacklisted));
            });
        });
    }
}
