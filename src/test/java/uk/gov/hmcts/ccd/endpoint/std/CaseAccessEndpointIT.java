package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.domain.model.std.UserId;

import java.util.List;
import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CaseAccessEndpointIT extends BaseTest {

    private static final String JURISDICTION = "PROBATE";
    private static final String CASE_TYPE = "TestAddressBookCase";
    private static final String USER_ID = "123";
    private static final String CASE_ID = "1504259907353529";

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

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WhenFindIdsCalled() throws Exception {

        final String url = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" +
            CASE_TYPE + "/cases/ids?userId=" + USER_ID;

        grantAccess();

        final String aUrl = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" +
            CASE_TYPE + "/cases/" + CASE_ID + "/users";

        mockMvc.perform(post(aUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new UserId(USER_ID))))
            .andExpect(status().isCreated())
            .andReturn();

        final MvcResult mvcResult = mockMvc.perform(get(url))
            .andExpect(status().isOk())
            .andReturn();

        List<String> response = mapper.readValue(mvcResult.getResponse().getContentAsString(),
            new TypeReference<List<String>>() {
            });

        assertThat(response)
            .contains(CASE_ID);
    }

    @Test
    public void shouldReturnEmptyListWhenFindIdsCalledAndNoGrantsExist() throws Exception {
        final String url = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" +
            CASE_TYPE + "/cases/ids?userId=12312312312321";

        final MvcResult mvcResult = mockMvc.perform(get(url))
            .andExpect(status().isOk())
            .andReturn();

        List<String> response = mapper.readValue(mvcResult.getResponse().getContentAsString(),
            new TypeReference<List<String>>() {
            }
        );

        assertThat(response)
            .size()
            .isEqualTo(0);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenGrantCalled() throws Exception {
        grantAccess();
    }


    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenGrantIsCalledMultipleTimesWithSameContent() throws Exception {
        grantAccess();
        grantAccess();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn204WhenRevokeCalled() throws Exception {
        grantAccess();
        revokeAccess();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn204WhenRevokeCalledTwice() throws Exception {
       grantAccess();
       revokeAccess();
       revokeAccess();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn204WhenGrantDoesnotExistAndRevokeCalled() throws Exception {
        revokeAccess();
    }

    private void revokeAccess() throws Exception {
        final String url = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" +
            CASE_TYPE + "/cases/" + CASE_ID + "/users/" + USER_ID;

        mockMvc.perform(delete(url))
            .andExpect(status().isNoContent())
            .andReturn();
    }

    private void grantAccess() throws Exception {
        final String url = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" +
            CASE_TYPE + "/cases/" + CASE_ID + "/users";

        mockMvc.perform(post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new UserId(USER_ID))))
            .andExpect(status().isCreated())
            .andReturn();
    }
}
