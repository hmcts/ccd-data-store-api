package uk.gov.hmcts.ccd.v2.internal.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
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
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewField;
import uk.gov.hmcts.ccd.domain.model.aggregated.CaseViewTab;
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseViewResource;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.*;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class UICaseControllerDCPIT extends WireMockBaseTest {
    private static final String GET_CASE = "/internal/cases/1587051668000989";
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
    @SuppressWarnings("unchecked")
    public void shouldGetCaseWithDCPConfigured() throws Exception {
        assertCaseDataResultSetSize();

        final MvcResult result = mockMvc.perform(get(GET_CASE)
            .contentType(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer user1")
            .header(V2.EXPERIMENTAL_HEADER, "true"))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String content = result.getResponse().getContentAsString();
        CaseViewResource savedCaseResource = mapper.readValue(content, CaseViewResource.class);

        CaseViewTab[] tabs = savedCaseResource.getTabs();

        CaseViewField textField = tabs[0].getFields()[0];
        CaseViewField complexCollectionField = tabs[0].getFields()[1];
        CaseViewField dateField = tabs[1].getFields()[0];
        CaseViewField dateTimeField = tabs[1].getFields()[1];
        CaseViewField collectionField = tabs[2].getFields()[0];
        CaseViewField complexField = tabs[2].getFields()[1];

        assertAll(
            () -> assertSimpleField(textField, TEXT_FIELD, null, "Case 1 Text", "Case 1 Text"),
            () -> assertSimpleField(dateField, DATE_FIELD, "#DATETIMEDISPLAY(dd, MMM yyyy)", "2000-10-20", "20, Oct 2000"),
            () -> assertSimpleField(dateTimeField, DATE_TIME_FIELD, null, "1987-11-15T12:30:00.000", "1987-11-15T12:30:00.000"),

            () -> assertCollectionField(collectionField, COLLECTION_FIELD, "#DATETIMEDISPLAY(dd/MM/yyyy)",
                new String[]{"2004-03-02T05:06:07.000", "2010-09-08T11:12:13.000"},
                new String[]{"02/03/2004", "08/09/2010"}),

            () -> assertThat(complexField.getFieldTypeDefinition().getChildren().get(0).getId(), is(COMPLEX_DATE_TIME_FIELD)),
            () -> assertThat(complexField.getFieldTypeDefinition().getChildren().get(0).getDisplayContextParameter(),
                is("#DATETIMEDISPLAY(yyyy),#DATETIMEENTRY(MM-yyyy)")),
            () -> assertThat(mapOf(complexField.getValue()).get(COMPLEX_DATE_TIME_FIELD), is("2005-03-28T07:45:30.000")),
            () -> assertThat(mapOf(complexField.getFormattedValue()).get(COMPLEX_DATE_TIME_FIELD), is("2005")),

            () -> assertThat(complexField.getFieldTypeDefinition().getChildren().get(1).getFieldTypeDefinition().getChildren().get(0)
                .getId(), is(NESTED_NUMBER_FIELD)),
            () -> assertThat(complexField.getFieldTypeDefinition().getChildren().get(1).getFieldTypeDefinition().getChildren().get(0)
                .getDisplayContextParameter(), is("#DATETIMEDISPLAY(dd MM yyyy),#DATETIMEENTRY(HHmm)")),
            () -> assertThat(mapOf(mapOf(complexField.getValue()).get(COMPLEX_NESTED_FIELD))
                .get(NESTED_NUMBER_FIELD), is(nullValue())),
            () -> assertThat(mapOf(mapOf(complexField.getFormattedValue()).get(COMPLEX_NESTED_FIELD))
                .get(NESTED_NUMBER_FIELD), is(nullValue())),

            () -> assertThat(complexCollectionField.getId(), is(COLLECTION_COMPLEX_DATE_TIME)),
            () -> assertComplexCollectionDCP(complexCollectionField, "#DATETIMEENTRY(dd-MM-yyyy),#DATETIMEDISPLAY(dd-MM-yyyy)",
                null, "#DATETIMEENTRY(yyyy-MM-dd'T'HH:mm),#DATETIMEDISPLAY(yyyy-MM-dd'T'HH:mm)", null,
                "#DATETIMEENTRY(MM-yyyy),#DATETIMEDISPLAY(MM-yyyy)", null,
                "#DATETIMEENTRY(yyyy-MM-dd),#DATETIMEDISPLAY(yyyy-MM-dd)", null),
            () -> assertComplexCollectionValues(arrayOf(complexCollectionField.getValue()), 0,
                "1963-05-07", "1999-08-19", "2008-04-02T16:37:00.000",
                "2010-06-17T19:20:00.000", "1981-02-08", "2020-02-19",
                "2002-03-04T02:02:00.000", "2007-07-17T07:07:00.000"),
            () -> assertComplexCollectionValues(arrayOf(complexCollectionField.getFormattedValue()), 0,
                "07-05-1963", "1999-08-19", "2008-04-02T16:37",
                "2010-06-17T19:20:00.000", "02-1981", "2020-02-19",
                "2002-03-04", "2007-07-17T07:07:00.000")
        );
    }

    private void assertComplexCollectionDCP(CaseViewField caseViewField,
                                            String dateFieldValue,
                                            String standardDateFieldValue,
                                            String dateTimeFieldValue,
                                            String standardDateTimeFieldValue,
                                            String nestedDateFieldValue,
                                            String nestedStandardDateFieldValue,
                                            String nestedDateTimeFieldValue,
                                            String nestedStandardDateTimeFieldValue) {
        assertAll(
            () -> assertThat(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getChildren().get(0).getDisplayContextParameter(),
                is(dateFieldValue)),
            () -> assertThat(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getChildren().get(1).getDisplayContextParameter(),
                is(dateTimeFieldValue)),
            () -> assertThat(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getChildren().get(2).getDisplayContextParameter(),
                is(standardDateFieldValue)),
            () -> assertThat(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getChildren().get(3).getDisplayContextParameter(),
                is(standardDateTimeFieldValue)),
            () -> assertThat(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getChildren().get(4).getFieldTypeDefinition()
                .getChildren().get(0).getDisplayContextParameter(), is(nestedDateFieldValue)),
            () -> assertThat(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getChildren().get(4).getFieldTypeDefinition()
                .getChildren().get(1).getDisplayContextParameter(), is(nestedDateTimeFieldValue)),
            () -> assertThat(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getChildren().get(4).getFieldTypeDefinition()
                .getChildren().get(2).getDisplayContextParameter(), is(nestedStandardDateFieldValue)),
            () -> assertThat(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getChildren().get(4).getFieldTypeDefinition()
                .getChildren().get(3).getDisplayContextParameter(), is(nestedStandardDateTimeFieldValue))
        );
    }

    private void assertComplexCollectionValues(ArrayList<LinkedHashMap<String, Object>> collection,
                                               int index,
                                               String dateFieldValue,
                                               String standardDateFieldValue,
                                               String dateTimeFieldValue,
                                               String standardDateTimeFieldValue,
                                               String nestedDateFieldValue,
                                               String nestedStandardDateFieldValue,
                                               String nestedDateTimeFieldValue,
                                               String nestedStandardDateTimeFieldValue) {
        LinkedHashMap<String, Object> value = mapOf(collection.get(index).get(CollectionValidator.VALUE));
        LinkedHashMap<String, Object> nestedComplex = mapOf(value.get(NESTED_COMPLEX));

        assertAll(
            () -> assertThat(value.get(DATE_FIELD), is(dateFieldValue)),
            () -> assertThat(value.get(STANDARD_DATE), is(standardDateFieldValue)),
            () -> assertThat(value.get(DATE_TIME_FIELD), is(dateTimeFieldValue)),
            () -> assertThat(value.get(STANDARD_DATE_TIME), is(standardDateTimeFieldValue)),

            () -> assertThat(nestedComplex.get(DATE_FIELD), is(nestedDateFieldValue)),
            () -> assertThat(nestedComplex.get(STANDARD_DATE), is(nestedStandardDateFieldValue)),
            () -> assertThat(nestedComplex.get(DATE_TIME_FIELD), is(nestedDateTimeFieldValue)),
            () -> assertThat(nestedComplex.get(STANDARD_DATE_TIME), is(nestedStandardDateTimeFieldValue))
        );
    }

    private void assertSimpleField(CaseViewField caseViewField,
                                   String id,
                                   String displayContextParameter,
                                   String value,
                                   String formattedValue) {
        assertThat(caseViewField.getId(), is(id));
        assertThat(caseViewField.getDisplayContextParameter(), is(displayContextParameter));
        assertThat(caseViewField.getValue(), is(value));
        assertThat(caseViewField.getFormattedValue(), is(formattedValue));
    }

    @SuppressWarnings("unchecked")
    private void assertCollectionField(CaseViewField caseViewField,
                                       String id,
                                       String displayContextParameter,
                                       String[] expectedValues,
                                       String[] expectedFormattedValues) {
        ArrayList<LinkedHashMap<String, Object>> value = arrayOf(caseViewField.getValue());
        ArrayList<LinkedHashMap<String, Object>> formattedValue = arrayOf(caseViewField.getFormattedValue());

        assertThat(caseViewField.getId(), is(id));
        assertThat(caseViewField.getDisplayContextParameter(),
            displayContextParameter == null ? is(nullValue()) : is(displayContextParameter));

        for (int i = 0; i < expectedValues.length; i++) {
            assertThat(value.get(i).get(CollectionValidator.VALUE), is(expectedValues[i]));
            assertThat(formattedValue.get(i).get(CollectionValidator.VALUE), is(expectedFormattedValues[i]));
        }
    }

    private void assertCaseDataResultSetSize() {
        final int count = template.queryForObject("SELECT count(1) as n FROM case_data", Integer.class);
        assertEquals("Incorrect case data size", NUMBER_OF_CASES, count);
    }
}
