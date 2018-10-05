package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.jooq.lambda.Unchecked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseDetailsSearchResult;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.ElasticSearchCaseDetailsDTO;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.mapper.CaseDetailsMapper;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

import java.io.IOException;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Service
public class ElasticSearchCaseDetailsSearchOperation implements CaseDetailsSearchOperation {

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
        if (result.isSucceeded()) {
            return toCaseDetailsSearchResult(result);
        } else {
            throw new BadSearchRequest(result.getErrorMessage());
        }
    }

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
