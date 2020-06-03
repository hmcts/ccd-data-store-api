package uk.gov.hmcts.ccd.endpoint.ui;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.aggregated.IdamProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.UserProfile;
import uk.gov.hmcts.ccd.domain.model.aggregated.WorkbasketDefault;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class UserProfileEndpointTest extends WireMockBaseTest {
    private static final String URL = "/caseworkers/user1/profile";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @Before
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void validUser() throws Exception {
        final MvcResult result = mockMvc.perform(get(URL)
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        final UserProfile userProfile = mapper.readValue(result.getResponse().getContentAsString(), UserProfile.class);

        final IdamProperties idamProperties = userProfile.getUser().getIdamProperties();
        assertEquals("123", idamProperties.getId());
        assertEquals("Cloud.Strife@test.com", idamProperties.getEmail());
        assertEquals("Cloud", idamProperties.getForename());
        assertEquals("Strife", idamProperties.getSurname());
        assertEquals("caseworker", idamProperties.getRoles()[0]);

        assertNull(userProfile.getChannels());

        final JurisdictionDisplayProperties[] jurisdictions = userProfile.getJurisdictions();
        assertEquals(3, jurisdictions.length);

        final JurisdictionDisplayProperties jurisdiction = jurisdictions[0];
        assertEquals("PROBATE", jurisdiction.getId());
        assertEquals("Test", jurisdiction.getName());
        assertEquals("Test Jurisdiction", jurisdiction.getDescription());

        final WorkbasketDefault workbasketDefault = userProfile.getDefaultSettings().getWorkbasketDefault();
        assertEquals("PROBATE", workbasketDefault.getJurisdictionId());
        assertEquals("TestAddressBookCase", workbasketDefault.getCaseTypeId());
        assertEquals("CaseCreated", workbasketDefault.getStateId());

        final JurisdictionDisplayProperties jurisdiction2 = jurisdictions[1];
        assertEquals("DIVORCE", jurisdiction2.getId());
        assertEquals("Test 2", jurisdiction2.getName());
        assertEquals("Test Jurisdiction 2", jurisdiction2.getDescription());

        final JurisdictionDisplayProperties jurisdiction3 = jurisdictions[2];
        assertEquals("SSCS", jurisdiction3.getId());
        assertEquals("Test 3", jurisdiction3.getName());
        assertEquals("Test Jurisdiction 3", jurisdiction3.getDescription());
    }
}
