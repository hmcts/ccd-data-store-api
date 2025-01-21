package uk.gov.hmcts.ccd.v2.external.controller;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;

import jakarta.inject.Inject;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.customheaders.CustomHeadersFilter;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.domain.service.common.UIDService;
import uk.gov.hmcts.ccd.util.ClientContextUtil;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.DocumentsResource;

@ActiveProfiles("test")
@AutoConfigureWireMock(port = 0)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:test.properties")
public class DocumentControllerITest {

    private static final String PRINTABLE_URL = "http://remote_host/print/cases/1565620330684549?jwt=test";
    private static final String CASE_ID = "1504259907353529";
    private static final String REQUEST_ID = "request-id";
    private static final String REQUEST_ID_VALUE = "1234567898765432";

    @Value("${wiremock.server.port}")
    private Integer wiremockPort;

    @Mock
    protected SecurityContext securityContext;

    @Mock
    protected Authentication authentication;

    @Inject
    protected WireMockServer wireMockServer;

    @Inject
    protected UIDService uidService;

    @Inject
    protected CacheManager cacheManager;

    @Inject
    private WebApplicationContext wac;

    @Inject
    private CustomHeadersFilter customHeadersFilter;

    @Inject
    protected ApplicationParams applicationParams;

    @Inject
    protected DataSource db;

    private MockMvc mockMvc;
    private String wiremockUrl;
    protected static final ObjectMapper mapper = new ObjectMapper();
    public static final JSONObject responseJson1 = new JSONObject("""
        {
            "user_task": {
                "task_data": {
                    "task_id": "0001",
                    "task_name": "Task 1 - description 1"
                },
                "complete_task": "false"
            }
        }
        """);
    private static final String caseTypeResponseString = """
        {\n
          \"id\": \"TestAddressBookCase\",\n
          \"version\": {\n
            \"number\": 1,\n
            \"live_from\": \"2017-01-01\"\n
          },\n
          \"name\": \"Test Address Book Case\",\n
          \"description\": \"Test Address Book Case\",\n
          \"printable_document_url\": \"http://localhost:%d/printables\",\n
          \"jurisdiction\": {\n
            \"id\": \"PROBATE\",\n
            \"name\": \"Test\",\n
            \"description\": \"Test Jurisdiction\"\n
          },\n
          \"security_classification\": \"PUBLIC\",\n
          \"acls\": [\n
            {\n
              \"role\": \"caseworker-probate-public\",\n
              \"create\": true,\n
              \"read\": true,\n
              \"update\": true,\n
              \"delete\": false\n
            },\n
            {\n
              \"role\": \"caseworker-probate-private\",\n
              \"create\": true,\n
              \"read\": true,\n
              \"update\": true,\n
              \"delete\": false\n
            },\n
            {\n
              \"role\": \"citizen\",\n
              \"create\": true,\n
              \"read\": true,\n
              \"update\": true,\n
              \"delete\": false\n
            }],\n
          \"events\": [\n
          ],\n
          \"states\": [\n
          ],\n
          \"case_fields\": [\n
          ]\n
        }""";

    private static String CUSTOM_CONTEXT = "";

    @BeforeEach
    public void setUp() throws ScriptException, SQLException {
        clearDownData();
        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).addFilters(customHeadersFilter).build();
        CUSTOM_CONTEXT = applicationParams.getCallbackPassthruHeaderContexts().get(0);
        mapper.registerModule(new Jackson2HalModule());
        wiremockUrl = "http://localhost:" + wiremockPort;
        ReflectionTestUtils.setField(applicationParams, "caseDefinitionHost", wiremockUrl);

        reset(uidService);
        when(uidService.validateUID(anyString())).thenCallRealMethod();
        when(uidService.checkSum(anyString())).thenCallRealMethod();
        when(uidService.checkSum(anyString(), anyBoolean())).thenCallRealMethod();

