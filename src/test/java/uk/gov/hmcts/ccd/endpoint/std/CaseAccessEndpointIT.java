package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.BaseTest;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.data.caseaccess.AMCaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserAuditEntity;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserEntity;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.domain.model.std.UserId;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestPropertySource(properties =
    {
        "ccd.am.write.to_both=TestAddressBookCase"
    })
public class CaseAccessEndpointIT extends BaseTest {

    private static final String JURISDICTION = "PROBATE";
    private static final String CASE_TYPE = "TestAddressBookCase";
    private static final String USER_ID = "123";
    private static final String CASE_ID = "1504259907353529";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    private JdbcTemplate template;

    @MockBean
    @Qualifier(AMCaseUserRepository.ACCESS_MANAGEMENT_QUALIFIER)
    private CaseUserRepository amCaseUserRepository;

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
        template = new JdbcTemplate(db);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WhenFindIdsCalled() throws Exception {

        grantAccess();

        final String aUrl = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" +
            CASE_TYPE + "/cases/" + CASE_ID + "/users";

        mockMvc.perform(post(aUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new UserId(USER_ID))))
            .andExpect(status().isCreated())
            .andReturn();

        final String url = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" +
            CASE_TYPE + "/cases/ids?userId=" + USER_ID;

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

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn500AndRollbackWhenCCDAccessGrantedButAMAccessGrantingFailedInWritingToBothMode() throws Exception {
        doThrow(RuntimeException.class).when(amCaseUserRepository).grantAccess(anyString(), anyString(), anyString(), anyLong(), anyString(), anyString());
        grantAccessFailsDueToAMFailure();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn500AndRollbackWhenCCDAccessRevokedButAMAccessRevokingFailedInWritingToBothMode() throws Exception {
        grantAccess();
        doThrow(RuntimeException.class).when(amCaseUserRepository).revokeAccess(anyString(), anyString(), anyString(), anyLong(), anyString(), anyString());
        revokeAccessFailsDueToAMFailure();
    }

    private void revokeAccess() throws Exception {
        final String url = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" +
            CASE_TYPE + "/cases/" + CASE_ID + "/users/" + USER_ID;

        mockMvc.perform(delete(url))
            .andExpect(status().isNoContent())
            .andReturn();
    }

    private void grantAccessFailsDueToAMFailure() throws Exception {
        final String url = "/caseworkers/0000-aaaa-2222-bbbb/jurisdictions/"
            + JURISDICTION + "/case-types/"
            + CASE_TYPE + "/cases/" + CASE_ID + "/users";

        mockMvc.perform(post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new UserId(USER_ID))))
            .andExpect(status().is5xxServerError())
            .andReturn();

        assertCCDDataRolledBack(0);
    }

    private void revokeAccessFailsDueToAMFailure() throws Exception {
        final String url = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" +
            CASE_TYPE + "/cases/" + CASE_ID + "/users/" + USER_ID;

        mockMvc.perform(delete(url))
            .andExpect(status().is5xxServerError())
            .andReturn();

        assertCCDDataRolledBack(1);
    }

    private void grantAccess() throws Exception {
        final String url = "/caseworkers/0000-aaaa-2222-bbbb/jurisdictions/"
            + JURISDICTION + "/case-types/"
            + CASE_TYPE + "/cases/" + CASE_ID + "/users";

        mockMvc.perform(post(url)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new UserId(USER_ID))))
            .andExpect(status().isCreated())
            .andReturn();
    }

    private void assertCCDDataRolledBack(int numEntries) throws Exception {
        // read from db using another thread / transaction
        ExecutorService es = Executors.newSingleThreadExecutor();
        Future<?> future = es.submit(() -> {
            final List<CaseUserAuditEntity> caseUserAuditEntities = template.query("SELECT * FROM case_users_audit", this::mapCaseUserAudit);
            assertEquals("Incorrect number of case user audit entities", numEntries, caseUserAuditEntities.size());
            final List<CaseUserEntity> caseUserEntities = template.query("SELECT * FROM case_users", this::mapCaseUser);
            assertEquals("Incorrect number of case user entities", numEntries, caseUserEntities.size());
            return null;
        });
        future.get();

    }


    @Test
    public void findCaseIdsGivenUserIdHasAccessToWithUuid() throws Exception {
        final String url = "/caseworkers/0000-aaaa-2222-bbbb/jurisdictions/"
            + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/ids?userId="
            + "0000-zzzz-9999-yyyy";

        final MvcResult mvcResult = mockMvc.perform(get(url))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus())
            .isEqualTo(200);

    }


}
