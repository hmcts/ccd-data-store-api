package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.util.List;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ElasticSearchOperation implements SearchOperation {

    @Autowired
    JestClient jestClient;

    @Override
    public List<CaseDetailsElastic> execute(String caseTypeId, String query) {
        Search search = new Search.Builder(query)
                // multiple index or types can be added.
                .addIndex(caseTypeId)
                .addType("case")
                .build();

        try {
            SearchResult result = jestClient.execute(search);
            List<SearchResult.Hit<CaseDetailsElastic, Void>> hits = result.getHits(CaseDetailsElastic.class);
            return hits.stream().map(hit -> hit.source).collect(toList());

        } catch (IOException e) {

            e.printStackTrace();
        }
        return null;
    }
}
