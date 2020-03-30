/*
package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.MediaType;
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
import uk.gov.hmcts.ccd.domain.model.definition.Document;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocument;
import uk.gov.hmcts.ccd.v2.external.domain.CaseDocumentMetadata;
import uk.gov.hmcts.ccd.v2.external.domain.Permission;
import uk.gov.hmcts.ccd.v2.external.resource.CaseDocumentResource;
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
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CaseDocumentControllerIT extends WireMockBaseTest {

    private static final String DOCUMENT_URL = "http://dm-store:8080/documents/a780ee98-3136-4be9-bf56-a46f8da1bc97";

    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;
    protected static final ObjectMapper mapper = new ObjectMapper();
    private String caseTypeResponseString = "{\n" +
        "      \"id\": \"TestCaseDocumentCase\",\n" +
        "      \"version\": {\n" +
        "        \"number\": 1,\n" +
        "        \"live_from\": \"2017-01-01\"\n" +
        "      },\n" +
        "      \"name\": \"Test Case Document Case\",\n" +
        "      \"description\": \"Test Case Document Case\",\n" +
        "      \"printable_document_url\": \"http://localhost:%s/printables\",\n" +
        "      \"jurisdiction\": {\n" +
        "        \"id\": \"PROBATE\",\n" +
        "        \"name\": \"Test\",\n" +
        "        \"description\": \"Test Jurisdiction\"\n" +
        "      },\n" +
        "      \"security_classification\": \"PUBLIC\",\n" +
        "      \"acls\": [\n" +
        "        {\n" +
        "          \"role\": \"caseworker-probate-public\",\n" +
        "          \"create\": true,\n" +
        "          \"read\": true,\n" +
        "          \"update\": true,\n" +
        "          \"delete\": false\n" +
        "        },\n" +
        "        {\n" +
        "          \"role\": \"caseworker-probate-private\",\n" +
        "          \"create\": true,\n" +
        "          \"read\": true,\n" +
        "          \"update\": true,\n" +
        "          \"delete\": false\n" +
        "        },\n" +
        "        {\n" +
        "          \"role\": \"citizen\",\n" +
        "          \"create\": true,\n" +
        "          \"read\": true,\n" +
        "          \"update\": true,\n" +
        "          \"delete\": false\n" +
        "        }],\n" +
        "      \"events\": [\n" +
        "      ],\n" +
        "      \"states\": [\n" +
        "      ],\n" +
        "      \"case_fields\": [\n" +
        "      ]\n" +
        "    }";

    @Inject
    protected DataSource db;

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
        mapper.registerModule(new Jackson2HalModule());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    public void shouldReturn200WhenGetValidCaseDocuments() throws Exception {

        stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlMatching("/api/data/case-type/TestCaseDocumentField"))
                    .willReturn(aResponse()
                                    .withHeader("Content-Type", "application/json")
                                    .withBody(String.format(caseTypeResponseString, super.wiremockPort))));

        final MvcResult result = mockMvc
            .perform(get(String.format("http://localhost:%s/cases/1504259907353123/documents/a780ee98-3136-4be9-bf56-a46f8da1bc97", super.wiremockPort))
                         .contentType(MediaType.APPLICATION_JSON)
                         .header("Accept", V2.MediaType.CASE_DOCUMENTS)
                         .header("experimental", true))
            .andExpect(status().is(200))
            .andReturn();

        final CaseDocumentResource caseDocumentResource = mapper.readValue(result.getResponse().getContentAsString(), CaseDocumentResource.class);

        CaseDocumentMetadata caseDocumentMetadata = caseDocumentResource.getDocumentMetadata();
        Optional<Link> self = caseDocumentResource.getLink("self");


        assertAll(
            () -> assertThat(self.get().getHref(),
                is(String.format("http://localhost:%s/cases/1504259907353123/documents/a780ee98-3136-4be9-bf56-a46f8da1bc97", super.wiremockPort))),
            () -> assertThat(caseDocumentResource.getDocumentMetadata(),
                allOf( hasProperty("caseId", is("Claimant ID")),
                       hasProperty("caseTypeId", is("Document identifying identity")),
                       hasProperty("jurisdictionId", is("ID")))),
            () -> assertThat(caseDocumentResource.getDocumentMetadata().getDocument(),
                allOf( hasProperty("url", is("Claimant Address")),
                       hasProperty("name", is("Document identifying address")),
                       hasProperty("type", is("Address")),
                       hasProperty("description", is(DOCUMENT_URL)),
                       hasProperty("id", is("Address")))),
            () -> assertThat(caseDocumentResource.getDocumentMetadata().getDocument().getPermissions(), hasSize(2)),
            () -> assertThat(caseDocumentResource.getDocumentMetadata().getDocument().getPermissions(),
                hasItems(Permission.READ, Permission.UPDATE)));
    }

}
*/
