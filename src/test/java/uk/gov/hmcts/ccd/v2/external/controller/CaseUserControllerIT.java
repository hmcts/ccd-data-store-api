package uk.gov.hmcts.ccd.v2.external.controller;

import com.google.common.collect.Sets;
import com.microsoft.applicationinsights.core.dependencies.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.v2.external.domain.CaseUser;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

public class CaseUserControllerIT extends WireMockBaseTest {

    private static final String UID = "123";

    @Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @SpyBean
    private AuditRepository auditRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, "caseworker-probate");

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
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
        String URL =  "/cases/" + caseId + "/users/" + userId;
        CaseUser caseUser = new CaseUser();
        caseUser.setCaseRoles(Sets.newHashSet(role1, role2));

        final MvcResult mvcResult = mockMvc.perform(put(URL)
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
