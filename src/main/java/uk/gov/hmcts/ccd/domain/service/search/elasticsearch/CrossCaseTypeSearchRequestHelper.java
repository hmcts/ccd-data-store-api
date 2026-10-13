package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;

import java.util.List;

@Slf4j
@Component
public class CrossCaseTypeSearchRequestHelper {

    private final ApplicationParams applicationParams;

    public CrossCaseTypeSearchRequestHelper(ApplicationParams applicationParams) {
        this.applicationParams = applicationParams;
    }

    public CrossCaseTypeSearchRequest buildCrossCaseTypeSearchRequest(List<String> caseTypeIds,
                                                                      ElasticsearchRequest elasticsearchRequest,
                                                                      boolean global) {
        CrossCaseTypeSearchRequest.Builder builder = new CrossCaseTypeSearchRequest.Builder()
            .withCaseTypes(caseTypeIds)
            .withSearchRequest(elasticsearchRequest);

        if (global) {
            SearchIndex searchIndex = new SearchIndex(
                applicationParams.getGlobalSearchIndexName(),
                applicationParams.getGlobalSearchIndexType()
            );
            builder.withSearchIndex(searchIndex);
            log.debug("pointing to global search index...");
        }

        return builder.build();
    }
}
