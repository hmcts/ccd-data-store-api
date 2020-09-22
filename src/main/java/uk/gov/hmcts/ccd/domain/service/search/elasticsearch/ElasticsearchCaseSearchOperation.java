package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

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
import uk.gov.hmcts.ccd.domain.model.search.CaseTypeResults;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.mapper.CaseDetailsMapper;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.CaseSearchRequestSecurity;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Service
@Qualifier(ElasticsearchCaseSearchOperation.QUALIFIER)
@Slf4j
public class ElasticsearchCaseSearchOperation implements CaseSearchOperation {

    public static final String QUALIFIER = "ElasticsearchCaseSearchOperation";
    static final String MULTI_SEARCH_ERROR_MSG_ROOT_CAUSE = "root_cause";
    private static final String HITS = "hits";

    private final JestClient jestClient;
    private final ObjectMapper objectMapper;
    private final CaseDetailsMapper caseDetailsMapper;
    private final ApplicationParams applicationParams;
    private final CaseSearchRequestSecurity caseSearchRequestSecurity;

    @Autowired
    public ElasticsearchCaseSearchOperation(JestClient jestClient,
                                            @Qualifier("DefaultObjectMapper") ObjectMapper objectMapper,
                                            CaseDetailsMapper caseDetailsMapper,
                                            ApplicationParams applicationParams,
                                            CaseSearchRequestSecurity caseSearchRequestSecurity) {
        this.jestClient = jestClient;
        this.objectMapper = objectMapper;
        this.caseDetailsMapper = caseDetailsMapper;
        this.applicationParams = applicationParams;
        this.caseSearchRequestSecurity = caseSearchRequestSecurity;
    }

    @Override
    public CaseSearchResult execute(CrossCaseTypeSearchRequest request) {
        MultiSearchResult result = search(request);
        if (result.isSucceeded()) {
            return toCaseDetailsSearchResult(result, request);
        } else {
            throw new BadSearchRequest(result.getErrorMessage());
        }
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
            .map(caseTypeId -> createSecuredSearch(caseTypeId, request))
            .collect(toList());

        return new MultiSearch.Builder(securedSearches).build();
    }

    private Search createSecuredSearch(String caseTypeId, CrossCaseTypeSearchRequest request) {
        CaseSearchRequest securedSearchRequest = caseSearchRequestSecurity.createSecuredSearchRequest(
            new CaseSearchRequest(caseTypeId, request.getElasticSearchRequest()));
        return new Search.Builder(securedSearchRequest.toJsonString())
            .addIndex(getCaseIndexName(caseTypeId))
            .addType(getCaseIndexType())
            .build();
    }

    private CaseSearchResult toCaseDetailsSearchResult(MultiSearchResult multiSearchResult,
                                                       CrossCaseTypeSearchRequest crossCaseTypeSearchRequest) {
        final List<CaseDetails> caseDetails = new ArrayList<>();
        final List<CaseTypeResults> caseTypeResults = new ArrayList<>();
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
                buildCaseTypesResults(response, caseTypeResults, crossCaseTypeSearchRequest);
                caseDetails.addAll(searchResultToCaseList(response.searchResult));
                totalHits += response.searchResult.getTotal();
            }
        }

        return new CaseSearchResult(totalHits,caseDetails,caseTypeResults);
    }

    private void buildCaseTypesResults(
        MultiSearchResult.MultiSearchResponse response,
        List<CaseTypeResults> caseTypeResults,
        CrossCaseTypeSearchRequest crossCaseTypeSearchRequest) {
        if (hitsIsNotEmpty(response)) {
            String indexName = getIndexName(response);
            caseTypeResults.add(new CaseTypeResults(getCaseTypeIDFromIndex(indexName,
                crossCaseTypeSearchRequest.getCaseTypeIds()),
                response.searchResult.getTotal())
            );
        }
    }

    private String getIndexName(MultiSearchResult.MultiSearchResponse response) {
        String quotedIndexName =  response.searchResult.getJsonObject().getAsJsonObject(HITS).get(HITS)
            .getAsJsonArray().get(0).getAsJsonObject().get("_index").toString();
        String unquotedIndexName = quotedIndexName.replaceAll("\"", "");
        return unquotedIndexName;
    }

    private boolean hitsIsNotEmpty(MultiSearchResult.MultiSearchResponse response) {
        return response.searchResult.getJsonObject().getAsJsonObject(HITS).get(HITS).getAsJsonArray().size() != 0;
    }

    private String getCaseTypeIDFromIndex(final String index, List<String> caseTypeIds) {
        String caseTypeIdGroupRegex = applicationParams.getCasesIndexNameCaseTypeIdGroup();
        int caseTypeIdGroupPosition = applicationParams.getCasesIndexNameCaseTypeIdGroupPosition();
        Pattern pattern = Pattern.compile(caseTypeIdGroupRegex);
        Matcher m = pattern.matcher(index);
        if (m.matches() && m.groupCount() > 1) {
            return caseTypeIds.stream().filter(
                caseTypeId -> caseTypeId.equalsIgnoreCase(m.group(caseTypeIdGroupPosition))
            ).findFirst().orElseThrow(() -> {
                log.error("Cannot match any known case type id from index '{}' extracted case type id : {}",
                           index, m.group(caseTypeIdGroupPosition));
                throw new ServiceException("Cannot determine case type id from ES index name - unknown "
                    + "extracted case type id");
            });
        } else {
            log.error("Cannot determine case type id from index name: '{}'. No capturing group configured or capturing"
                + " group not matching: '{}'.",
                index, caseTypeIdGroupRegex);
            throw new ServiceException("Cannot determine case type id from ES index name - cannot extract"
                + " case type id");
        }
    }

    private List<CaseDetails> searchResultToCaseList(SearchResult searchResult) {
        List<String> casesAsString = searchResult.getSourceAsStringList();
        List<ElasticSearchCaseDetailsDTO> dtos = toElasticSearchCasesDTO(casesAsString);
        return caseDetailsMapper.dtosToCaseDetailsList(dtos);
    }

    private List<ElasticSearchCaseDetailsDTO> toElasticSearchCasesDTO(List<String> cases) {
        return cases
            .stream()
            .map(Unchecked.function(caseDetail
                -> objectMapper.readValue(caseDetail, ElasticSearchCaseDetailsDTO.class)))
            .collect(toList());
    }

    private String getCaseIndexName(String caseTypeId) {
        return format(applicationParams.getCasesIndexNameFormat(), caseTypeId.toLowerCase());
    }

    private String getCaseIndexType() {
        return applicationParams.getCasesIndexType();
    }

}
