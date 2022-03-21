package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.Sets;
import com.microsoft.applicationinsights.core.dependencies.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
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
import uk.gov.hmcts.ccd.v2.external.domain.CaseUser;
import uk.gov.hmcts.reform.idam.client.models.UserInfo;

import javax.inject.Inject;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class CaseUserControllerIT extends WireMockBaseTest {

    @Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @SpyBean
    private AuditRepository auditRepository;

    @Before
    public void setUp() throws JsonProcessingException {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, "caseworker-probate");

        String uidNoEventAccess = "1234";
        UserInfo userInfo = UserInfo.builder()
            .uid(uidNoEventAccess)
            .roles(com.google.common.collect.Lists.newArrayList(MockUtils.ROLE_CASEWORKER_PUBLIC))
            .build();
        stubFor(WireMock.post(urlMatching("/o/token"))
            .willReturn(okJson(mapper.writeValueAsString(userInfo)).withStatus(200)));
        stubFor(WireMock.get(urlMatching("/api/v1/users/.*"))
            .willReturn(okJson(mapper.writeValueAsString(userInfo)).withStatus(200)));

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases.sql",
        "classpath:sql/insert_case_users.sql"
    })
    public void shouldGrantRoles() throws Exception {
        String role1 = "[DEFENDANT]";
        String role2 = "[CLAIMANT]";
        String userId = "a_target_user_id";
        String caseId = "1504259907353529";
        String requestUrl =  "/cases/" + caseId + "/users/" + userId;
        CaseUser caseUser = new CaseUser();
        caseUser.setCaseRoles(Sets.newHashSet(role1, role2));

        stubIdamRolesForUser(userId);

        final MvcResult mvcResult = mockMvc.perform(put(requestUrl)
            .contentType(JSON_CONTENT_TYPE)
            .accept(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsString(caseUser))
        ).andReturn();

        assertEquals(mvcResult.getResponse().getContentAsString(), 204, mvcResult.getResponse().getStatus());

        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.UPDATE_CASE_ACCESS.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(caseId));
        assertThat(captor.getValue().getTargetIdamId(), is(userId));
        assertThat(captor.getValue().getTargetCaseRoles(), is(Lists.newArrayList(role1, role2)));
    }
}
