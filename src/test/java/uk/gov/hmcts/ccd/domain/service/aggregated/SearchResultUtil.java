package uk.gov.hmcts.ccd.domain.service.aggregated;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResult;
import uk.gov.hmcts.ccd.domain.model.definition.SearchResultField;

import java.util.Map;

class SearchResultUtil {

    private SearchResultUtil() {
        // Hide Utility Class Constructor : Utility classes should not have a public or default constructor (squid:S1118)
    }

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);

    public static class SearchResultBuilder {
        private final SearchResult searchResult;

        private SearchResultBuilder() {
            this.searchResult = new SearchResult();
        }

        @SuppressWarnings("checkstyle:MethodName") // method naming predates checkstyle implementation in module
        static SearchResultUtil.SearchResultBuilder searchResult() {
            return new SearchResultBuilder();
        }

        public SearchResultUtil.SearchResultBuilder withSearchResultFields(SearchResultField... searchResultFields) {
            searchResult.setFields(searchResultFields);
            return this;
        }

        public SearchResult build() {
            return searchResult;
        }
    }

    static SearchResultField buildSearchResultField(String caseTypedId,
                                                    String caseFieldId,
                                                    String caseFieldPath,
                                                    String label,
                                                    String displayContextParameter) {
        SearchResultField searchResultField = new SearchResultField();
        searchResultField.setCaseFieldId(caseFieldId);
        searchResultField.setCaseFieldPath(caseFieldPath);
        searchResultField.setCaseTypeId(caseTypedId);
        searchResultField.setLabel(label);
        searchResultField.setDisplayOrder(1);
        searchResultField.setDisplayContextParameter(displayContextParameter);
        return searchResultField;
    }

    static Map<String, JsonNode> buildData(String... dataFieldIds) {
        Map<String, JsonNode> dataMap = Maps.newHashMap();
        Lists.newArrayList(dataFieldIds).forEach(dataFieldId -> {
            dataMap.put(dataFieldId, JSON_NODE_FACTORY.textNode(dataFieldId));
        });
        return dataMap;
    }
}
