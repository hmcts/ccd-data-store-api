package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.core.MultiSearch;
import io.searchbox.core.MultiSearchResult;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.model.search.UseCase;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.UICaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.aggregated.MergeDataToSearchCasesOperation;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.mapper.CaseDetailsMapper;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.CaseSearchRequestSecurity;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Service
@Qualifier(ElasticsearchCaseSearchOperation.QUALIFIER)
@Slf4j
public class ElasticsearchCaseSearchOperation implements CaseSearchOperation {

    public static final String QUALIFIER = "ElasticsearchCaseSearchOperation";
    static final String MULTI_SEARCH_ERROR_MSG_ROOT_CAUSE = "root_cause";

    private final JestClient jestClient;
    private final ObjectMapper objectMapper;
    private final CaseDetailsMapper caseDetailsMapper;
    private final ApplicationParams applicationParams;
    private final CaseSearchRequestSecurity caseSearchRequestSecurity;
    private final MergeDataToSearchCasesOperation mergeDataToSearchCasesOperation;

    @Autowired
    public ElasticsearchCaseSearchOperation(JestClient jestClient,
                                            @Qualifier("DefaultObjectMapper") ObjectMapper objectMapper,
                                            CaseDetailsMapper caseDetailsMapper,
                                            ApplicationParams applicationParams,
                                            CaseSearchRequestSecurity caseSearchRequestSecurity,
                                            MergeDataToSearchCasesOperation mergeDataToSearchCasesOperation) {
        this.jestClient = jestClient;
        this.objectMapper = objectMapper;
        this.caseDetailsMapper = caseDetailsMapper;
        this.applicationParams = applicationParams;
        this.caseSearchRequestSecurity = caseSearchRequestSecurity;
        this.mergeDataToSearchCasesOperation = mergeDataToSearchCasesOperation;
    }

    @Override
    public CaseSearchResult executeExternal(CrossCaseTypeSearchRequest request) {
        MultiSearchResult result = search(request);
        if (result.isSucceeded()) {
            return toCaseDetailsSearchResult(result);
        } else {
            throw new BadSearchRequest(result.getErrorMessage());
        }
    }

    @Override
    public UICaseSearchResult executeInternal(CaseSearchResult caseSearchResult, List<String> caseTypeIds, UseCase useCase) {
        return mergeDataToSearchCasesOperation.execute(caseTypeIds, caseSearchResult, useCase);
    }

    private MultiSearchResult search(CrossCaseTypeSearchRequest request) {
        MultiSearch multiSearch = secureAndTransformSearchRequest(request);
        try {
            return jestClient.execute(multiSearch);
        } catch (IOException e) {
            throw new ServiceException("Exception executing search : " + e.getMessage(), e);
        }
    }

    private MultiSearch secureAndTransformSearchRequest(CrossCaseTypeSearchRequest request) {
        Collection<Search> securedSearches = request.getCaseTypeIds()
            .stream()
            .map(caseTypeId -> createSecuredSearch(caseTypeId, request.getSearchRequestJsonNode()))
            .collect(toList());

        return new MultiSearch.Builder(securedSearches).build();
    }

    private Search createSecuredSearch(String caseTypeId, JsonNode searchRequestJsonNode) {
        CaseSearchRequest securedSearchRequest = caseSearchRequestSecurity.createSecuredSearchRequest(new CaseSearchRequest(caseTypeId, searchRequestJsonNode));
        return new Search.Builder(securedSearchRequest.toJsonString())
            .addIndex(getCaseIndexName(caseTypeId))
            .addType(getCaseIndexType())
            .build();
    }

    private CaseSearchResult toCaseDetailsSearchResult(MultiSearchResult multiSearchResult) {
        List<CaseDetails> caseDetails = new ArrayList<>();
        long totalHits = 0L;

        for (MultiSearchResult.MultiSearchResponse response : multiSearchResult.getResponses()) {
            if (response.isError) {
                JsonObject errorObject = response.error.getAsJsonObject();
                String errMsg = errorObject.toString();
                log.error("Elasticsearch query execution error: {}", errMsg);
                if (errorObject.has(MULTI_SEARCH_ERROR_MSG_ROOT_CAUSE)) {
                    errMsg = errorObject.get(MULTI_SEARCH_ERROR_MSG_ROOT_CAUSE).toString();
                }
                throw new BadSearchRequest(errMsg);
            }
            if (response.searchResult != null) {
                caseDetails.addAll(searchResultToCaseList(response.searchResult));
                totalHits += response.searchResult.getTotal();
            }
        }

        return new CaseSearchResult(totalHits, caseDetails);
    }

    private List<CaseDetails> searchResultToCaseList(SearchResult searchResult) {
        List<String> casesAsString = searchResult.getSourceAsStringList();
        List<ElasticSearchCaseDetailsDTO> dtos = toElasticSearchCasesDTO(casesAsString);
        return caseDetailsMapper.dtosToCaseDetailsList(dtos);
    }

    private List<ElasticSearchCaseDetailsDTO> toElasticSearchCasesDTO(List<String> cases) {
        return cases
            .stream()
            .map(Unchecked.function(caseDetail -> objectMapper.readValue(caseDetail, ElasticSearchCaseDetailsDTO.class)))
            .collect(toList());
    }

    private String getCaseIndexName(String caseTypeId) {
        return format(applicationParams.getCasesIndexNameFormat(), caseTypeId.toLowerCase());
    }

    private String getCaseIndexType() {
        return applicationParams.getCasesIndexType();
    }

}
