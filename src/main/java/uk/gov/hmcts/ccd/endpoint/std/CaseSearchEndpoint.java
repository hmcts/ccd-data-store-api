package uk.gov.hmcts.ccd.endpoint.std;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
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
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.AuthorisedCaseSearchOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

@RestController
@RequestMapping(path = "/",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
@Api(value = "/", description = "New ElasticSearch based search API")
public class CaseSearchEndpoint {

    private final CaseSearchOperation caseSearchOperation;
    private final ApplicationParams applicationParams;
    private final ObjectMapperService objectMapperService;

    @Autowired
    public CaseSearchEndpoint(@Qualifier(AuthorisedCaseSearchOperation.QUALIFIER) CaseSearchOperation caseSearchOperation,
                              ApplicationParams applicationParams, ObjectMapperService objectMapperService) {
        this.caseSearchOperation = caseSearchOperation;
        this.applicationParams = applicationParams;
        this.objectMapperService = objectMapperService;
    }

    @RequestMapping(value = "/searchCases", method = RequestMethod.POST)
    @ApiOperation("Search case data according to the provided ElasticSearch query")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "List of case data for the given search request")
    })
    public CaseSearchResult searchCases(
        @ApiParam(value = "Case type ID", required = true)
        @RequestParam("ctid") String caseTypeId,
        @ApiParam(name = "native ElasticSearch Search API request. Please refer to the ElasticSearch official documentation", required = true)
        @RequestBody String jsonSearchRequest) {

        rejectBlackListedQuery(jsonSearchRequest);
        CaseSearchRequest caseSearchRequest = new CaseSearchRequest(objectMapperService, caseTypeId, jsonSearchRequest);
        return caseSearchOperation.execute(caseSearchRequest);
    }

    private void rejectBlackListedQuery(String jsonSearchRequest) {
        List<String> blackListedQueries = applicationParams.getSearchBlackList();
        Optional<String> blackListedQueryOpt = blackListedQueries.stream().filter(blacklisted -> {
                Pattern p = Pattern.compile("\\b" + blacklisted + "\\b");
                Matcher m = p.matcher(jsonSearchRequest);
                return m.find();
            }
        ).findFirst();
        blackListedQueryOpt.ifPresent(blacklisted -> {
            throw new BadSearchRequest(String.format("Query of type '%s' is not allowed", blacklisted));
        });
    }
}
