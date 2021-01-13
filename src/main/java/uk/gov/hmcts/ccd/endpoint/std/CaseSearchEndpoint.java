package uk.gov.hmcts.ccd.endpoint.std;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.LogAudit;
import uk.gov.hmcts.ccd.data.definition.CachedCaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.data.user.DefaultUserRepository;
import uk.gov.hmcts.ccd.data.user.UserRepository;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CrossCaseTypeSearchRequest;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.ElasticsearchQueryHelper;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.AuthorisedCaseSearchOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadRequestException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.CASE_ID_SEPARATOR;
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.MAX_CASE_IDS_LIST;

@RestController
@RequestMapping(path = "/",
    consumes = MediaType.APPLICATION_JSON_VALUE,
    produces = MediaType.APPLICATION_JSON_VALUE)
@Api(tags = {"Elastic Based Search API"})
@SwaggerDefinition(tags = {
    @Tag(name = "Elastic Based Search API", description = "ElasticSearch based search API")
})
@Slf4j
public class CaseSearchEndpoint {

    private final CaseSearchOperation caseSearchOperation;
    private final ElasticsearchQueryHelper elasticsearchQueryHelper;
    private final CaseDefinitionRepository caseDefinitionRepository;
    private final UserRepository userRepository;
    private final ApplicationParams applicationParams;

    @Autowired
    public CaseSearchEndpoint(@Qualifier(AuthorisedCaseSearchOperation.QUALIFIER)
                                      CaseSearchOperation caseSearchOperation,
                              @Qualifier(CachedCaseDefinitionRepository.QUALIFIER)
                                  CaseDefinitionRepository caseDefinitionRepository,
                              @Qualifier(DefaultUserRepository.QUALIFIER) UserRepository userRepository,
                              ElasticsearchQueryHelper elasticsearchQueryHelper,
                              ApplicationParams applicationParams) {
        this.caseSearchOperation = caseSearchOperation;
        this.elasticsearchQueryHelper = elasticsearchQueryHelper;
        this.caseDefinitionRepository = caseDefinitionRepository;
        this.userRepository = userRepository;
        this.applicationParams = applicationParams;
    }

    @PostMapping(value = "/searchCases")
    @ApiOperation("Search cases according to the provided ElasticSearch query. Supports searching across multiple case"
        + " types.")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "List of case data for the given search request")
    })
    @LogAudit(operationType = AuditOperationType.SEARCH_CASE, caseTypeIds = "#caseTypeIds",
        caseId = "T(uk.gov.hmcts.ccd.endpoint.std.CaseSearchEndpoint).buildCaseIds(#result)")
    public CaseSearchResult searchCases(
        @ApiParam(value = "Comma separated list of case type ID(s) or '*' if the search should be applied on any "
            + "existing case type. Note that using '*' is an expensive operation and might have low response times so "
            + "always prefer explicitly listing the case types when known in advance", required = true)
        @RequestParam("ctid") List<String> caseTypeIds,
        @ApiParam(value = "Native ElasticSearch Search API request. Please refer to the ElasticSearch official "
            + "documentation. For cross case type search, "
            + "the search results will contain only metadata by default (no case field data). To get case data in the "
            + "search results, please state the alias fields to be returned in the _source property for e.g."
            + " \"_source\":[\"alias.customer\",\"alias.postcode\"]",
            required = true)
        @RequestBody String jsonSearchRequest) {

        Instant start = Instant.now();
        validateCtid(caseTypeIds);
        ElasticsearchRequest elasticsearchRequest =
            elasticsearchQueryHelper.validateAndConvertRequest(jsonSearchRequest);

        if (!elasticsearchRequest.hasRequestedSupplementaryData()) {
            elasticsearchRequest.setRequestedSupplementaryData(ElasticsearchRequest.WILDCARD);
        }

        CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
            .withCaseTypes(getCaseTypeIds(caseTypeIds))
            .withSearchRequest(elasticsearchRequest)
            .build();

        CaseSearchResult result = caseSearchOperation.execute(request);

        Duration between = Duration.between(start, Instant.now());
        log.debug("searchCases execution completed in {} millisecs...", between.toMillis());

        return result;
    }

    private List<String> getCaseTypeIds(List<String> caseTypeIds) {
        if (isAllCaseTypesRequest(caseTypeIds)) {
            return getCaseTypes();
        }
        return caseTypeIds;
    }

    private List<String> getCaseTypes() {
        if (userRepository.anyRoleEqualsAnyOf(applicationParams.getCcdAccessControlCrossJurisdictionRoles())) {
            return caseDefinitionRepository.getAllCaseTypesIDs();
        } else {
            return getCaseTypesFromIdamRoles();
        }
    }

    private List<String> getCaseTypesFromIdamRoles() {
        List<String> jurisdictions = userRepository.getCaseworkerUserRolesJurisdictions();
        return caseDefinitionRepository.getCaseTypesIDsByJurisdictions(jurisdictions);
    }

    private void validateCtid(List<String> caseTypeIds) {
        if (caseTypeIds == null || caseTypeIds.size() == 0) {
            throw new BadRequestException("Missing required case type. Please provide a case type or list of case types"
                + " to search.");
        }
    }

    private boolean isAllCaseTypesRequest(List<String> caseTypeIds) {
        return ElasticsearchRequest.WILDCARD.equals(caseTypeIds.get(0));
    }

    public static String buildCaseIds(CaseSearchResult caseSearchResult) {
        return caseSearchResult.getCases().stream().limit(MAX_CASE_IDS_LIST)
            .map(c -> String.valueOf(c.getReference()))
            .collect(Collectors.joining(CASE_ID_SEPARATOR));
    }
}
