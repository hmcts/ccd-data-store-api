package uk.gov.hmcts.ccd.endpoint.ui;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.data.casedetails.search.MetaData;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseHistoryView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseUpdateViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseView;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewActionableEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewEvent;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewJurisdiction;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTab;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewType;
import uk.gov.hmcts.ccd.domain.model.aggregated.JurisdictionDisplayProperties;
import uk.gov.hmcts.ccd.domain.model.aggregated.ProfileCaseState;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultView;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewColumn;
import uk.gov.hmcts.ccd.domain.model.search.SearchResultViewItem;
import uk.gov.hmcts.ccd.domain.model.std.AuditEvent;

import javax.inject.Inject;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField.READONLY;
import static uk.gov.hmcts.ccd.domain.service.aggregated.SearchQueryOperation.WORKBASKET;
import static uk.gov.hmcts.ccd.domain.types.CollectionValidator.VALUE;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.COLLECTION_FIELD;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.COMPLEX_DATE_TIME_FIELD;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.COMPLEX_FIELD;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.DATE_FIELD;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.DATE_TIME_FIELD;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.arrayOf;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.mapOf;

public class QueryEndpointIT extends WireMockBaseTest {
    private static final String GET_CASES = "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/"
        + "TestAddressBookCase/cases";
    private static final String GET_CASES_NO_READ_CASE_FIELD_ACCESS =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCaseNoReadFieldAccess/cases";
    private static final String GET_CASES_NO_READ_CASE_TYPE_ACCESS = "/aggregated/caseworkers/0/jurisdictions/PROBATE/"
        + "case-types/TestAddressBookCase4/cases";
    private static final String GET_DRAFT = "/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/"
        + "drafts/4444333322221111";
    private static final String GET_CASE = "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/"
        + "TestAddressBookCase/cases/1504259907353529";
    private static final String GET_CASE_NO_EVENT_READ_ACCESS =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCaseNoReadEventAccess/cases/"
            + "1504259907353636";
    private static final String GET_PRIVATE_CASE = "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/"
        + "TestAddressBookCase/cases/1504259907353545";
    private static final String GET_COMPLEX_CASE =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestComplexAddressBookCase/cases/1504259907353537";
    private static final String GET_EVENT_TRIGGER_FOR_CASE_TYPE_VALID =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/event-triggers/"
            + "NO_PRE_STATES_EVENT";
    private static final String GET_EVENT_TRIGGER_FOR_CASE_VALID =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases/1504259907353545/"
            + "event-triggers/HAS_PRE_STATES_EVENT";
    private static final String GET_EVENT_TRIGGER_FOR_CASE_PRIVATE =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases/1504259907353545/"
            + "event-triggers/HAS_PRE_STATES_EVENT";
    private static final String GET_CASE_TYPES_READ_ACCESS = "/aggregated/caseworkers/0/jurisdictions/PROBATE/"
        + "case-types?access=read";
    private static final String GET_JURISDICTIONS_READ_ACCESS = "/aggregated/caseworkers/0/jurisdictions?access=read";


    private static final String GET_CASE_TYPES_NO_ACCESS_PARAM = "/aggregated/caseworkers/0/jurisdictions/PROBATE/"
        + "case-types";
    private static final String GET_CASE_TYPES_MISNAMED_ACCESS_PARAM = "/aggregated/caseworkers/0/jurisdictions/"
        + "PROBATE/invalid=read";
    private static final String GET_CASE_TYPES_INVALID_ACCESS_PARAM = "/aggregated/caseworkers/0/jurisdictions/"
        + "PROBATE/case-types?access=INVALID";
    private static final String GET_NULL_CASE = "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/"
        + "TestComplexAddressBookCase/cases/9999999999999995";
    private static final String GET_CASE_INVALID_STATE =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestComplexAddressBookCase/cases/1504259907352539";
    private static final String GET_EVENT_TRIGGER_FOR_CASE_TYPE_INVALID_PRE_STATES =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/event-triggers/"
            + "HAS_PRE_STATES_EVENT";
    private static final String GET_EVENT_TRIGGER_FOR_CASE_TYPE_INVALID_EVENT =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/event-triggers/NOT_AN_EVENT";
    private static final String GET_EVENT_TRIGGER_FOR_CASE_INVALID_STATE =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases/1504259907353552/"
            + "event-triggers/TEST_EVENT";
    private static final String GET_EVENT_TRIGGER_FOR_CASE_INVALID_CASE =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases/9999999999999995/"
            + "event-triggers/HAS_PRE_STATES_EVENT";
    private static final String GET_EVENT_TRIGGER_FOR_CASE_INVALID_EVENT =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases/1504259907353552/"
            + "event-triggers/NOT_AN_EVENT";
    private static final String GET_CASE_INVALID_REFERENCE =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases/invalidReference";
    private static final String GET_EVENT_TRIGGER_FOR_CASE_INVALID_CASE_REFERENCE =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases/xxx/event-triggers/"
            + "HAS_PRE_STATES_EVENT";
    private static final String GET_CASES_INVALID_JURISDICTION = "/aggregated/caseworkers/0/jurisdictions/XYZ/"
        + "case-types/TestAddressBookCase/cases";
    private static final String GET_CASES_INVALID_CASE_TYPE = "/aggregated/caseworkers/0/jurisdictions/PROBATE/"
        + "case-types/XYZAddressBookCase/cases";
    private static final String GET_CASES_DCP =
        "/aggregated/caseworkers/0/jurisdictions/DCPTest1/case-types/DCP/cases";

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final String TEST_CASE_TYPE = "TestAddressBookCase";
    private static final String TEST_JURISDICTION = "PROBATE";

    private static final String GET_CASE_HISTORY_FOR_EVENT =
        "/aggregated/caseworkers/0/jurisdictions/PROBATE/case-types/TestAddressBookCase/cases/1504259907353529/"
            + "events/%d/case-history";


    @Inject
    private WebApplicationContext wac;

    @SpyBean
    private AuditRepository auditRepository;
    private MockMvc mockMvc;
    private JdbcTemplate template;

    @Before
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void validSearch() throws Exception {
        // Check that we have the expected test data set size, this is to ensure
        // that state filtering is correct
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());

        final MvcResult result = mockMvc.perform(get(GET_CASES)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .param("view", WORKBASKET)
                                                     .param("case_type", TEST_CASE_TYPE)
                                                     .param("jurisdiction", TEST_JURISDICTION)
                                                     .param("state", "CaseCreated")
                                                     .param("page", "1")
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is(200))
                                        .andReturn();

        final SearchResultView searchResultView = mapper.readValue(result.getResponse().getContentAsString(),
                                                                   SearchResultView.class);
        final List<SearchResultViewColumn> searchResultViewColumns = searchResultView.getSearchResultViewColumns();
        final List<SearchResultViewItem> searchResultViewItems = searchResultView.getSearchResultViewItems();

        assertEquals("Incorrect view columns count", 3, searchResultViewColumns.size());

        assertEquals("PersonFirstName", searchResultViewColumns.get(0).getCaseFieldId());
        assertEquals("First Name", searchResultViewColumns.get(0).getLabel());
        assertEquals(1, searchResultViewColumns.get(0).getOrder().intValue());

        assertEquals("PersonLastName", searchResultViewColumns.get(1).getCaseFieldId());
        assertEquals("Last Name", searchResultViewColumns.get(1).getLabel());
        assertEquals(1, searchResultViewColumns.get(1).getOrder().intValue());

        assertEquals("PersonAddress", searchResultViewColumns.get(2).getCaseFieldId());
        assertEquals("Address", searchResultViewColumns.get(2).getLabel());
        assertEquals(1, searchResultViewColumns.get(2).getOrder().intValue());
        assertEquals("Address", searchResultViewColumns.get(2).getCaseFieldTypeDefinition().getId());
        assertEquals("Complex", searchResultViewColumns.get(2).getCaseFieldTypeDefinition().getType());

        assertEquals("Incorrect view items count", 3, searchResultViewItems.size());

        assertNotNull(searchResultViewItems.get(0).getCaseId());
        assertEquals("John", searchResultViewItems.get(0).getFields().get("PersonFirstName"));
        assertEquals("Smith", searchResultViewItems.get(0).getFields().get("PersonLastName"));
        assertEquals(null, searchResultViewItems.get(0).getFields().get("PersonAddress"));

        assertNotNull(searchResultViewItems.get(1).getCaseId());
        assertEquals("Janet", searchResultViewItems.get(1).getFields().get("PersonFirstName"));
        assertEquals("Parker", searchResultViewItems.get(1).getFields().get("PersonLastName"));
        assertEquals("123", ((Map) searchResultViewItems.get(1).getFields().get("PersonAddress"))
            .get("AddressLine1"));
        assertEquals("Fake Street", ((Map) searchResultViewItems.get(1).getFields().get("PersonAddress"))
            .get("AddressLine2"));
        assertEquals("Hexton", ((Map) searchResultViewItems.get(1).getFields().get("PersonAddress"))
            .get("AddressLine3"));
        assertEquals("England", ((Map) searchResultViewItems.get(1).getFields().get("PersonAddress"))
            .get("Country"));
        assertEquals("HX08 5TG", ((Map) searchResultViewItems.get(1).getFields().get("PersonAddress"))
            .get("Postcode"));

