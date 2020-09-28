package uk.gov.hmcts.ccd.v2.external.controller;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.model.std.CaseAssignedUserRoleWithOrganisation;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.domain.CaseAssignedUserRolesRequest;
import uk.gov.hmcts.ccd.v2.external.domain.CaseAssignedUserRolesResponse;

import java.util.List;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController.ADD_SUCCESS_MESSAGE;

class AddCaseAssignedUserRolesControllerIT extends BaseCaseAssignedUserRolesControllerIT {

    // RDM-8606: AC-1
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql"
    })
    @DisplayName(
        "addCaseUserRoles: AC-1: must successfully assign a user and case role for a specific case by a user calling "
            + "through/from an authorised application"
    )
    void addCaseUserRoles_shouldAddCaseUserRoleForAuthorisedApp() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10001"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1)
        );

        // ACT
        final MvcResult result = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isCreated())
            .andReturn();

        // ASSERT
        assertEquals(result.getResponse().getContentAsString(), 201, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseAssignedUserRolesResponse response = mapper.readValue(content, CaseAssignedUserRolesResponse.class);
        assertNotNull("Response should not be null", response);
        assertEquals("Success message should be returned", ADD_SUCCESS_MESSAGE, response.getStatus());

        // check data has been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(1, caseRoles.size());
        assertThat(caseRoles, hasItems(CASE_ROLE_1));

        verifyAuditForAddCaseUserRoles(HttpStatus.CREATED, caseUserRoles);
    }

    // RDM-8606: AC-2
    @Test
    @DisplayName("addCaseUserRoles: AC-2: Must return an error response for a missing Case ID")
    void addCaseUserRoles_shouldThrowExceptionWhenCaseIDNotPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10002"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1),
            new CaseAssignedUserRoleWithOrganisation(null, userId, CASE_ROLE_1)
        );

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
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

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1),
            new CaseAssignedUserRoleWithOrganisation(INVALID_CASE_ID.toString(), userId, CASE_ROLE_1)
        );

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
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

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1),
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, null, CASE_ROLE_1)
        );

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
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

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1),
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, "", CASE_ROLE_1)
        );

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
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
    @DisplayName("addCaseUserRoles: AC-6: must return an error response when the request is made from an un-authorised"
        + " application")
    void addCaseUserRoles_shouldThrowExceptionWhenCalledFromUnauthorisedApp() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10006"; // don't need the users to exist in the repository but want unique for each AC

        // override s2s token in HTTP headers
        HttpHeaders httpHeaders = createHttpHeaders();
        httpHeaders.set(SecurityUtils.SERVICE_AUTHORIZATION, "Bearer "
            + MockUtils.generateDummyS2SToken(UNAUTHORISED_ADD_SERVICE));

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1)
        );

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
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

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1),
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, INVALID_CASE_ROLE)
        );

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
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

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1),
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, null)
        );

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
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
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(null)))
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

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList();

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
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

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(caseReferenceValidButNonExistent, userId, CASE_ROLE_1)
        );

        // ACT
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
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

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1),
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1)
        );

        // ACT
        final MvcResult result = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isCreated())
            .andReturn();

        // ASSERT
        assertEquals(result.getResponse().getContentAsString(), 201, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseAssignedUserRolesResponse response = mapper.readValue(content, CaseAssignedUserRolesResponse.class);
        assertNotNull("Response should not be null", response);
        assertEquals("Success message should be returned", ADD_SUCCESS_MESSAGE, response.getStatus());

        // check data has been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(1, caseRoles.size());
        assertThat(caseRoles, hasItems(CASE_ROLE_1));

        verifyAuditForAddCaseUserRoles(HttpStatus.CREATED, caseUserRoles);
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
        httpHeaders.set(SecurityUtils.SERVICE_AUTHORIZATION,
            MockUtils.generateDummyS2SToken(AUTHORISED_ADD_SERVICE_2));

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1),
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_2)
        );

        // ACT
        final MvcResult result = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
            .headers(httpHeaders))
            .andExpect(status().isCreated())
            .andReturn();

        // ASSERT
        assertEquals(result.getResponse().getContentAsString(), 201, result.getResponse().getStatus());
        String content = result.getResponse().getContentAsString();
        assertNotNull("Content Should not be null", content);
        CaseAssignedUserRolesResponse response = mapper.readValue(content, CaseAssignedUserRolesResponse.class);
        assertNotNull("Response should not be null", response);
        assertEquals("Success message should be returned", ADD_SUCCESS_MESSAGE, response.getStatus());

        // check data has been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(2, caseRoles.size());
        assertThat(caseRoles, hasItems(CASE_ROLE_1));
        assertThat(caseRoles, hasItems(CASE_ROLE_2));

        verifyAuditForAddCaseUserRoles(HttpStatus.CREATED, caseUserRoles);
    }

    // RDM-8842: AC-1
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql"
    })
    @DisplayName(
        "addCaseUserRoles: RDM-8442.AC-1: must successfully increment Assigned User Count when assigning a user and "
            + "case role for a specific case"
    )
    void addCaseUserRoles_shouldIncrementOrganisationUserCountForNewRelationships() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId1 = "8842-001-1"; // don't need the users to exist in the repository but want unique for each AC
        String userId2 = "8842-001-2";

        final List<CaseAssignedUserRoleWithOrganisation> caseUserRoles1 = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId1, CASE_ROLE_1, ORGANISATION_ID_1)
        );
        final List<CaseAssignedUserRoleWithOrganisation> caseUserRoles2 = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId2, CASE_ROLE_1, ORGANISATION_ID_1)
        );

        // ACT
        // initial user count
        final long prerequisiteCounter = getOrgUserCountFromSupData(CASE_ID_1, ORGANISATION_ID_1);
        // first call
        mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles1)))
            .headers(createHttpHeaders()))
            .andExpect(status().isCreated())
            .andReturn();
        // first verify counter
        final long verifyCounter1 = getOrgUserCountFromSupData(CASE_ID_1, ORGANISATION_ID_1);
        // second call (repeat)
        mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles1)))
            .headers(createHttpHeaders()))
            .andExpect(status().isCreated())
            .andReturn();
        // second verify counter
        final long verifyCounter2 = getOrgUserCountFromSupData(CASE_ID_1, ORGANISATION_ID_1);
        // third call (different user)
        mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles2)))
            .headers(createHttpHeaders()))
            .andExpect(status().isCreated())
            .andReturn();
        // third verify counter
        final long verifyCounter3 = getOrgUserCountFromSupData(CASE_ID_1, ORGANISATION_ID_1);

        // ASSERT
        assertEquals(prerequisiteCounter + 1L, verifyCounter1); // incremented
        assertEquals(verifyCounter1, verifyCounter2); // unchanged
        assertEquals(verifyCounter2 + 1L, verifyCounter3); // incremented

        // check data has been saved
        List<String> caseRoles1 = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId1);
        assertEquals(1, caseRoles1.size());
        assertThat(caseRoles1, hasItems(CASE_ROLE_1));
        List<String> caseRoles2 = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId2);
        assertEquals(1, caseRoles2.size());
        assertThat(caseRoles2, hasItems(CASE_ROLE_1));
    }

    // RDM-8842: AC-2
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql",
        "classpath:sql/insert_case_users_valid_case_ids.sql"
    })
    @DisplayName(
        "addCaseUserRoles: RDM-8442.AC-2: Must not increment Assigned User Count when assigning a user and case role"
            + " for a specific case if there was already a case user role assignment with the respective values in "
            + "the request"
    )
    void addCaseUserRoles_shouldNotIncrementOrganisationUserCountForExistingRelationship() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "8842-002"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_EXTRA, userId, CASE_ROLE_1, ORGANISATION_ID_2)
        );

        // NB: CASE_ID_EXTRA has existing role assigned for user defined in SQL file

        // ACT
        // initial user count
        final long prerequisiteCounter = getOrgUserCountFromSupData(CASE_ID_EXTRA, ORGANISATION_ID_2);
        // make test call
        mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isCreated())
            .andReturn();
        // verify counter
        final long verifyCounter = getOrgUserCountFromSupData(CASE_ID_EXTRA, ORGANISATION_ID_2);

        // ASSERT
        assertEquals(prerequisiteCounter, verifyCounter); // unchanged

        // check data has been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_EXTRA), userId);
        assertEquals(2, caseRoles.size()); // i.e. 1 + 1: one added + one existing
        assertThat(caseRoles, hasItems(CASE_ROLE_1));
    }

    // RDM-8842: AC-3
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql"
    })
    @DisplayName(
        "addCaseUserRoles: RDM-8442.AC-3: No organisation ID is provided by the user"
    )
    void addCaseUserRoles_shouldNotIncrementOrganisationUserCountersWhenNoOrganisationSpecified() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "8842-003"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_EXTRA, userId, CASE_ROLE_1)
        );

        // set a default count for any organisation
        supplementaryDataRepository.setSupplementaryData(CASE_ID_EXTRA, getOrgUserCountSupDataKey(ORGANISATION_ID_2),
            0L);

        // ACT
        // initial user counters
        final Object orgUserCountersBefore = supplementaryDataRepository.findSupplementaryData(CASE_ID_EXTRA, null)
            .getResponse().getOrDefault(ORGANISATION_ASSIGNED_USER_COUNTER_KEY, null);
        // make test call
        mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isCreated())
            .andReturn();
        // verify counters
        final Object orgUserCountersAfter = supplementaryDataRepository.findSupplementaryData(CASE_ID_EXTRA, null)
            .getResponse().getOrDefault(ORGANISATION_ASSIGNED_USER_COUNTER_KEY, null);

        // ASSERT
        assertEquals(orgUserCountersBefore, orgUserCountersAfter); // unchanged

        // check data has been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_EXTRA), userId);
        assertEquals(1, caseRoles.size());
        assertThat(caseRoles, hasItems(CASE_ROLE_1));
    }

    // RDM-8842: AC-4
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql"
    })
    @DisplayName(
        "addCaseUserRoles: RDM-8442.AC-4: Invalid Organisation ID provided"
    )
    void addCaseUserRoles_shouldThrowExceptionWhenInvalidOrganisationIDPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "8442-004"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_EXTRA, userId, CASE_ROLE_1, INVALID_ORGANISATION_ID)
        );

        // set a default count for any organisation
        supplementaryDataRepository.setSupplementaryData(CASE_ID_EXTRA, getOrgUserCountSupDataKey(ORGANISATION_ID_2),
            0L);

        // ACT
        // initial user counters
        final Object orgUserCountersBefore = supplementaryDataRepository.findSupplementaryData(CASE_ID_EXTRA, null)
            .getResponse().getOrDefault(ORGANISATION_ASSIGNED_USER_COUNTER_KEY, null);
        // make test call
        Exception exception = mockMvc.perform(post(postCaseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();
        // verify counters
        final Object orgUserCountersAfter = supplementaryDataRepository.findSupplementaryData(CASE_ID_EXTRA, null)
            .getResponse().getOrDefault(ORGANISATION_ASSIGNED_USER_COUNTER_KEY, null);

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.ORGANISATION_ID_INVALID));

        assertEquals(orgUserCountersBefore, orgUserCountersAfter); // unchanged

        // check data has not been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(0, caseRoles.size());
    }
}

