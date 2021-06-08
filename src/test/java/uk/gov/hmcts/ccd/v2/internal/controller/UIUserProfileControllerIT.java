package uk.gov.hmcts.ccd.v2.internal.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
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
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.UserProfileViewResource;

import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UIUserProfileControllerIT extends WireMockBaseTest {

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
        final MvcResult result =
            mockMvc.perform(get("/internal/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .header(AUTHORIZATION, "Bearer user1")
                .header(V2.EXPERIMENTAL_HEADER, "true"))
                .andExpect(status().is(200))
                .andReturn();
        UserProfileViewResource response =
            mapper.readValue(result.getResponse().getContentAsString(), UserProfileViewResource.class);

        assertAll(
            () -> assertThat(response.getUserProfile().getUser().getIdamProperties().getId(), is("123")),
            () -> assertThat(response.getUserProfile().getUser().getIdamProperties().getEmail(),
                is("Cloud.Strife@test.com")),
            () -> assertThat(response.getUserProfile().getJurisdictions().length, is(4)),
            () -> assertThat(response.getUserProfile().getJurisdictions()[0].getCaseTypeDefinitions().size(),
                is(1)),
            () -> assertThat(response.getUserProfile().getJurisdictions()[0].getCaseTypeDefinitions()
                .get(0).getStates().size(), is(3)),
            () -> assertThat(response.getUserProfile().getDefaultSettings().getWorkbasketDefault().getStateId(),
                is("CaseCreated")),
            () -> assertThat(response.getUserProfile().getDefaultSettings().getWorkbasketDefault().getJurisdictionId(),
                is("PROBATE"))
        );

    }

}
