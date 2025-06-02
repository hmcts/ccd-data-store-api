package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.auditlog.AuditEntry;
import uk.gov.hmcts.ccd.auditlog.AuditOperationType;
import uk.gov.hmcts.ccd.auditlog.AuditRepository;
import uk.gov.hmcts.ccd.customheaders.CustomHeadersFilter;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;

import java.util.UUID;
import javax.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static uk.gov.hmcts.ccd.ApplicationParams.encode;
import static uk.gov.hmcts.ccd.TestFixtures.loadCaseTypeDefinition;


class CaseFileViewControllerTestIT extends WireMockBaseTest {

    @Inject
    private WebApplicationContext wac;

    @Inject
    private CustomHeadersFilter customHeadersFilter;

    @Inject
    protected ApplicationParams applicationParams;

    private MockMvc mockMvc;

    @SpyBean
    private AuditRepository auditRepository;

    private static final String REQUEST_ID = "request-id";
    private static final String REQUEST_ID_VALUE = "1234567898765432";
    private static String CUSTOM_CONTEXT = "";

    @BeforeEach
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).addFilters(customHeadersFilter).build();
        CUSTOM_CONTEXT = applicationParams.getCallbackPassthruHeaderContexts().get(0);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testShouldGetCategoriesAndDocuments() throws Exception {
        final String caseTypeId = "FT_CaseFileView";
        final String jsonFile = "mappings/ft-case-file-view-definition.json";
        final CaseTypeDefinition caseTypeDefinition = loadCaseTypeDefinition(jsonFile);

        stubSuccess(String.format("/api/data/case-type/%s", encode(caseTypeId)),
            objectToJsonString(caseTypeDefinition),
            UUID.randomUUID());

        final String caseId = "1504259907353529";
        final String URL = "/categoriesAndDocuments/" + caseId;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final MvcResult mvcResult = mockMvc.perform(get(URL)
                .header(REQUEST_ID, REQUEST_ID_VALUE)
                .header(CUSTOM_CONTEXT, responseJson1.toString())
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();
        ArgumentCaptor<AuditEntry> captor = ArgumentCaptor.forClass(AuditEntry.class);

        verify(auditRepository).save(captor.capture());

        assertTrue(mvcResult.getResponse().getHeaderNames().contains(CUSTOM_CONTEXT));
        assertThat(mvcResult.getResponse())
            .isNotNull()
            .satisfies(response -> {
                assertThat(response.getStatus()).isEqualTo(200);
                assertThat(response.getContentAsString()).isNotEmpty();

                assertThat(captor.getValue().getOperationType())
                    .isEqualTo(AuditOperationType.CATEGORIES_AND_DOCUMENTS_ACCESSED.getLabel());
                assertThat(captor.getValue().getCaseId()).isEqualTo(caseId);
            });
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testGetCategoriesAndDocumentsShouldReturn404WhenCaseDoesNotExist() throws Exception {
        final String caseId = "4259907353529155";
        final String URL = "/categoriesAndDocuments/" + caseId;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final MvcResult mvcResult = mockMvc.perform(get(URL)
                .header(REQUEST_ID, REQUEST_ID_VALUE)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse())
            .isNotNull()
            .satisfies(response -> assertThat(response.getStatus()).isEqualTo(404));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testGetCategoriesAndDocumentsShouldReturn404WhenUserIsNotAllowedAccessToCase() throws Exception {
        final String caseId = "1504259907353529";
        final String URL = "/categoriesAndDocuments/" + caseId;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CITIZEN);

        final MvcResult mvcResult = mockMvc.perform(get(URL)
                .header(REQUEST_ID, REQUEST_ID_VALUE)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse())
            .isNotNull()
            .satisfies(response -> assertThat(response.getStatus()).isEqualTo(404));
    }

    @Test
    void testGetCategoriesAndDocumentsShouldReturnBadRequestWhenCaseRefHasWrongFormat() throws Exception {
        final String badCaseId = "1504259907353529000";
        final String URL = "/categoriesAndDocuments/" + badCaseId;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final MvcResult mvcResult = mockMvc.perform(get(URL)
                .header(REQUEST_ID, REQUEST_ID_VALUE)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        assertThat(mvcResult.getResponse())
            .isNotNull()
            .satisfies(response -> assertThat(response.getStatus()).isEqualTo(400));
    }

}
