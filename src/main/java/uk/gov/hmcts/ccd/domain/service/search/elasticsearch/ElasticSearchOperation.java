package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.CaseDetailsElasticDTO;
import uk.gov.hmcts.ccd.domain.service.search.elasticsearch.dto.CaseDetailsMapper;

@Service
public class ElasticSearchOperation implements SearchOperation {

    @Autowired
    JestClient jestClient;

    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CaseDetailsMapper caseDetailsMapper;

    @Override
    public List<CaseDetails> execute(String caseTypeId, String query) {
        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(caseTypeId)
                .addType("case")
                .build();

        try {
            SearchResult result = jestClient.execute(search);
            List<String> resultStrings = result.getSourceAsStringList();

            List<CaseDetailsElasticDTO> dtos = resultStrings.stream().map(source ->
                    {
                        try {
                            return objectMapper.readValue(source, CaseDetailsElasticDTO.class);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
            ).collect(toList());

            return caseDetailsMapper.dtosToCaseDetailsList(dtos);
        } catch (IOException e) {

            e.printStackTrace();
        }
        return null;
//        SearchRequest searchRequest = new SearchRequest(caseTypeId);
//        searchRequest.types("case");
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
//        searchSourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
//        searchSourceBuilder.query(new WrapperQueryBuilder(query));
//        searchRequest.source(searchSourceBuilder);
//
//        try {
//            SearchResponse searchResponse = restHighLevelClient.search(searchRequest);
//
//            SearchHits hits = searchResponse.getHits();
//            for (SearchHit hit : hits) {
//                String sourceAsString = hit.getSourceAsString();
//
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
    }
}
