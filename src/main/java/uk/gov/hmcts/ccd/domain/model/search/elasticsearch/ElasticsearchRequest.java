package uk.gov.hmcts.ccd.domain.model.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.NonNull;

@Data
public class ElasticsearchRequest {

    public static final String SORT = "sort";
    public static final String SOURCE = "_source";
    public static final String QUERY = "query";

    @NonNull
    private JsonNode searchRequest;

    public boolean isSorted() {
        return searchRequest.has(SORT);
    }

    public JsonNode getSort() {
        return searchRequest.get(SORT);
    }

    public void setSort(ArrayNode sortNode) {
        ((ObjectNode) searchRequest).set(SORT, sortNode);
    }

    public boolean hasSource() {
        return searchRequest.has(SOURCE);
    }

    public JsonNode getSource() {
        return searchRequest.get(SOURCE);
    }

    public boolean hasQuery() {
        return searchRequest.has(QUERY);
    }

    public JsonNode getQuery() {
        return searchRequest.get(QUERY);
    }

    public String toJson() {
        return searchRequest.toString();
    }
}