        ScriptUtils.executeSqlScript(db.getConnection(), new ClassPathResource("sql/insert_cases.sql"));
    }

    @AfterEach
    public void clearDownData() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(db);
        List<String> tables = determineTables(jdbcTemplate);
        List<String> sequences = determineSequences(jdbcTemplate);

        String truncateTablesQuery =
            "START TRANSACTION;\n"
                + tables.stream()
                    .map(record -> String.format("TRUNCATE TABLE %s CASCADE;", record))
                    .collect(Collectors.joining("\n"))
                + "\nCOMMIT;";
        jdbcTemplate.execute(truncateTablesQuery);

        sequences.forEach(sequence -> jdbcTemplate.execute("ALTER SEQUENCE " + sequence + " RESTART WITH 1"));

        cacheManager.getCacheNames().forEach(
            cacheName -> Objects.requireNonNull(cacheManager.getCache(cacheName)).clear());
    }

    @Test
    // @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WhenGetValidCaseDocuments() throws Exception {

        stubFor(WireMock
            .get(urlMatching("/api/data/case-type/TestAddressBookCase"))
                    .willReturn(aResponse()
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(String.format(caseTypeResponseString, wiremockPort))));

        final MvcResult result = mockMvc
            .perform(
                get(wiremockUrl + "/cases/" + CASE_ID + "/documents")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Accept", V2.MediaType.CASE_DOCUMENTS)
                    .header(REQUEST_ID, REQUEST_ID_VALUE)
                    .header(CUSTOM_CONTEXT, ClientContextUtil.encodeToBase64(responseJson1.toString()))
                    .header("experimental", true))
            .andExpect(status().is(200))
            .andReturn();

        final DocumentsResource documentsResource = mapper.readValue(result.getResponse().getContentAsString(),
            DocumentsResource.class);
        List<Document> documentResources = documentsResource.getDocumentResources();
        Optional<Link> self = documentsResource.getLink("self");

        assertTrue(result.getResponse().getHeaderNames().contains(CUSTOM_CONTEXT));
        assertEquals(responseJson1.toString(),
            ClientContextUtil.decodeFromBase64(result.getResponse().getHeader(CUSTOM_CONTEXT)));
        assertAll(
            () -> assertThat(self.get().getHref(),
                is(wiremockUrl + "/cases/" + CASE_ID + "/documents")),
            () -> assertThat(documentResources,
                hasItems(allOf(hasProperty("name", is("Claimant ID")),
                    hasProperty("description", is("Document identifying identity")),
                    hasProperty("type", is("ID")),
                    hasProperty("url", is(PRINTABLE_URL))))),
            () -> assertThat(documentResources,
                hasItems(allOf(hasProperty("name", is("Claimant Address")),
                    hasProperty("description", is("Document identifying address")),
                    hasProperty("type", is("Address")),
                    hasProperty("url", is(PRINTABLE_URL))))));
    }

    public void stubFor(MappingBuilder mappingBuilder) {
        wireMockServer.stubFor(mappingBuilder);
    }

    private List<String> determineTables(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForList("SELECT * FROM pg_catalog.pg_tables").stream()
            .filter(tableInfo -> tableInfo.get("schemaname").equals("public"))
            .map(tableInfo -> (String)tableInfo.get("tablename"))
            .filter(DocumentControllerITest::notFlyaway)
            .collect(Collectors.toList());
    }

    private static boolean notFlyaway(String tableName) {
        return !tableName.equals("flyway_schema_history");
    }

    private List<String> determineSequences(JdbcTemplate jdbcTemplate) {
        final String sequenceNameKey = "relname";
        String query = "SELECT c.relname FROM pg_class c WHERE c.relkind = 'S'";
        return jdbcTemplate.queryForList(query).stream()
            .map(sequenceInfo -> (String) sequenceInfo.get(sequenceNameKey))
            .collect(Collectors.toList());
    }

}
