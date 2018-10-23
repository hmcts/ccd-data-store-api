package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.io.IOException;
import java.util.List;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
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
    public CaseSearchResult execute(CaseSearchRequest caseSearchRequest) {
        SearchResult result = search(caseSearchRequest);
        if (result.isSucceeded()) {
            return toCaseDetailsSearchResult(result);
        } else {
            throw new BadSearchRequest(result.getErrorMessage());
        }
    }

    private SearchResult search(CaseSearchRequest caseSearchRequest) {
        Search searchRequest = secureAndTransformSearchRequest(caseSearchRequest);
        try {
            return jestClient.execute(searchRequest);
        } catch (IOException e) {
            throw new ServiceException("Exception executing Elasticsearch : " + e.getMessage(), e);
        }
    }

    private Search secureAndTransformSearchRequest(CaseSearchRequest caseSearchRequest) {
        CaseSearchRequest securedSearchRequest = caseSearchRequestSecurity.createSecuredSearchRequest(caseSearchRequest);
        return new Search.Builder(securedSearchRequest.toJsonString())
            .addIndex(getCaseIndexName(caseSearchRequest.getCaseTypeId()))
            .addType(getCaseIndexType())
            .build();
    }

    private CaseSearchResult toCaseDetailsSearchResult(SearchResult result) {
        List<String> casesAsString = result.getSourceAsStringList();
        List<ElasticSearchCaseDetailsDTO> dtos = toElasticSearchCasesDTO(casesAsString);
        List<CaseDetails> caseDetails = caseDetailsMapper.dtosToCaseDetailsList(dtos);
        return new CaseSearchResult(result.getTotal(), caseDetails);
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
