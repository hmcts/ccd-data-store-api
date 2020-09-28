package uk.gov.hmcts.ccd.v2.internal.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.SearchInputsViewResource;
import uk.gov.hmcts.ccd.v2.internal.resource.WorkbasketInputsViewResource;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UIDefinitionControllerIT extends WireMockBaseTest {

    @Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql" })
    public void shouldReturnWorkBasketInputDefinitions() throws Exception {

        final MvcResult result =
            mockMvc.perform(get("/internal/case-types/TestAddressBookCase/work-basket-inputs")
            .contentType(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer user1")
            .header(V2.EXPERIMENTAL_HEADER, "true"))
            .andExpect(status().is(200))
            .andReturn();

        WorkbasketInputsViewResource response =
            mapper.readValue(result.getResponse().getContentAsString(), WorkbasketInputsViewResource.class);
        WorkbasketInputsViewResource.WorkbasketInputView[] workbasketInputs = response.getWorkbasketInputs();

        assertThat(workbasketInputs[0].getLabel(), is("First Name"));
        assertThat(workbasketInputs[0].getField().getId(), is("PersonFirstName"));
        assertThat(workbasketInputs[0].getField().getShowCondition(), is("PersonLastName=\"tom\""));

        assertThat(workbasketInputs[1].getLabel(), is("Last Name"));
        assertThat(workbasketInputs[1].getField().getId(), is("PersonLastName"));
        assertThat(workbasketInputs[1].getField().getShowCondition(), nullValue());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql" })
    public void shouldReturnWorkSearchInputDefinitions() throws Exception {

        final MvcResult result =
            mockMvc.perform(get("/internal/case-types/TestAddressBookCase/search-inputs")
            .contentType(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer user1")
            .header(V2.EXPERIMENTAL_HEADER, "true"))
            .andExpect(status().is(200))
            .andReturn();

        SearchInputsViewResource response =
            mapper.readValue(result.getResponse().getContentAsString(), SearchInputsViewResource.class);
        SearchInputsViewResource.SearchInputView[] searchInputs = response.getSearchInputs();

        assertThat(searchInputs[0].getLabel(), is("First Name"));
        assertThat(searchInputs[0].getField().getId(), is("PersonFirstName"));
        assertThat(searchInputs[0].getField().getShowCondition(), nullValue());

        assertThat(searchInputs[1].getLabel(), is("Last Name"));
        assertThat(searchInputs[1].getField().getId(), is("PersonLastName"));
        assertThat(searchInputs[1].getField().getShowCondition(), is("PersonFirstName=\"fred\""));

    }
}
