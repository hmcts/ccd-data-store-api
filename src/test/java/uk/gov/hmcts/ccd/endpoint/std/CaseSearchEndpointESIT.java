package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.ElasticsearchBaseTest;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;

import javax.inject.Inject;

import static org.elasticsearch.index.query.QueryBuilders.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.test.ElasticsearchTestHelper.*;

class CaseSearchEndpointESIT extends ElasticsearchBaseTest {

    private static final String POST_SEARCH_CASES = "/searchCases";
    private static final String CASE_TYPE_ID_PARAM = "ctid";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, AUTOTEST1_PUBLIC, AUTOTEST2_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Nested
    class CrossCaseTypeSearch {

        @Test // Note that cross case type searches do NOT return case data
        void shouldReturnAllCasesForAllSpecifiedCaseTypes() throws Exception {
            String searchRequest = ElasticsearchTestRequest.builder()
                .query(matchAllQuery())
                .build().toJsonString();

            MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
                .contentType(MediaType.APPLICATION_JSON)
                .param(CASE_TYPE_ID_PARAM, "AAT,MAPPER")
                .content(searchRequest))
                .andExpect(status().is(200))
                .andReturn();

            String responseAsString = result.getResponse().getContentAsString();
            CaseSearchResult caseSearchResult = mapper.readValue(responseAsString, CaseSearchResult.class);

            assertAll(
                () -> assertThat(caseSearchResult.getTotal(), is(3L)),
                () -> assertThat(caseSearchResult.getCaseReferences(CASE_TYPE_A).size(), is(2)),
                () -> assertThat(caseSearchResult.getCaseReferences(CASE_TYPE_B).size(), is(1)),
                () -> assertThat(caseSearchResult.getCases().get(0).getData().size(), is(0)),
                () -> assertThat(caseSearchResult.getCases().get(1).getData().size(), is(0)),
                () -> assertThat(caseSearchResult.getCases().get(2).getData().size(), is(0))
            );
        }

        @Test
        void shouldReturnRequestedAliasSource() throws Exception {
            String searchRequest = ElasticsearchTestRequest.builder()
                .query(matchAllQuery())
                .source(alias(TEXT_ALIAS))
                .sort(CREATED_DATE)
                .build().toJsonString();

            MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
                .contentType(MediaType.APPLICATION_JSON)
                .param(CASE_TYPE_ID_PARAM, "AAT,MAPPER")
                .content(searchRequest))
                .andExpect(status().is(200))
                .andReturn();

            String responseAsString = result.getResponse().getContentAsString();
            CaseSearchResult caseSearchResult = mapper.readValue(responseAsString, CaseSearchResult.class);

