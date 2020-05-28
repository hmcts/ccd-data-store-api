package uk.gov.hmcts.ccd.endpoint.std;

import org.junit.Before;
import org.junit.Test;
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
import uk.gov.hmcts.ccd.domain.model.search.CaseSearchResult;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CaseSearchEndpointESIT extends ElasticsearchBaseTest {

    private static final String POST_SEARCH_CASES = "/searchCases";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, "caseworker-autotest1");

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void shouldWork() throws Exception {
        String searchRequest = "{\"query\":{\"match_all\":{}}}";

        final MvcResult result = mockMvc.perform(post(POST_SEARCH_CASES)
            .contentType(MediaType.APPLICATION_JSON)
            .param("ctid", "AAT,MAPPER")
            .content(searchRequest))
            .andExpect(status().is(200))
            .andReturn();

        String responseAsString = result.getResponse().getContentAsString();
        CaseSearchResult caseSearchResult = mapper.readValue(responseAsString, CaseSearchResult.class);

        assertThat(caseSearchResult.getTotal(), is(3L));
    }
}
