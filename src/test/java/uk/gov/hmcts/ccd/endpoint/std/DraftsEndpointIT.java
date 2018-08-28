package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

public class DraftsEndpointIT extends WireMockBaseTest {
    private static final String CTID = "TestAddressBookCase";
    private static final String ETID = "CreateCase";
    private static final String DID = "5";
    private static final String WRONG_DID = "6";
    private static final String JID = "PROBATE";
    private static final String TEST_EVENT_ID = "TEST_EVENT";
    private static final String UID = "0";
    private JsonNode data;

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Before
    public void setUp() throws IOException {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();

        data = mapper.readTree("{\n" +
                                "    \"PersonFirstName\": \"John\",\n" +
                                "    \"PersonLastName\": \"Smith\"\n" +
                                " }\n");
    }

    @Test
    public void shouldReturn201WhenSaveDraftForCaseworker() throws Exception {
        final String URL = "/caseworkers/" + UID + "/jurisdictions/" + JID + "/case-types/" + CTID + "/event-trigger/" + ETID + "/drafts";
        CaseDataContent caseDetailsToSave = newCaseDataContent()
            .withData(getData(data))
            .withEvent(anEvent()
                           .withEventId(TEST_EVENT_ID)
                           .build())
            .withToken(generateEventTokenNewCase(UID, JID, CTID, TEST_EVENT_ID))
            .build();

        final MvcResult mvcResult = mockMvc.perform(post(URL)
                                                        .contentType(JSON_CONTENT_TYPE)
                                                        .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals("Incorrect Response Status Code", 201, mvcResult.getResponse().getStatus());
        Draft actualData = mapper.readValue(mapper.readTree(mvcResult.getResponse().getContentAsString()).toString(), Draft.class);

        Assertions.assertAll(
            () -> assertThat(actualData, hasProperty("id", is("4")))
        );
    }

    @Test
    public void shouldReturn400WhenSaveDraftForCaseworkerWithMalformedData() throws Exception {
        final String URL = "/caseworkers/" + UID + "/jurisdictions/" + JID + "/case-types/" + CTID + "/event-trigger/" + ETID + "/drafts";

        {
            mockMvc.perform(post(URL)
                                .contentType(JSON_CONTENT_TYPE))
                .andExpect(status().is(400))
                .andReturn();
        }
    }

    @Test
    public void shouldReturn200WhenUpdateDraftForCaseworker() throws Exception {
        final String URL = "/caseworkers/" + UID + "/jurisdictions/" + JID + "/case-types/" + CTID + "/event-trigger/" + ETID + "/drafts/" + DID;
        CaseDataContent caseDetailsToUpdate = newCaseDataContent()
            .withData(getData(data))
            .withEvent(anEvent()
                           .withEventId(TEST_EVENT_ID)
                           .build())
            .withToken(generateEventTokenNewCase(UID, JID, CTID, TEST_EVENT_ID))
            .build();

        final MvcResult mvcResult = mockMvc.perform(put(URL)
                                                        .contentType(JSON_CONTENT_TYPE)
                                                        .content(mapper.writeValueAsBytes(caseDetailsToUpdate))
        ).andReturn();

        assertEquals("Incorrect Response Status Code", 200, mvcResult.getResponse().getStatus());
        Draft actualData = mapper.readValue(mapper.readTree(mvcResult.getResponse().getContentAsString()).toString(), Draft.class);

        assertThat(actualData, hasProperty("id", is(DID)));
    }

    @Test
    public void shouldReturn400WhenUpdateDraftForCaseworkerWithMalformedData() throws Exception {
        final String URL = "/caseworkers/" + UID + "/jurisdictions/" + JID + "/case-types/" + CTID + "/event-trigger/" + ETID + "/drafts/" + DID;

        {
            mockMvc.perform(put(URL)
                                .contentType(JSON_CONTENT_TYPE))
                .andExpect(status().is(400))
                .andReturn();
        }
    }

    @Test
    public void shouldReturn404WhenUpdateDraftForCaseworkerWithDraftIdNotFound() throws Exception {
        final String URL = "/caseworkers/" + UID + "/jurisdictions/" + JID + "/case-types/" + CTID + "/event-trigger/" + ETID + "/drafts/" + WRONG_DID;
        CaseDataContent caseDetailsToUpdate = newCaseDataContent()
            .withData(getData(data))
            .withEvent(anEvent()
                           .withEventId(TEST_EVENT_ID)
                           .build())
            .withToken(generateEventTokenNewCase(UID, JID, CTID, TEST_EVENT_ID))
            .build();

        final MvcResult mvcResult = mockMvc.perform(put(URL)
                                                        .contentType(JSON_CONTENT_TYPE)
                                                        .content(mapper.writeValueAsBytes(caseDetailsToUpdate))
        ).andReturn();

        assertEquals("Incorrect Response Status Code", 404, mvcResult.getResponse().getStatus());
        String actualResponse = mapper.readTree(mvcResult.getResponse().getContentAsString()).toString();
        assertThat(actualResponse, containsString("\"message\":\"No draft found ( draft reference = '6' )\""));
    }

    private Map<String, JsonNode> getData(JsonNode expectedData) {
        return mapper.convertValue(expectedData, new TypeReference<HashMap<String, JsonNode>>() {
        });
    }

}
