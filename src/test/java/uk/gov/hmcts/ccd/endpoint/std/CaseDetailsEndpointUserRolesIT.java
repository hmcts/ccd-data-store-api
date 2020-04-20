package uk.gov.hmcts.ccd.endpoint.std;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.data.casedetails.search.PaginatedSearchMetadata;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;
import uk.gov.hmcts.ccd.domain.model.std.CaseDataContent;
import uk.gov.hmcts.ccd.domain.model.std.Event;

import javax.inject.Inject;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PRIVATE;
import static uk.gov.hmcts.ccd.domain.model.std.EventBuilder.anEvent;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDataContentBuilder.newCaseDataContent;

public class CaseDetailsEndpointUserRolesIT extends WireMockBaseTest {
    private static final String JURISDICTION = "PROBATE";
    private static final String TEST_EVENT_ID = "TEST_EVENT";
    private static final String GET_PAGINATED_SEARCH_METADATA_CITIZENS = "/citizens/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases/pagination_metadata";
    private static final String UID = "123";
    private static final int NUMBER_OF_CASES = 18;
    private static final String CASE_TYPE_CREATOR_ROLE = "TestAddressBookCreatorCase";
    private static final String CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS = "TestAddressBookCreatorNoCreateAccessCase";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    private JdbcTemplate template;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private static final String REFERENCE_2 = "1504259907353545";

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithCreatorRoleWithNoDataForCaseworker() throws Exception {
        final String DESCRIPTION = "A very long comment.......";
        final String SUMMARY = "Short comment";

        final String URL = "/caseworkers/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_CREATOR_ROLE + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(DESCRIPTION);
        triggeringEvent.setSummary(SUMMARY);
        caseDetailsToSave.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE_CREATOR_ROLE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Expected empty case data", "", mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").asText());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue("Incorrect Case Reference", uidService.validateUID(String.valueOf(savedCaseDetails.getReference())));
        assertEquals("Incorrect Case Type", CASE_TYPE_CREATOR_ROLE, savedCaseDetails.getCaseTypeId());
        assertEquals("Incorrect Data content", "{}", savedCaseDetails.getData().toString());
        assertEquals("state3", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case in state 3", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getCreatedDate(), caseAuditEvent.getCreatedDate());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals("Event ID", TEST_EVENT_ID, caseAuditEvent.getEventId());
        assertEquals("Description", DESCRIPTION, caseAuditEvent.getDescription());
        assertEquals("Summary", SUMMARY, caseAuditEvent.getSummary());
        assertTrue(caseAuditEvent.getDataClassification().isEmpty());
        assertThat(caseAuditEvent.getSecurityClassification(), equalTo(PRIVATE));
    }

    @Test
    public void shouldReturn201WhenPostCreateCaseWithCreatorRoleWithNoDataForCitizen() throws Exception {
        final String DESCRIPTION = "A very long comment.......";
        final String SUMMARY = "Short comment";

        final String URL = "/citizens/0/jurisdictions/" + JURISDICTION + "/case-types/" + CASE_TYPE_CREATOR_ROLE + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();
        final Event triggeringEvent = anEvent().build();
        triggeringEvent.setEventId(TEST_EVENT_ID);
        triggeringEvent.setDescription(DESCRIPTION);
        triggeringEvent.setSummary(SUMMARY);
        caseDetailsToSave.setEvent(triggeringEvent);
        final String token = generateEventTokenNewCase(UID, JURISDICTION, CASE_TYPE_CREATOR_ROLE, TEST_EVENT_ID);
        caseDetailsToSave.setToken(token);


        final MvcResult mvcResult = mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(201))
            .andReturn();

        assertEquals("Expected empty case data", "", mapper.readTree(mvcResult.getResponse().getContentAsString()).get("case_data").asText());

        final List<CaseDetails> caseDetailsList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect number of cases", 1, caseDetailsList.size());

        final CaseDetails savedCaseDetails = caseDetailsList.get(0);
        assertTrue("Incorrect Case Reference", uidService.validateUID(String.valueOf(savedCaseDetails.getReference())));
        assertEquals("Incorrect Case Type", CASE_TYPE_CREATOR_ROLE, savedCaseDetails.getCaseTypeId());
        assertEquals("Incorrect Data content", "{}", savedCaseDetails.getData().toString());
        assertEquals("state3", savedCaseDetails.getState());

        final List<AuditEvent> caseAuditEventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect number of case events", 1, caseAuditEventList.size());

        final AuditEvent caseAuditEvent = caseAuditEventList.get(0);
        assertEquals("123", caseAuditEvent.getUserId());
        assertEquals(savedCaseDetails.getId(), caseAuditEvent.getCaseDataId());
        assertEquals(savedCaseDetails.getCaseTypeId(), caseAuditEvent.getCaseTypeId());
        assertEquals(1, caseAuditEvent.getCaseTypeVersion().intValue());
        assertEquals(savedCaseDetails.getState(), caseAuditEvent.getStateId());
        assertEquals("Case in state 3", caseAuditEvent.getStateName());
        assertEquals(savedCaseDetails.getCreatedDate(), caseAuditEvent.getCreatedDate());
        assertEquals(savedCaseDetails.getData(), caseAuditEvent.getData());
        assertEquals("Event ID", TEST_EVENT_ID, caseAuditEvent.getEventId());
        assertEquals("Description", DESCRIPTION, caseAuditEvent.getDescription());
        assertEquals("Summary", SUMMARY, caseAuditEvent.getSummary());
        assertTrue(caseAuditEvent.getDataClassification().isEmpty());
        assertThat(caseAuditEvent.getSecurityClassification(), equalTo(PRIVATE));
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccessOnCreatorRoleForCaseworker() throws Exception {
        shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccess("caseworkers", CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS);
    }

    @Test
    public void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccessOnCreatorRoleForCitizen() throws Exception {
        shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccess("citizens", CASE_TYPE_CREATOR_ROLE_NO_CREATE_ACCESS);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_cases.sql" })
    public void shouldReturnPaginatedSearchMetadataForCitizen() throws Exception {

        assertCaseDataResultSetSize();
        MockUtils.setSecurityAuthorities(authentication, "role-citizen");

        MvcResult result = mockMvc.perform(get(GET_PAGINATED_SEARCH_METADATA_CITIZENS)
            .contentType(JSON_CONTENT_TYPE)
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();


        String responseAsString = result.getResponse().getContentAsString();
        PaginatedSearchMetadata metadata = mapper.readValue(responseAsString, PaginatedSearchMetadata.class);

        assertThat(metadata.getTotalPagesCount(), is(4));
        assertThat(metadata.getTotalResultsCount(), is(7));
    }

    /**
     * Checks that we have the expected test data set size, this is to ensure
     * that state filtering is correct.
     */
    private void assertCaseDataResultSetSize() {
        final int count = template.queryForObject("SELECT count(1) as n FROM case_data",Integer.class);
        assertEquals("Incorrect case data size", NUMBER_OF_CASES, count);
    }

    private void shouldReturn404WhenPostCreateCaseWithNoCreateCaseAccess(String role, String caseType) throws Exception {
        final String URL = "/" + role + "/0/jurisdictions/" + JURISDICTION + "/case-types/" + caseType + "/cases";

        final CaseDataContent caseDetailsToSave = newCaseDataContent().build();


        mockMvc.perform(post(URL)
            .contentType(JSON_CONTENT_TYPE)
            .content(mapper.writeValueAsBytes(caseDetailsToSave))
        ).andExpect(status().is(404))
            .andReturn();
    }

}
