package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static uk.gov.hmcts.ccd.domain.service.search.elasticsearch.CaseSearchRequest.QUERY;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.definition.SearchAliasField;
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
    private static final String SEARCH_ALIAS_FIELD_PREFIX = "alias.";

    private final List<String> caseTypeIds = new ArrayList<>();
    private final JsonNode searchRequestJsonNode;
    private final boolean multiCaseTypeSearch;
    private final List<String> aliasFields = new ArrayList<>();

    private CrossCaseTypeSearchRequest(List<String> caseTypeIds, JsonNode searchRequestJsonNode, boolean multiCaseTypeSearch,
                                       List<String> aliasFields) {
        this.caseTypeIds.addAll(caseTypeIds);
        this.searchRequestJsonNode = searchRequestJsonNode;
        this.multiCaseTypeSearch = multiCaseTypeSearch;
        this.aliasFields.addAll(aliasFields);
        addMetadataSourceFields();
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

    public List<String> getAliasFields() {
        return aliasFields;
    }

    public boolean hasAliasField(SearchAliasField searchAliasField) {
        return aliasFields.stream().anyMatch(aliasField -> aliasField.equalsIgnoreCase(searchAliasField.getId()));
    }

    private void addMetadataSourceFields() {
        JsonNode sourceNode = searchRequestJsonNode.get(SOURCE);
        if (sourceNode != null && sourceNode.isArray()) {
            Arrays.stream(MetaData.CaseField.values())
                .forEach(field -> ((ArrayNode)sourceNode).add(new TextNode(field.getDbColumnName())));
        }
    }

    public static class Builder {

        private final List<String> caseTypeIds = new ArrayList<>();
        private JsonNode searchRequestJsonNode;
        private boolean multiCaseTypeSearch;
        private final List<String> sourceFilterAliasFields = new ArrayList<>();

        public Builder withCaseTypes(List<String> caseTypeIds) {
            if (caseTypeIds != null) {
                this.caseTypeIds.addAll(caseTypeIds);
                multiCaseTypeSearch = this.caseTypeIds.size() > 1;
            }
            return this;
        }

        public Builder withSearchRequest(JsonNode searchRequestJsonNode) {
            this.searchRequestJsonNode = searchRequestJsonNode;
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
            if (multiCaseTypeSearch && searchRequestJsonNode != null) {
                JsonNode multiCaseTypeSearchSourceNode = searchRequestJsonNode.get(SOURCE);
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
            ((ObjectNode) this.searchRequestJsonNode).remove(SOURCE);
        }

        public CrossCaseTypeSearchRequest build() {
            setSourceFilterAliasFields();
            return new CrossCaseTypeSearchRequest(caseTypeIds, searchRequestJsonNode, multiCaseTypeSearch, sourceFilterAliasFields);
        }

    }

}
