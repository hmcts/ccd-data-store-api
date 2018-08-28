package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.CaseDetailsMapper;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

@Service
public class ElasticSearchCaseDetailsSearchOperation implements CaseDetailsSearchOperation {

    private static final String INDEX_TYPE = "case";

    @Autowired
    private JestClient jestClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CaseDetailsMapper caseDetailsMapper;

    @Override
    public List<CaseDetails> execute(String caseTypeId, String query) throws IOException {

        Search search = createSearchRequest(caseTypeId, query);
        SearchResult result = jestClient.execute(search);
        if (result.isSucceeded()) {
            return toCaseDetails(result);
        } else {
            throw new BadSearchRequest(result.getErrorMessage());
        }
    }

    private Search createSearchRequest(String caseTypeId, String query) {
        return new Search.Builder(query)
                    .addIndex(caseTypeId)
                    .addType(INDEX_TYPE)
                    .build();
    }

    private List<CaseDetails> toCaseDetails(SearchResult result) {
        List<String> casesAsString = result.getSourceAsStringList();
        List<ElasticSearchCaseDetailsDTO> dtos = toElasticSearchCasesDTO(casesAsString);
        return caseDetailsMapper.dtosToCaseDetailsList(dtos);
    }

    private List<ElasticSearchCaseDetailsDTO> toElasticSearchCasesDTO(List<String> cases) {
        return cases.stream().map(Unchecked.function(caseDetail ->
            objectMapper.readValue(caseDetail, ElasticSearchCaseDetailsDTO.class)
        )).collect(toList());
    }
}
