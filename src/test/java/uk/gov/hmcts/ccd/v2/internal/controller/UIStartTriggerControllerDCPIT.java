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
import uk.gov.hmcts.ccd.domain.types.CollectionValidator;
import uk.gov.hmcts.ccd.v2.V2;
import uk.gov.hmcts.ccd.v2.internal.resource.CaseUpdateViewEventResource;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.doReturn;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.COLLECTION_COMPLEX_DATE_TIME;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.COLLECTION_FIELD;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.COMPLEX_DATE_TIME_FIELD;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.COMPLEX_NESTED_FIELD;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.DATE_FIELD;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.DATE_TIME_FIELD;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.NESTED_COMPLEX;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.NESTED_NUMBER_FIELD;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.STANDARD_DATE;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.STANDARD_DATE_TIME;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.TEXT_FIELD;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.arrayOf;
import static uk.gov.hmcts.ccd.v2.DCPTestHelper.mapOf;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public class UIStartTriggerControllerDCPIT extends WireMockBaseTest {
    private static final String GET_START_TRIGGER = "/internal/cases/1587051668000989/event-triggers/UPDATE";
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
    public void shouldGetStartTriggerForCaseWithDCPConfigured() throws Exception {
        assertCaseDataResultSetSize();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final MvcResult result = mockMvc.perform(get(GET_START_TRIGGER)
            .contentType(MediaType.APPLICATION_JSON)
            .header(AUTHORIZATION, "Bearer user1")
            .header(V2.EXPERIMENTAL_HEADER, "true"))
            .andExpect(status().is(200))
            .andReturn();

        assertEquals(result.getResponse().getContentAsString(), 200, result.getResponse().getStatus());

        String content = result.getResponse().getContentAsString();
        CaseUpdateViewEventResource caseUpdateViewEventResource =
            mapper.readValue(content, CaseUpdateViewEventResource.class);

        List<CaseViewField> caseFields = caseUpdateViewEventResource.getCaseUpdateViewEvent().getCaseFields();

        CaseViewField textField = caseFields.get(1);
        CaseViewField dateField = caseFields.get(2);
        CaseViewField dateTimeField = caseFields.get(3);
        CaseViewField collectionField = caseFields.get(4);
        CaseViewField complexField = caseFields.get(5);
        CaseViewField complexCollectionField = caseFields.get(6);

        assertAll(
            () -> assertSimpleField(textField, TEXT_FIELD, null, "Case 1 Text",
                "Case 1 Text"),
            () -> assertSimpleField(dateField, DATE_FIELD, null, "2000-10-20",
                "2000-10-20"),
            () -> assertSimpleField(dateTimeField, DATE_TIME_FIELD, "#DATETIMEENTRY(yyyy-dd)",
                "1987-11-15T12:30:00.000", "1987-15"),

            () -> assertCollectionField(collectionField, COLLECTION_FIELD,
                "#DATETIMEENTRY(yyyyHHmm),#COLLECTION(allowDelete,allowInsert)",
                new String[]{"2004-03-02T05:06:07.000", "2010-09-08T11:12:13.000"},
                new String[]{"20040506", "20101112"}),

            () -> assertThat(complexField.getFieldTypeDefinition().getChildren().get(0).getId(),
                is(COMPLEX_DATE_TIME_FIELD)),
            () -> assertThat(complexField.getFieldTypeDefinition().getChildren().get(0).getDisplayContextParameter(),
                is("#DATETIMEDISPLAY(yyyy),#DATETIMEENTRY(MM-yyyy)")),
            () -> assertThat(mapOf(complexField.getValue()).get(COMPLEX_DATE_TIME_FIELD),
                is("2005-03-28T07:45:30.000")),
            // () -> assertThat(mapOf(complexField.getFormattedValue()).get(COMPLEX_DATE_TIME_FIELD), is("03-2005")),

            () -> assertThat(complexField.getFieldTypeDefinition().getChildren().get(1).getFieldTypeDefinition()
                .getChildren().get(0).getId(), is(NESTED_NUMBER_FIELD)),
            () -> assertThat(complexField.getFieldTypeDefinition().getChildren().get(1).getFieldTypeDefinition()
                .getChildren().get(0).getDisplayContextParameter(),
                is("#DATETIMEDISPLAY(dd MM yyyy),#DATETIMEENTRY(HHmm)")),
            () -> assertThat(mapOf(mapOf(complexField.getValue()).get(COMPLEX_NESTED_FIELD))
                .get(NESTED_NUMBER_FIELD), is(nullValue())),
            () -> assertThat(mapOf(mapOf(complexField.getFormattedValue()).get(COMPLEX_NESTED_FIELD))
                .get(NESTED_NUMBER_FIELD), is(nullValue())),

            () -> assertThat(complexCollectionField.getId(), is(COLLECTION_COMPLEX_DATE_TIME)),
            () -> assertComplexCollectionDCP(complexCollectionField),
            () -> assertComplexCollectionValues(arrayOf(complexCollectionField.getValue()), 0,
                "1963-05-07", "1999-08-19",
                "2008-04-02T16:37:00.000", "2010-06-17T19:20:00.000",
                "1981-02-08", "2020-02-19",
                "2002-03-04T02:02:00.000",
                "2007-07-17T07:07:00.000"),
            () -> assertComplexCollectionValues(arrayOf(complexCollectionField.getFormattedValue()), 0,
                "07-05-1963", "1999-08-19", "2008-04-02T16:37",
                "2010-06-17T19:20:00.000", "02-1981",
                "2020-02-19",
                "2002-03-04", "2007-07-17T07:07:00.000")
        );
    }

    private void assertComplexCollectionDCP(CaseViewField caseViewField) {
        assertAll(
            () -> assertThat(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getChildren()
                    .get(0).getDisplayContextParameter(),
                is("#DATETIMEENTRY(dd-MM-yyyy),#DATETIMEDISPLAY(dd-MM-yyyy)")),
            () -> assertThat(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getChildren()
                    .get(1).getDisplayContextParameter(),
                is("#DATETIMEENTRY(yyyy-MM-dd'T'HH:mm),#DATETIMEDISPLAY(yyyy-MM-dd'T'HH:mm)")),
            () -> assertThat(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getChildren()
                    .get(2).getDisplayContextParameter(),
                is((String) null)),
            () -> assertThat(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getChildren()
                    .get(3).getDisplayContextParameter(),
                is((String) null)),
            () -> assertThat(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getChildren()
                .get(4).getFieldTypeDefinition().getChildren().get(0).getDisplayContextParameter(),
                is("#DATETIMEENTRY(MM-yyyy),#DATETIMEDISPLAY(MM-yyyy)")),
            () -> assertThat(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getChildren()
                .get(4).getFieldTypeDefinition().getChildren().get(1).getDisplayContextParameter(),
                is("#DATETIMEENTRY(yyyy-MM-dd),#DATETIMEDISPLAY(yyyy-MM-dd)")),
            () -> assertThat(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getChildren()
                .get(4).getFieldTypeDefinition().getChildren().get(2).getDisplayContextParameter(), is((String) null)),
            () -> assertThat(caseViewField.getFieldTypeDefinition().getCollectionFieldTypeDefinition().getChildren()
                .get(4).getFieldTypeDefinition().getChildren().get(3).getDisplayContextParameter(), is((String) null))
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
