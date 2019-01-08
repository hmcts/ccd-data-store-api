package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest.QUERY;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

/**
 * Sample ES json search request.
 * {
 *   "_source": ["alias.searchField1", "alias.searchField2"],
 *   "query": {
 *     "bool": {
 *       "filter": {
 *         "match": { "state": "AwaitingPayment"}
 *       }
 *     }
 *   },
 *  "sort": {
 *     "id": { "order":"asc" }
 *   }
 * }
 */
public class CrossCaseTypeSearchRequest {

    private static final String SOURCE = "_source";
    private static final String CROSS_CASE_TYPE_SEARCH_ALIAS_FIELD_PREFIX = "alias.";

    private final List<String> caseTypeIds = new ArrayList<>();
    private final JsonNode searchRequestJsonNode;
    private final boolean multiCaseTypeSearch;
    private final List<String> sourceFilterAliasFields = new ArrayList<>();

    private CrossCaseTypeSearchRequest(List<String> caseTypeIds, JsonNode searchRequestJsonNode, boolean multiCaseTypeSearch,
                                       List<String> sourceFilterAliasFields) {
        this.caseTypeIds.addAll(caseTypeIds);
        this.searchRequestJsonNode = searchRequestJsonNode;
        this.multiCaseTypeSearch = multiCaseTypeSearch;
        this.sourceFilterAliasFields.addAll(sourceFilterAliasFields);
        validateJsonSearchRequest();
    }

    public List<String> getCaseTypeIds() {
        return Collections.unmodifiableList(caseTypeIds);
    }

    public JsonNode getSearchRequestJsonNode() {
        return searchRequestJsonNode;
    }

    public boolean isMultiCaseTypeSearch() {
        return multiCaseTypeSearch;
    }

    private void validateJsonSearchRequest() {
        if (!searchRequestJsonNode.has(QUERY)) {
            throw new BadSearchRequest("missing required field 'query'");
        }
    }

    public List<String> getSourceFilterAliasFields() {
        return sourceFilterAliasFields;
    }


    public static class Builder {

        private final List<String> caseTypeIds = new ArrayList<>();
        private JsonNode searchRequestJsonNode;
        private boolean multiCaseTypeSearch;
        private final List<String> sourceFilterAliasFields = new ArrayList<>();

        public Builder withCaseTypes(List<String> caseTypeIds) {
            if (caseTypeIds != null) {
                this.caseTypeIds.addAll(caseTypeIds);
            }
            return this;
        }

        public Builder withSearchRequest(JsonNode searchRequestJsonNode) {
            this.searchRequestJsonNode = searchRequestJsonNode;
            return this;
        }

        public Builder withMultiCaseTypeSearch(boolean multiCaseTypeSearch) {
            this.multiCaseTypeSearch = multiCaseTypeSearch;
            if (multiCaseTypeSearch && searchRequestJsonNode.has(SOURCE)) {
                setSourceFilterAliasFields(searchRequestJsonNode.get(SOURCE));
            }
            return this;
        }

        public Builder withSourceFilterAliasFields(List<String> sourceFilterAliasFields) {
            if (sourceFilterAliasFields != null) {
                this.sourceFilterAliasFields.addAll(sourceFilterAliasFields);
            }
            return this;
        }

        private void setSourceFilterAliasFields(JsonNode multiCaseTypeSearchSourceNode) {
            if (multiCaseTypeSearchSourceNode != null && multiCaseTypeSearchSourceNode.isArray()) {
                // Alias fields are expected in source filter for multi-case types searches. If no aliases are defined in source filter, only meta data will
                // be returned with no data fields. If aliases are defined in the source filter, alias fields with data and metadata will be returned.
                sourceFilterAliasFields.addAll(StreamSupport.stream(multiCaseTypeSearchSourceNode.spliterator(), false)
                                                   .map(JsonNode::asText)
                                                   .filter(nodeText -> nodeText.startsWith(CROSS_CASE_TYPE_SEARCH_ALIAS_FIELD_PREFIX))
                                                   .map(nodeText -> nodeText.replaceFirst(CROSS_CASE_TYPE_SEARCH_ALIAS_FIELD_PREFIX, ""))
                                                   .collect(Collectors.toList()));
                // remove source filter defined for multi case type search
                removeSourceFilter();
            }
        }

        private void removeSourceFilter() {
            ((ObjectNode) this.searchRequestJsonNode).remove(SOURCE);
        }

        public CrossCaseTypeSearchRequest build() {
            return new CrossCaseTypeSearchRequest(caseTypeIds, searchRequestJsonNode, multiCaseTypeSearch, sourceFilterAliasFields);
        }

    }

}
