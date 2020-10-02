package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import uk.gov.hmcts.ccd.domain.model.definition.SearchAliasField;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.SOURCE;

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

    private static final String SEARCH_ALIAS_FIELD_PREFIX = "alias.";

    private final List<String> caseTypeIds = new ArrayList<>();
    private final ElasticsearchRequest elasticsearchRequest;
    private final boolean multiCaseTypeSearch;
    private final List<String> aliasFields = new ArrayList<>();

    private CrossCaseTypeSearchRequest(List<String> caseTypeIds,
                                       ElasticsearchRequest elasticsearchRequest,
                                       boolean multiCaseTypeSearch,
                                       List<String> aliasFields) {
        this.caseTypeIds.addAll(caseTypeIds);
        this.elasticsearchRequest = elasticsearchRequest;
        this.multiCaseTypeSearch = multiCaseTypeSearch;
        this.aliasFields.addAll(aliasFields);
        validateJsonSearchRequest();
    }

    public List<String> getCaseTypeIds() {
        return Collections.unmodifiableList(caseTypeIds);
    }

    public ElasticsearchRequest getElasticSearchRequest() {
        return elasticsearchRequest;
    }

    public JsonNode getSearchRequestJsonNode() {
        return elasticsearchRequest.getNativeSearchRequest();
    }

    public boolean isMultiCaseTypeSearch() {
        return multiCaseTypeSearch;
    }

    private void validateJsonSearchRequest() {
        if (!getElasticSearchRequest().hasQuery()) {
            throw new BadSearchRequest("missing required field 'query'");
        }
    }

    public List<String> getAliasFields() {
        return aliasFields;
    }

    public boolean hasAliasField(SearchAliasField searchAliasField) {
        return aliasFields.stream().anyMatch(aliasField -> aliasField.equalsIgnoreCase(searchAliasField.getId()));
    }

    public static class Builder {

        private final List<String> caseTypeIds = new ArrayList<>();
        private ElasticsearchRequest elasticsearchRequest;
        private boolean multiCaseTypeSearch;
        private final List<String> sourceFilterAliasFields = new ArrayList<>();

        public Builder withCaseTypes(List<String> caseTypeIds) {
            if (caseTypeIds != null) {
                this.caseTypeIds.addAll(caseTypeIds);
                multiCaseTypeSearch = this.caseTypeIds.size() > 1;
            }
            return this;
        }

        public Builder withSearchRequest(ElasticsearchRequest elasticsearchRequest) {
            this.elasticsearchRequest = elasticsearchRequest;
            return this;
        }

        public Builder withMultiCaseTypeSearch(boolean multiCaseTypeSearch) {
            this.multiCaseTypeSearch = multiCaseTypeSearch;
            return this;
        }

        public Builder withSourceFilterAliasFields(List<String> sourceFilterAliasFields) {
            if (sourceFilterAliasFields != null) {
                this.sourceFilterAliasFields.addAll(sourceFilterAliasFields);
            }
            return this;
        }

        private void setSourceFilterAliasFields() {
            if (multiCaseTypeSearch && elasticsearchRequest.getNativeSearchRequest() != null) {
                JsonNode multiCaseTypeSearchSourceNode = elasticsearchRequest.getSource();
                if (multiCaseTypeSearchSourceNode != null && multiCaseTypeSearchSourceNode.isArray()) {
                    sourceFilterAliasFields.addAll(sourceFilterToAliasFields(multiCaseTypeSearchSourceNode));
                    removeSourceFilter();
                }
            }
        }

        private List<String> sourceFilterToAliasFields(JsonNode multiCaseTypeSearchSourceNode) {
            return StreamSupport.stream(multiCaseTypeSearchSourceNode.spliterator(), false)
                .map(JsonNode::asText)
                .filter(nodeText -> nodeText.startsWith(SEARCH_ALIAS_FIELD_PREFIX))
                .map(nodeText -> nodeText.replaceFirst(SEARCH_ALIAS_FIELD_PREFIX, ""))
                .collect(Collectors.toList());
        }

        private void removeSourceFilter() {
            ((ObjectNode) this.elasticsearchRequest.getNativeSearchRequest()).remove(SOURCE);
        }

        public CrossCaseTypeSearchRequest build() {
            setSourceFilterAliasFields();
            return new CrossCaseTypeSearchRequest(caseTypeIds, elasticsearchRequest, multiCaseTypeSearch,
                                                  sourceFilterAliasFields);
        }

    }

}