        assertNotNull(searchResultViewItems.get(2).getCaseId());
        assertEquals("George", searchResultViewItems.get(2).getFields().get("PersonFirstName"));
        assertEquals("Roof", searchResultViewItems.get(2).getFields().get("PersonLastName"));
        assertEquals("Flat 9", ((Map) searchResultViewItems.get(2).getFields().get("PersonAddress"))
            .get("AddressLine1"));
        assertEquals("2 Hubble Avenue", ((Map) searchResultViewItems.get(2).getFields().get("PersonAddress"))
            .get("AddressLine2"));
        assertEquals("ButtonVillie", ((Map) searchResultViewItems.get(2).getFields().get("PersonAddress"))
            .get("AddressLine3"));
        assertEquals("Wales", ((Map) searchResultViewItems.get(2).getFields().get("PersonAddress"))
            .get("Country"));
        assertEquals("W11 5DF", ((Map) searchResultViewItems.get(2).getFields().get("PersonAddress"))
            .get("Postcode"));

        // audit-log assertions
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.SEARCH_CASE.getLabel()));
        assertThat(captor.getValue().getJurisdiction(), is(TEST_JURISDICTION));
        assertThat(captor.getValue().getCaseType(), is(TEST_CASE_TYPE));
        assertThat(captor.getValue().getCaseId(), is("DRAFT4444333322221111,1504259907353529,1504259907353545"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturnResultsWithFilteredOutFieldsWhenRelevantCaseFieldAccessNotGranted() throws Exception {
        // Check that we have the expected test data set size, this is to ensure
        // that state filtering is correct
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());

        final MvcResult result = mockMvc.perform(get(GET_CASES_NO_READ_CASE_FIELD_ACCESS)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .param("view", WORKBASKET)
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is(200))
                                        .andReturn();

        final SearchResultView searchResultView = mapper.readValue(result.getResponse().getContentAsString(),
                                                                   SearchResultView.class);
        final List<SearchResultViewItem> searchResultViewItems = searchResultView.getSearchResultViewItems();

        assertEquals("Incorrect view items count", 2, searchResultViewItems.size());

        assertEquals("Incorrect view items count", 2, searchResultViewItems.size());
        assertNotNull(searchResultViewItems.get(0).getCaseId());

        SearchResultViewItem findItemByLastName =
            getFindItemByLastName(searchResultViewItems, "Pullen");

        assertThat(findItemByLastName.getFields().get("PersonFirstName"), is(nullValue()));
        assertEquals("Pullen", findItemByLastName.getFields().get("PersonLastName"));
        assertEquals("Governer House", ((Map) findItemByLastName.getFields().get("PersonAddress"))
            .get("AddressLine1"));
        assertEquals("1 Puddle Lane", ((Map) findItemByLastName.getFields().get("PersonAddress"))
            .get("AddressLine2"));
        assertEquals("London", ((Map) findItemByLastName.getFields().get("PersonAddress"))
            .get("AddressLine3"));
        assertEquals("England", ((Map) findItemByLastName.getFields().get("PersonAddress"))
            .get("Country"));
        assertEquals("SE1 4EE", ((Map) findItemByLastName.getFields().get("PersonAddress"))
            .get("Postcode"));

        findItemByLastName =
            getFindItemByLastName(searchResultViewItems, "Parker");

        assertNotNull(findItemByLastName.getCaseId());
        assertThat(findItemByLastName.getFields().get("PersonFirstName"), is(nullValue()));
        assertEquals("Parker", findItemByLastName.getFields().get("PersonLastName"));
        assertEquals("123", ((Map) findItemByLastName.getFields().get("PersonAddress"))
            .get("AddressLine1"));
        assertEquals("Fake Street", ((Map) findItemByLastName.getFields().get("PersonAddress"))
            .get("AddressLine2"));
        assertEquals("Hexton", ((Map) findItemByLastName.getFields().get("PersonAddress"))
            .get("AddressLine3"));
        assertEquals("England", ((Map) findItemByLastName.getFields().get("PersonAddress"))
            .get("Country"));
        assertEquals("HX08 5TG", ((Map) findItemByLastName.getFields().get("PersonAddress"))
            .get("Postcode"));
    }

    private SearchResultViewItem getFindItemByLastName(List<SearchResultViewItem> searchResultViewItems,
                                                       String lastName) {
        return searchResultViewItems.stream().filter(e -> e.getFields().get("PersonLastName").equals(lastName))
            .findFirst().get();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturnEmptyResultsWhenRelevantCaseTypeAccessNotGranted() throws Exception {
        // Check that we have the expected test data set size, this is to ensure
        // that state filtering is correct
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation",NUMBER_OF_CASES, resultList.size());

        final MvcResult result = mockMvc.perform(get(GET_CASES_NO_READ_CASE_TYPE_ACCESS)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .param("view", WORKBASKET)
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is(200))
                                        .andReturn();

        final SearchResultView searchResultView = mapper.readValue(result.getResponse().getContentAsString(),
                                                                   SearchResultView.class);
        final List<SearchResultViewItem> searchResultViewItems = searchResultView.getSearchResultViewItems();

        assertEquals("Incorrect view items count", 0, searchResultViewItems.size());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void validNonBasketSearch() throws Exception {
        // Check that we have the expected test data set size, this is to ensure
        // that state filtering is correct
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());

        final MvcResult result = mockMvc.perform(get(GET_CASES)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .param("case.PersonFirstName", "Janet ")
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is(200))
                                        .andReturn();

        final SearchResultView searchResultView = mapper.readValue(result.getResponse().getContentAsString(),
                                                                   SearchResultView.class);
        final List<SearchResultViewColumn> searchResultViewColumns = searchResultView.getSearchResultViewColumns();
        final List<SearchResultViewItem> searchResultViewItems = searchResultView.getSearchResultViewItems();

        assertEquals("Incorrect view columns count", 3, searchResultViewColumns.size());

        assertEquals("PersonFirstName", searchResultViewColumns.get(0).getCaseFieldId());
        assertEquals("First Name", searchResultViewColumns.get(0).getLabel());
        assertEquals(1, searchResultViewColumns.get(0).getOrder().intValue());

        assertEquals("PersonLastName", searchResultViewColumns.get(1).getCaseFieldId());
        assertEquals("Last Name", searchResultViewColumns.get(1).getLabel());
        assertEquals(1, searchResultViewColumns.get(1).getOrder().intValue());

        assertEquals("PersonAddress", searchResultViewColumns.get(2).getCaseFieldId());
        assertEquals("Address", searchResultViewColumns.get(2).getLabel());
        assertEquals(1, searchResultViewColumns.get(2).getOrder().intValue());

        assertEquals("Incorrect view items count", 2, searchResultViewItems.size());

        assertNotNull(searchResultViewItems.get(0).getCaseId());
        assertEquals("Janet", searchResultViewItems.get(0).getFields().get("PersonFirstName"));
        assertEquals("Parker", searchResultViewItems.get(0).getFields().get("PersonLastName"));
        assertEquals("123", ((Map) searchResultViewItems.get(0).getFields().get("PersonAddress"))
            .get("AddressLine1"));
        assertEquals("Fake Street", ((Map) searchResultViewItems.get(0).getFields().get("PersonAddress"))
            .get("AddressLine2"));
        assertEquals("Hexton", ((Map) searchResultViewItems.get(0).getFields().get("PersonAddress"))
            .get("AddressLine3"));
        assertEquals("England", ((Map) searchResultViewItems.get(0).getFields().get("PersonAddress"))
            .get("Country"));
        assertEquals("HX08 5TG", ((Map) searchResultViewItems.get(0).getFields().get("PersonAddress"))
            .get("Postcode"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void validNonBasketNoResultSearch() throws Exception {
        // Check that we have the expected test data set size, this is to ensure
        // that state filtering is correct
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());

        final MvcResult result = mockMvc.perform(get(GET_CASES)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .param("case_type", TEST_CASE_TYPE)
                                                     .param("jurisdiction", TEST_JURISDICTION)
                                                     .param("case.PersonFirstName", "JanetX")
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is2xxSuccessful())
                                        .andReturn();

        final SearchResultView searchResultView = mapper.readValue(result.getResponse().getContentAsString(),
                                                                   SearchResultView.class);
        final List<SearchResultViewColumn> searchResultViewColumns = searchResultView.getSearchResultViewColumns();
        final List<SearchResultViewItem> searchResultViewItems = searchResultView.getSearchResultViewItems();

        assertEquals("Incorrect view columns count", 3, searchResultViewColumns.size());

        assertEquals("PersonFirstName", searchResultViewColumns.get(0).getCaseFieldId());
        assertEquals("First Name", searchResultViewColumns.get(0).getLabel());
        assertEquals(1, searchResultViewColumns.get(0).getOrder().intValue());

        assertEquals("PersonLastName", searchResultViewColumns.get(1).getCaseFieldId());
        assertEquals("Last Name", searchResultViewColumns.get(1).getLabel());
        assertEquals(1, searchResultViewColumns.get(1).getOrder().intValue());

        assertEquals("PersonAddress", searchResultViewColumns.get(2).getCaseFieldId());
        assertEquals("Address", searchResultViewColumns.get(2).getLabel());
        assertEquals(1, searchResultViewColumns.get(2).getOrder().intValue());

        assertEquals("Incorrect view items count", 0, searchResultViewItems.size());

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void validNonBasketBadRequestSearch() throws Exception {
        // Check that we have the expected test data set size, this is to ensure
        // that state filtering is correct
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());

        final MvcResult result = mockMvc.perform(get(GET_CASES)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .param("case_type", TEST_CASE_TYPE)
                                                     .param("jurisdiction", TEST_JURISDICTION)
                                                     .param("case.PersonFirstName$", "JanetX")
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is(400))
                                        .andReturn();

    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void validMultipleNonBasketSearch() throws Exception {
        // Check that we have the expected test data set size, this is to ensure
        // that state filtering is correct
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());

        final MvcResult result = mockMvc.perform(get(GET_CASES)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .param("case_type", TEST_CASE_TYPE)
                                                     .param("jurisdiction", TEST_JURISDICTION)
                                                     .param("case.PersonAddress.Country", "EnglanD")
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is(200))
                                        .andReturn();

        final SearchResultView searchResultView = mapper.readValue(result.getResponse().getContentAsString(),
                                                                   SearchResultView.class);
        final List<SearchResultViewColumn> searchResultViewColumns = searchResultView.getSearchResultViewColumns();
        final List<SearchResultViewItem> searchResultViewItems = searchResultView.getSearchResultViewItems();

        assertEquals("Incorrect view columns count", 3, searchResultViewColumns.size());

        assertEquals("PersonFirstName", searchResultViewColumns.get(0).getCaseFieldId());
        assertEquals("First Name", searchResultViewColumns.get(0).getLabel());
        assertEquals(1, searchResultViewColumns.get(0).getOrder().intValue());

        assertEquals("PersonLastName", searchResultViewColumns.get(1).getCaseFieldId());
        assertEquals("Last Name", searchResultViewColumns.get(1).getLabel());
        assertEquals(1, searchResultViewColumns.get(1).getOrder().intValue());

        assertEquals("PersonAddress", searchResultViewColumns.get(2).getCaseFieldId());
        assertEquals("Address", searchResultViewColumns.get(2).getLabel());
        assertEquals(1, searchResultViewColumns.get(2).getOrder().intValue());

        assertEquals("Incorrect view items count", 2, searchResultViewItems.size());

        assertNotNull(searchResultViewItems.get(0).getCaseId());
        assertNotNull(searchResultViewItems.get(1).getCaseId());
        assertNotEquals(searchResultViewItems.get(1).getCaseId(), searchResultViewItems.get(0).getCaseId());
        assertEquals("England",
                     ((Map) searchResultViewItems.get(0).getFields().get("PersonAddress")).get("Country"));
        assertEquals("England",
                     ((Map) searchResultViewItems.get(1).getFields().get("PersonAddress")).get("Country"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void validBlankEnteredNonBasketSearch() throws Exception {
        // Check that we have the expected test data set size, this is to ensure
        // that state filtering is correct
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());

        final MvcResult result = mockMvc.perform(get(GET_CASES)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .param("case_type", TEST_CASE_TYPE)
                                                     .param("jurisdiction", TEST_JURISDICTION)
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is(200))
                                        .andReturn();

        final SearchResultView searchResultView = mapper.readValue(result.getResponse().getContentAsString(),
                                                                   SearchResultView.class);
        final List<SearchResultViewColumn> searchResultViewColumns = searchResultView.getSearchResultViewColumns();
        final List<SearchResultViewItem> searchResultViewItems = searchResultView.getSearchResultViewItems();

        assertEquals("Incorrect view columns count", 3, searchResultViewColumns.size());

        assertEquals("PersonFirstName", searchResultViewColumns.get(0).getCaseFieldId());
        assertEquals("First Name", searchResultViewColumns.get(0).getLabel());
        assertEquals(1, searchResultViewColumns.get(0).getOrder().intValue());

        assertEquals("PersonLastName", searchResultViewColumns.get(1).getCaseFieldId());
        assertEquals("Last Name", searchResultViewColumns.get(1).getLabel());
        assertEquals(1, searchResultViewColumns.get(1).getOrder().intValue());

        assertEquals("PersonAddress", searchResultViewColumns.get(2).getCaseFieldId());
        assertEquals("Address", searchResultViewColumns.get(2).getLabel());
        assertEquals(1, searchResultViewColumns.get(2).getOrder().intValue());

        assertEquals("Incorrect view items count", 2, searchResultViewItems.size());

        assertNotNull(searchResultViewItems.get(0).getCaseId());
        assertNotNull(searchResultViewItems.get(1).getCaseId());

        assertNotEquals(searchResultViewItems.get(1).getCaseId(), searchResultViewItems.get(0).getCaseId());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void validSearchAllWithCaseType() throws Exception {
        // Check that we have the expected test data set size, this is to ensure
        // that state filtering is correct
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());

        final MvcResult result = mockMvc.perform(get(GET_CASES)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .param("view", WORKBASKET)
                                                     .param("case_type", TEST_CASE_TYPE)
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is(200))
                                        .andReturn();

        final SearchResultView searchResultView = mapper.readValue(result.getResponse().getContentAsString(),
                                                                   SearchResultView.class);
        final List<SearchResultViewColumn> searchResultViewColumns = searchResultView.getSearchResultViewColumns();
        final List<SearchResultViewItem> searchResultViewItems = searchResultView.getSearchResultViewItems();

        assertEquals("Incorrect view columns count", 3, searchResultViewColumns.size());

        assertEquals("PersonFirstName", searchResultViewColumns.get(0).getCaseFieldId());
        assertEquals("First Name", searchResultViewColumns.get(0).getLabel());
        assertEquals(1, searchResultViewColumns.get(0).getOrder().intValue());

        assertEquals("PersonLastName", searchResultViewColumns.get(1).getCaseFieldId());
        assertEquals("Last Name", searchResultViewColumns.get(1).getLabel());
        assertEquals(1, searchResultViewColumns.get(1).getOrder().intValue());

        assertEquals("PersonAddress", searchResultViewColumns.get(2).getCaseFieldId());
        assertEquals("Address", searchResultViewColumns.get(2).getLabel());
        assertEquals(1, searchResultViewColumns.get(2).getOrder().intValue());

        assertEquals("Incorrect view items count", 2, searchResultViewItems.size());

        assertNotNull(searchResultViewItems.get(0).getCaseId());
        assertEquals("Janet", searchResultViewItems.get(0).getFields().get("PersonFirstName"));
        assertEquals("Parker", searchResultViewItems.get(0).getFields().get("PersonLastName"));
        assertEquals("123", ((Map) searchResultViewItems.get(0).getFields().get("PersonAddress"))
            .get("AddressLine1"));
        assertEquals("Fake Street", ((Map) searchResultViewItems.get(0).getFields().get("PersonAddress"))
            .get("AddressLine2"));
        assertEquals("Hexton", ((Map) searchResultViewItems.get(0).getFields().get("PersonAddress"))
            .get("AddressLine3"));
        assertEquals("England", ((Map) searchResultViewItems.get(0).getFields().get("PersonAddress"))
            .get("Country"));
        assertEquals("HX08 5TG", ((Map) searchResultViewItems.get(0).getFields().get("PersonAddress"))
            .get("Postcode"));

        assertNotNull(searchResultViewItems.get(1).getCaseId());
        assertEquals("George", searchResultViewItems.get(1).getFields().get("PersonFirstName"));
        assertEquals("Roof", searchResultViewItems.get(1).getFields().get("PersonLastName"));
        assertEquals("Flat 9", ((Map) searchResultViewItems.get(1).getFields().get("PersonAddress"))
            .get("AddressLine1"));
        assertEquals("2 Hubble Avenue", ((Map) searchResultViewItems.get(1).getFields().get("PersonAddress"))
            .get("AddressLine2"));
        assertEquals("ButtonVillie", ((Map) searchResultViewItems.get(1).getFields().get("PersonAddress"))
            .get("AddressLine3"));
        assertEquals("Wales", ((Map) searchResultViewItems.get(1).getFields().get("PersonAddress"))
            .get("Country"));
        assertEquals("W11 5DF", ((Map) searchResultViewItems.get(1).getFields().get("PersonAddress"))
            .get("Postcode"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void validNonBasketSearchAllWithCaseType() throws Exception {
        // Check that we have the expected test data set size, this is to ensure
        // that state filtering is correct
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());

        final MvcResult result = mockMvc.perform(get(GET_CASES)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .param("case_type", TEST_CASE_TYPE)
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is(200))
                                        .andReturn();

        final SearchResultView searchResultView = mapper.readValue(result.getResponse().getContentAsString(),
                                                                   SearchResultView.class);
        final List<SearchResultViewColumn> searchResultViewColumns = searchResultView.getSearchResultViewColumns();
        final List<SearchResultViewItem> searchResultViewItems = searchResultView.getSearchResultViewItems();

        assertEquals("Incorrect view columns count", 3, searchResultViewColumns.size());

        assertEquals("PersonFirstName", searchResultViewColumns.get(0).getCaseFieldId());
        assertEquals("First Name", searchResultViewColumns.get(0).getLabel());
        assertEquals(1, searchResultViewColumns.get(0).getOrder().intValue());

        assertEquals("PersonLastName", searchResultViewColumns.get(1).getCaseFieldId());
        assertEquals("Last Name", searchResultViewColumns.get(1).getLabel());
        assertEquals(1, searchResultViewColumns.get(1).getOrder().intValue());

        assertEquals("PersonAddress", searchResultViewColumns.get(2).getCaseFieldId());
        assertEquals("Address", searchResultViewColumns.get(2).getLabel());
        assertEquals(1, searchResultViewColumns.get(2).getOrder().intValue());

        assertEquals("Incorrect view items count", 2, searchResultViewItems.size());

        assertNotNull(searchResultViewItems.get(0).getCaseId());
        assertEquals("Janet", searchResultViewItems.get(0).getFields().get("PersonFirstName"));
        assertEquals("Parker", searchResultViewItems.get(0).getFields().get("PersonLastName"));
        assertEquals("123", ((Map) searchResultViewItems.get(0).getFields().get("PersonAddress"))
            .get("AddressLine1"));
        assertEquals("Fake Street", ((Map) searchResultViewItems.get(0).getFields().get("PersonAddress"))
            .get("AddressLine2"));
        assertEquals("Hexton", ((Map) searchResultViewItems.get(0).getFields().get("PersonAddress"))
            .get("AddressLine3"));
        assertEquals("England", ((Map) searchResultViewItems.get(0).getFields().get("PersonAddress"))
            .get("Country"));
        assertEquals("HX08 5TG", ((Map) searchResultViewItems.get(0).getFields().get("PersonAddress"))
            .get("Postcode"));

        assertNotNull(searchResultViewItems.get(1).getCaseId());
        assertEquals("George", searchResultViewItems.get(1).getFields().get("PersonFirstName"));
        assertEquals("Roof", searchResultViewItems.get(1).getFields().get("PersonLastName"));
        assertEquals("Flat 9", ((Map) searchResultViewItems.get(1).getFields().get("PersonAddress"))
            .get("AddressLine1"));
        assertEquals("2 Hubble Avenue", ((Map) searchResultViewItems.get(1).getFields().get("PersonAddress"))
            .get("AddressLine2"));
        assertEquals("ButtonVillie", ((Map) searchResultViewItems.get(1).getFields().get("PersonAddress"))
            .get("AddressLine3"));
        assertEquals("Wales", ((Map) searchResultViewItems.get(1).getFields().get("PersonAddress"))
            .get("Country"));
        assertEquals("W11 5DF", ((Map) searchResultViewItems.get(1).getFields().get("PersonAddress"))
            .get("Postcode"));
    }

    @Test
    @Ignore // this should default to Search view,
    public void missingViewParam() throws Exception {
        mockMvc.perform(get(GET_CASES)
                            .contentType(JSON_CONTENT_TYPE)
                            .param("case_type", TEST_CASE_TYPE)
                            .param("jurisdiction", TEST_JURISDICTION)
                            .param("state", "CaseCreated")
                            .header(AUTHORIZATION, "Bearer user1"))
               .andExpect(status().is(400));
    }

    @Test
    public void invalidJurisdiction() throws Exception {
        mockMvc.perform(get(GET_CASES_INVALID_JURISDICTION)
                            .contentType(JSON_CONTENT_TYPE)
                            .param("view", WORKBASKET)
                            .header(AUTHORIZATION, "Bearer user1"))
               .andExpect(status().is(404));
    }

    @Test
    public void invalidCaseType() throws Exception {
        mockMvc.perform(get(GET_CASES_INVALID_CASE_TYPE)
                            .contentType(JSON_CONTENT_TYPE)
                            .param("view", WORKBASKET)
                            .header(AUTHORIZATION, "Bearer user1"))
               .andExpect(status().is(404));
    }

    @Test
    public void validGetDraft() throws Exception {

        final MvcResult result = mockMvc.perform(get(GET_DRAFT)
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
        assertEquals("Unexpected Jurisdiction Id", TEST_JURISDICTION, caseViewJurisdiction.getId());
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

        final CaseViewField[] addressFields = addressTab.getFields();
        assertThat("Fields are not empty", addressFields, arrayWithSize(0));
        assertEquals("Unexpected number of fields", 0, addressFields.length);

        final CaseViewTab documentTab = caseViewTabs[2];
        assertNotNull("First tab is null", documentTab);
        assertEquals("Unexpected tab Id", "DocumentsTab", documentTab.getId());
        assertEquals("Unexpected tab label", "Documents", documentTab.getLabel());
        assertEquals("Unexpected tab show condition", "PersonFistName=\"George\"", documentTab.getShowCondition());
        assertEquals("Unexpected tab order", 3, documentTab.getOrder().intValue());

        final CaseViewField[] documentFields = documentTab.getFields();
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
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void validGetCase() throws Exception {

        // Check that we have the expected test data set size
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());

        final MvcResult result = mockMvc.perform(get(GET_CASE)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is(200))
                                        .andReturn();

        final CaseView caseView = mapper.readValue(result.getResponse().getContentAsString(), CaseView.class);
        assertNotNull("Case View is null", caseView);
        assertEquals("Unexpected Case ID", Long.valueOf(1504259907353529L), Long.valueOf(caseView.getCaseId()));

        final CaseViewType caseViewType = caseView.getCaseType();
        assertNotNull("Case View Type is null", caseViewType);
        assertEquals("Unexpected Case Type Id", "TestAddressBookCase", caseViewType.getId());
        assertEquals("Unexpected Case Type name", "Test Address Book Case", caseViewType.getName());
        assertEquals("Unexpected Case Type description", "Test Address Book Case", caseViewType.getDescription());

        final CaseViewJurisdiction caseViewJurisdiction = caseViewType.getJurisdiction();
        assertNotNull("Case View Jurisdiction is null", caseViewJurisdiction);
        assertEquals("Unexpected Jurisdiction Id", TEST_JURISDICTION, caseViewJurisdiction.getId());
        assertEquals("Unexpected Jurisdiction name", "Test", caseViewJurisdiction.getName());
        assertEquals("Unexpected Jurisdiction description", "Test Jurisdiction", caseViewJurisdiction
            .getDescription());

        final ProfileCaseState profileCaseState = caseView.getState();
        assertNotNull("Profile Case State is null", profileCaseState);
        assertEquals("Unexpected State Id", "CaseCreated", profileCaseState.getId());
        assertEquals("Unexpected State name", "Case Created", profileCaseState.getName());

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
        assertEquals("Unexpected Field value", "Janet", firstNameField.getValue());

        final CaseViewField lastNameField = nameFields[1];
        assertNotNull("Field is null", lastNameField);
        assertEquals("Unexpected Field id", "PersonLastName", lastNameField.getId());
        assertEquals("Unexpected Field label", "Last Name", lastNameField.getLabel());
        assertEquals("Unexpected Field order", 2, lastNameField.getOrder().intValue());
        assertEquals("Unexpected Field show condition", "PersonFirstName=\"Tom\"", lastNameField.getShowCondition());
        assertEquals("Unexpected Field field type", "Text", lastNameField.getFieldTypeDefinition().getType());
        assertEquals("Unexpected Field value", "Parker", lastNameField.getValue());

        final CaseViewTab addressTab = caseViewTabs[1];
        assertNotNull("First tab is null", addressTab);
        assertEquals("Unexpected tab Id", "AddressTab", addressTab.getId());
        assertEquals("Unexpected tab label", "Address", addressTab.getLabel());
        assertEquals("Unexpected tab show condition", "PersonLastName=\"Smith\"", addressTab.getShowCondition());
        assertEquals("Unexpected tab order", 2, addressTab.getOrder().intValue());

        final CaseViewField[] addressFields = addressTab.getFields();
        assertNotNull("Fields are null", addressFields);
        assertEquals("Unexpected number of fields", 1, addressFields.length);

        final CaseViewField addressField = addressFields[0];
        assertNotNull("Field is null", addressField);
        assertEquals("Unexpected Field id", "PersonAddress", addressField.getId());
        assertEquals("Unexpected Field label", "Address", addressField.getLabel());
        assertEquals("Unexpected Field order", 1, addressField.getOrder().intValue());
        assertEquals("Unexpected Field show condition", "PersonLastName=\"Smart\"", addressField.getShowCondition());
        assertEquals("Unexpected Field field type", "Address", addressField.getFieldTypeDefinition().getType());

        final Map addressNode = (Map) addressField.getValue();
        assertNotNull("Null address value", addressNode);
        assertEquals("Unexpected address value", "123", addressNode.get("AddressLine1"));
        assertEquals("Unexpected address value", "Fake Street", addressNode.get("AddressLine2"));
        assertEquals("Unexpected address value", "Hexton", addressNode.get("AddressLine3"));
        assertEquals("Unexpected address value", "England", addressNode.get("Country"));
        assertEquals("Unexpected address value", "HX08 5TG", addressNode.get("Postcode"));

        final CaseViewTab documentTab = caseViewTabs[2];
        assertNotNull("First tab is null", documentTab);
        assertEquals("Unexpected tab Id", "DocumentsTab", documentTab.getId());
        assertEquals("Unexpected tab label", "Documents", documentTab.getLabel());
        assertEquals("Unexpected tab show condition", "PersonFistName=\"George\"", documentTab.getShowCondition());
        assertEquals("Unexpected tab order", 3, documentTab.getOrder().intValue());

        final CaseViewField[] documentFields = documentTab.getFields();
        assertNotNull("Fields are null", documentFields);
        assertEquals("Unexpected number of fields", 1, documentFields.length);

        final CaseViewField documentField = documentFields[0];
        assertNotNull("Field is null", documentField);
        assertEquals("Unexpected Field id", "D8Document", documentField.getId());
        assertEquals("Unexpected Field label", "Document", documentField.getLabel());
        assertEquals("Unexpected Field show condition", "PersonLastName=\"Dumb\"", documentField.getShowCondition());
        assertEquals("Unexpected Field order", 1, documentField.getOrder().intValue());
        assertEquals("Unexpected Field field type", "Document", documentField.getFieldTypeDefinition().getType());

        final Map documentNode = (Map) documentField.getValue();
        assertNotNull("Null address value", documentNode);
        assertEquals("Unexpected address value",
                     "http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1",
                     documentNode.get("document_url"));
        assertEquals("Unexpected address value",
                     "http://localhost:[port]/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1/binary", documentNode
                         .get("document_binary_url"));
        assertEquals("Unexpected address value",
                     "Seagulls_Square.jpg", documentNode.get("document_filename"));

        final CaseViewEvent[] events = caseView.getEvents();
        assertNotNull("Events are null", events);
        assertEquals("Unexpected number of events", 2, events.length);

        final CaseViewEvent event = events[0];
        assertNotNull("Null event value", event);
        assertEquals("Event ID", "Goodness", event.getEventId());
        assertEquals("Event Name", "GRACIOUS", event.getEventName());
        assertEquals("Current case state id", "state4", event.getStateId());
        assertEquals("Current case state name", "Case in state 4", event.getStateName());
        assertEquals("User ID", "0", event.getUserId());
        assertEquals("User First name", "Justin", event.getUserFirstName());
        assertEquals("User Last name", "Smith", event.getUserLastName());
        assertEquals("Summary", "The summary 2", event.getSummary());
        assertEquals("Comment", "Some comment 2", event.getComment());
        assertEquals("Timestamp", "2017-05-09T15:31:43", event.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME));

        final CaseViewEvent event2 = events[1];
        assertNotNull("Null event value", event2);
        assertEquals("Event ID", "TEST_EVENT", event2.getEventId());
        assertEquals("Event Name", "TEST TRIGGER_EVENT NAME", event2.getEventName());
        assertEquals("Current case state id", "CaseCreated", event2.getStateId());
        assertEquals("Current case state name", "Created a case", event2.getStateName());
        assertEquals("User ID", "0", event2.getUserId());
        assertEquals("User First name", "Justin", event2.getUserFirstName());
        assertEquals("User Last name", "Smith", event2.getUserLastName());
        assertEquals("Summary", "The summary", event2.getSummary());
        assertEquals("Comment", "Some comment", event2.getComment());
        assertEquals("Timestamp", "2017-05-09T14:31:43", event2.getTimestamp()
            .format(DateTimeFormatter.ISO_DATE_TIME));

        final CaseViewActionableEvent[] actionableEvents = caseView.getActionableEvents();
        assertNotNull("Triggers are null", actionableEvents);
        assertEquals("Should only get valid triggers", 1, actionableEvents.length);

        // checks Trigger 1 content
        assertEquals("Trigger ID", "HAS_PRE_STATES_EVENT", actionableEvents[0].getId());
        assertEquals("Trigger Name", "HAS PRE STATES EVENT", actionableEvents[0].getName());
        assertEquals("Trigger Description", "Test event for non null pre-states", actionableEvents[0]
            .getDescription());
        assertEquals("Trigger Order", Integer.valueOf(1), actionableEvents[0].getOrder());

        // audit-log assertions
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditRepository).save(captor.capture());

        assertThat(captor.getValue().getOperationType(), is(AuditOperationType.SEARCH_CASE.getLabel()));
        assertThat(captor.getValue().getJurisdiction(), is(TEST_JURISDICTION));
        assertThat(captor.getValue().getCaseType(), is(TEST_CASE_TYPE));
        assertThat(captor.getValue().getCaseId(), is("1504259907353529"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturnEmptyTriggerEventListSinceNoAcccessForStateUpdate() throws Exception {

        // Check that we have the expected test data set size
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());

        // set the state to a state where this user has no update access
        template.execute("UPDATE case_data SET state='some-state' WHERE reference='1504259907353529'");

        final MvcResult result = mockMvc.perform(get(GET_CASE)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is(200))
                                        .andReturn();

        final CaseView caseView = mapper.readValue(result.getResponse().getContentAsString(), CaseView.class);
        assertNotNull("Case View is null", caseView);
        assertEquals("Unexpected Case ID", Long.valueOf(1504259907353529L), Long.valueOf(caseView.getCaseId()));

        final CaseViewActionableEvent[] actionableEvents = caseView.getActionableEvents();
        assertEquals("Should only no valid triggers", 0, actionableEvents.length);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldFilterOutAuditEventIfNoEventReadAccess() throws Exception {

        // Check that we have the expected test data set size
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());

        final MvcResult result = mockMvc.perform(get(GET_CASE_NO_EVENT_READ_ACCESS)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is(200))
                                        .andReturn();

        final CaseView caseView = mapper.readValue(result.getResponse().getContentAsString(), CaseView.class);
        final CaseViewEvent[] events = caseView.getEvents();
        assertNotNull("Events are null", events);
        assertEquals("Unexpected number of events", 1, events.length);

        final CaseViewEvent event = events[0];
        assertNotNull("Null event value", event);
        assertEquals("Event ID", "Goodness", event.getEventId());
        assertEquals("Event Name", "GRACIOUS", event.getEventName());
        assertEquals("Current case state id", "state4", event.getStateId());
        assertEquals("Current case state name", "Case in state 4", event.getStateName());
        assertEquals("User ID", "0", event.getUserId());
        assertEquals("User First name", "Justin", event.getUserFirstName());
        assertEquals("User Last name", "Smith", event.getUserLastName());
        assertEquals("Summary", "The summary 2", event.getSummary());
        assertEquals("Comment", "Some comment 2", event.getComment());
        assertEquals("Timestamp", "2017-05-09T15:31:43", event.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_private_cases.sql"})
    public void validGetCase_classificationTooHigh() throws Exception {

        mockMvc.perform(
            get(GET_PRIVATE_CASE)
                .contentType(JSON_CONTENT_TYPE)
                .header(AUTHORIZATION, "Bearer user1")
        )
               .andExpect(status().is(404))
               .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn400WhenGetCaseIfCaseReferenceInvalid() throws Exception {

        // Check that we have the expected test data set size
        List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());

        mockMvc.perform(get(GET_CASE_INVALID_REFERENCE)
            .contentType(JSON_CONTENT_TYPE)
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(400))
            .andReturn();

        resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_complex_case.sql"})
    public void validGetComplexCase() throws Exception {

        // Check that we have the expected test data set size
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", 2, resultList.size());

        final MvcResult result = mockMvc.perform(get(GET_COMPLEX_CASE)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is(200))
                                        .andReturn();

        final CaseView caseView = mapper.readValue(result.getResponse().getContentAsString(), CaseView.class);
        assertNotNull("Case View is null", caseView);
        assertEquals("Unexpected Case ID", Long.valueOf(1504259907353537L), Long.valueOf(caseView.getCaseId()));

        final CaseViewType caseViewType = caseView.getCaseType();
        assertNotNull("Case View Type is null", caseViewType);
        assertEquals("Unexpected Case Type Id", "TestComplexAddressBookCase", caseViewType.getId());
        assertEquals("Unexpected Case Type name", "Test Complex Address Book Case", caseViewType.getName());
        assertEquals("Unexpected Case Type description", "Test Complex Address Book Case", caseViewType
            .getDescription());

        final CaseViewJurisdiction caseViewJurisdiction = caseViewType.getJurisdiction();
        assertNotNull("Case View Jurisdiction is null", caseViewJurisdiction);
        assertEquals("Unexpected Jurisdiction Id", TEST_JURISDICTION, caseViewJurisdiction.getId());
        assertEquals("Unexpected Jurisdiction name", "Test", caseViewJurisdiction.getName());
        assertEquals("Unexpected Jurisdiction description", "Test Jurisdiction", caseViewJurisdiction
            .getDescription());

        final CaseViewTab[] caseViewTabs = caseView.getTabs();
        assertNotNull("Tabs are null", caseViewTabs);
        assertEquals("Unexpected number of tabs", 2, caseViewTabs.length);

        final CaseViewTab companyTab = caseViewTabs[0];
        assertNotNull("First tab is null", companyTab);
        assertEquals("Unexpected tab Id", "CompanyTab", companyTab.getId());
        assertEquals("Unexpected tab label", "Company", companyTab.getLabel());
        assertEquals("Unexpected tab order", 1, companyTab.getOrder().intValue());

        final CaseViewField[] companyFields = companyTab.getFields();
        assertNotNull("Fields are null", companyFields);
        assertEquals("Unexpected number of fields", 1, companyFields.length);

        final CaseViewField companyField = companyFields[0];
        assertNotNull("Field is null", companyField);
        assertEquals("Unexpected Field id", "Company", companyField.getId());
        assertEquals("Unexpected Field label", "Company", companyField.getLabel());
        assertEquals("Unexpected Field order", 1, companyField.getOrder().intValue());
        assertEquals("Unexpected Field field type", "Company", companyField.getFieldTypeDefinition().getId());
        assertEquals("Unexpected Field field type", "Complex", companyField.getFieldTypeDefinition().getType());

        // Check complex fields are mapped correctly
        final List<CaseFieldDefinition> companyComplexFields =
            companyField.getFieldTypeDefinition().getComplexFields();
        assertEquals("Unexpected number of Complex Fields", 2, companyComplexFields.size());

        // Get the Address complex field from the Company
        final CaseFieldDefinition addressField = companyComplexFields.get(1);
        assertEquals("Unexpected Field id", "PostalAddress", addressField.getId());
        assertEquals("Unexpected Field label", "Postal Address", addressField.getLabel());
        assertEquals("Unexpected Field type", "Address", addressField.getFieldTypeDefinition().getId());
        assertEquals("Unexpected Field type", "Complex", addressField.getFieldTypeDefinition().getType());
        assertEquals("Unexpected number of complex fields", 6, addressField.getFieldTypeDefinition().getComplexFields()
            .size());

        // Get the Occupant complex field from the Address
        final CaseFieldDefinition occupantField = addressField.getFieldTypeDefinition().getComplexFields().get(5);
        assertEquals("Unexpected Field id", "Occupant", occupantField.getId());
        assertEquals("Unexpected Field label", "Occupant", occupantField.getLabel());
        assertEquals("Unexpected Field type", "Person", occupantField.getFieldTypeDefinition().getId());
        assertEquals("Unexpected Field type", "Complex", occupantField.getFieldTypeDefinition().getType());
        assertEquals("Unexpected number of complex fields", 7, occupantField.getFieldTypeDefinition()
            .getComplexFields().size());

        // Check all field values are mapped correctly
        Map companyNode = (Map) companyField.getValue();
        assertEquals("Unexpected Field value", "Test Company", companyNode.get("Name"));
        assertEquals("Unexpected Field value", "New Country", ((Map) companyNode.get("PostalAddress")).get("Country"));
        Map addressNode = (Map) companyNode.get("PostalAddress");
        assertEquals("Unexpected Field value", "PP01 PPQ", addressNode.get("Postcode"));
        assertEquals("Unexpected Field value", "123", addressNode.get("AddressLine1"));
        assertEquals("Unexpected Field value", "New Street", addressNode.get("AddressLine2"));
        assertEquals("Unexpected Field value", "Some Town", addressNode.get("AddressLine3"));
        Map occupantNode = ((Map) addressNode.get("Occupant"));
        assertEquals("Unexpected Field value", "Mr", occupantNode.get("Title"));
        assertEquals("Unexpected Field value", "The", occupantNode.get("FirstName"));
        assertEquals("Unexpected Field value", "Test", occupantNode.get("MiddleName"));
        assertEquals("Unexpected Field value", "Occupant", occupantNode.get("LastName"));
        assertEquals("Unexpected Field value", "01/01/1990", occupantNode.get("DateOfBirth"));
        assertEquals("Unexpected Field value", "MARRIAGE", occupantNode.get("MarritalStatus"));
        assertEquals("Unexpected Field value", "AB112233A", occupantNode.get("NationalInsuranceNumber"));

        final CaseViewTab otherInfoTab = caseViewTabs[1];
        assertNotNull("First tab is null", otherInfoTab);
        assertEquals("Unexpected tab Id", "OtherInfoTab", otherInfoTab.getId());
        assertEquals("Unexpected tab label", "Other Info", otherInfoTab.getLabel());
        assertEquals("Unexpected tab order", 2, otherInfoTab.getOrder().intValue());

        final CaseViewField[] otherInfoFields = otherInfoTab.getFields();
        assertNotNull("Fields are null", otherInfoFields);
        assertEquals("Unexpected number of fields", 1, otherInfoFields.length);

        final CaseViewField otherInfoField = otherInfoFields[0];
        assertNotNull("Field is null", otherInfoField);
        assertEquals("Unexpected Field id", "OtherInfo", otherInfoField.getId());
        assertEquals("Unexpected Field label", "Other Info", otherInfoField.getLabel());
        assertEquals("Unexpected Field order", 1, otherInfoField.getOrder().intValue());
        assertEquals("Unexpected Field field type", "Text", otherInfoField.getFieldTypeDefinition().getType());
        assertEquals("Unexpected Field value", "Extra Info", otherInfoField.getValue());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_complex_case.sql"})
    public void nullCaseDetailsTest() throws Exception {
        // Check that we have the expected test data set size
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", 2, resultList.size());

        // Given - a Case Id which doesn't exist (99)
        // When - trying to find the Case
        // Then - assert that the expected error is returned
        mockMvc.perform(get(GET_NULL_CASE)
                            .contentType(JSON_CONTENT_TYPE)
                            .header(AUTHORIZATION, "Bearer user1"))
               .andExpect(status().is(404));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_complex_case.sql"})
    public void invalidStateTest() throws Exception {
        // Check that we have the expected test data set size
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", 2, resultList.size());

        // Given - a Case which has an invalid state ("Invalid")
        // When - trying to find the Case
        // Then - assert that the expected error is returned
        mockMvc.perform(get(GET_CASE_INVALID_STATE)
                            .contentType(JSON_CONTENT_TYPE)
                            .header(AUTHORIZATION, "Bearer user1"))
               .andExpect(status().is(404));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getEventTriggerForCaseType_valid() throws Exception {
        // Check that we have the expected test data set size
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());

        final MvcResult result = mockMvc.perform(get(GET_EVENT_TRIGGER_FOR_CASE_TYPE_VALID)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is(200))
                                        .andReturn();

        final CaseUpdateViewEvent caseUpdateViewEvent = mapper.readValue(result.getResponse().getContentAsString(),
                                                               CaseUpdateViewEvent.class);
        assertNotNull("Event Trigger is null", caseUpdateViewEvent);

        assertThat("Unexpected Case ID", caseUpdateViewEvent.getCaseId(), is(nullValue()));
        assertEquals("Unexpected Event ID", "NO_PRE_STATES_EVENT", caseUpdateViewEvent.getId());
        assertEquals("Unexpected Event Name", "NO PRE STATES EVENT", caseUpdateViewEvent.getName());
        assertEquals("Unexpected Event Show Event Notes", true, caseUpdateViewEvent.getShowEventNotes());
        assertEquals("Unexpected Event Description", "Test event for null pre-states", caseUpdateViewEvent
            .getDescription());
        assertEquals("Unexpected Case Fields", 2, caseUpdateViewEvent.getCaseFields().size());

        final CaseViewField field1 = caseUpdateViewEvent.getCaseFields().get(0);
        assertThat(field1.getLabel(), equalTo("First name"));
        assertThat(field1.getOrder(), is(nullValue()));
        assertThat(field1.getFieldTypeDefinition().getId(), equalTo("Text"));
        assertThat(field1.getFieldTypeDefinition().getType(), equalTo("Text"));
        assertThat(field1.getId(), equalTo("PersonFirstName"));
        assertThat(field1.getDisplayContext(), equalTo(READONLY));

        final CaseViewField field2 = caseUpdateViewEvent.getCaseFields().get(1);
        assertThat(field2.getLabel(), equalTo("Last name"));
        assertThat(field2.getOrder(), is(nullValue()));
        assertThat(field2.getFieldTypeDefinition().getId(), equalTo("Text"));
        assertThat(field2.getFieldTypeDefinition().getType(), equalTo("Text"));
        assertThat(field2.getId(), equalTo("PersonLastName"));
        assertThat(field2.getDisplayContext(), equalTo("OPTIONAL"));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getEventTriggerForCaseType_invalidPreState() throws Exception {
        mockMvc.perform(get(GET_EVENT_TRIGGER_FOR_CASE_TYPE_INVALID_PRE_STATES)
                            .contentType(JSON_CONTENT_TYPE)
                            .header(AUTHORIZATION, "Bearer user1"))
               .andExpect(status().is(422))
               .andExpect(content().string(containsString("The case status did not qualify for the event")))
               .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getEventTriggerForCaseType_invalidEvent() throws Exception {
        mockMvc.perform(get(GET_EVENT_TRIGGER_FOR_CASE_TYPE_INVALID_EVENT)
                            .contentType(JSON_CONTENT_TYPE)
                            .header(AUTHORIZATION, "Bearer user1"))
               .andExpect(status().is(404))
               .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getEventTriggerForCase_valid() throws Exception {

        // Check that we have the expected test data set size
        final List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", NUMBER_OF_CASES, resultList.size());

        final MvcResult result = mockMvc.perform(get(GET_EVENT_TRIGGER_FOR_CASE_VALID)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is(200))
                                        .andReturn();

        final CaseUpdateViewEvent caseUpdateViewEvent = mapper.readValue(result.getResponse().getContentAsString(),
                                                               CaseUpdateViewEvent.class);
        assertNotNull("Event Trigger is null", caseUpdateViewEvent);

        assertEquals("Unexpected Case Reference", "1504259907353545", caseUpdateViewEvent.getCaseId());
        assertEquals("Unexpected Event ID", "HAS_PRE_STATES_EVENT", caseUpdateViewEvent.getId());
        assertEquals("Unexpected Event Name", "HAS PRE STATES EVENT", caseUpdateViewEvent.getName());
        assertEquals("Unexpected Show Event Notes", false, caseUpdateViewEvent.getShowEventNotes());
        assertEquals("Unexpected Event Description", "Test event for non null pre-states", caseUpdateViewEvent
            .getDescription());
        assertEquals("Unexpected Case Fields", 2, caseUpdateViewEvent.getCaseFields().size());

        final CaseViewField field1 = caseUpdateViewEvent.getCaseFields().get(0);
        assertThat(field1.getValue(), equalTo("George"));
        assertThat(field1.getLabel(), equalTo("First name"));
        assertThat(field1.getOrder(), is(nullValue()));
        assertThat(field1.getFieldTypeDefinition().getId(), equalTo("Text"));
        assertThat(field1.getFieldTypeDefinition().getType(), equalTo("Text"));
        assertThat(field1.getId(), equalTo("PersonFirstName"));
        assertThat(field1.getDisplayContext(), equalTo(READONLY));
        assertThat(field1.getShowSummaryContentOption(), equalTo(2));

        final CaseViewField field2 = caseUpdateViewEvent.getCaseFields().get(1);
        assertThat(field2.getValue(), equalTo("Roof"));
        assertThat(field2.getLabel(), equalTo("Last name"));
        assertThat(field2.getOrder(), is(nullValue()));
        assertThat(field2.getFieldTypeDefinition().getId(), equalTo("Text"));
        assertThat(field2.getFieldTypeDefinition().getType(), equalTo("Text"));
        assertThat(field2.getId(), equalTo("PersonLastName"));
        assertThat(field2.getDisplayContext(), equalTo("OPTIONAL"));
        assertThat(field2.getShowSummaryContentOption(), equalTo(1));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_private_cases.sql"})
    public void getEventTriggerForCase_classificationTooHigh() throws Exception {

        mockMvc.perform(
            get(GET_EVENT_TRIGGER_FOR_CASE_PRIVATE)
                .contentType(JSON_CONTENT_TYPE)
                .header(AUTHORIZATION, "Bearer user1")
        )
               .andExpect(status().is(404))
               .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getEventTriggerForCase_invalidPreState() throws Exception {
        mockMvc.perform(get(GET_EVENT_TRIGGER_FOR_CASE_INVALID_STATE)
                            .contentType(JSON_CONTENT_TYPE)
                            .header(AUTHORIZATION, "Bearer user1"))
               .andExpect(status().is(422))
               .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getEventTriggerForCase_invalidCaseReference() throws Exception {
        mockMvc.perform(get(GET_EVENT_TRIGGER_FOR_CASE_INVALID_CASE_REFERENCE)
                            .contentType(JSON_CONTENT_TYPE)
                            .header(AUTHORIZATION, "Bearer user1"))
               .andExpect(status().is(400))
               .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getEventTriggerForCase_invalidCase() throws Exception {
        mockMvc.perform(get(GET_EVENT_TRIGGER_FOR_CASE_INVALID_CASE)
                            .contentType(JSON_CONTENT_TYPE)
                            .header(AUTHORIZATION, "Bearer user1"))
               .andExpect(status().is(404))
               .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void getEventTriggerForCase_invalidEvent() throws Exception {
        mockMvc.perform(get(GET_EVENT_TRIGGER_FOR_CASE_INVALID_EVENT)
                            .contentType(JSON_CONTENT_TYPE)
                            .header(AUTHORIZATION, "Bearer user1"))
               .andExpect(status().is(404))
               .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn400WhenGetCaseTypesWithNoAccessParam() throws Exception {
        mockMvc.perform(get(GET_CASE_TYPES_NO_ACCESS_PARAM)
                            .contentType(JSON_CONTENT_TYPE)
                            .header(AUTHORIZATION, "Bearer user1"))
               .andExpect(status().is(400))
               .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenGetCaseTypesWithMisnamedAccessParam() throws Exception {
        mockMvc.perform(get(GET_CASE_TYPES_MISNAMED_ACCESS_PARAM)
                            .contentType(JSON_CONTENT_TYPE)
                            .header(AUTHORIZATION, "Bearer user1"))
               .andExpect(status().is(404))
               .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn404WhenGetCaseTypesWithInvalidAccessParam() throws Exception {
        mockMvc.perform(get(GET_CASE_TYPES_INVALID_ACCESS_PARAM)
                            .contentType(JSON_CONTENT_TYPE)
                            .header(AUTHORIZATION, "Bearer user1"))
               .andExpect(status().is(404))
               .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldGetCaseTypesForReadAccess() throws Exception {
        final MvcResult result = mockMvc.perform(get(GET_CASE_TYPES_READ_ACCESS)
                                                     .contentType(JSON_CONTENT_TYPE)
                                                     .header(AUTHORIZATION, "Bearer user1"))
                                        .andExpect(status().is(200))
                                        .andReturn();

        final CaseTypeDefinition[] caseTypeDefinitions = mapper.readValue(result.getResponse().getContentAsString(),
            CaseTypeDefinition[].class);

        assertAll(
            () -> assertThat(caseTypeDefinitions.length, is(equalTo(3))),
            () -> assertThat(caseTypeDefinitions[0], hasProperty("id",
                equalTo("TestAddressBookCase"))),
            () -> assertThat(caseTypeDefinitions[0].getEvents(), hasSize(1)),
            // added a create event with read access for testing drafts properly
            () -> assertThat(caseTypeDefinitions[0].getCaseFieldDefinitions(), hasSize(3)),
            () -> assertThat(caseTypeDefinitions[0].getCaseFieldDefinitions(),
                        hasItems(hasProperty("id", equalTo("PersonFirstName")),
                        hasProperty("id", equalTo("PersonLastName")),
                        hasProperty("id", equalTo("PersonAddress")))),
            () -> assertThat(caseTypeDefinitions[1],
                        hasProperty("id", equalTo("TestAddressBookCase3"))),
            () -> assertThat(caseTypeDefinitions[1].getEvents(), hasSize(1)),
            () -> assertThat(caseTypeDefinitions[1].getEvents(),
                        hasItems(hasProperty("id", equalTo("TEST_EVENT_3")))),
            () -> assertThat(caseTypeDefinitions[1].getCaseFieldDefinitions(), hasSize(2)),
            () -> assertThat(caseTypeDefinitions[1].getCaseFieldDefinitions(),
                        hasItems(hasProperty("id", equalTo("PersonLastName")),
                                 hasProperty("id", equalTo("PersonAddress")))),
            () -> assertThat(caseTypeDefinitions[2],
                        hasProperty("id", equalTo("TestAddressBookCaseNoReadFieldAccess"))),
            () -> assertThat(caseTypeDefinitions[2].getEvents(), hasSize(1)),
            () -> assertThat(caseTypeDefinitions[2].getEvents(),
                        hasItems(hasProperty("id", equalTo("TEST_EVENT_NO_READ_FIELD_ACCESS")))),
            () -> assertThat(caseTypeDefinitions[2].getCaseFieldDefinitions(), hasSize(2)),
            () -> assertThat(caseTypeDefinitions[2].getCaseFieldDefinitions(),
                hasItems(hasProperty("id", equalTo("PersonLastName")),
                         hasProperty("id", equalTo("PersonAddress"))))
        );
    }


    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldGetJurisdictionsForReadAccess() throws Exception {
        final MvcResult result = mockMvc.perform(get(GET_JURISDICTIONS_READ_ACCESS)
            .contentType(JSON_CONTENT_TYPE)
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        final JurisdictionDisplayProperties[] jurisdictions = mapper.readValue(
            result.getResponse().getContentAsString(), JurisdictionDisplayProperties[].class);

        assertAll(
            () -> assertThat(jurisdictions.length, is(equalTo(3))),
            () -> assertThat(jurisdictions[0].getCaseTypeDefinitions().size(), is(equalTo(1))),
            () -> assertThat(jurisdictions[0].getCaseTypeDefinitions().get(0).getStates().size(),
                is(equalTo(2))),
            () -> assertThat(jurisdictions[0].getCaseTypeDefinitions().get(0).getEvents().size(),
                is(equalTo(2)))
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_case_event_history.sql"})
    public void shouldGetCaseHistoryForEvent() throws Exception {

        // Check that we have the expected test data set size
        List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", 1, resultList.size());

        List<AuditEvent> eventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect data initiation", 3, eventList.size());

        MvcResult result = mockMvc.perform(get(String.format(GET_CASE_HISTORY_FOR_EVENT, eventList.get(1).getId()))
                                               .contentType(JSON_CONTENT_TYPE)
                                               .header(AUTHORIZATION, "Bearer user1"))
                                  .andExpect(status().is(200))
                                  .andReturn();

        final CaseHistoryView caseHistoryView = mapper.readValue(result.getResponse().getContentAsString(),
                                                                 CaseHistoryView.class);
        assertNotNull("Case View is null", caseHistoryView);
        assertEquals("Unexpected Case ID", Long.valueOf(1504259907353529L), Long.valueOf(caseHistoryView.getCaseId()));

        final CaseViewType caseViewType = caseHistoryView.getCaseType();
        assertNotNull("Case View Type is null", caseViewType);
        assertEquals("Unexpected Case Type Id", "TestAddressBookCase", caseViewType.getId());
        assertEquals("Unexpected Case Type name", "Test Address Book Case", caseViewType.getName());
        assertEquals("Unexpected Case Type description", "Test Address Book Case", caseViewType.getDescription());

        final CaseViewJurisdiction caseViewJurisdiction = caseViewType.getJurisdiction();
        assertNotNull("Case View Jurisdiction is null", caseViewJurisdiction);
        assertEquals("Unexpected Jurisdiction Id", TEST_JURISDICTION, caseViewJurisdiction.getId());
        assertEquals("Unexpected Jurisdiction name", "Test", caseViewJurisdiction.getName());
        assertEquals("Unexpected Jurisdiction description", "Test Jurisdiction", caseViewJurisdiction
            .getDescription());

        final CaseViewTab[] caseViewTabs = caseHistoryView.getTabs();
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
        assertEquals("Unexpected Field value", "Janet", firstNameField.getValue());

        final CaseViewField lastNameField = nameFields[1];
        assertNotNull("Field is null", lastNameField);
        assertEquals("Unexpected Field id", "PersonLastName", lastNameField.getId());
        assertEquals("Unexpected Field label", "Last Name", lastNameField.getLabel());
        assertEquals("Unexpected Field order", 2, lastNameField.getOrder().intValue());
        assertEquals("Unexpected Field show condition", "PersonFirstName=\"Tom\"", lastNameField.getShowCondition());
        assertEquals("Unexpected Field field type", "Text", lastNameField.getFieldTypeDefinition().getType());
        assertEquals("Unexpected Field value", "Parker", lastNameField.getValue());

        final CaseViewTab addressTab = caseViewTabs[1];
        assertNotNull("First tab is null", addressTab);
        assertEquals("Unexpected tab Id", "AddressTab", addressTab.getId());
        assertEquals("Unexpected tab label", "Address", addressTab.getLabel());
        assertEquals("Unexpected tab show condition", "PersonLastName=\"Smith\"", addressTab.getShowCondition());
        assertEquals("Unexpected tab order", 2, addressTab.getOrder().intValue());

        final CaseViewField[] addressFields = addressTab.getFields();
        assertNotNull("Fields are null", addressFields);
        assertEquals("Unexpected number of fields", 1, addressFields.length);

        final CaseViewField addressField = addressFields[0];
        assertNotNull("Field is null", addressField);
        assertEquals("Unexpected Field id", "PersonAddress", addressField.getId());
        assertEquals("Unexpected Field label", "Address", addressField.getLabel());
        assertEquals("Unexpected Field order", 1, addressField.getOrder().intValue());
        assertEquals("Unexpected Field show condition", "PersonLastName=\"Smart\"", addressField.getShowCondition());
        assertEquals("Unexpected Field field type", "Address", addressField.getFieldTypeDefinition().getType());

        final Map addressNode = (Map) addressField.getValue();
        assertNotNull("Null address value", addressNode);
        assertEquals("Unexpected address value", "123", addressNode.get("AddressLine1"));
        assertEquals("Unexpected address value", "Fake Street", addressNode.get("AddressLine2"));
        assertEquals("Unexpected address value", "Hexton", addressNode.get("AddressLine3"));
        assertEquals("Unexpected address value", "England", addressNode.get("Country"));
        assertEquals("Unexpected address value", "HX08 5TG", addressNode.get("Postcode"));

        final CaseViewTab documentTab = caseViewTabs[2];
        assertNotNull("First tab is null", documentTab);
        assertEquals("Unexpected tab Id", "DocumentsTab", documentTab.getId());
        assertEquals("Unexpected tab label", "Documents", documentTab.getLabel());
        assertEquals("Unexpected tab show condition", "PersonFistName=\"George\"", documentTab.getShowCondition());
        assertEquals("Unexpected tab order", 3, documentTab.getOrder().intValue());

        final CaseViewField[] documentFields = documentTab.getFields();
        assertNotNull("Fields are null", documentFields);
        assertEquals("Unexpected number of fields", 1, documentFields.length);

        final CaseViewField documentField = documentFields[0];
        assertNotNull("Field is null", documentField);
        assertEquals("Unexpected Field id", "D8Document", documentField.getId());
        assertEquals("Unexpected Field label", "Document", documentField.getLabel());
        assertEquals("Unexpected Field show condition", "PersonLastName=\"Dumb\"", documentField.getShowCondition());
        assertEquals("Unexpected Field order", 1, documentField.getOrder().intValue());
        assertEquals("Unexpected Field field type", "Document", documentField.getFieldTypeDefinition().getType());

        final Map documentNode = (Map) documentField.getValue();
        final int dmApiPort = 10000;
        assertNotNull("Null address value", documentNode);
        assertEquals("Unexpected address value",
                     "http://localhost:" + dmApiPort + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1",
                     documentNode.get("document_url"));
        assertEquals("Unexpected address value",
                     "http://localhost:" + dmApiPort + "/documents/05e7cd7e-7041-4d8a-826a-7bb49dfd83d1/binary",
                     documentNode.get("document_binary_url"));
        assertEquals("Unexpected address value",
                     "Seagulls_Square.jpg", documentNode.get("document_filename"));

        final CaseViewEvent event = caseHistoryView.getEvent();
        assertNotNull("Null event value", event);
        assertEquals("Event ID", "Goodness", event.getEventId());
        assertEquals("Event Name", "GRACIOUS", event.getEventName());
        assertEquals("Current case state id", "state2", event.getStateId());
        assertEquals("Current case state name", "Case in state 2", event.getStateName());
        assertEquals("User ID", "0", event.getUserId());
        assertEquals("User First name", "Justin", event.getUserFirstName());
        assertEquals("User Last name", "Smith", event.getUserLastName());
        assertEquals("Summary", "The summary 2", event.getSummary());
        assertEquals("Comment", "Some comment 2", event.getComment());
        assertEquals("Timestamp", "2017-05-09T15:31:43", event.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = {"classpath:sql/insert_case_event_history.sql"})
    public void shouldReturn404WhenEventClassificationDoesNotMatchUserRole() throws Exception {

        // Check that we have the expected test data set size
        List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", 1, resultList.size());

        List<AuditEvent> eventList = template.query("SELECT * FROM case_event", this::mapAuditEvent);
        assertEquals("Incorrect data initiation", 3, eventList.size());

        // User role has access to PUBLIC and event is classified as PRIVATE
        mockMvc.perform(get(String.format(GET_CASE_HISTORY_FOR_EVENT, eventList.get(2).getId()))
                            .contentType(JSON_CONTENT_TYPE)
                            .header(AUTHORIZATION, "Bearer user1"))
               .andExpect(status().is(404))
               .andReturn();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_case_dcp.sql"})
    public void shouldSearchCasesWithFormattedDCP() throws Exception {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_DCP_CASEWORKER);

        List<CaseDetails> resultList = template.query("SELECT * FROM case_data", this::mapCaseData);
        assertEquals("Incorrect data initiation", 2, resultList.size());

        final MvcResult result = mockMvc.perform(get(GET_CASES_DCP)
            .contentType(JSON_CONTENT_TYPE)
            .param("state", "TODO")
            .param("view", "WORKBASKET")
            .param("case.DateField", "12-1987")
            .param("case.DateTimeField", "1999")
            .param("case.CollectionField.0.value", "02031994")
            .param("case.ComplexField.ComplexDateTimeField", "01-2004")
            // .param("last_modified_date", "22-04-20") // Uncomment after RDM-8269
            .header(AUTHORIZATION, "Bearer user1"))
            .andExpect(status().is(200))
            .andReturn();

        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        final SearchResultView searchResultView = mapper.readValue(result.getResponse().getContentAsString(),
            SearchResultView.class);

        SearchResultViewItem case1 = searchResultView.getSearchResultViewItems().get(0);

        assertAll(
            () -> assertThat(searchResultView.getSearchResultViewItems().size(), is(1)),
            () -> assertThat(case1.getCaseId(), is("1587471326156579")),
            () -> assertThat(case1.getFields().get(DATE_FIELD), is("1987-12-01")),
            () -> assertThat(case1.getFieldsFormatted().get(DATE_FIELD), is("01 Dec 1987")),
            () -> assertThat(case1.getFields().get(DATE_TIME_FIELD), is("1999-01-01T00:00:00.000")),
            () -> assertThat(case1.getFieldsFormatted().get(DATE_TIME_FIELD), is("1999")),
            () -> assertThat(arrayOf(case1.getFields().get(COLLECTION_FIELD)).get(0).get(VALUE),
                is("1994-03-02T00:00:00.000")),
            () -> assertThat(arrayOf(case1.getFieldsFormatted().get(COLLECTION_FIELD)).get(0).get(VALUE),
                is("03-1994")),
            () -> assertThat(arrayOf(case1.getFields().get(COLLECTION_FIELD)).get(1).get(VALUE),
                is("1999-09-08T00:00:00.000")),
            () -> assertThat(arrayOf(case1.getFieldsFormatted().get(COLLECTION_FIELD)).get(1).get(VALUE),
                is("09-1999")),
            () -> assertThat(mapOf(case1.getFields().get(COMPLEX_FIELD)).get(COMPLEX_DATE_TIME_FIELD),
                is("2004-01-01T00:00:00.000")),
            () -> assertThat(mapOf(case1.getFieldsFormatted().get(COMPLEX_FIELD)).get(COMPLEX_DATE_TIME_FIELD),
                is("2004")),
            () -> assertThat(case1.getFields().get(MetaData.CaseField.LAST_MODIFIED_DATE.getReference()),
                is("2020-04-22T14:45:39.987")),
            () -> assertThat(case1.getFieldsFormatted().get(MetaData.CaseField.LAST_MODIFIED_DATE.getReference()),
                is("22 Apr 2020 14:45:39"))
        );
    }
}
