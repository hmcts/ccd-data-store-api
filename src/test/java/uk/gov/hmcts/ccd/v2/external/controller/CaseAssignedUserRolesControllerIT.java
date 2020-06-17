package uk.gov.hmcts.ccd.v2.external.controller;

import com.google.common.collect.Lists;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.DefaultCaseUserRepository;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.domain.AddCaseAssignedUserRolesResponse;
import uk.gov.hmcts.ccd.v2.external.resource.CaseAssignedUserRolesResource;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.v2.V2.Error.OTHER_USER_CASE_ROLE_ACCESS_NOT_GRANTED;
import static uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController.ADD_SUCCESS_MESSAGE;

class CaseAssignedUserRolesControllerIT extends WireMockBaseTest {

    private static final Long INVALID_CASE_ID = 222L;

    private static final String AUTHORISED_ADD_SERVICE_1 = "ADD_SERVICE_1";
    private static final String AUTHORISED_ADD_SERVICE_2 = "ADD_SERVICE_2";

    private static final String CASE_ID_1 = "7578590391163133";
    private static final String CASE_ID_2 = "6375837333991692";
    private static final String CASE_IDS = CASE_ID_1 + "," + CASE_ID_2;

    private static final String CASE_ID_ADD_SUCCESS = "1111222233334444";
    private static final String CASE_ID_ADD_FAILURE = "4444333322221111";

    private static final String CASE_ROLE_1 = "[case-role-1]";
    private static final String CASE_ROLE_2 = "[case-role-2]";
    private static final String INVALID_CASE_ROLE = "bad-role";

    private static final String USER_IDS_1 = "89000";
    private static final String USER_IDS_2 = "89001";
    private static final String USER_IDS_3 = "89002";
    private static final String USER_IDS_4 = "89003";
    private static final String USER_IDS = USER_IDS_1 + "," + USER_IDS_2;

    private static final String INVALID_USER_IDS = USER_IDS_1 + ", ," + USER_IDS_2;

    @SuppressWarnings("SpellCheckingInspection")
    private static final String SERVICE_NAME = "servicename";

    private final String caseworkerCaa = "caseworker-caa";
    private final String getCaseAssignedUserRoles = "/case-users";
    private final String postCaseAssignedUserRoles = "/case-users";

    private static final String PARAM_CASE_IDS = "case_ids";
    private static final String PARAM_USER_IDS = "user_ids";

    @Inject
    private ApplicationParams applicationParams;

    @SpyBean @Inject
    private AuditRepository auditRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private MockMvc mockMvc;

    @Inject @Qualifier(DefaultCaseUserRepository.QUALIFIER)
    private CaseUserRepository caseUserRepository;

    @Inject
    private WebApplicationContext wac;

    @BeforeEach
    void setUp() throws IOException {
        super.initMock();
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        ReflectionTestUtils.setField(
            applicationParams,
            "authorisedServicesForAddUserCaseRoles",
            Lists.newArrayList(AUTHORISED_ADD_SERVICE_1, AUTHORISED_ADD_SERVICE_2)
        );

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    // RDM-8606: AC-1
    @Test
    @DisplayName(
        "addCaseUserRoles: AC-1: must successfully assign a user and case role for a specific case by a user calling through/from an authorised application"
    )
    public void addCaseUserRoles_shouldAddCaseUserRoleForAuthorisedApp() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        ReflectionTestUtils.setField(authentication.getPrincipal(), SERVICE_NAME, AUTHORISED_ADD_SERVICE_1);

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_ADD_SUCCESS, USER_IDS_1, CASE_ROLE_1);
        caseUserRoles.add(caseUserRole1);

