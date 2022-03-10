package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class SearchIndex {
    String indexName;
    String indexType;
}
