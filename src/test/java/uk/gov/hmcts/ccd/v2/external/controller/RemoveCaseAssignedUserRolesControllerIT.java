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
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.v2.external.controller.CaseAssignedUserRolesController.REMOVE_SUCCESS_MESSAGE;

class RemoveCaseAssignedUserRolesControllerIT extends BaseCaseAssignedUserRolesControllerIT {

    private final String caseAssignedUserRoles = "/case-users";

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql",
    })
    @DisplayName(
        "AC-1: must successfully remove a user and case role for a specific case by a user calling through/from an "
            + "authorised application"
    )
    void shouldRemoveCaseUserRoleForAuthorisedApp() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10001"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1)
        );
        // ACT
        final MvcResult result = mockMvc.perform(delete(caseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isOk())
            .andReturn();

        // ASSERT
        String content = result.getResponse().getContentAsString();
        CaseAssignedUserRolesResponse response = mapper.readValue(content, CaseAssignedUserRolesResponse.class);
        assertEquals("Success message should be returned", REMOVE_SUCCESS_MESSAGE, response.getStatus());

        // check data has been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(0, caseRoles.size());

        verifyAuditForRemoveCaseUserRoles(HttpStatus.OK, caseUserRoles);
    }

    @Test
    @DisplayName("AC-3: Must return an error response for a missing Case ID")
    void removeCaseUserRoles_shouldThrowExceptionWhenCaseIDNotPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10002"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1),
            new CaseAssignedUserRoleWithOrganisation(null, userId, CASE_ROLE_1)
        );

        // ACT
        Exception exception = mockMvc.perform(delete(caseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();

        // ASSERT
        assertThat(exception.getMessage(), containsString(V2.Error.CASE_ID_INVALID));

        verifyAuditForRemoveCaseUserRoles(HttpStatus.BAD_REQUEST, caseUserRoles);
    }

    @Test
    @DisplayName("AC-4: Must return an error response for a malformed Case ID")
    void removeCaseUserRoles_shouldThrowExceptionWhenInvalidCaseIDPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10003"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1),
            new CaseAssignedUserRoleWithOrganisation(INVALID_CASE_ID.toString(), userId, CASE_ROLE_1)
        );

        // ACT
        Exception exception = mockMvc.perform(delete(caseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();

        // ASSERT
        assertThat(exception.getMessage(), containsString(V2.Error.CASE_ID_INVALID));

        verifyAuditForRemoveCaseUserRoles(HttpStatus.BAD_REQUEST, caseUserRoles);
    }

    @Test
    @DisplayName("AC-5: Must return an error response for a missing User ID")
    void removeCaseUserRoles_shouldThrowExceptionWhenUserIDIsNotPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10005"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1),
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, null, CASE_ROLE_1)
        );

        // ACT
        Exception exception = mockMvc.perform(delete(caseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();

        // ASSERT
        assertThat(exception.getMessage(), containsString(V2.Error.USER_ID_INVALID));

        verifyAuditForRemoveCaseUserRoles(HttpStatus.BAD_REQUEST, caseUserRoles);
    }

    @Test
    @DisplayName("AC-6: Must return an error response for a malformed User ID Provided")
    void removeCaseUserRoles_shouldThrowExceptionWhenInvalidUserIDPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10005"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
                new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1),
                new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, "", CASE_ROLE_1)
        );

        // ACT
        Exception exception = mockMvc.perform(delete(caseAssignedUserRoles)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
                .headers(createHttpHeaders()))
                .andExpect(status().isBadRequest())
                .andReturn().getResolvedException();

        // ASSERT
        assertThat(exception.getMessage(), containsString(V2.Error.USER_ID_INVALID));

        verifyAuditForRemoveCaseUserRoles(HttpStatus.BAD_REQUEST, caseUserRoles);
    }

    @Test
    @DisplayName("AC-7: must return an error response when the request is made from an un-authorised application")
    void removeCaseUserRoles_shouldThrowExceptionWhenCalledFromUnauthorisedApp() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10006"; // don't need the users to exist in the repository but want unique for each AC

        // override s2s token in HTTP headers
        HttpHeaders httpHeaders = createHttpHeaders();
        httpHeaders.set(SecurityUtils.SERVICE_AUTHORIZATION, "Bearer " + MockUtils.generateDummyS2SToken(
            UNAUTHORISED_ADD_SERVICE));

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1)
        );

        // ACT
        Exception exception = mockMvc.perform(delete(caseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
            .headers(httpHeaders))
            .andExpect(status().isForbidden())
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.CLIENT_SERVICE_NOT_AUTHORISED_FOR_OPERATION));

        verifyAuditForRemoveCaseUserRoles(HttpStatus.FORBIDDEN, caseUserRoles);
    }

    @Test
    @DisplayName("AC-8: Must return an error response for a malformed Case Role provided")
    void removeCaseUserRoles_shouldThrowExceptionWhenInvalidCaseRolePassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10007"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1),
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, INVALID_CASE_ROLE)
        );

        // ACT
        Exception exception = mockMvc.perform(delete(caseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();

        // ASSERT
        assertThat(exception.getMessage(), containsString(V2.Error.CASE_ROLE_FORMAT_INVALID));

        verifyAuditForRemoveCaseUserRoles(HttpStatus.BAD_REQUEST, caseUserRoles);
    }

    @Test
    @DisplayName("AC-9: Must return an error response for a missing Case Role")
    void removeCaseUserRoles_shouldThrowExceptionWhenCaseRoleNotPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "10008"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1),
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, null)
        );

        // ACT
        Exception exception = mockMvc.perform(delete(caseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString(V2.Error.CASE_ROLE_FORMAT_INVALID));

        verifyAuditForRemoveCaseUserRoles(HttpStatus.BAD_REQUEST, caseUserRoles);
    }

    @Test
    @DisplayName("AC-12 case not found: should throw exception")
    void removeCaseUserRoles_shouldThrowExceptionWhenCaseNotFound() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "1111"; // don't need the users to exist in the repository but want unique for each AC
        String caseReferenceValidButNonExistent = "1111222233334444";

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(caseReferenceValidButNonExistent, userId, CASE_ROLE_1)
        );

        // ACT
        Exception exception = mockMvc.perform(delete(caseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
            .headers(createHttpHeaders()))
            .andExpect(status().isNotFound())
            .andReturn().getResolvedException();

        // ASSERT
        assertNotNull(exception);
        assertThat(exception.getMessage(), containsString("No case found for reference: 1111222233334444"));

        // NB: usually audit for HttpStatus.NOT_FOUND will be suppressed by applicationParams.auditLogIgnoreStatuses
        verifyAuditForRemoveCaseUserRoles(HttpStatus.NOT_FOUND, caseUserRoles);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql"
    })
    @DisplayName("duplicate: should remove cases even though request has duplicates")
    void removeCaseUserRoles_shouldRemoveCaseUserRoleWhenDuplicatePassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "2222"; // don't need the users to exist in the repository but want unique for each test

        addCaseUserRoles(List.of(new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1)));

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1),
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1)
        );

        // ACT
        final MvcResult result = mockMvc.perform(delete(caseAssignedUserRoles)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
                .headers(createHttpHeaders()))
                .andExpect(status().isOk())
                .andReturn();

        // ASSERT
        String content = result.getResponse().getContentAsString();
        CaseAssignedUserRolesResponse response = mapper.readValue(content, CaseAssignedUserRolesResponse.class);
        assertEquals("Success message should be returned", REMOVE_SUCCESS_MESSAGE, response.getStatus());

        // check data has been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(0, caseRoles.size());

        verifyAuditForRemoveCaseUserRoles(HttpStatus.OK, caseUserRoles);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql"
    })
    @DisplayName("ACA-2: multiple: should allow multiple CaseUserRoles to be removed in single call")
    void removeCaseUserRoles_shouldAddMultipleCaseUserRoles() throws Exception {
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

        addCaseUserRoles(caseUserRoles);

        // check data has been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(2, caseRoles.size());
        assertThat(caseRoles, hasItems(CASE_ROLE_1));
        assertThat(caseRoles, hasItems(CASE_ROLE_2));

        // ACT
        final MvcResult result = mockMvc.perform(delete(caseAssignedUserRoles)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
                .headers(createHttpHeaders()))
                .andExpect(status().isOk())
                .andReturn();

        // ASSERT
        String content = result.getResponse().getContentAsString();
        CaseAssignedUserRolesResponse response = mapper.readValue(content, CaseAssignedUserRolesResponse.class);
        assertEquals("Success message should be returned", REMOVE_SUCCESS_MESSAGE, response.getStatus());

        // check data has been removed
        caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(0, caseRoles.size());

        verifyAuditForRemoveCaseUserRoles(HttpStatus.OK, caseUserRoles);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql"
    })
    @DisplayName(
        "AC-13: must successfully decrease Assigned User Count when removing a user and case role for a specific case"
    )
    void removeCaseUserRoles_shouldIncrementOrganisationUserCountForNewRelationships() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId1 = "8842-001-1"; // don't need the users to exist in the repository but want unique for each AC
        String userId2 = "8842-001-2";

        final List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = List.of(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId1, CASE_ROLE_1, ORGANISATION_ID_1),
                new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId2, CASE_ROLE_1, ORGANISATION_ID_1)
        );
        final long prerequisiteCounter = getOrgUserCountFromSupData(CASE_ID_1, ORGANISATION_ID_1);

        addCaseUserRoles(caseUserRoles);
        final long afterAddCounter = getOrgUserCountFromSupData(CASE_ID_1, ORGANISATION_ID_1);
        assertEquals(prerequisiteCounter + 2L,  afterAddCounter);

        // ACT & ASSERT
        // first call
        mockMvc.perform(delete(caseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(
                    List.of(new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId1, CASE_ROLE_1,
                        ORGANISATION_ID_1)))))
            .headers(createHttpHeaders()))
            .andExpect(status().isOk())
            .andReturn();
        // check data has been removed
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId1);
        assertEquals(0, caseRoles.size());
        // verify counter
        final long verifyCounter1 = getOrgUserCountFromSupData(CASE_ID_1, ORGANISATION_ID_1);
        assertEquals(afterAddCounter - 1L, verifyCounter1); // decremented

        // second call (repeat)
        mockMvc.perform(delete(caseAssignedUserRoles)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(
                        List.of(new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId1, CASE_ROLE_1,
                            ORGANISATION_ID_1)))))
                .headers(createHttpHeaders()))
                .andExpect(status().isOk())
                .andReturn();
        // verify counter
        final long verifyCounter2 = getOrgUserCountFromSupData(CASE_ID_1, ORGANISATION_ID_1);
        assertEquals(verifyCounter1, verifyCounter2); // unchanged

        // third call (different user)
        mockMvc.perform(delete(caseAssignedUserRoles)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(
                        List.of(new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId2, CASE_ROLE_1,
                            ORGANISATION_ID_1)))))
                .headers(createHttpHeaders()))
                .andExpect(status().isOk())
                .andReturn();
        // third verify counter
        final long verifyCounter3 = getOrgUserCountFromSupData(CASE_ID_1, ORGANISATION_ID_1);

        assertEquals(verifyCounter2 - 1L, verifyCounter3); // decremented

        // check data has been saved
        List<String> caseRoles1 = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId1);
        assertEquals(0, caseRoles1.size());
        List<String> caseRoles2 = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId2);
        assertEquals(0, caseRoles2.size());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql",
        "classpath:sql/insert_case_users_valid_case_ids.sql"
    })
    @DisplayName(
        "AC - 14: must not decrease Assigned User Count when unassigning a user and case role for a specific case"
                + " if there was already a different case user role assignment"
    )
    void removeCaseUserRoles_shouldNotDecrementOrganisationUserCountForExistingRelationship() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "8842-002"; // don't need the users to exist in the repository but want unique for each AC

        List<CaseAssignedUserRoleWithOrganisation> caseUserRoles = Lists.newArrayList(
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_1, ORGANISATION_ID_1),
            new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_2, ORGANISATION_ID_1)
        );

        addCaseUserRoles(caseUserRoles);

        // initial user count
        final long prerequisiteCounter = getOrgUserCountFromSupData(CASE_ID_1, ORGANISATION_ID_1);

        // ACT
        // make test call
        mockMvc.perform(delete(caseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(
                    List.of(new CaseAssignedUserRoleWithOrganisation(CASE_ID_1, userId, CASE_ROLE_2,
                        ORGANISATION_ID_1))
            )))
            .headers(createHttpHeaders()))
            .andExpect(status().isOk())
            .andReturn();
        // verify counter
        final long verifyCounter = getOrgUserCountFromSupData(CASE_ID_1, ORGANISATION_ID_1);

        // ASSERT
        assertEquals(prerequisiteCounter, verifyCounter); // unchanged

        // check data has been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(1, caseRoles.size()); // i.e. 1 + 1: one added + one existing
        assertThat(caseRoles, hasItems(CASE_ROLE_1));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql"
    })
    @DisplayName(
        "AC - 15: must not decrease Assigned User Count when when no organisation ID is provided"
    )
    void removeCaseUserRoles_shouldNotDecrementOrganisationUserCountersWhenNoOrganisationSpecified() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "8842-003"; // don't need the users to exist in the repository but want unique for each AC

        // set a default count for any organisation
        supplementaryDataRepository.setSupplementaryData(CASE_ID_EXTRA, getOrgUserCountSupDataKey(ORGANISATION_ID_2),
            0L);

        addCaseUserRoles(List.of(new CaseAssignedUserRoleWithOrganisation(CASE_ID_EXTRA, userId, CASE_ROLE_1,
            ORGANISATION_ID_2)));

        // initial user counters
        final Object orgUserCountersBefore = supplementaryDataRepository.findSupplementaryData(CASE_ID_EXTRA, null)
                .getResponse().getOrDefault(ORGANISATION_ASSIGNED_USER_COUNTER_KEY, null);

        // ACT
        // make test call
        mockMvc.perform(delete(caseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(
                    List.of(new CaseAssignedUserRoleWithOrganisation(CASE_ID_EXTRA, userId, CASE_ROLE_1)))))
            .headers(createHttpHeaders()))
            .andExpect(status().isOk())
            .andReturn();
        // verify counters
        final Object orgUserCountersAfter = supplementaryDataRepository.findSupplementaryData(CASE_ID_EXTRA, null)
            .getResponse().getOrDefault(ORGANISATION_ASSIGNED_USER_COUNTER_KEY, null);

        // ASSERT
        assertEquals(orgUserCountersBefore, orgUserCountersAfter); // unchanged

        // check data has been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_EXTRA), userId);
        assertTrue(caseRoles.isEmpty());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {
        "classpath:sql/insert_cases_with_valid_case_ids.sql"
    })
    @DisplayName(
        "AC-16:  must reject request when an invalid Organisation ID is provided"
    )
    void removeCaseUserRoles_shouldThrowExceptionWhenInvalidOrganisationIDPassed() throws Exception {
        // ARRANGE
        MockUtils.setSecurityAuthorities(authentication);
        String userId = "8442-004"; // don't need the users to exist in the repository but want unique for each AC

        // set a default count for any organisation
        supplementaryDataRepository.setSupplementaryData(CASE_ID_EXTRA, getOrgUserCountSupDataKey(ORGANISATION_ID_2),
            0L);

        addCaseUserRoles(List.of(new CaseAssignedUserRoleWithOrganisation(CASE_ID_EXTRA, userId, CASE_ROLE_1,
            ORGANISATION_ID_2)));

        // initial user counters
        final Object orgUserCountersBefore = supplementaryDataRepository.findSupplementaryData(CASE_ID_EXTRA, null)
                .getResponse().getOrDefault(ORGANISATION_ASSIGNED_USER_COUNTER_KEY, null);
        // ACT
        // make test call
        Exception exception = mockMvc.perform(delete(caseAssignedUserRoles)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(
                    List.of(new CaseAssignedUserRoleWithOrganisation(CASE_ID_EXTRA, userId, CASE_ROLE_1,
                        INVALID_ORGANISATION_ID))
            )))
            .headers(createHttpHeaders()))
            .andExpect(status().isBadRequest())
            .andReturn().getResolvedException();
        // verify counters
        final Object orgUserCountersAfter = supplementaryDataRepository.findSupplementaryData(CASE_ID_EXTRA, null)
            .getResponse().getOrDefault(ORGANISATION_ASSIGNED_USER_COUNTER_KEY, null);

        // ASSERT
        assertThat(exception.getMessage(), containsString(V2.Error.ORGANISATION_ID_INVALID));

        assertEquals(orgUserCountersBefore, orgUserCountersAfter); // unchanged

        // check data has not been saved
        List<String> caseRoles = caseUserRepository.findCaseRoles(Long.valueOf(CASE_ID_1), userId);
        assertEquals(0, caseRoles.size());
    }

    private void addCaseUserRoles(List<CaseAssignedUserRoleWithOrganisation> caseUserRoles) throws Exception  {

        mockMvc.perform(post(caseAssignedUserRoles)
                .contentType(JSON_CONTENT_TYPE)
                .content(mapper.writeValueAsBytes(new CaseAssignedUserRolesRequest(caseUserRoles)))
                .headers(createHttpHeaders()))
                .andExpect(status().isCreated());

        verifyAuditForAddCaseUserRoles(HttpStatus.CREATED, caseUserRoles);
    }

}
