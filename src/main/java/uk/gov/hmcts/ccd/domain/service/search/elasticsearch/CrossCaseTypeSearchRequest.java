package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest.QUERY_NAME;

import com.fasterxml.jackson.databind.JsonNode;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

/**
 * Sample ES json search request.
 * {
 *   "_source": ["aliasSearchField1", "aliasSearchField2"],
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
public class CrossCaseTypeSearchRequest {

    private final List<String> caseTypeIds = new ArrayList<>();
    private final JsonNode searchRequestJsonNode;

    public CrossCaseTypeSearchRequest(List<String> caseTypeIds, JsonNode searchRequestJsonNode) {
        this.caseTypeIds.addAll(caseTypeIds);
        this.searchRequestJsonNode = searchRequestJsonNode;
        validateJsonSearchRequest();
    }

    public List<String> getCaseTypeIds() {
        return Collections.unmodifiableList(caseTypeIds);
    }

    public JsonNode getSearchRequestJsonNode() {
        return searchRequestJsonNode;
    }

    public boolean isMultiCaseTypeSearch() {
        return caseTypeIds.size() > 1;
    }

    private void validateJsonSearchRequest() {
        if (!searchRequestJsonNode.has(QUERY_NAME)) {
            throw new BadSearchRequest("missing required field 'query'");
        }
    }
}
