package uk.gov.hmcts.ccd.endpoint.ui;

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
import uk.gov.hmcts.ccd.domain.model.search.SearchInput;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SearchInputEndpointTest extends WireMockBaseTest {
    private static final String URL = "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/inputs";

    @Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
            "classpath:sql/insert_cases.sql" })
    public void validSearch() throws Exception {

        final MvcResult result = mockMvc.perform(get(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, "Bearer user1"))
                .andExpect(status().is(200))
                .andReturn();

        final SearchInput[] searchInputs = mapper.readValue(result.getResponse().getContentAsString(), SearchInput[].class);

        assertEquals("First Name", searchInputs[0].getLabel());
        assertEquals(1, searchInputs[0].getOrder());
        assertEquals("PersonFirstName", searchInputs[0].getField().getId());
        assertEquals("Text", searchInputs[0].getField().getType().getId());
        assertEquals("First Name", searchInputs[0].getLabel());

        assertEquals("Last Name", searchInputs[1].getLabel());
        assertEquals(2, searchInputs[1].getOrder());
        assertEquals("PersonLastName",searchInputs[1].getField().getId());
        assertEquals("Text", searchInputs[1].getField().getType().getId());
    }
}
