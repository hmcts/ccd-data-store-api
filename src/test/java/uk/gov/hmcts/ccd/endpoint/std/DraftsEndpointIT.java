package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import org.junit.Before;
import org.junit.ClassRule;
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
import uk.gov.hmcts.ccd.domain.model.draft.CaseDataContentDraft;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;

import javax.inject.Inject;
import java.util.*;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsIn.isIn;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class DraftsEndpointIT extends WireMockBaseTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String CTID = "TestAddressBookCase";
    private static final String ETID = "CreateCase";
    private static final String JID = "PROBATE";
    private static final String TEST_EVENT_ID = "TEST_EVENT";
    private static final String UID = "0";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    public void shouldReturn201WhenPostSaveDraftForCaseworker() throws Exception {
        final String URL = "/caseworkers/" + UID + "/jurisdictions/" + JID + "/case-types/" + CTID + "/event-trigger/" + ETID + "/drafts";
        final JsonNode DATA = mapper.readTree("{\n" +
         "    \"PersonFirstName\": \"John\",\n" +
         "    \"PersonLastName\": \"Smith\"\n" +
         " }\n");
        final CaseDataContent caseDetailsToSave = new CaseDataContent();
        caseDetailsToSave.setEvent(new Event());
        caseDetailsToSave.getEvent().setEventId(TEST_EVENT_ID);
        caseDetailsToSave.setData(mapper.convertValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        }));
        final String token = generateEventTokenNewCase(UID, JID, CTID, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);

        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andReturn();

        assertEquals("Incorrect Response Status Code", 201, mvcResult.getResponse().getStatus());
        Draft actualData = mapper.readValue(mapper.readTree(mvcResult.getResponse().getContentAsString()).toString(), Draft.class);

        Assertions.assertAll(
            () -> assertThat(actualData, hasProperty("id", is(4L)))
        );
    }

    @Test
    public void shouldReturn400WhenPostSaveDraftForCaseworker() throws Exception {
        final String URL = "/caseworkers/" + UID + "/jurisdictions/" + JID + "/case-types/" + CTID + "/event-trigger/" + ETID + "/drafts";
        final JsonNode WRONG_DATA = mapper.readTree("{\n " +
         " \"data\": {\n" +
         "    \"PersonFirstName\": \"John\",\n" +
         "    \"PersonLastName\": \"Smith\"\n" +
         " }\n" +
         "}\n");
        final CaseDataContent caseDetailsToSave = new CaseDataContent();
        caseDetailsToSave.setEvent(new Event());
        caseDetailsToSave.getEvent().setEventId(TEST_EVENT_ID);
        caseDetailsToSave.setData(mapper.convertValue(WRONG_DATA, new TypeReference<HashMap<String, JsonNode>>() {
        }));
        final String token = generateEventTokenNewCase(UID, JID, CTID, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);

        {
            mockMvc.perform(post(URL)
                                .contentType(JSON_CONTENT_TYPE))
                .andExpect(status().is(400))
                .andReturn();
        }
    }

    private JsonNode getTextNode(String value) {
        return JSON_NODE_FACTORY.textNode(value);
    }
}