        // ACT
        final MvcResult result = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().is(200))
            .andReturn();

        // ASSERT
        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        AddCaseAssignedUserRolesResponse response = mapper.readValue(content, AddCaseAssignedUserRolesResponse.class);
        assertNotNull("Response should not be null", response);
        assertEquals("Success message should be returned", ADD_SUCCESS_MESSAGE, response.getStatus());

        // check data has been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_ADD_SUCCESS), USER_IDS_1);
        assertEquals(1, caseRoles.size());
        assertThat(caseRoles, hasItems(CASE_ROLE_1));
    }

    // RDM-8606: AC-2
    @Test
    @DisplayName("addCaseUserRoles: AC-2: Must return an error response for a missing Case ID")
    public void addCaseUserRoles_shouldThrowExceptionWhenCaseIDNotPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        ReflectionTestUtils.setField(authentication.getPrincipal(), SERVICE_NAME, AUTHORISED_ADD_SERVICE_1);

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_ADD_FAILURE, USER_IDS_1, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(null, USER_IDS_1, CASE_ROLE_1);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().is(400))
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.CASE_ID_INVALID));

        // check data has not been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_ADD_FAILURE), USER_IDS_1);
        assertEquals(0, caseRoles.size());
    }

    // RDM-8606: AC-3
    @Test
    @DisplayName("addCaseUserRoles: AC-3: Must return an error response for a malformed Case ID")
    public void addCaseUserRoles_shouldThrowExceptionWhenInvalidCaseIDPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        ReflectionTestUtils.setField(authentication.getPrincipal(), SERVICE_NAME, AUTHORISED_ADD_SERVICE_1);

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_ADD_FAILURE, USER_IDS_1, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(INVALID_CASE_ID.toString(), USER_IDS_1, CASE_ROLE_1);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().is(400))
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.CASE_ID_INVALID));

        // check data has not been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_ADD_FAILURE), USER_IDS_2);
        assertEquals(0, caseRoles.size());
    }

    // RDM-8606: AC-4
    @Test
    @DisplayName("addCaseUserRoles: AC-4: Must return an error response for a missing User ID")
    public void addCaseUserRoles_shouldThrowExceptionWhenUserIDNotPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        ReflectionTestUtils.setField(authentication.getPrincipal(), SERVICE_NAME, AUTHORISED_ADD_SERVICE_1);

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_ADD_FAILURE, USER_IDS_1, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(CASE_ID_ADD_FAILURE, null, CASE_ROLE_1);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().is(400))
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.USER_ID_INVALID));

        // check data has not been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_ADD_FAILURE), USER_IDS_1);
        assertEquals(0, caseRoles.size());
    }

    // RDM-8606: AC-5
    @Test
    @DisplayName("addCaseUserRoles: AC-5: Must return an error response for a malformed User ID Provided")
    public void addCaseUserRoles_shouldThrowExceptionWhenInvalidUserIDPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        ReflectionTestUtils.setField(authentication.getPrincipal(), SERVICE_NAME, AUTHORISED_ADD_SERVICE_1);

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_ADD_FAILURE, USER_IDS_1, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(CASE_ID_ADD_FAILURE, "", CASE_ROLE_1);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().is(400))
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.USER_ID_INVALID));

        // check data has not been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_ADD_FAILURE), USER_IDS_1);
        assertEquals(0, caseRoles.size());
    }

    // RDM-8606: AC-6
    @Test
    @DisplayName("addCaseUserRoles: AC-6: must return an error response when the request is made from an un-authorised application")
    public void addCaseUserRoles_shouldThrowExceptionWhenCalledFromUnauthorisedApp() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        ReflectionTestUtils.setField(authentication.getPrincipal(), SERVICE_NAME, "UNAUTHORISED_ADD_SERVICE");

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_ADD_FAILURE, USER_IDS_1, CASE_ROLE_1);
        caseUserRoles.add(caseUserRole1);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().is(403))
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION));

        // check data has not been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_ADD_FAILURE), USER_IDS_1);
        assertEquals(0, caseRoles.size());
    }

    // RDM-8606: AC-7
    @Test
    @DisplayName("addCaseUserRoles: AC-7: Must return an error response for a malformed Case Role provided")
    public void addCaseUserRoles_shouldThrowExceptionWhenInvalidCaseRolePassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        ReflectionTestUtils.setField(authentication.getPrincipal(), SERVICE_NAME, AUTHORISED_ADD_SERVICE_1);

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_ADD_FAILURE, USER_IDS_1, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(CASE_ID_ADD_FAILURE, USER_IDS_1, INVALID_CASE_ROLE);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().is(400))
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.CASE_ROLE_FORMAT_INVALID));

        // check data has not been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_ADD_FAILURE), USER_IDS_1);
        assertEquals(0, caseRoles.size());
    }

    // RDM-8606: AC-8
    @Test
    @DisplayName("addCaseUserRoles: AC-8: Must return an error response for a missing Case Role")
    public void addCaseUserRoles_shouldThrowExceptionWhenCaseRoleNotPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        ReflectionTestUtils.setField(authentication.getPrincipal(), SERVICE_NAME, AUTHORISED_ADD_SERVICE_1);

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_ADD_FAILURE, USER_IDS_1, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(CASE_ID_ADD_FAILURE, USER_IDS_1, null);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().is(400))
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.CASE_ROLE_FORMAT_INVALID));

        // check data has not been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_ADD_FAILURE), USER_IDS_1);
        assertEquals(0, caseRoles.size());
    }

    // RDM-8606: null
    @Test
    @DisplayName("addCaseUserRoles: null: should throw exception")
    public void addCaseUserRoles_shouldThrowExceptionWhenNullListPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        ReflectionTestUtils.setField(authentication.getPrincipal(), SERVICE_NAME, AUTHORISED_ADD_SERVICE_1);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(null)))
            .headers(createHttpHeaders()))
            .andExpect(status().is(400))
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.EMPTY_CASE_USER_ROLE_LIST));
    }

    // RDM-8606: empty-list
    @Test
    @DisplayName("addCaseUserRoles: empty-list: should throw exception")
    public void addCaseUserRoles_shouldThrowExceptionWhenEmptyListPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        ReflectionTestUtils.setField(authentication.getPrincipal(), SERVICE_NAME, AUTHORISED_ADD_SERVICE_1);

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().is(400))
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.EMPTY_CASE_USER_ROLE_LIST));
    }

    // RDM-8606: duplicate
    @Test
    @DisplayName("addCaseUserRoles: duplicate: should not generate duplicates")
    public void addCaseUserRoles_shouldAddSingleCaseUserRoleWhenDuplicatePassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        ReflectionTestUtils.setField(authentication.getPrincipal(), SERVICE_NAME, AUTHORISED_ADD_SERVICE_1);

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_ADD_SUCCESS, USER_IDS_2, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(CASE_ID_ADD_SUCCESS, USER_IDS_2, CASE_ROLE_1);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        final MvcResult result = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().is(200))
            .andReturn();

        // ASSERT
        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        AddCaseAssignedUserRolesResponse response = mapper.readValue(content, AddCaseAssignedUserRolesResponse.class);
        assertNotNull("Response should not be null", response);
        assertEquals("Success message should be returned", ADD_SUCCESS_MESSAGE, response.getStatus());

        // check data has been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_ADD_SUCCESS), USER_IDS_2);
        assertEquals(1, caseRoles.size());
        assertThat(caseRoles, hasItems(CASE_ROLE_1));
    }

    // RDM-8606: multiple
    @Test
    @DisplayName("addCaseUserRoles: multiple: should allow multiple CaseUserRoles to be added in single call")
    public void addCaseUserRoles_shouldAddMultipleCaseUserRoles() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        ReflectionTestUtils.setField(authentication.getPrincipal(), SERVICE_NAME, AUTHORISED_ADD_SERVICE_1);

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_ADD_SUCCESS, USER_IDS_3, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(CASE_ID_ADD_SUCCESS, USER_IDS_3, CASE_ROLE_2);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        final MvcResult result = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().is(200))
            .andReturn();

        // ASSERT
        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        AddCaseAssignedUserRolesResponse response = mapper.readValue(content, AddCaseAssignedUserRolesResponse.class);
        assertNotNull("Response should not be null", response);
        assertEquals("Success message should be returned", ADD_SUCCESS_MESSAGE, response.getStatus());

        // check data has been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_ADD_SUCCESS), USER_IDS_3);
        assertEquals(2, caseRoles.size());
        assertThat(caseRoles, hasItems(CASE_ROLE_1));
        assertThat(caseRoles, hasItems(CASE_ROLE_2));
    }

    // RDM-8606: log-audit
    @Test
    @DisplayName("addCaseUserRoles: log-audit: should allow multiple CaseUserRoles to be added in single call")
    public void addCaseUserRoles_shouldCreateLogAuditWhenCalled() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        ReflectionTestUtils.setField(authentication.getPrincipal(), SERVICE_NAME, AUTHORISED_ADD_SERVICE_1);

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_ADD_SUCCESS, USER_IDS_4, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(CASE_ID_ADD_SUCCESS, USER_IDS_4, CASE_ROLE_2);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().is(200))
            .andReturn();

        // ASSERT
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.ADD_CASE_ASSIGNED_USER_ROLES.getLabel()));
        assertThat(captor.getValue().getCaseId(), is(CASE_ID_ADD_SUCCESS + "," + CASE_ID_ADD_SUCCESS));
        assertThat(captor.getValue().getTargetCaseRoles(), is(Lists.newArrayList(CASE_ROLE_1, CASE_ROLE_2)));
        assertThat(captor.getValue().getTargetIdamId(), is(USER_IDS_4 + "," + USER_IDS_4));
        assertThat(captor.getValue().getInvokingService(), is(AUTHORISED_ADD_SERVICE_1));
        assertThat(captor.getValue().getHttpStatus(), is(200));
        assertThat(captor.getValue().getPath(), is(postCaseAssignedUserRoles));
        assertThat(captor.getValue().getHttpMethod(), is(HttpMethod.POST.name()));
    }

    // AC-1
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql",
        "classpath:sql/insert_case_users_valid_case_ids.sql"
    })
    public void getUserCaseRolesAssignedToUser() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, caseworkerCaa);

        final MvcResult result = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, CASE_IDS)
            .param(PARAM_USER_IDS, USER_IDS)
            .headers(createHttpHeaders()))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseAssignedUserRolesResource caseAssignedUserRolesResource = mapper.readValue(content, CaseAssignedUserRolesResource.class);
        assertNotNull("Case Assigned User Roles should not be null", caseAssignedUserRolesResource);
    }

    // AC-2
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql",
        "classpath:sql/insert_case_users_valid_case_ids.sql"
    })
    public void shouldGetSelfCaseUserRolesAssigned() throws Exception {
        MockUtils.setSecurityAuthorities("89000", authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        final MvcResult result = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, CASE_IDS)
            .param(PARAM_USER_IDS, USER_IDS_1)
            .headers(createHttpHeaders()))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseAssignedUserRolesResource caseAssignedUserRolesResource = mapper.readValue(content, CaseAssignedUserRolesResource.class);
        assertNotNull("Case Assigned User Roles should not be null", caseAssignedUserRolesResource);
        assertEquals(1, caseAssignedUserRolesResource.getCaseAssignedUserRoles().size());
        assertEquals("7578590391163133", caseAssignedUserRolesResource.getCaseAssignedUserRoles().get(0).getCaseDataId());
        assertEquals("89000", caseAssignedUserRolesResource.getCaseAssignedUserRoles().get(0).getUserId());
        assertEquals("[CREATOR]", caseAssignedUserRolesResource.getCaseAssignedUserRoles().get(0).getCaseRole());
    }

    // AC-3
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql",
        "classpath:sql/insert_case_users_valid_case_ids.sql"
    })
    public void shouldGetAllUserCaseRolesRelatingToAllUsersWhenNoUserIDPassedForPassedCaseId() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, caseworkerCaa);

        final MvcResult result = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, CASE_ID_2)
            .headers(createHttpHeaders()))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseAssignedUserRolesResource caseAssignedUserRolesResource = mapper.readValue(content, CaseAssignedUserRolesResource.class);
        assertNotNull("Case Assigned User Roles should not be null", caseAssignedUserRolesResource);
        assertEquals(3, caseAssignedUserRolesResource.getCaseAssignedUserRoles().size());

        assertAll(
            () -> assertThat(caseAssignedUserRolesResource.getCaseAssignedUserRoles(),
                hasItems(allOf(hasProperty("caseDataId", Matchers.is(CASE_ID_2)),
                    hasProperty("userId", Matchers.is(USER_IDS_2)),
                    hasProperty("caseRole", Matchers.is("[DEFENDANT]"))))),
            () -> assertThat(caseAssignedUserRolesResource.getCaseAssignedUserRoles(),
                hasItems(allOf(hasProperty("caseDataId", Matchers.is(CASE_ID_2)),
                    hasProperty("userId", Matchers.is(USER_IDS_2)),
                    hasProperty("caseRole", Matchers.is("[SOLICITOR]"))))),
            () -> assertThat(caseAssignedUserRolesResource.getCaseAssignedUserRoles(),
                hasItems(allOf(hasProperty("caseDataId", Matchers.is(CASE_ID_2)),
                    hasProperty("userId", Matchers.is(USER_IDS_3)),
                    hasProperty("caseRole", Matchers.is("[DEFENDANT]"))))));
    }

    // AC-4
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql",
        "classpath:sql/insert_case_users_valid_case_ids.sql"
    })
    public void shouldGetAllUserCaseRolesRelatingToAllUsersWhenNoUserIDPassedForListOfCaseIds() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, caseworkerCaa);

        final MvcResult result = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, CASE_IDS)
            .headers(createHttpHeaders()))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseAssignedUserRolesResource caseAssignedUserRolesResource = mapper.readValue(content, CaseAssignedUserRolesResource.class);
        assertNotNull("Case Assigned User Roles should not be null", caseAssignedUserRolesResource);
        assertEquals(4, caseAssignedUserRolesResource.getCaseAssignedUserRoles().size());

        assertAll(
            () -> assertThat(caseAssignedUserRolesResource.getCaseAssignedUserRoles(),
                hasItems(allOf(hasProperty("caseDataId", Matchers.is(CASE_ID_1)),
                    hasProperty("userId", Matchers.is(USER_IDS_1)),
                    hasProperty("caseRole", Matchers.is("[CREATOR]"))))),
            () -> assertThat(caseAssignedUserRolesResource.getCaseAssignedUserRoles(),
                hasItems(allOf(hasProperty("caseDataId", Matchers.is(CASE_ID_2)),
                    hasProperty("userId", Matchers.is(USER_IDS_2)),
                    hasProperty("caseRole", Matchers.is("[DEFENDANT]"))))));
    }

    // AC-5
    @Test
    public void shouldThrowExceptionWhenEmptyCaseIDListPassed() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, caseworkerCaa);

        Exception exception = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, "")
            .param(PARAM_USER_IDS, USER_IDS)
            .headers(createHttpHeaders()))
            .andExpect(status().is(400))
            .andReturn().getResolvedException();

        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.EMPTY_CASE_ID_LIST));
    }

    @Test
    public void shouldThrowExceptionWhenCaseIDNotPassed() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, caseworkerCaa);

        mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_USER_IDS, USER_IDS)
            .headers(createHttpHeaders()))
            .andExpect(status().is(400))
            .andReturn();
    }

    // AC-6
    @Test
    public void shouldThrowExceptionWhenInvalidCaseIDIsPassed() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, caseworkerCaa);

        Exception exception = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, INVALID_CASE_ID.toString())
            .param(PARAM_USER_IDS, USER_IDS)
            .headers(createHttpHeaders()))
            .andExpect(status().is(400))
            .andReturn().getResolvedException();

        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.CASE_ID_INVALID));
    }

    // AC-7
    @Test
    public void shouldThrowExceptionWhenInvalidUserIdDataPassed() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, caseworkerCaa);

        Exception exception = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, CASE_IDS)
            .param(PARAM_USER_IDS, INVALID_USER_IDS)
            .headers(createHttpHeaders()))
            .andExpect(status().is(400))
            .andReturn().getResolvedException();

        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.USER_ID_INVALID));
    }

    // AC-8
    @Test
    public void shouldThrowExceptionWhenInvokingUserHasNoPrivileges() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        Exception exception = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, CASE_IDS)
            .param(PARAM_USER_IDS, USER_IDS)
            .headers(createHttpHeaders()))
            .andExpect(status().is(403))
            .andReturn().getResolvedException();

        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(OTHER_USER_CASE_ROLE_ACCESS_NOT_GRANTED));
    }

    @Test
    public void shouldThrowExceptionWhenUserRequestedForSelfCaseRoleAccessAlongWithOtherUsers() throws Exception {
        MockUtils.setSecurityAuthorities("89000", authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        Exception exception = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, CASE_IDS)
            .param(PARAM_USER_IDS, USER_IDS)
            .headers(createHttpHeaders()))
            .andExpect(status().is(403))
            .andReturn().getResolvedException();

        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(OTHER_USER_CASE_ROLE_ACCESS_NOT_GRANTED));
    }

    private HttpHeaders createHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer user1");
        headers.add("ServiceAuthorization", "Bearer service1");
        return headers;
    }

}
