package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseDetailsSearchResult;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.mapper.CaseDetailsMapper;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

@Service
@Qualifier(ElasticsearchCaseDetailsSearchOperation.QUALIFIER)
public class ElasticsearchCaseDetailsSearchOperation implements CaseDetailsSearchOperation {

    public static final String QUALIFIER = "ElasticsearchCaseDetailsSearchOperation";

    private final JestClient jestClient;
    private final ObjectMapper objectMapper;
    private final CaseDetailsMapper caseDetailsMapper;
    private final CaseSearchRequestFactory<Search> caseSearchRequestFactory;

    @Autowired
    public ElasticsearchCaseDetailsSearchOperation(JestClient jestClient,
                                                   @Qualifier("caseDetailsObjectMapper") ObjectMapper objectMapper,
                                                   CaseDetailsMapper caseDetailsMapper,
                                                   CaseSearchRequestFactory<Search> caseSearchRequestFactory) {
        this.jestClient = jestClient;
        this.objectMapper = objectMapper;
        this.caseDetailsMapper = caseDetailsMapper;
        this.caseSearchRequestFactory = caseSearchRequestFactory;
    }

    @Override
    public CaseDetailsSearchResult execute(String caseTypeId, String jsonQuery) {
        SearchResult result = search(caseTypeId, jsonQuery);
        if (result.isSucceeded()) {
            return toCaseDetailsSearchResult(result);
        } else {
            throw new BadSearchRequest(result.getErrorMessage());
        }
    }

    private SearchResult search(String caseTypeId, String jsonQuery) {
        Search searchRequest = caseSearchRequestFactory.create(caseTypeId, jsonQuery);
        try {
            return jestClient.execute(searchRequest);
        } catch (IOException e) {
            throw new ServiceException("Exception executing Elasticsearch : " + e.getMessage(), e);
        }
    }

    private CaseDetailsSearchResult toCaseDetailsSearchResult(SearchResult result) {
        List<String> casesAsString = result.getSourceAsStringList();
        List<ElasticSearchCaseDetailsDTO> dtos = toElasticSearchCasesDTO(casesAsString);
        List<CaseDetails> caseDetails = caseDetailsMapper.dtosToCaseDetailsList(dtos);
        return new CaseDetailsSearchResult(caseDetails, result.getTotal());
    }

    private List<ElasticSearchCaseDetailsDTO> toElasticSearchCasesDTO(List<String> cases) {
        return cases
            .stream()
            .map(Unchecked.function(caseDetail -> objectMapper.readValue(caseDetail, ElasticSearchCaseDetailsDTO.class)))
            .collect(toList());
    }
}