            assertAll(
                () -> assertThat(caseSearchResult.getTotal(), is(3L)),
                () -> assertThat(caseSearchResult.getCases().get(0).getData().get(TEXT_ALIAS).asText(), is(TEXT_VALUE)),
                () -> assertThat(caseSearchResult.getCases().get(1).getData().get(TEXT_ALIAS).asText(), is("CCC TextValue")),
                () -> assertThat(caseSearchResult.getCases().get(2).getData().get(TEXT_ALIAS).asText(), is("BBB TextValue")),
                () -> assertThat(caseSearchResult.getCases().get(0).getData().size(), is(1)),
                () -> assertThat(caseSearchResult.getCases().get(1).getData().size(), is(1)),
                () -> assertThat(caseSearchResult.getCases().get(2).getData().size(), is(1))
            );
        }

        @Test // Note that the size and sort is applied to each separate case type then results combined
        void shouldReturnPaginatedResults() throws Exception {
            String searchRequest = ElasticsearchTestRequest.builder()
                .query(matchAllQuery())
                .sort(CREATED_DATE)
                .size(1)
                .build().toJsonString();

            MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
                .contentType(MediaType.APPLICATION_JSON)
                .param(CASE_TYPE_ID_PARAM, "AAT,MAPPER")
                .content(searchRequest))
                .andExpect(status().is(200))
                .andReturn();

            String responseAsString = result.getResponse().getContentAsString();
            CaseSearchResult caseSearchResult = mapper.readValue(responseAsString, CaseSearchResult.class);

            assertAll(
                () -> assertThat(caseSearchResult.getTotal(), is(3L)),
                () -> assertThat(caseSearchResult.getCases().size(), is(2)) // = Size * Number of case types
            );
        }

        @Test
        void shouldQueryOnAliasField() throws Exception {
            String searchRequest = ElasticsearchTestRequest.builder()
                .query(matchQuery(alias(FIXED_LIST_ALIAS), FIXED_LIST_VALUE))
                .build().toJsonString();

            MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
                .contentType(MediaType.APPLICATION_JSON)
                .param(CASE_TYPE_ID_PARAM, "AAT,MAPPER")
                .content(searchRequest))
                .andExpect(status().is(200))
                .andReturn();

            String responseAsString = result.getResponse().getContentAsString();
            CaseSearchResult caseSearchResult = mapper.readValue(responseAsString, CaseSearchResult.class);

            assertAll(
                () -> assertThat(caseSearchResult.getTotal(), is(2L)),
                () -> assertThat(caseSearchResult.getCases().get(0).getReference(), is(1588866820969121L)),
                () -> assertThat(caseSearchResult.getCases().get(1).getReference(), is(1588870615652827L))
            );
        }
    }

    @Nested
    class SingleCaseTypeSearch {

        @Test
        void shouldReturnAllCaseDetails() throws Exception {
            String searchRequest = ElasticsearchTestRequest.builder()
                .query(boolQuery()
                    .must(matchQuery(caseData(NUMBER_FIELD), NUMBER_VALUE)) // ES Double
                    .must(matchQuery(caseData(YES_OR_NO_FIELD), YES_OR_NO_VALUE)) // ES Keyword
                    .must(matchQuery(caseData(TEXT_FIELD), TEXT_VALUE)) // ES Text
                    .must(matchQuery(caseData(DATE_FIELD), DATE_VALUE)) // ES Date
                    .must(matchQuery(caseData(PHONE_FIELD), PHONE_VALUE)) // ES Phone
                    .must(matchQuery(caseData(COUNTRY_FIELD), COUNTRY_VALUE)) // Complex
                    .must(matchQuery(caseData(COLLECTION_FIELD) + VALUE_SUFFIX, COLLECTION_VALUE)) // Collection
                    .must(matchQuery(STATE, STATE_VALUE))) // Metadata
                .build().toJsonString();

            MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
                .contentType(MediaType.APPLICATION_JSON)
                .param(CASE_TYPE_ID_PARAM, CASE_TYPE_A)
                .content(searchRequest))
                .andExpect(status().is(200))
                .andReturn();

            String responseAsString = result.getResponse().getContentAsString();
            CaseSearchResult caseSearchResult = mapper.readValue(responseAsString, CaseSearchResult.class);

            CaseDetails caseDetails = caseSearchResult.getCases().get(0);
            assertAll(
                () -> assertThat(caseSearchResult.getTotal(), is(1L)),
                () -> assertExampleCaseMetadata(caseDetails),
                () -> assertExampleCaseData(caseDetails)
            );
        }

        @Test
        void shouldErrorWhenInvalidCaseTypeIsProvided() throws Exception {
            String searchRequest = ElasticsearchTestRequest.builder()
                .query(matchAllQuery())
                .build().toJsonString();

            MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
                .contentType(MediaType.APPLICATION_JSON)
                .param(CASE_TYPE_ID_PARAM, "INVALID")
                .content(searchRequest))
                .andExpect(status().is(404))
                .andReturn();

            String responseAsString = result.getResponse().getContentAsString();
            JsonNode exceptionNode = mapper.readTree(responseAsString);

            assertAll(
                () -> assertThat(exceptionNode.get("message").asText(),
                    startsWith("Resource not found when getting case type definition for INVALID"))
            );
        }
    }
}
