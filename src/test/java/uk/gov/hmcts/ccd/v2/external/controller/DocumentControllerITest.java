package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.DocumentsResource;

import javax.inject.Inject;
import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SuppressWarnings("checkstyle:OperatorWrap") // too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
public class DocumentControllerITest extends WireMockBaseTest {

    private static final String PRINTABLE_URL = "http://remote_host/print/cases/1565620330684549?jwt=test";
    private static final String CASE_ID = "1504259907353529";
    private static final String REQUEST_ID = "request-id";
    private static final String REQUEST_ID_VALUE = "1234567898765432";

    @SpyBean
    private AuditRepository auditRepository;

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    protected static final ObjectMapper mapper = new ObjectMapper();
    private String caseTypeResponseString = "{\n"
        + "      \"id\": \"TestAddressBookCase\",\n"
        + "      \"version\": {\n"
        + "        \"number\": 1,\n"
        + "        \"live_from\": \"2017-01-01\"\n"
        + "      },\n"
        + "      \"name\": \"Test Address Book Case\",\n"
        + "      \"description\": \"Test Address Book Case\",\n"
        + "      \"printable_document_url\": \"http://localhost:%s/printables\",\n"
        + "      \"jurisdiction\": {\n"
        + "        \"id\": \"PROBATE\",\n"
        + "        \"name\": \"Test\",\n"
        + "        \"description\": \"Test Jurisdiction\"\n"
        + "      },\n"
        + "      \"security_classification\": \"PUBLIC\",\n"
        + "      \"acls\": [\n"
        + "        {\n"
        + "          \"role\": \"caseworker-probate-public\",\n"
        + "          \"create\": true,\n"
        + "          \"read\": true,\n"
        + "          \"update\": true,\n"
        + "          \"delete\": false\n"
        + "        },\n"
        + "        {\n"
        + "          \"role\": \"caseworker-probate-private\",\n"
        + "          \"create\": true,\n"
        + "          \"read\": true,\n"
        + "          \"update\": true,\n"
        + "          \"delete\": false\n"
        + "        },\n"
        + "        {\n"
        + "          \"role\": \"citizen\",\n"
        + "          \"create\": true,\n"
        + "          \"read\": true,\n"
        + "          \"update\": true,\n"
        + "          \"delete\": false\n"
        + "        }],\n"
        + "      \"events\": [\n"
        + "      ],\n"
        + "      \"states\": [\n"
        + "      ],\n"
        + "      \"case_fields\": [\n"
        + "      ]\n"
        + "    }";

    @Inject
    protected DataSource db;

    @Before
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        mapper.registerModule(new Jackson2HalModule());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WhenGetValidCaseDocuments() throws Exception {

        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlMatching("/api/data/case-type/TestAddressBookCase"))
                    .willReturn(aResponse()
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(String.format(caseTypeResponseString, super.wiremockPort))));

        final MvcResult result = mockMvc
            .perform(
                get(String.format("http://localhost:%s/cases/" + CASE_ID + "/documents", super.wiremockPort))
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Accept", V2.MediaType.CASE_DOCUMENTS)
                    .header(REQUEST_ID, REQUEST_ID_VALUE)
                    .header("experimental", true))
            .andExpect(status().is(200))
            .andReturn();

        final DocumentsResource documentsResource = mapper.readValue(result.getResponse().getContentAsString(), DocumentsResource.class);
        List<Document> documentResources = documentsResource.getDocumentResources();
        Optional<Link> self = documentsResource.getLink("self");

        assertAll(
            () -> assertThat(self.get().getHref(), is(String.format("http://localhost:%s/cases/" + CASE_ID + "/documents", super.wiremockPort))),
            () -> assertThat(documentResources, hasItems(allOf(hasProperty("name", is("Claimant ID")),
                                                               hasProperty("description", is("Document identifying identity")),
                                                               hasProperty("type", is("ID")),
                                                               hasProperty("url", is(PRINTABLE_URL))))),
            () -> assertThat(documentResources, hasItems(allOf(hasProperty("name", is("Claimant Address")),
                                                               hasProperty("description", is("Document identifying address")),
                                                               hasProperty("type", is("Address")),
                                                               hasProperty("url", is(PRINTABLE_URL))))));
    }

}
