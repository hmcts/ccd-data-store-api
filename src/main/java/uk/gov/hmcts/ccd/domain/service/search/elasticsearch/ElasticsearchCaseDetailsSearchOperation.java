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
import uk.gov.hmcts.ccd.domain.service.search.filter.CaseSearchRequestFactory;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.ServiceException;

@Service
@Qualifier(ElasticsearchCaseDetailsSearchOperation.QUALIFIER)
public class ElasticsearchCaseDetailsSearchOperation implements CaseDetailsSearchOperation {

<<<<<<< HEAD:src/main/java/uk/gov/hmcts/ccd/domain/service/search/elasticsearch/ElasticSearchCaseDetailsSearchOperation.java
    @Autowired
    private ApplicationParams applicationParams;

    @Autowired
    private JestClient jestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CaseDetailsMapper caseDetailsMapper;

    @Override
    public CaseDetailsSearchResult execute(List<String> caseTypeIds, String query) throws IOException {

        Search search = createSearchRequest(caseTypeIds, query);
        SearchResult result = jestClient.execute(search);
=======
    public static final String QUALIFIER = "ElasticsearchCaseDetailsSearchOperation";

    private final JestClient jestClient;
    private final ObjectMapper objectMapper;
    private final CaseDetailsMapper caseDetailsMapper;
    private final CaseSearchRequestFactory<Search> caseSearchRequestFactory;

    @Autowired
    public ElasticsearchCaseDetailsSearchOperation(JestClient jestClient,
                                                   ObjectMapper objectMapper,
                                                   CaseDetailsMapper caseDetailsMapper,
                                                   CaseSearchRequestFactory<Search> caseSearchRequestFactory) {
        this.jestClient = jestClient;
        this.objectMapper = objectMapper;
        this.caseDetailsMapper = caseDetailsMapper;
        this.caseSearchRequestFactory = caseSearchRequestFactory;
    }

    @Override
    public CaseDetailsSearchResult execute(String caseTypeId, String query) {
        SearchResult result = search(caseTypeId, query);
>>>>>>> c3eb9b8... RDM-2811 - add security to search endpoint (Elasticsearch):src/main/java/uk/gov/hmcts/ccd/domain/service/search/elasticsearch/ElasticsearchCaseDetailsSearchOperation.java
        if (result.isSucceeded()) {
            return toCaseDetailsSearchResult(result);
        } else {
            throw new BadSearchRequest(result.getErrorMessage());
        }
    }

<<<<<<< HEAD:src/main/java/uk/gov/hmcts/ccd/domain/service/search/elasticsearch/ElasticSearchCaseDetailsSearchOperation.java
    private Search createSearchRequest(List<String> caseTypesId, String query) {
        return new Search.Builder(query)
                    .addIndices(indices(caseTypesId))
                    .addType(applicationParams.getCasesIndexType())
                    .build();
    }

    private List<String> indices(List<String> caseTypesId) {
        return caseTypesId.stream().map(caseTypeId ->
                String.format(applicationParams.getCasesIndexNameFormat(), caseTypeId))
                .collect(toList());
=======
    private SearchResult search(String caseTypeId, String query) {
        Search searchRequest = caseSearchRequestFactory.create(caseTypeId, query);
        try {
            return jestClient.execute(searchRequest);
        } catch (IOException e) {
            throw new ServiceException("Exception executing Elasticsearch : " + e.getMessage(), e);
        }
>>>>>>> c3eb9b8... RDM-2811 - add security to search endpoint (Elasticsearch):src/main/java/uk/gov/hmcts/ccd/domain/service/search/elasticsearch/ElasticsearchCaseDetailsSearchOperation.java
    }

    private CaseDetailsSearchResult toCaseDetailsSearchResult(SearchResult result) {
        List<String> casesAsString = result.getSourceAsStringList();
        List<ElasticSearchCaseDetailsDTO> dtos = toElasticSearchCasesDTO(casesAsString);
        List<CaseDetails> caseDetails = caseDetailsMapper.dtosToCaseDetailsList(dtos);
        return new CaseDetailsSearchResult(caseDetails, result.getTotal());
    }

    private List<ElasticSearchCaseDetailsDTO> toElasticSearchCasesDTO(List<String> cases) {
        return cases.stream().map(Unchecked.function(caseDetail ->
            objectMapper.readValue(caseDetail, ElasticSearchCaseDetailsDTO.class)
        )).collect(toList());
    }
}
