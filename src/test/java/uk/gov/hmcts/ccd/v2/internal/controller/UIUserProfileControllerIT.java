package uk.gov.hmcts.ccd.v2.internal.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.UserProfileViewResource;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.GET_ROLE_ASSIGNMENTS_PREFIX;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.organisationalRoleAssignmentJson;
import static uk.gov.hmcts.ccd.test.RoleAssignmentsHelper.roleAssignmentResponseJson;

public class UIUserProfileControllerIT extends WireMockBaseTest {

    private static final String CASE_ID_1 = "1504259907353552";
    private static final String CASE_TYPE = "GrantOfRepresentation";
    private static final String JURISDICTION = "PROBATE";
    private static final String CASE_ROLE_1 = "[DEFENDANT]";

    @Inject
    private WebApplicationContext wac;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {
            "classpath:sql/insert_cases.sql", "classpath:sql/insert_case_users.sql"})
    public void shouldReturnWorkBasketInputFilterWithCaseRoleStates() throws Exception {
        if (applicationParams.getEnableAttributeBasedAccessControl()) {
            String userId = "123";
            String roleAssignmentResponseJson = roleAssignmentResponseJson(
                organisationalRoleAssignmentJson(CASE_ROLE_1, JURISDICTION, CASE_TYPE, CASE_ID_1),
                organisationalRoleAssignmentJson("idam:" + "caseworker-probate-solicitor", JURISDICTION,
                    CASE_TYPE, CASE_ID_1)
            );

            stubFor(WireMock.get(urlMatching(GET_ROLE_ASSIGNMENTS_PREFIX + userId))
                .willReturn(okJson(roleAssignmentResponseJson).withStatus(200)));
        }

        final MvcResult result =
            mockMvc.perform(get("/internal/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, "Bearer user1")
                .header(V2.EXPERIMENTAL_HEADER, "true"))
                .andExpect(status().is(200))
                .andReturn();
        UserProfileViewResource response =
            mapper.readValue(result.getResponse().getContentAsString(), UserProfileViewResource.class);

        JurisdictionDisplayProperties[] jurisdictions = response.getUserProfile().getJurisdictions();
        Optional<JurisdictionDisplayProperties> jurisdiction = Arrays.stream(jurisdictions)
            .filter(j -> j.getId().equals("Test Case Role")).findFirst();
        assertAll(
            () -> assertThat(response.getUserProfile().getUser().getIdamProperties().getId(), is("123")),
            () -> assertThat(response.getUserProfile().getUser().getIdamProperties().getEmail(),
                is("Cloud.Strife@test.com")),
            () -> assertThat(jurisdictions.length, is(4)),
            () -> assertThat(jurisdiction.get().getCaseTypeDefinitions().size(), is(1)),
            () -> assertThat("Should have 3 states", jurisdiction.get().getCaseTypeDefinitions()
                .get(0).getStates().size(), is(3)),
            () -> assertThat(response.getUserProfile().getDefaultSettings().getWorkbasketDefault().getStateId(),
                is("CaseCreated")),
            () -> assertThat(response.getUserProfile().getDefaultSettings().getWorkbasketDefault().getJurisdictionId(),
                is("PROBATE"))
        );

    }

}
