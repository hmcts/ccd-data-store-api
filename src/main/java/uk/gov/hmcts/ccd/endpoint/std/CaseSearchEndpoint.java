package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.common.ObjectMapperService;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.AuthorisedCaseSearchOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.CASE_ID_SEPARATOR;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.MAX_CASE_IDS_LIST;

@RestController
@RequestMapping(path = "/",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
@Api(tags = {"Elastic Based Search API"})
@SwaggerDefinition(tags = {
    @Tag(name = "Elastic Based Search API", description = "New ElasticSearch based search API")
})
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

    @PostMapping(value = "/searchCases")
    @ApiOperation("Search cases according to the provided ElasticSearch query. Supports searching across multiple case types.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "List of case data for the given search request")
    })
    @LogAudit(operationType = OperationType.SEARCH_CASE, caseTypeIds = "#caseTypeIds",
        caseId = "T(uk.gov.hmcts.ccd.endpoint.std.CaseSearchEndpoint).buildCaseIds(#result)")
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
        log.debug("searchCases execution completed in {} millisecs...", between.toMillis());

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

    public static String buildCaseIds(CaseSearchResult caseSearchResult) {
        return caseSearchResult.getCases().stream().limit(MAX_CASE_IDS_LIST)
            .map(c -> String.valueOf(c.getReference()))
            .collect(Collectors.joining(CASE_ID_SEPARATOR));
    }
}
