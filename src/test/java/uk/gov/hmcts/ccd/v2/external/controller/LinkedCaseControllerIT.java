package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.hmcts.ccd.MockUtils;
import uk.gov.hmcts.ccd.WireMockBaseTest;

import javax.inject.Inject;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

class LinkedCaseControllerIT extends WireMockBaseTest {

    private static final String REQUEST_ID = "request-id";
    private static final String REQUEST_ID_VALUE = "1234567898765432";
    @Inject
    private WebApplicationContext wac;
    private MockMvc mockMvc;

    @BeforeEach
    public void setUp() {
        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_CASEWORKER_PUBLIC);
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testShouldGetLinkedCases() throws Exception {
        final String caseId = "1504259907353529";
        final String URL = "/getLinkedCase/" + caseId;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final MvcResult mvcResult = mockMvc.perform(get(URL)
                .header(REQUEST_ID, REQUEST_ID_VALUE)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        Assertions.assertThat(mvcResult.getResponse())
            .isNotNull()
            .satisfies(response -> Assertions.assertThat(response.getStatus()).isEqualTo(204));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testShouldGetLinkedCasesOptionalParameters() throws Exception {
        final String caseId = "1504259907353529";
        final String URL = "/getLinkedCase/" + caseId + "?startRecordNumber=1&?maxReturnRecordCount=1";
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final MvcResult mvcResult = mockMvc.perform(get(URL)
                .header(REQUEST_ID, REQUEST_ID_VALUE)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        Assertions.assertThat(mvcResult.getResponse())
            .isNotNull()
            .satisfies(response -> Assertions.assertThat(response.getStatus()).isEqualTo(204));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testShouldGetLinkedCasesNoCaseReferenceShouldReturn400() throws Exception {
        final String caseId = "1504259907353529";
        final String URL = "/getLinkedCase/";
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final MvcResult mvcResult = mockMvc.perform(get(URL)
                .header(REQUEST_ID, REQUEST_ID_VALUE)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        Assertions.assertThat(mvcResult.getResponse())
            .isNotNull()
            .satisfies(response -> Assertions.assertThat(response.getStatus()).isEqualTo(404));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testShouldGetLinkedCasesReturn404WhenCaseDoesNotExist() throws Exception {
        final String caseId = "4259907353529155";
        final String URL = "/getLinkedCase/" + caseId;
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final MvcResult mvcResult = mockMvc.perform(get(URL)
                .header(REQUEST_ID, REQUEST_ID_VALUE)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        Assertions.assertThat(mvcResult.getResponse())
            .isNotNull()
            .satisfies(response -> Assertions.assertThat(response.getStatus()).isEqualTo(404));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testShouldGetLinkedCasesStartRecordNumberNotNumericReturn400() throws Exception {
        final String caseId = "1504259907353529";
        final String URL = "/getLinkedCase/" + caseId + "?startRecordNumber=A";
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final MvcResult mvcResult = mockMvc.perform(get(URL)
                .header(REQUEST_ID, REQUEST_ID_VALUE)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        Assertions.assertThat(mvcResult.getResponse())
            .isNotNull()
            .satisfies(response -> Assertions.assertThat(response.getStatus()).isEqualTo(400));
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = {"classpath:sql/insert_cases.sql"})
    void testShouldGetLinkedCasesMaxRecordCountNotNumericReturn400() throws Exception {
        final String caseId = "1504259907353529";
        final String URL = "/getLinkedCase/" + caseId + "?maxReturnRecordCount=A";
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final MvcResult mvcResult = mockMvc.perform(get(URL)
                .header(REQUEST_ID, REQUEST_ID_VALUE)
                .contentType(JSON_CONTENT_TYPE))
            .andReturn();

        Assertions.assertThat(mvcResult.getResponse())
            .isNotNull()
            .satisfies(response -> Assertions.assertThat(response.getStatus()).isEqualTo(400));
    }

}
