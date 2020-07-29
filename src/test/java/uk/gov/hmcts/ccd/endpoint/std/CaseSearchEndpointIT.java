package uk.gov.hmcts.ccd.endpoint.std;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.searchbox.client.JestClient;
import io.searchbox.core.MultiSearchResult;
import io.searchbox.core.SearchResult;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.reflect.Whitebox;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;

import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("checkstyle:OperatorWrap") // too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
public class CaseSearchEndpointIT extends WireMockBaseTest {

    private static final String POST_SEARCH_CASES = "/searchCases";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    @MockBean
    private JestClient jestClient;

    @SpyBean
    private AuditRepository auditRepository;

    @Before
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void testSearchCaseDetails() throws Exception {
        stubElasticSearchSearchRequestWillReturn();
        String searchRequest = "{\"query\": {\"match_all\": {}}}";
        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
                .contentType(JSON_CONTENT_TYPE)
                .param("ctid", "TestAddressBookCase")
                .content(searchRequest))
                .andExpect(status().is(200))
                .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResults = mapper.readValue(responseAsString,
                                                              CaseSearchResult.class);

        assertThat(caseSearchResults.getTotal(), is(2L));
        List<CaseDetails> caseDetails = caseSearchResults.getCases();
        assertThat(caseDetails, hasSize(2));
        assertThat(caseDetails, hasItem(hasProperty("reference", equalTo(1593528446017551L))));
        assertThat(caseDetails, hasItem(hasProperty("reference", equalTo(1594134773278525L))));
        assertThat(caseDetails, hasItem(hasProperty("jurisdiction", equalTo("AUTOTEST1"))));
        assertThat(caseDetails, hasItem(hasProperty("caseTypeId", equalTo("AAT"))));
        assertThat(caseDetails, hasItem(hasProperty("lastModified",
                                                    equalTo(LocalDateTime.parse("2020-06-30T14:52:36.335")))));
        assertThat(caseDetails, hasItem(hasProperty("createdDate",
                equalTo(LocalDateTime.parse("2020-06-30T14:47:26.061")))));
        assertThat(caseDetails, hasItem(hasProperty("state", equalTo("TODO"))));
        assertThat(caseDetails, hasItem(hasProperty("securityClassification",
                equalTo(SecurityClassification.PUBLIC))));
    }

    @Test
    public void shouldAuditLogSearchCases() throws Exception {
        stubElasticSearchSearchRequestWillReturn();
        String searchRequest = "{\"query\": {\"match_all\": {}}}";
        MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(JSON_CONTENT_TYPE)
            .param("ctid", "TestAddressBookCase", "TestAddressBookCase4")
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResults = mapper.readValue(responseAsString,
            CaseSearchResult.class);

        List<CaseDetails> caseDetails = caseSearchResults.getCases();
        assertThat(caseDetails, hasSize(2));
        assertThat(caseDetails, hasItem(hasProperty("reference", equalTo(1593528446017551L))));
        assertThat(caseDetails, hasItem(hasProperty("reference", equalTo(1594134773278525L))));

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.SEARCH_CASE.getLabel()));
        assertThat(captor.getValue().getCaseId(), is("1535450291607660,1535450291607670"));
        assertThat(captor.getValue().getIdamId(), is("Cloud.Strife@test.com"));
        assertThat(captor.getValue().getInvokingService(), is(MockUtils.CCD_GW));
        assertThat(captor.getValue().getHttpStatus(), is(200));
        assertThat(captor.getValue().getListOfCaseTypes(), is("TestAddressBookCase,TestAddressBookCase4"));
    }

    private void stubElasticSearchSearchRequestWillReturn() throws java.io.IOException {
        MultiSearchResult multiSearchResult = mock(MultiSearchResult.class);
        when(multiSearchResult.isSucceeded()).thenReturn(true);

        String json = "{\n"
                + "    \"took\": 96,\n"
                + "    \"timed_out\": false,\n"
                + "    \"_shards\": {\n"
                + "        \"total\": 2,\n"
                + "        \"successful\": 2,\n"
                + "        \"skipped\": 0,\n"
                + "        \"failed\": 0\n"
                + "    },\n"
                + "    \"hits\": {\n"
                + "        \"total\": 2,\n"
                + "        \"max_score\": 0.18232156,\n"
                + "        \"hits\": [\n"
                + "            {\n"
                + "                \"_index\": \"aat_cases-000001\",\n"
                + "                \"_type\": \"_doc\",\n"
                + "                \"_id\": \"355\",\n"
                + "                \"_score\": 0.18232156,\n"
                + "                \"_source\": {\n"
                + "                    \"jurisdiction\": \"AUTOTEST1\",\n"
                + "                    \"case_type_id\": \"AAT\",\n"
                + "                    \"data\": {\n"
                + "                        \"FixedRadioListField\": null,\n"
                + "                        \"TextAreaField\": null,\n"
                + "                        \"ComplexField\": {\n"
                + "                            \"ComplexTextField\": null,\n"
                + "                            \"ComplexFixedListField\": null,\n"
                + "                            \"ComplexNestedField\": {\n"
                + "                                \"NestedNumberField\": null,\n"
                + "                                \"NestedCollectionTextField\": [\n"
                + "                                    \n"
                + "                                ]\n"
                + "                            }\n"
                + "                        },\n"
                + "                        \"EmailField\": null,\n"
                + "                        \"TextField\": null,\n"
                + "                        \"AddressUKField\": {\n"
                + "                            \"Country\": null,\n"
                + "                            \"AddressLine3\": null,\n"
                + "                            \"County\": null,\n"
                + "                            \"AddressLine1\": null,\n"
                + "                            \"PostCode\": null,\n"
                + "                            \"AddressLine2\": null,\n"
                + "                            \"PostTown\": null\n"
                + "                        },\n"
                + "                        \"CollectionField\": [\n"
                + "                            \n"
                + "                        ],\n"
                + "                        \"MoneyGBPField\": null,\n"
                + "                        \"DateField\": null,\n"
                + "                        \"MultiSelectListField\": [\n"
                + "                            \n"
                + "                        ],\n"
                + "                        \"PhoneUKField\": null,\n"
                + "                        \"YesOrNoField\": null,\n"
                + "                        \"NumberField\": null,\n"
                + "                        \"DateTimeField\": null,\n"
                + "                        \"FixedListField\": null\n"
                + "                    },\n"
                + "                    \"created_date\": \"2020-06-30T14:47:26.061Z\",\n"
                + "                    \"id\": 355,\n"
                + "                    \"last_modified\": \"2020-06-30T14:52:36.335Z\",\n"
                + "                    \"@timestamp\": \"2020-07-16T22:58:33.430Z\",\n"
                + "                    \"index_id\": \"aat_cases\",\n"
                + "                    \"@version\": \"1\",\n"
                + "                    \"data_classification\": {\n"
                + "                        \"FixedRadioListField\": \"PUBLIC\",\n"
                + "                        \"TextAreaField\": \"PUBLIC\",\n"
                + "                        \"ComplexField\": {\n"
                + "                            \"classification\": \"PUBLIC\",\n"
                + "                            \"value\": {\n"
                + "                                \"ComplexTextField\": \"PUBLIC\",\n"
                + "                                \"ComplexFixedListField\": \"PUBLIC\",\n"
                + "                                \"ComplexNestedField\": {\n"
                + "                                    \"classification\": \"PUBLIC\",\n"
                + "                                    \"value\": {\n"
                + "                                        \"NestedNumberField\": \"PUBLIC\",\n"
                + "                                        \"NestedCollectionTextField\": {\n"
                + "                                            \"classification\": \"PUBLIC\",\n"
                + "                                            \"value\": [\n"
                + "                                                \n"
                + "                                            ]\n"
                + "                                        }\n"
                + "                                    }\n"
                + "                                }\n"
                + "                            }\n"
                + "                        },\n"
                + "                        \"EmailField\": \"PUBLIC\",\n"
                + "                        \"TextField\": \"PUBLIC\",\n"
                + "                        \"AddressUKField\": {\n"
                + "                            \"classification\": \"PUBLIC\",\n"
                + "                            \"value\": {\n"
                + "                                \"Country\": \"PUBLIC\",\n"
                + "                                \"AddressLine3\": \"PUBLIC\",\n"
                + "                                \"County\": \"PUBLIC\",\n"
                + "                                \"AddressLine1\": \"PUBLIC\",\n"
                + "                                \"PostCode\": \"PUBLIC\",\n"
                + "                                \"AddressLine2\": \"PUBLIC\",\n"
                + "                                \"PostTown\": \"PUBLIC\"\n"
                + "                            }\n"
                + "                        },\n"
                + "                        \"CollectionField\": {\n"
                + "                            \"classification\": \"PUBLIC\",\n"
                + "                            \"value\": [\n"
                + "                                \n"
                + "                            ]\n"
                + "                        },\n"
                + "                        \"MoneyGBPField\": \"PUBLIC\",\n"
                + "                        \"DateField\": \"PUBLIC\",\n"
                + "                        \"MultiSelectListField\": \"PUBLIC\",\n"
                + "                        \"PhoneUKField\": \"PUBLIC\",\n"
                + "                        \"YesOrNoField\": \"PUBLIC\",\n"
                + "                        \"NumberField\": \"PUBLIC\",\n"
                + "                        \"DateTimeField\": \"PUBLIC\",\n"
                + "                        \"FixedListField\": \"PUBLIC\"\n"
                + "                    },\n"
                + "                    \"security_classification\": \"PUBLIC\",\n"
                + "                    \"state\": \"TODO\",\n"
                + "                    \"reference\": 1593528446017551\n"
                + "                }\n"
                + "            },\n"
                + "            {\n"
                + "                \"_index\": \"aat_cases-000001\",\n"
                + "                \"_type\": \"_doc\",\n"
                + "                \"_id\": \"357\",\n"
                + "                \"_score\": 0.18232156,\n"
                + "                \"_source\": {\n"
                + "                    \"jurisdiction\": \"AUTOTEST1\",\n"
                + "                    \"case_type_id\": \"AAT\",\n"
                + "                    \"data\": {\n"
                + "                        \"FixedRadioListField\": null,\n"
                + "                        \"TextAreaField\": null,\n"
                + "                        \"ComplexField\": {\n"
                + "                            \"ComplexTextField\": null,\n"
                + "                            \"ComplexFixedListField\": null,\n"
                + "                            \"ComplexNestedField\": {\n"
                + "                                \"NestedNumberField\": null,\n"
                + "                                \"NestedCollectionTextField\": [\n"
                + "                                    \n"
                + "                                ]\n"
                + "                            }\n"
                + "                        },\n"
                + "                        \"EmailField\": \"email1@gmail.com\",\n"
                + "                        \"TextField\": \"Text Field 1\",\n"
                + "                        \"AddressUKField\": {\n"
                + "                            \"Country\": null,\n"
                + "                            \"AddressLine3\": null,\n"
                + "                            \"County\": null,\n"
                + "                            \"AddressLine1\": null,\n"
                + "                            \"PostCode\": null,\n"
                + "                            \"AddressLine2\": null,\n"
                + "                            \"PostTown\": null\n"
                + "                        },\n"
                + "                        \"CollectionField\": [\n"
                + "                            \n"
                + "                        ],\n"
                + "                        \"MoneyGBPField\": null,\n"
                + "                        \"DateField\": null,\n"
                + "                        \"MultiSelectListField\": [\n"
                + "                            \n"
                + "                        ],\n"
                + "                        \"PhoneUKField\": null,\n"
                + "                        \"YesOrNoField\": null,\n"
                + "                        \"NumberField\": null,\n"
                + "                        \"DateTimeField\": null,\n"
                + "                        \"FixedListField\": null\n"
                + "                    },\n"
                + "                    \"created_date\": \"2020-07-07T15:12:53.258Z\",\n"
                + "                    \"id\": 357,\n"
                + "                    \"last_modified\": \"2020-07-07T15:14:04.635Z\",\n"
                + "                    \"@timestamp\": \"2020-07-16T22:58:33.435Z\",\n"
                + "                    \"index_id\": \"aat_cases\",\n"
                + "                    \"@version\": \"1\",\n"
                + "                    \"data_classification\": {\n"
                + "                        \"FixedRadioListField\": \"PUBLIC\",\n"
                + "                        \"TextAreaField\": \"PUBLIC\",\n"
                + "                        \"ComplexField\": {\n"
                + "                            \"classification\": \"PUBLIC\",\n"
                + "                            \"value\": {\n"
                + "                                \"ComplexTextField\": \"PUBLIC\",\n"
                + "                                \"ComplexFixedListField\": \"PUBLIC\",\n"
                + "                                \"ComplexNestedField\": {\n"
                + "                                    \"classification\": \"PUBLIC\",\n"
                + "                                    \"value\": {\n"
                + "                                        \"NestedNumberField\": \"PUBLIC\",\n"
                + "                                        \"NestedCollectionTextField\": {\n"
                + "                                            \"classification\": \"PUBLIC\",\n"
                + "                                            \"value\": [\n"
                + "                                                \n"
                + "                                            ]\n"
                + "                                        }\n"
                + "                                    }\n"
                + "                                }\n"
                + "                            }\n"
                + "                        },\n"
                + "                        \"EmailField\": \"PUBLIC\",\n"
                + "                        \"TextField\": \"PUBLIC\",\n"
                + "                        \"AddressUKField\": {\n"
                + "                            \"classification\": \"PUBLIC\",\n"
                + "                            \"value\": {\n"
                + "                                \"Country\": \"PUBLIC\",\n"
                + "                                \"AddressLine3\": \"PUBLIC\",\n"
                + "                                \"County\": \"PUBLIC\",\n"
                + "                                \"AddressLine1\": \"PUBLIC\",\n"
                + "                                \"PostCode\": \"PUBLIC\",\n"
                + "                                \"AddressLine2\": \"PUBLIC\",\n"
                + "                                \"PostTown\": \"PUBLIC\"\n"
                + "                            }\n"
                + "                        },\n"
                + "                        \"CollectionField\": {\n"
                + "                            \"classification\": \"PUBLIC\",\n"
                + "                            \"value\": [\n"
                + "                                \n"
                + "                            ]\n"
                + "                        },\n"
                + "                        \"MoneyGBPField\": \"PUBLIC\",\n"
                + "                        \"DateField\": \"PUBLIC\",\n"
                + "                        \"MultiSelectListField\": \"PUBLIC\",\n"
                + "                        \"PhoneUKField\": \"PUBLIC\",\n"
                + "                        \"YesOrNoField\": \"PUBLIC\",\n"
                + "                        \"NumberField\": \"PUBLIC\",\n"
                + "                        \"DateTimeField\": \"PUBLIC\",\n"
                + "                        \"FixedListField\": \"PUBLIC\"\n"
                + "                    },\n"
                + "                    \"security_classification\": \"PUBLIC\",\n"
                + "                    \"state\": \"TODO\",\n"
                + "                    \"reference\": 1594134773278525\n"
                + "                }\n"
                + "            }\n"
                + "        ]\n"
                + "    }\n"
                + "}";
        JsonObject convertedObject = new Gson().fromJson(json, JsonObject.class);
        SearchResult searchResult;
        Gson gson = new Gson();
        searchResult = new SearchResult(gson);
        searchResult.setSucceeded(true);
        searchResult.setJsonObject(convertedObject);
        searchResult.setJsonString(convertedObject.toString());
        searchResult.setPathToResult("hits/hits/_source");

        MultiSearchResult.MultiSearchResponse response = mock(MultiSearchResult.MultiSearchResponse.class);
        when(multiSearchResult.getResponses()).thenReturn(Collections.singletonList(response));
        when(searchResult.getJsonObject()).thenReturn(convertedObject);
        Whitebox.setInternalState(response, "searchResult", searchResult);

        given(jestClient.execute(any())).willReturn(multiSearchResult);
    }
}
