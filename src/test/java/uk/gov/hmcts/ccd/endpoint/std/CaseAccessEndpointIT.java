package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.Lists;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.data.casedataaccesscontrol.RoleAssignmentResponse;
import uk.gov.hmcts.ccd.domain.model.std.UserId;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import javax.inject.Inject;
import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.createRoleAssignmentRecord;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.createRoleAssignmentResponse;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.emptyRoleAssignmentResponseJson;

public class CaseAccessEndpointIT extends WireMockBaseTest {

    private static final String JURISDICTION = "PROBATE";
    private static final String CASE_TYPE = "TestAddressBookCase";
    private static final String USER_ID = "123";
    private static final String CASE_ID = "1504259907353529";
    private static final String ASSIGNMENT = "assignment1";
    protected static final String CASE_ROLE = "[CREATOR]";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    @SpyBean
    private AuditRepository auditRepository;

    @Before
    public void setUp() throws IOException {
        super.initMock();
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, "caseworker-probate");
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        String uidNoEventAccess = "1234";
        UserInfo userInfo = UserInfo.builder()
            .uid(uidNoEventAccess)
            .roles(Lists.newArrayList(MockUtils.ROLE_CASEWORKER_PUBLIC))
            .build();
        stubFor(WireMock.post(urlMatching("/o/token"))
            .willReturn(okJson(mapper.writeValueAsString(userInfo)).withStatus(200)));
        stubFor(WireMock.get(urlMatching("/api/v1/users/.*"))
            .willReturn(okJson(mapper.writeValueAsString(userInfo)).withStatus(200)));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WhenFindIdsCalled() throws Exception {

        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            // no existing roleAssignments
            stubFor(WireMock.post(urlMatching("/am/role-assignments/query"))
                .inScenario("MultiRoleAssignmentQueryCalls")
                .willReturn(okJson(emptyRoleAssignmentResponseJson()).withStatus(200))
                .willSetStateTo("FirstAttempt")
            );

            // one role assignment
            RoleAssignmentResponse roleAssignmentResponse = createRoleAssignmentResponse(
                singletonList(createRoleAssignmentRecord(ASSIGNMENT, CASE_ID, CASE_ROLE, USER_ID)));

            stubFor(WireMock.post(urlMatching("/am/role-assignments/query"))
                .inScenario("MultiRoleAssignmentQueryCalls")
                .whenScenarioStateIs("FirstAttempt")
                .willReturn(okJson(defaultObjectMapper.writeValueAsString(roleAssignmentResponse)).withStatus(200))
                .willSetStateTo("SecondAttempt")
            );

            // stored role assignment with case access
            stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + USER_ID))
                .willReturn(okJson(defaultObjectMapper.writeValueAsString(
                    createRoleAssignmentResponse(
                        singletonList(createRoleAssignmentRecord(ASSIGNMENT, CASE_ID, CASE_TYPE, JURISDICTION,
                            CASE_ROLE, USER_ID, false)))))
                    .withStatus(200)));
        }

        final String url = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/ids?userId=" + USER_ID;

        grantAccess();

        final String aUrl = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/" + CASE_ID + "/users";

        mockMvc.perform(post(aUrl)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(new UserId(USER_ID))))
            .andExpect(status().isCreated())
            .andReturn();

        final MvcResult mvcResult = mockMvc.perform(get(url))
            .andExpect(status().isOk())
            .andReturn();

        List<String> response = mapper.readValue(mvcResult.getResponse().getContentAsString(),
            new TypeReference<>() {});

        assertThat(response)
            .contains(CASE_ID);
    }

    @Test
    public void shouldReturnEmptyListWhenFindIdsCalledAndNoGrantsExist() throws Exception {
        String userId = "12312312312321";

        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            // no existing roleAssignments
            stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + userId))
                .willReturn(okJson(emptyRoleAssignmentResponseJson()).withStatus(200)));
        }

        final String url = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE
            + "/cases/ids?userId=" + userId;

        final MvcResult mvcResult = mockMvc.perform(get(url))
            .andExpect(status().isOk())
            .andReturn();

        List<String> response = mapper.readValue(mvcResult.getResponse().getContentAsString(),
            new TypeReference<>() {});

        assertThat(response)
            .size()
            .isEqualTo(0);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenGrantCalled() throws Exception {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            // no existing roleAssignments
            stubFor(WireMock.post(urlMatching("/am/role-assignments/query"))
                .willReturn(okJson(emptyRoleAssignmentResponseJson()).withStatus(200)));
        }

        grantAccess();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldLogAndAuditGrantAccessToCase() throws Exception {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            // no existing roleAssignments
            stubFor(WireMock.post(urlMatching("/am/role-assignments/query"))
                .willReturn(okJson(emptyRoleAssignmentResponseJson()).withStatus(200)));
        }
        grantAccess();

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        MatcherAssert.assertThat(captor.getValue().getOperationType(),
            is(AuditOperationType.GRANT_CASE_ACCESS.getLabel()));
        MatcherAssert.assertThat(captor.getValue().getCaseId(), is(CASE_ID));
        MatcherAssert.assertThat(captor.getValue().getJurisdiction(), is(JURISDICTION));
        MatcherAssert.assertThat(captor.getValue().getIdamId(), is(USER_ID));
        MatcherAssert.assertThat(captor.getValue().getInvokingService(), is("ccd_gw"));
        MatcherAssert.assertThat(captor.getValue().getHttpStatus(), is(201));
        MatcherAssert.assertThat(captor.getValue().getTargetIdamId(), is(USER_ID));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn201WhenGrantIsCalledMultipleTimesWithSameContent() throws Exception {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            // no existing roleAssignments
            stubFor(WireMock.post(urlMatching("/am/role-assignments/query"))
                .inScenario("MultiRoleAssignmentQueryCalls")
                .willReturn(okJson(emptyRoleAssignmentResponseJson()).withStatus(200))
                .willSetStateTo("FirstAttempt")
            );

            // one role assignment
            RoleAssignmentResponse roleAssignmentResponse = createRoleAssignmentResponse(
                singletonList(createRoleAssignmentRecord(ASSIGNMENT, CASE_ID, CASE_ROLE, USER_ID)));

            stubFor(WireMock.post(urlMatching("/am/role-assignments/query"))
                .inScenario("MultiRoleAssignmentQueryCalls")
                .whenScenarioStateIs("FirstAttempt")
                .willReturn(okJson(defaultObjectMapper.writeValueAsString(roleAssignmentResponse)).withStatus(200))
                .willSetStateTo("SecondAttempt")
            );
        }
        grantAccess();
        grantAccess();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn204WhenRevokeCalled() throws Exception {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            // no existing roleAssignments
            stubFor(WireMock.post(urlMatching("/am/role-assignments/query"))
                .willReturn(okJson(emptyRoleAssignmentResponseJson()).withStatus(200)));
        }
        grantAccess();
        revokeAccess();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldLogAndAuditRevokeAccessToCase() throws Exception {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            // no existing roleAssignments
            stubFor(WireMock.post(urlMatching("/am/role-assignments/query"))
                .willReturn(okJson(emptyRoleAssignmentResponseJson()).withStatus(200)));
        }
        grantAccess();
        revokeAccess();

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository, times(2)).save(captor.capture());
        List<AuditEntry> auditEntry = captor.getAllValues();

        MatcherAssert.assertThat(auditEntry.get(1).getOperationType(),
            is(AuditOperationType.REVOKE_CASE_ACCESS.getLabel()));
        MatcherAssert.assertThat(auditEntry.get(1).getCaseId(), is(CASE_ID));
        MatcherAssert.assertThat(auditEntry.get(1).getJurisdiction(), is(JURISDICTION));
        MatcherAssert.assertThat(auditEntry.get(1).getIdamId(), is(USER_ID));
        MatcherAssert.assertThat(auditEntry.get(1).getInvokingService(), is("ccd_gw"));
        MatcherAssert.assertThat(auditEntry.get(1).getHttpStatus(), is(204));
        MatcherAssert.assertThat(auditEntry.get(1).getTargetIdamId(), is(USER_ID));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn204WhenRevokeCalledTwice() throws Exception {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            // no existing roleAssignments
            stubFor(WireMock.post(urlMatching("/am/role-assignments/query"))
                .willReturn(okJson(emptyRoleAssignmentResponseJson()).withStatus(200)));
        }
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
    public void findCaseIdsGivenUserIdHasAccessToWithUuid() throws Exception {
        String userId = "0000-zzzz-9999-yyyy";

        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            // no existing roleAssignments
            stubFor(WireMock.get(urlMatching("/am/role-assignments/actors/" + userId))
                .willReturn(okJson(emptyRoleAssignmentResponseJson()).withStatus(200)));
        }

        final String url = "/caseworkers/0000-aaaa-2222-bbbb/jurisdictions/"
            + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/ids?userId="
            + userId;

        final MvcResult mvcResult = mockMvc.perform(get(url))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(mvcResult.getResponse().getStatus())
            .isEqualTo(200);
    }

    private void revokeAccess() throws Exception {
        final String url = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE + "/cases/"
            + CASE_ID + "/users/" + USER_ID;

        mockMvc.perform(delete(url))
            .andExpect(status().isNoContent())
            .andReturn();
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
}
