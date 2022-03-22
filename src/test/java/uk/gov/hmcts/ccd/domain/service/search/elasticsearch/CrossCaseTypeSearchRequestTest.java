package uk.gov.hmcts.ccd.domain.service.search.elasticsearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest;
import uk.gov.hmcts.ccd.endpoint.exceptions.BadSearchRequest;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.ccd.domain.model.search.elasticsearch.ElasticsearchRequest.SOURCE;

class CrossCaseTypeSearchRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String NAME =  "name";
    private static final String ALIAS_NAME =  "alias." + NAME;
    private static final String SEARCH_REQUEST_SIMPLE_JSON =  "{\"query\":{}}";
    private static final String SEARCH_REQUEST_WITH_SOURCE_JSON =
        "{\"_source\":[\"" + ALIAS_NAME + "\"], \"query\":{}}";


    @Test
    @DisplayName("should throw exception when query node not found")
    void shouldThrowExceptionWhenQueryNodeNotFound() throws JsonProcessingException {

        // ARRANGE
        String query = "{}";

        // ACT / ASSERT
        CrossCaseTypeSearchRequest.Builder builder = new CrossCaseTypeSearchRequest.Builder()
            .withCaseTypes(singletonList("caseType"))
            .withSearchRequest(new ElasticsearchRequest(objectMapper.readValue(query, JsonNode.class)));
        assertThrows(BadSearchRequest.class, builder::build);
    }


    @Nested
    @DisplayName("builder")
    class Builder {

        @Test
        @DisplayName("should build non-multi-case-type search request")
        void shouldBuildNonMultiCaseTypeSearch() throws Exception {

            // ARRANGE
            ElasticsearchRequest elasticsearchRequest =
                new ElasticsearchRequest(objectMapper.readTree(SEARCH_REQUEST_SIMPLE_JSON));
            List<String> caseTypeIds = singletonList("CT");

            // ACT
            CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
                .withSearchRequest(elasticsearchRequest)
                .withCaseTypes(caseTypeIds)
                .build();

            // ASSERT
            assertThat(request.isMultiCaseTypeSearch(), is(false));
            assertThat(request.getSearchRequestJsonNode(), is(elasticsearchRequest.getNativeSearchRequest()));
            assertThat(request.getCaseTypeIds(), hasSize(1));
            assertThat(request.getAliasFields().isEmpty(), is(true));
            assertThat(request.getSearchIndex().isPresent(), is(false));
        }

        @Test
        @DisplayName("should build multi-case-type search request")
        void shouldBuildMultiCaseTypeSearch() throws Exception {

            // ARRANGE
            ElasticsearchRequest elasticsearchRequest =
                new ElasticsearchRequest(objectMapper.readTree(SEARCH_REQUEST_WITH_SOURCE_JSON));
            List<String> caseTypeIds = Arrays.asList("CT", "PT");

            // ACT
            CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
                .withSearchRequest(elasticsearchRequest)
                .withCaseTypes(caseTypeIds)
                .withMultiCaseTypeSearch(true)
                .build();

            // ASSERT
            assertThat(request.isMultiCaseTypeSearch(), is(true));
            assertThat(request.getSearchRequestJsonNode(), is(elasticsearchRequest.getNativeSearchRequest()));
            assertNull(request.getSearchRequestJsonNode().get(SOURCE)); // i.e. SOURCE should be cleared
            assertThat(request.getCaseTypeIds(), is(caseTypeIds));
            assertThat(request.getAliasFields(), hasItem(NAME));
            assertThat(request.getSearchIndex().isPresent(), is(false));
        }

        @Test
        @DisplayName("should build search request: but skip auto-population of alias fields if using fixed SearchIndex")
        void shouldBuildButSkipAutoPopulationOfAliasFieldsIfSearchIndexIsSet() throws Exception {

            // ARRANGE
            ElasticsearchRequest elasticsearchRequest =
                new ElasticsearchRequest(objectMapper.readTree(SEARCH_REQUEST_WITH_SOURCE_JSON));
            List<String> caseTypeIds = Arrays.asList("CT", "PT");
            SearchIndex searchIndex = createSearchIndex();

            // ACT
            CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder()
                .withSearchRequest(elasticsearchRequest)
                .withCaseTypes(caseTypeIds)
                .withMultiCaseTypeSearch(true)
                .withSearchIndex(searchIndex)
                .build();

            // ASSERT
            assertThat(request.isMultiCaseTypeSearch(), is(true));
            assertThat(request.getSearchRequestJsonNode(), is(elasticsearchRequest.getNativeSearchRequest()));
            assertNotNull(request.getSearchRequestJsonNode().get(SOURCE)); // i.e. SOURCE should be retained
            JSONAssert.assertEquals(
                "[\"" + ALIAS_NAME + "\"]",
                request.getSearchRequestJsonNode().get(SOURCE).toString(),
                JSONCompareMode.LENIENT
            );
            assertThat(request.getCaseTypeIds(), is(caseTypeIds));
            assertThat(request.getAliasFields().isEmpty(), is(true));
            assertThat(request.getSearchIndex().isPresent(), is(true));
            assertThat(request.getSearchIndex().get(), is(searchIndex));
        }

    }


    @Nested
    @DisplayName("clone builder")
    class CloneBuilder {

        @Test
        @DisplayName("should build a clone of a non-multi-case-type search request")
        void shouldBuildACloneOfANonMultiCaseTypeSearch() throws Exception {

            // ARRANGE
            ElasticsearchRequest elasticsearchRequest =
                new ElasticsearchRequest(objectMapper.readTree(SEARCH_REQUEST_SIMPLE_JSON));
            List<String> caseTypeIds = singletonList("CT");

            CrossCaseTypeSearchRequest originalRequest = new CrossCaseTypeSearchRequest.Builder()
                .withSearchRequest(elasticsearchRequest)
                .withCaseTypes(caseTypeIds)
                .build();

            // ACT
            CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder(originalRequest).build();

            // ASSERT
            assertThat(request.isMultiCaseTypeSearch(), is(false));
            assertThat(request.getSearchRequestJsonNode(), is(elasticsearchRequest.getNativeSearchRequest()));
            assertThat(request.getCaseTypeIds(), hasSize(1));
            assertThat(request.getAliasFields().isEmpty(), is(true));
        }

        @Test
        @DisplayName("should build a clone of a multi-case-type search request")
        void shouldBuildACloneOfAMultiCaseTypeSearch() throws Exception {

            // ARRANGE
            ElasticsearchRequest elasticsearchRequest =
                new ElasticsearchRequest(objectMapper.readTree(SEARCH_REQUEST_WITH_SOURCE_JSON));
            List<String> caseTypeIds = Arrays.asList("CT", "PT");

            CrossCaseTypeSearchRequest originalRequest = new CrossCaseTypeSearchRequest.Builder()
                .withSearchRequest(elasticsearchRequest)
                .withCaseTypes(caseTypeIds)
                .withMultiCaseTypeSearch(true)
                .build();

            // ACT
            CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder(originalRequest).build();

            // ASSERT
            assertThat(request.isMultiCaseTypeSearch(), is(true));
            assertThat(request.getSearchRequestJsonNode(), is(elasticsearchRequest.getNativeSearchRequest()));
            assertNull(request.getSearchRequestJsonNode().get(SOURCE)); // i.e. SOURCE should be cleared
            assertThat(request.getCaseTypeIds(), is(caseTypeIds));
            assertThat(request.getAliasFields(), hasItem(NAME)); // i.e. populated with alias from SOURCE
            assertThat(request.getSearchIndex().isPresent(), is(false));
        }

        @Test
        @DisplayName("should build a clone of a search request: but allow overrides")
        void shouldBuildACloneOfASearchRequestWithOverrides() throws Exception {

            // ARRANGE
            ElasticsearchRequest elasticsearchRequest =
                new ElasticsearchRequest(objectMapper.readTree(SEARCH_REQUEST_SIMPLE_JSON));
            List<String> caseTypeIds = Arrays.asList("CT", "PT");
            List<String> aliasFields = Arrays.asList("field1", "field2");
            SearchIndex searchIndex = createSearchIndex();

            CrossCaseTypeSearchRequest originalRequest = new CrossCaseTypeSearchRequest.Builder()
                .withSearchRequest(elasticsearchRequest)
                .withCaseTypes(caseTypeIds)
                .withMultiCaseTypeSearch(true)
                .withSourceFilterAliasFields(aliasFields)
                .withSearchIndex(searchIndex)
                .build();

            // overrides
            ElasticsearchRequest elasticsearchRequestOverride =
                new ElasticsearchRequest(objectMapper.readTree(SEARCH_REQUEST_WITH_SOURCE_JSON));
            List<String> caseTypeIdsOverride = singletonList("CT-override");
            List<String> aliasFieldsOverride = Arrays.asList("field1-override", "field2-override");
            SearchIndex searchIndexOverride = new SearchIndex(
                "my_index_name_override",
                "my_index_type_override"
            );

            // ACT
            CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder(originalRequest)
                .withSearchRequest(elasticsearchRequestOverride)
                .withCaseTypes(caseTypeIdsOverride)
                .withSourceFilterAliasFields(aliasFieldsOverride)
                .withSearchIndex(searchIndexOverride)
                .build();

            // ASSERT
            assertThat(request.isMultiCaseTypeSearch(), is(false)); // NB: now a single cate type search
            assertThat(request.getSearchRequestJsonNode(), is(elasticsearchRequestOverride.getNativeSearchRequest()));
            assertThat(request.getCaseTypeIds(), is(caseTypeIdsOverride));
            assertThat(request.getAliasFields(), is(aliasFieldsOverride));
            assertThat(request.getSearchIndex().isPresent(), is(true));
            assertThat(request.getSearchIndex().get(), is(searchIndexOverride));
        }

        @Test
        @DisplayName("should build a clone of a search request: but allow some overrides set to null")
        void shouldBuildACloneOfASearchRequestWithOverridesSetToNull() throws Exception {

            // ARRANGE
            ElasticsearchRequest elasticsearchRequest =
                new ElasticsearchRequest(objectMapper.readTree(SEARCH_REQUEST_SIMPLE_JSON));
            List<String> caseTypeIds = Arrays.asList("CT", "PT");
            List<String> aliasFields = Arrays.asList("field1", "field2");
            SearchIndex searchIndex = createSearchIndex();

            CrossCaseTypeSearchRequest originalRequest = new CrossCaseTypeSearchRequest.Builder()
                .withSearchRequest(elasticsearchRequest)
                .withCaseTypes(caseTypeIds)
                .withMultiCaseTypeSearch(true)
                .withSourceFilterAliasFields(aliasFields)
                .withSearchIndex(searchIndex)
                .build();

            // ACT
            CrossCaseTypeSearchRequest request = new CrossCaseTypeSearchRequest.Builder(originalRequest)
                .withCaseTypes(null)
                .withSourceFilterAliasFields(null)
                .withSearchIndex(null)
                .build();

            // ASSERT
            assertThat(request.isMultiCaseTypeSearch(), is(false)); // i.e. reset to default after null case types
            assertThat(request.getCaseTypeIds().isEmpty(), is(true));
            assertThat(request.getAliasFields().isEmpty(), is(true));
            assertThat(request.getSearchIndex().isPresent(), is(false));
        }

    }


    private SearchIndex createSearchIndex() {
        return new SearchIndex(
            "my_index_name",
            "my_index_type"
        );
    }

}
