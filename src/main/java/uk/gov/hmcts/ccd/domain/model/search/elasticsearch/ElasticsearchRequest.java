package uk.gov.hmcts.ccd.domain.model.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.NonNull;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Data
public class ElasticsearchRequest {

    public static final String SORT = "sort";
    public static final String SOURCE = "_source";
    public static final String QUERY = "query";
    public static final String CASE_DATA_PREFIX = "data.";
    public static final String COLLECTION_VALUE_SUFFIX = ".value";
    public static final String SOURCE_WILDCARD = "*";

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
        // If a source is empty or only has a wildcard element, then equivalent to no provided source
        return searchRequest.has(SOURCE) && !getSource().isEmpty()
               && !(getSource().size() == 1 && getSource().get(0).asText().equals(SOURCE_WILDCARD));
    }

    public JsonNode getSource() {
        return searchRequest.get(SOURCE);
    }

    public List<String> getRequestedFields() {
        if (hasSource() && getSource().isArray()) {
            return StreamSupport.stream(getSource().spliterator(), false)
                .map(JsonNode::asText)
                .map(this::getFieldId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        return Collections.emptyList();
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

    private String getFieldId(String fieldSourceName) {
        if (fieldSourceName.startsWith(CASE_DATA_PREFIX)) {
            return fieldSourceName.replace(CASE_DATA_PREFIX, "");
        } else {
            try {
                return MetaData.CaseField.valueOfColumnName(fieldSourceName).getReference();
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
