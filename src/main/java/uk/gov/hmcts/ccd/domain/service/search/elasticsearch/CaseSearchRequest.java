package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

/**
 * Sample ES json search request.
 * {
 *   "query": {
 *     "bool": {
 *       "filter": {
 *         "match": { "state": "AwaitingPayment"}
 *       }
 *     }
 *   },
 *  "sort": {
 *   "id": { "order":"asc" }
 *   }
 * }
 */
@Slf4j
public class CaseSearchRequest {

    private final String caseTypeId;
    ElasticsearchRequest elasticsearchRequest;

    public CaseSearchRequest(String caseTypeId, ElasticsearchRequest elasticsearchRequest) {
        this.caseTypeId = caseTypeId;
        this.elasticsearchRequest = elasticsearchRequest;
        validateJsonSearchRequest();
    }

    private void validateJsonSearchRequest() {
        if (!elasticsearchRequest.hasQuery()) {
            throw new BadSearchRequest("missing required field 'query'");
        }
    }

    public String getCaseTypeId() {
        return caseTypeId;
    }

    public String getQueryValue() {
        return elasticsearchRequest.getQuery().toString();
    }

    public String toJsonString() {
        String jsonString = elasticsearchRequest.toJson();
        log.debug("json search request: {}", jsonString);
        return jsonString;
    }
}
