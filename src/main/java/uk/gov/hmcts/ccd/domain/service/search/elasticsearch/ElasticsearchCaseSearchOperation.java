package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.core.MultiSearch;
import io.searchbox.core.MultiSearchResult;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.mapper.CaseDetailsMapper;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.security.CaseSearchRequestSecurity;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

@Service
@Qualifier(ElasticsearchCaseSearchOperation.QUALIFIER)
public class ElasticsearchCaseSearchOperation implements CaseSearchOperation {

    public static final String QUALIFIER = "ElasticsearchCaseSearchOperation";

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
            return toCaseDetailsSearchResult(result);
        } else {
            throw new BadSearchRequest(result.getErrorMessage());
        }
    }

    private MultiSearchResult search(CrossCaseTypeSearchRequest request) {
        MultiSearch multiSearchAction = secureAndTransformSearchRequest(request);
        try {
            return jestClient.execute(multiSearchAction);
        } catch (IOException e) {
            throw new ServiceException("Exception executing Elasticsearch : " + e.getMessage(), e);
        }
    }

    private MultiSearch secureAndTransformSearchRequest(CrossCaseTypeSearchRequest request) {
        Collection<Search> securedSearchActions = request.getCaseSearchRequests()
            .stream()
            .map(this::createSecuredSearchAction)
            .collect(Collectors.toList());

        return new MultiSearch.Builder(securedSearchActions).build();
    }

    private Search createSecuredSearchAction(CaseSearchRequest caseSearchRequest) {
        CaseSearchRequest securedSearchRequest = caseSearchRequestSecurity.createSecuredSearchRequest(caseSearchRequest);
        return new Search.Builder(securedSearchRequest.toJsonString())
            .addIndex(getCaseIndexName(caseSearchRequest.getCaseTypeId()))
            .addType(getCaseIndexType())
            .build();
    }

    private CaseSearchResult toCaseDetailsSearchResult(MultiSearchResult multiSearchResult) {
        long totalHits = 0;
        List<CaseDetails> caseDetails = new ArrayList<>();

        for (MultiSearchResult.MultiSearchResponse response : multiSearchResult.getResponses()) {
            SearchResult searchResult = response.searchResult;
            caseDetails.addAll(searchResultToCaseList(searchResult));

            totalHits += searchResult.getTotal();
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
