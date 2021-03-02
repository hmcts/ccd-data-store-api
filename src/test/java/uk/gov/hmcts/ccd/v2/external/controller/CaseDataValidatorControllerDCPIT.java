package uk.gov.hmcts.ccd.v2.external.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
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
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataProcessingException;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.external.resource.CaseDataResource;

import javax.inject.Inject;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.COLLECTION_COMPLEX_DATE_TIME;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.DATE_FIELD;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.DATE_TIME_FIELD;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.NESTED_COMPLEX;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.STANDARD_DATE;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.STANDARD_DATE_TIME;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.TEXT_FIELD;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.invalidValidateContent;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.validateContent;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class CaseDataValidatorControllerDCPIT extends WireMockBaseTest {
    private static final String VALIDATE = "/case-types/DCP/validate?pageId=UPDATEfirst";
    private static final int NUMBER_OF_CASES = 2;

    @Inject
    private WebApplicationContext wac;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private MockMvc mockMvc;

    private JdbcTemplate template;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        doReturn(authentication).when(securityContext).getAuthentication();
        SecurityContextHolder.setContext(securityContext);

        MockUtils.setSecurityAuthorities(authentication, MockUtils.ROLE_DCP_CASEWORKER);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_case_dcp.sql" })
    public void shouldValidateWithFormattedDCPValues() throws Exception {
        assertCaseDataResultSetSize();

        final MvcResult result = mockMvc.perform(post(VALIDATE)
            .content(validateContent())
            .contentType(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer user1")
            .header(V2.EXPERIMENTAL_HEADER, "true"))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = result.getResponse().getContentAsString();
        CaseDataResource caseDataResource = mapper.readValue(content, CaseDataResource.class);

        JsonNode data = caseDataResource.getData();
        String collectionComplexDateTimeValuePointer = String.join("/", "", COLLECTION_COMPLEX_DATE_TIME, "0",
            CollectionValidator.VALUE);

        assertAll(
            () -> assertThat(data.get(TEXT_FIELD).asText(), is("Case 1 Text")),
            () -> assertThat(data.at(String.join("/", collectionComplexDateTimeValuePointer, DATE_FIELD)).asText(),
                is("1963-05-07")),
            () -> assertThat(data.at(String.join("/", collectionComplexDateTimeValuePointer, DATE_TIME_FIELD))
                    .asText(),
                is("2008-04-02T16:37:00.000")),
            () -> assertThat(data.at(String.join("/", collectionComplexDateTimeValuePointer, STANDARD_DATE)).asText(),
                is("1999-08-19")),
            () -> assertThat(data.at(String.join("/", collectionComplexDateTimeValuePointer, STANDARD_DATE_TIME))
                    .asText(),
                is("2010-06-17T19:20:00.000")),
            () -> assertThat(data.at(String.join("/", collectionComplexDateTimeValuePointer, NESTED_COMPLEX,
                DATE_FIELD)).asText(),
                is("1981-02-01")),
            () -> assertThat(data.at(String.join("/", collectionComplexDateTimeValuePointer, NESTED_COMPLEX,
                DATE_TIME_FIELD)).asText(),
                is("2002-03-04T00:00:00.000")),
            () -> assertThat(data.at(String.join("/", collectionComplexDateTimeValuePointer, NESTED_COMPLEX,
                STANDARD_DATE)).asText(),
                is("2020-02-19")),
            () -> assertThat(data.at(String.join("/", collectionComplexDateTimeValuePointer, NESTED_COMPLEX,
                STANDARD_DATE_TIME)).asText(),
                is("2007-07-17T07:07:00.000"))
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_case_dcp.sql" })
    public void shouldFail() throws Exception {
        assertCaseDataResultSetSize();

        final MvcResult result = mockMvc.perform(post(VALIDATE)
            .content(invalidValidateContent())
            .contentType(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer user1")
            .header(V2.EXPERIMENTAL_HEADER, "true"))
            .andExpect(status().is(422))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 422, result.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = result.getResponse().getContentAsString();
        DataProcessingException exception = mapper.readValue(content, DataProcessingException.class);

        assertAll(
            () -> assertThat(exception.getMessage(), is("Processing of data failed")),
            () -> assertThat(exception.getDetails(),
                is("Unable to process field CollectionComplexDateTime.NestedComplex.DateField with value 2000. "
                    + "Expected format to be either MM-yyyy or yyyy-MM-dd"))
        );
    }

    private void assertCaseDataResultSetSize() {
        final int count = template.queryForObject("SELECT count(1) as n FROM case_data", Integer.class);
        assertEquals("Incorrect case data size", NUMBER_OF_CASES, count);
    }
}
