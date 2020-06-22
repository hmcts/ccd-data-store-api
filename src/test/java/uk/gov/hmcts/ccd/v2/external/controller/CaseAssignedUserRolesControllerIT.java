package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.http.HttpStatus;
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
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.data.caseaccess.CaseUserRepository;
import uk.gov.hmcts.ccd.data.caseaccess.DefaultCaseUserRepository;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRole;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.domain.AddCaseAssignedUserRolesResponse;
import uk.gov.hmcts.ccd.v2.external.resource.CaseAssignedUserRolesResource;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
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
import static uk.gov.hmcts.ccd.auditlog.aop.AuditContext.CASE_ID_SEPARATOR;
import static uk.gov.hmcts.ccd.data.SecurityUtils.SERVICE_AUTHORIZATION;
import static uk.gov.hmcts.ccd.v2.V2.Error.OTHER_USER_CASE_ROLE_ACCESS_NOT_GRANTED;
import static uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController.ADD_SUCCESS_MESSAGE;

class CaseAssignedUserRolesControllerIT extends WireMockBaseTest {

    private static final Long INVALID_CASE_ID = 222L;

    private static final String AUTHORISED_ADD_SERVICE_1 = "ADD_SERVICE_1";
    private static final String AUTHORISED_ADD_SERVICE_2 = "ADD_SERVICE_2";
    private static final String UNAUTHORISED_ADD_SERVICE = "UNAUTHORISED_ADD_SERVICE";

    private static final String CASE_ID_1 = "7578590391163133";
    private static final String CASE_ID_2 = "6375837333991692";
    private static final String CASE_IDS = CASE_ID_1 + "," + CASE_ID_2;

    private static final String CASE_ROLE_1 = "[case-role-1]";
    private static final String CASE_ROLE_2 = "[case-role-2]";
    private static final String INVALID_CASE_ROLE = "bad-role";

    private static final String USER_IDS_1 = "89000";
    private static final String USER_IDS_2 = "89001";
    private static final String USER_IDS_3 = "89002";
    private static final String USER_IDS = USER_IDS_1 + "," + USER_IDS_2;

