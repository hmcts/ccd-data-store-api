package uk.gov.hmcts.ccd.endpoint.std;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.config.JacksonUtils;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewActionableEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewJurisdiction;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTab;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewType;
import uk.gov.hmcts.ccd.domain.model.aggregated.CommonField;
import uk.gov.hmcts.ccd.domain.model.draft.Draft;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

@SuppressWarnings("checkstyle:OperatorWrap") // too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
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

    @Before
    public void setUp() throws IOException {
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

    @Test
    public void shouldReturn200WhenGetValidDraft() throws Exception {
        final String URL = "/caseworkers/" + UID + "/jurisdictions/" + JID + "/case-types/" + CTID + "/drafts/" + DID;
        final MvcResult result = mockMvc.perform(get(URL)
            .contentType(JSON_CONTENT_TYPE)
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        final CaseView caseView = mapper.readValue(result.getResponse().getContentAsString(), CaseView.class);
        assertNotNull("Case View is null", caseView);
        assertEquals("Unexpected Case ID", "DRAFT5", caseView.getCaseId());

        final CaseViewType caseViewType = caseView.getCaseType();
        assertNotNull("Case View Type is null", caseViewType);
        assertEquals("Unexpected Case Type Id", "TestAddressBookCase", caseViewType.getId());
        assertEquals("Unexpected Case Type name", "Test Address Book Case", caseViewType.getName());
        assertEquals("Unexpected Case Type description", "Test Address Book Case", caseViewType.getDescription());

        final CaseViewJurisdiction caseViewJurisdiction = caseViewType.getJurisdiction();
        assertNotNull("Case View Jurisdiction is null", caseViewJurisdiction);
        assertEquals("Unexpected Jurisdiction Id", JID, caseViewJurisdiction.getId());
        assertEquals("Unexpected Jurisdiction name", "Test", caseViewJurisdiction.getName());
        assertEquals("Unexpected Jurisdiction description", "Test Jurisdiction", caseViewJurisdiction.getDescription());

        final String[] channels = caseView.getChannels();
        assertNotNull("Channel is null", channels);
        assertEquals("Unexpected number of channels", 1, channels.length);
        assertEquals("Unexpected channel", "channel1", channels[0]);

        final CaseViewTab[] caseViewTabs = caseView.getTabs();
        assertNotNull("Tabs are null", caseViewTabs);
        assertEquals("Unexpected number of tabs", 3, caseViewTabs.length);

        final CaseViewTab nameTab = caseViewTabs[0];
        assertNotNull("First tab is null", nameTab);
        assertEquals("Unexpected tab Id", "NameTab", nameTab.getId());
        assertEquals("Unexpected tab label", "Name", nameTab.getLabel());
        assertEquals("Unexpected tab show condition", "PersonFirstName=\"George\"", nameTab.getShowCondition());
        assertEquals("Unexpected tab order", 1, nameTab.getOrder().intValue());

        final CaseViewField[] nameFields = nameTab.getFields();
        assertNotNull("Fields are null", nameFields);
        assertEquals("Unexpected number of fields", 2, nameFields.length);

        final CaseViewField firstNameField = nameFields[0];
        assertNotNull("Field is null", firstNameField);
        assertEquals("Unexpected Field id", "PersonFirstName", firstNameField.getId());
        assertEquals("Unexpected Field label", "First Name", firstNameField.getLabel());
        assertEquals("Unexpected Field order", 1, firstNameField.getOrder().intValue());
        assertEquals("Unexpected Field show condition", "PersonLastName=\"Jones\"", firstNameField.getShowCondition());
        assertEquals("Unexpected Field field type", "Text", firstNameField.getFieldTypeDefinition().getType());
        assertEquals("Unexpected Field value", "John", firstNameField.getValue());

        final CaseViewField lastNameField = nameFields[1];
        assertNotNull("Field is null", lastNameField);
        assertEquals("Unexpected Field id", "PersonLastName", lastNameField.getId());
        assertEquals("Unexpected Field label", "Last Name", lastNameField.getLabel());
        assertEquals("Unexpected Field order", 2, lastNameField.getOrder().intValue());
        assertEquals("Unexpected Field show condition", "PersonFirstName=\"Tom\"", lastNameField.getShowCondition());
        assertEquals("Unexpected Field field type", "Text", lastNameField.getFieldTypeDefinition().getType());
        assertEquals("Unexpected Field value", "Smith", lastNameField.getValue());

        final CaseViewTab addressTab = caseViewTabs[1];
        assertNotNull("First tab is null", addressTab);
        assertEquals("Unexpected tab Id", "AddressTab", addressTab.getId());
        assertEquals("Unexpected tab label", "Address", addressTab.getLabel());
        assertEquals("Unexpected tab show condition", "PersonLastName=\"Smith\"", addressTab.getShowCondition());
        assertEquals("Unexpected tab order", 2, addressTab.getOrder().intValue());

        final CommonField[] addressFields = addressTab.getFields();
        assertThat("Fields are not empty", addressFields, arrayWithSize(0));
        assertEquals("Unexpected number of fields", 0, addressFields.length);

        final CaseViewTab documentTab = caseViewTabs[2];
        assertNotNull("First tab is null", documentTab);
        assertEquals("Unexpected tab Id", "DocumentsTab", documentTab.getId());
        assertEquals("Unexpected tab label", "Documents", documentTab.getLabel());
        assertEquals("Unexpected tab show condition", "PersonFistName=\"George\"", documentTab.getShowCondition());
        assertEquals("Unexpected tab order", 3, documentTab.getOrder().intValue());

        final CommonField[] documentFields = documentTab.getFields();
        assertThat("Fields are not empty", documentFields, arrayWithSize(0));
        assertEquals("Unexpected number of fields", 0, documentFields.length);

        final CaseViewEvent[] events = caseView.getEvents();
        assertThat("Events are not empty", events, arrayWithSize(2));

        assertEquals("Event ID", "Draft updated", events[0].getEventId());
        assertEquals("Event Name", "Draft updated", events[0].getEventName());
        assertEquals("Event State Name", "Draft", events[0].getStateName());
        assertEquals("Event State ID", "Draft", events[0].getStateId());

        assertEquals("Event ID", "Draft created", events[1].getEventId());
        assertEquals("Event Name", "Draft created", events[1].getEventName());
        assertEquals("Event State Name", "Draft", events[1].getStateName());
        assertEquals("Event State ID", "Draft", events[1].getStateId());

        final CaseViewActionableEvent[] actionableEvents = caseView.getActionableEvents();
        assertNotNull("Triggers are null", actionableEvents);
        assertEquals("Should only get resume and delete triggers", 2, actionableEvents.length);

        assertEquals("Trigger ID", "createCase", actionableEvents[0].getId());
        assertEquals("Trigger Name", "Resume", actionableEvents[0].getName());
        assertEquals("Trigger Description", "This event will create a new case", actionableEvents[0].getDescription());
        assertEquals("Trigger Order", Integer.valueOf(1), actionableEvents[0].getOrder());

        assertEquals("Trigger ID", "DELETE", actionableEvents[1].getId());
        assertEquals("Trigger Name", "Delete", actionableEvents[1].getName());
        assertEquals("Trigger Description", "Delete draft", actionableEvents[1].getDescription());
        assertEquals("Trigger Order", Integer.valueOf(2), actionableEvents[1].getOrder());
    }

    @Test
    public void shouldReturn404WhenGetInvalidDraft() throws Exception {
        final String URL = "/caseworkers/" + UID + "/jurisdictions/" + JID + "/case-types/" + CTID + "/drafts/" + WRONG_DID;
        CaseDataContent caseDetailsToUpdate = newCaseDataContent()
            .withData(getData(data))
            .withEvent(anEvent()
                .withEventId(TEST_EVENT_ID)
                .build())
            .withToken(generateEventTokenNewCase(UID, JID, CTID, TEST_EVENT_ID))
            .build();

        final MvcResult mvcResult = mockMvc.perform(get(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToUpdate))
        ).andReturn();

        assertEquals("Incorrect Response Status Code", 404, mvcResult.getResponse().getStatus());
        String actualResponse = mapper.readTree(mvcResult.getResponse().getContentAsString()).toString();
        assertThat(actualResponse, containsString("\"message\":\"No draft found ( draft reference = '6' )\""));
    }

    @Test
    public void shouldReturn200WhenDeleteValidDraftForCaseworker() throws Exception {
        final String URL = "/caseworkers/" + UID + "/jurisdictions/" + JID + "/case-types/" + CTID + "/drafts/" + DID;

        final MvcResult mvcResult = mockMvc.perform(delete(URL).contentType(JSON_CONTENT_TYPE)).andReturn();

        assertEquals("Incorrect Response Status Code", 200, mvcResult.getResponse().getStatus());
    }

    @Test
    public void shouldReturn404WhenDeleteInvalidDraftForCaseworker() throws Exception {
        final String URL = "/caseworkers/" + UID + "/jurisdictions/" + JID + "/case-types/" + CTID + "/drafts/" + WRONG_DID;

        final MvcResult mvcResult = mockMvc.perform(delete(URL).contentType(JSON_CONTENT_TYPE)).andReturn();

        assertEquals("Incorrect Response Status Code", 404, mvcResult.getResponse().getStatus());
    }

    private Map<String, JsonNode> getData(JsonNode expectedData) {
        return JacksonUtils.convertValue(expectedData);
    }

}