    private static final String INVALID_USER_IDS = USER_IDS_1 + ", ," + USER_IDS_2;

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
        // disable suppression of audit logs
        ReflectionTestUtils.setField(
            applicationParams,
            "auditLogIgnoreStatuses",
            Lists.newArrayList()
        );

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        String userJson = "{\n"
            + "          \"sub\": \"Cloud.Strife@test.com\",\n"
            + "          \"uid\": \"89000\",\n"
            + "          \"roles\": [\n"
            + "            \"caseworker\",\n"
            + "            \"caseworker-probate-public\"\n"
            + "          ],\n"
            + "          \"name\": \"Cloud Strife\"\n"
            + "        }";
        stubFor(WireMock.get(urlMatching("/o/userinfo"))
            .willReturn(okJson(userJson).withStatus(200)));
    }

    // RDM-8606: AC-1
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql"
    })
    @DisplayName(
        "addCaseUserRoles: AC-1: must successfully assign a user and case role for a specific case by a user calling through/from an authorised application"
    )
    void addCaseUserRoles_shouldAddCaseUserRoleForAuthorisedApp() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10001"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_1, userId, CASE_ROLE_1);
        caseUserRoles.add(caseUserRole1);

        // ACT
        final MvcResult result = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isOk())
            .andReturn();

        // ASSERT
        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        AddCaseAssignedUserRolesResponse response = mapper.readValue(content, AddCaseAssignedUserRolesResponse.class);
        assertNotNull("Response should not be null", response);
        assertEquals("Success message should be returned", ADD_SUCCESS_MESSAGE, response.getStatus());

        // check data has been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(1, caseRoles.size());
        assertThat(caseRoles, hasItems(CASE_ROLE_1));

        verifyAuditForAddCaseUserRoles(HttpStatus.OK, caseUserRoles);
    }

    // RDM-8606: AC-2
    @Test
    @DisplayName("addCaseUserRoles: AC-2: Must return an error response for a missing Case ID")
    void addCaseUserRoles_shouldThrowExceptionWhenCaseIDNotPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10002"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_1, userId, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(null, userId, CASE_ROLE_1);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.CASE_ID_INVALID));

        // check data has not been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(0, caseRoles.size());

        verifyAuditForAddCaseUserRoles(HttpStatus.BAD_REQUEST, caseUserRoles);
    }

    // RDM-8606: AC-3
    @Test
    @DisplayName("addCaseUserRoles: AC-3: Must return an error response for a malformed Case ID")
    void addCaseUserRoles_shouldThrowExceptionWhenInvalidCaseIDPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10003"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_1, userId, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(INVALID_CASE_ID.toString(), userId, CASE_ROLE_1);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.CASE_ID_INVALID));

        // check data has not been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(0, caseRoles.size());

        verifyAuditForAddCaseUserRoles(HttpStatus.BAD_REQUEST, caseUserRoles);
    }

    // RDM-8606: AC-4
    @Test
    @DisplayName("addCaseUserRoles: AC-4: Must return an error response for a missing User ID")
    void addCaseUserRoles_shouldThrowExceptionWhenUserIDNotPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10004"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_1, userId, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(CASE_ID_1, null, CASE_ROLE_1);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.USER_ID_INVALID));

        // check data has not been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(0, caseRoles.size());

        verifyAuditForAddCaseUserRoles(HttpStatus.BAD_REQUEST, caseUserRoles);
    }

    // RDM-8606: AC-5
    @Test
    @DisplayName("addCaseUserRoles: AC-5: Must return an error response for a malformed User ID Provided")
    void addCaseUserRoles_shouldThrowExceptionWhenInvalidUserIDPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10005"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_1, userId, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(CASE_ID_1, "", CASE_ROLE_1);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.USER_ID_INVALID));

        // check data has not been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(0, caseRoles.size());

        verifyAuditForAddCaseUserRoles(HttpStatus.BAD_REQUEST, caseUserRoles);
    }

    // RDM-8606: AC-6
    @Test
    @DisplayName("addCaseUserRoles: AC-6: must return an error response when the request is made from an un-authorised application")
    void addCaseUserRoles_shouldThrowExceptionWhenCalledFromUnauthorisedApp() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10006"; // don't need the users to exist in the repository but want unique for each AC

        // override s2s token in HTTP headers
        HttpHeaders httpHeaders = createHttpHeaders();
        httpHeaders.set(SecurityUtils.SERVICE_AUTHORIZATION, "Bearer " + MockUtils.generateDummyS2SToken(UNAUTHORISED_ADD_SERVICE));

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_1, userId, CASE_ROLE_1);
        caseUserRoles.add(caseUserRole1);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(httpHeaders))
            .andExpect(status().isForbidden())
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION));

        // check data has not been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(0, caseRoles.size());

        verifyAuditForAddCaseUserRoles(HttpStatus.FORBIDDEN, caseUserRoles);
    }

    // RDM-8606: AC-7
    @Test
    @DisplayName("addCaseUserRoles: AC-7: Must return an error response for a malformed Case Role provided")
    void addCaseUserRoles_shouldThrowExceptionWhenInvalidCaseRolePassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10007"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_1, userId, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(CASE_ID_1, userId, INVALID_CASE_ROLE);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.CASE_ROLE_FORMAT_INVALID));

        // check data has not been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(0, caseRoles.size());

        verifyAuditForAddCaseUserRoles(HttpStatus.BAD_REQUEST, caseUserRoles);
    }

    // RDM-8606: AC-8
    @Test
    @DisplayName("addCaseUserRoles: AC-8: Must return an error response for a missing Case Role")
    void addCaseUserRoles_shouldThrowExceptionWhenCaseRoleNotPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10008"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_1, userId, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(CASE_ID_1, userId, null);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.CASE_ROLE_FORMAT_INVALID));

        // check data has not been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(0, caseRoles.size());

        verifyAuditForAddCaseUserRoles(HttpStatus.BAD_REQUEST, caseUserRoles);
    }

    // RDM-8606: null list
    @Test
    @DisplayName("addCaseUserRoles: null: should throw exception")
    void addCaseUserRoles_shouldThrowExceptionWhenNullListPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(null)))
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.EMPTY_CASE_USER_ROLE_LIST));

        verifyAuditForAddCaseUserRoles(HttpStatus.BAD_REQUEST, null);
    }

    // RDM-8606: empty-list
    @Test
    @DisplayName("addCaseUserRoles: empty-list: should throw exception")
    void addCaseUserRoles_shouldThrowExceptionWhenEmptyListPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.EMPTY_CASE_USER_ROLE_LIST));

        verifyAuditForAddCaseUserRoles(HttpStatus.BAD_REQUEST, caseUserRoles);
    }

    // RDM-8606: case not found
    @Test
    @DisplayName("addCaseUserRoles: case not found: should throw exception")
    void addCaseUserRoles_shouldThrowExceptionWhenCaseNotFound() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "1111"; // don't need the users to exist in the repository but want unique for each AC
        String caseReferenceValidButNonExistent = "1111222233334444";

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(caseReferenceValidButNonExistent, userId, CASE_ROLE_1);
        caseUserRoles.add(caseUserRole1);

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isNotFound())
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString("No case found for reference: 1111222233334444"));

        // NB: usually audit for HttpStatus.NOT_FOUND will be suppressed by applicationParams.auditLogIgnoreStatuses
        verifyAuditForAddCaseUserRoles(HttpStatus.NOT_FOUND, caseUserRoles);
    }

    // RDM-8606: duplicate
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql"
    })
    @DisplayName("addCaseUserRoles: duplicate: should not generate duplicates")
    void addCaseUserRoles_shouldAddSingleCaseUserRoleWhenDuplicatePassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "2222"; // don't need the users to exist in the repository but want unique for each test

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_1, userId, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(CASE_ID_1, userId, CASE_ROLE_1);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        final MvcResult result = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isOk())
            .andReturn();

        // ASSERT
        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        AddCaseAssignedUserRolesResponse response = mapper.readValue(content, AddCaseAssignedUserRolesResponse.class);
        assertNotNull("Response should not be null", response);
        assertEquals("Success message should be returned", ADD_SUCCESS_MESSAGE, response.getStatus());

        // check data has been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(1, caseRoles.size());
        assertThat(caseRoles, hasItems(CASE_ROLE_1));

        verifyAuditForAddCaseUserRoles(HttpStatus.OK, caseUserRoles);
    }

    // RDM-8606: multiple
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql"
    })
    @DisplayName("addCaseUserRoles: multiple: should allow multiple CaseUserRoles to be added in single call")
    void addCaseUserRoles_shouldAddMultipleCaseUserRoles() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "3333"; // don't need the users to exist in the repository but want unique for each test

        // override s2s token in HTTP headers to check it also supports S2S token without bearer
        HttpHeaders httpHeaders = createHttpHeaders();
        httpHeaders.set(SecurityUtils.SERVICE_AUTHORIZATION, MockUtils.generateDummyS2SToken(AUTHORISED_ADD_SERVICE_2));

        List<CaseAssignedUserRole> caseUserRoles = Lists.newArrayList();
        CaseAssignedUserRole caseUserRole1 = new CaseAssignedUserRole(CASE_ID_1, userId, CASE_ROLE_1);
        CaseAssignedUserRole caseUserRole2 = new CaseAssignedUserRole(CASE_ID_1, userId, CASE_ROLE_2);
        caseUserRoles.add(caseUserRole1);
        caseUserRoles.add(caseUserRole2);

        // ACT
        final MvcResult result = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesResource(caseUserRoles)))
            .headers(httpHeaders))
            .andExpect(status().isOk())
            .andReturn();

        // ASSERT
        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        AddCaseAssignedUserRolesResponse response = mapper.readValue(content, AddCaseAssignedUserRolesResponse.class);
        assertNotNull("Response should not be null", response);
        assertEquals("Success message should be returned", ADD_SUCCESS_MESSAGE, response.getStatus());

        // check data has been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(2, caseRoles.size());
        assertThat(caseRoles, hasItems(CASE_ROLE_1));
        assertThat(caseRoles, hasItems(CASE_ROLE_2));

        verifyAuditForAddCaseUserRoles(HttpStatus.OK, caseUserRoles);
    }

    // AC-1
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql",
        "classpath:sql/insert_case_users_valid_case_ids.sql"
    })
    void getUserCaseRolesAssignedToUser() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, caseworkerCaa);

        final MvcResult result = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, CASE_IDS)
            .param(PARAM_USER_IDS, USER_IDS)
            .headers(createHttpHeaders()))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseAssignedUserRolesResource caseAssignedUserRolesResource = mapper.readValue(content, CaseAssignedUserRolesResource.class);
        assertNotNull("Case Assigned User Roles should not be null", caseAssignedUserRolesResource);

        verifyAuditForGetCaseUserRoles(HttpStatus.OK, CASE_IDS, USER_IDS);
    }

    // AC-2
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql",
        "classpath:sql/insert_case_users_valid_case_ids.sql"
    })
    void shouldGetSelfCaseUserRolesAssigned() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        final MvcResult result = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, CASE_IDS)
            .param(PARAM_USER_IDS, USER_IDS_1)
            .headers(createHttpHeaders()))
            .andExpect(status().isOk())
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

        verifyAuditForGetCaseUserRoles(HttpStatus.OK, CASE_IDS, USER_IDS_1);
    }

    // AC-3
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql",
        "classpath:sql/insert_case_users_valid_case_ids.sql"
    })
    void shouldGetAllUserCaseRolesRelatingToAllUsersWhenNoUserIDPassedForPassedCaseId() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, caseworkerCaa);

        final MvcResult result = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, CASE_ID_2)
            .headers(createHttpHeaders()))
            .andExpect(status().isOk())
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

        verifyAuditForGetCaseUserRoles(HttpStatus.OK, CASE_ID_2, null);
    }

    // AC-4
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql",
        "classpath:sql/insert_case_users_valid_case_ids.sql"
    })
    void shouldGetAllUserCaseRolesRelatingToAllUsersWhenNoUserIDPassedForListOfCaseIds() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, caseworkerCaa);

        final MvcResult result = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, CASE_IDS)
            .headers(createHttpHeaders()))
            .andExpect(status().isOk())
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

        verifyAuditForGetCaseUserRoles(HttpStatus.OK, CASE_IDS, null);
    }

    // AC-5
    @Test
    void shouldThrowExceptionWhenEmptyCaseIDListPassed() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, caseworkerCaa);

        Exception exception = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, "")
            .param(PARAM_USER_IDS, USER_IDS)
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();

        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.EMPTY_CASE_ID_LIST));

        verifyAuditForGetCaseUserRoles(HttpStatus.BAD_REQUEST, null, USER_IDS);
    }

    @Test
    void shouldThrowExceptionWhenCaseIDNotPassed() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, caseworkerCaa);

        mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_USER_IDS, USER_IDS)
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn();
    }

    // AC-6
    @Test
    void shouldThrowExceptionWhenInvalidCaseIDIsPassed() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, caseworkerCaa);

        Exception exception = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, INVALID_CASE_ID.toString())
            .param(PARAM_USER_IDS, USER_IDS)
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();

        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.CASE_ID_INVALID));

        verifyAuditForGetCaseUserRoles(HttpStatus.BAD_REQUEST, INVALID_CASE_ID.toString(), USER_IDS);
    }

    // AC-7
    @Test
    void shouldThrowExceptionWhenInvalidUserIdDataPassed() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC, caseworkerCaa);

        Exception exception = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, CASE_IDS)
            .param(PARAM_USER_IDS, INVALID_USER_IDS)
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();

        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.USER_ID_INVALID));

        verifyAuditForGetCaseUserRoles(HttpStatus.BAD_REQUEST, CASE_IDS, INVALID_USER_IDS);
    }

    // AC-8
    @Test
    void shouldThrowExceptionWhenInvokingUserHasNoPrivileges() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        Exception exception = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, CASE_IDS)
            .param(PARAM_USER_IDS, USER_IDS)
            .headers(createHttpHeaders()))
            .andExpect(status().isForbidden())
            .andReturn().getResolvedException();

        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(OTHER_USER_CASE_ROLE_ACCESS_NOT_GRANTED));

        verifyAuditForGetCaseUserRoles(HttpStatus.FORBIDDEN, CASE_IDS, USER_IDS.replace(" ", ""));
    }

    @Test
    void shouldThrowExceptionWhenUserRequestedForSelfCaseRoleAccessAlongWithOtherUsers() throws Exception {
        MockUtils.setSecurityAuthorities("89000", authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        Exception exception = mockMvc.perform(get(getCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .param(PARAM_CASE_IDS, CASE_IDS)
            .param(PARAM_USER_IDS, USER_IDS)
            .headers(createHttpHeaders()))
            .andExpect(status().isForbidden())
            .andReturn().getResolvedException();

        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(OTHER_USER_CASE_ROLE_ACCESS_NOT_GRANTED));

        verifyAuditForGetCaseUserRoles(HttpStatus.FORBIDDEN, CASE_IDS, USER_IDS);
    }

    private HttpHeaders createHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(AUTHORIZATION, "Bearer user1");
        String s2SToken = MockUtils.generateDummyS2SToken(AUTHORISED_ADD_SERVICE_1);
        headers.add(SERVICE_AUTHORIZATION, "Bearer " + s2SToken);
        return headers;
    }

    private void verifyAuditForAddCaseUserRoles(HttpStatus status, List<CaseAssignedUserRole> caseUserRoles) {
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.ADD_CASE_ASSIGNED_USER_ROLES.getLabel()));
        assertThat(captor.getValue().getHttpStatus(), is(status.value()));
        assertThat(captor.getValue().getPath(), is(postCaseAssignedUserRoles));
        assertThat(captor.getValue().getHttpMethod(), is(HttpMethod.POST.name()));

        if (caseUserRoles != null) {
            String caseIds = caseUserRoles.stream().map(CaseAssignedUserRole::getCaseDataId)
                .collect(Collectors.joining(CASE_ID_SEPARATOR));
            String caseRoles = caseUserRoles.stream().map(CaseAssignedUserRole::getCaseRole)
                .collect(Collectors.joining(CASE_ID_SEPARATOR));
            String userIds = caseUserRoles.stream().map(CaseAssignedUserRole::getUserId)
                .collect(Collectors.joining(CASE_ID_SEPARATOR));

            assertThat(captor.getValue().getCaseId(), is(caseIds));
            assertThat(StringUtils.join(captor.getValue().getTargetCaseRoles(), CASE_ID_SEPARATOR), is(caseRoles));
            assertThat(captor.getValue().getTargetIdamId(), is(userIds));
        }
    }

    private void verifyAuditForGetCaseUserRoles(HttpStatus status, String caseIds, String userIds) {
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.GET_CASE_ASSIGNED_USER_ROLES.getLabel()));
        assertThat(captor.getValue().getHttpStatus(), is(status.value()));
        assertThat(captor.getValue().getPath(), is(getCaseAssignedUserRoles));
        assertThat(captor.getValue().getHttpMethod(), is(HttpMethod.GET.name()));

        if (caseIds != null) {
            assertThat(captor.getValue().getCaseId(), is(trimSpacesFromCsvValues(caseIds)));
        }
        if (userIds != null) {
            assertThat(captor.getValue().getTargetIdamId(), is(trimSpacesFromCsvValues(userIds)));
        }
    }

    private String trimSpacesFromCsvValues(String csvInput) {
        return Arrays.stream(csvInput.split(CASE_ID_SEPARATOR))
            .map(String::trim)
            .collect(Collectors.joining(CASE_ID_SEPARATOR));
    }

}
