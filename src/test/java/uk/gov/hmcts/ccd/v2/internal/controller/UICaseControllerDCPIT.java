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
import uk.gov.hmcts.ccd.v2.internal.resource.UICaseViewResource;

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

public class UICaseControllerDCPIT extends WireMockBaseTest {
    private static final String GET_CASE = "/internal/cases/1587051668000989";
    private static final int NUMBER_OF_CASES = 1;

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

        MockUtils.setSecurityAuthorities(authentication, "caseworker-dcptest1");

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        template = new JdbcTemplate(db);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = { "classpath:sql/insert_case_dcp.sql" })
    @SuppressWarnings("unchecked")
    public void shouldWork() throws Exception {
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
        UICaseViewResource savedCaseResource = mapper.readValue(content, UICaseViewResource.class);

        CaseViewTab[] tabs = savedCaseResource.getTabs();

        CaseViewField textField = tabs[0].getFields()[0];
        CaseViewField complexCollectionField = tabs[0].getFields()[1];
        CaseViewField dateField = tabs[1].getFields()[0];
        CaseViewField dateTimeField = tabs[1].getFields()[1];
        CaseViewField collectionField = tabs[2].getFields()[0];
        CaseViewField complexField = tabs[2].getFields()[1];

        assertAll(
            () -> assertSimpleField(textField, "TextField", null, "Case 1 Text", "Case 1 Text"),
            () -> assertSimpleField(dateField, "DateField", "#DATETIMEDISPLAY(dd, MMM yyyy)", "2000-10-20", "20, Oct 2000"),
            () -> assertSimpleField(dateTimeField, "DateTimeField", null, "1987-11-15T12:30:00.000", "1987-11-15T12:30:00.000"),

            () -> assertCollectionField(collectionField, "CollectionField", "#DATETIMEDISPLAY(dd/MM/yyyy)",
                new String[]{"2004-03-02T05:06:07.000", "2010-09-08T11:12:13.000"},
                new String[]{"02/03/2004", "08/09/2010"}),

            () -> assertThat(complexField.getFieldType().getChildren().get(0).getId(), is("ComplexDateTimeField")),
            () -> assertThat(complexField.getFieldType().getChildren().get(0).getDisplayContextParameter(), is("#DATETIMEDISPLAY(yyyy),#DATETIMEENTRY(MM-yyyy)")),
            () -> assertThat(mapOf(complexField.getValue()).get("ComplexDateTimeField"), is("2005-03-28T07:45:30.000")),
            () -> assertThat(mapOf(complexField.getFormattedValue()).get("ComplexDateTimeField"), is("2005")),

            () -> assertThat(complexField.getFieldType().getChildren().get(1).getFieldType().getChildren().get(0)
                    .getId(), is("NestedNumberField")),
            () -> assertThat(complexField.getFieldType().getChildren().get(1).getFieldType().getChildren().get(0)
                    .getDisplayContextParameter(), is("#DATETIMEDISPLAY(dd MM yyyy),#DATETIMEENTRY(HHmm)")),
            () -> assertThat(mapOf(mapOf(complexField.getValue()).get("ComplexNestedField"))
                .get("NestedNumberField"), is(nullValue())),
            () -> assertThat(mapOf(mapOf(complexField.getFormattedValue()).get("ComplexNestedField"))
                .get("NestedNumberField"), is(nullValue()))
        );

        assertNotNull(savedCaseResource);

    }

    private void assertComplexCollectionValues(CaseViewField caseViewField,
                                               ArrayList<LinkedHashMap<String, Object>> collection,
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

        assertThat(value.get("DateField"), is(dateFieldValue));
        assertThat(value.get("StandardDate"), is(standardDateFieldValue));
        assertThat(value.get("DateTimeField"), is(dateTimeFieldValue));
        assertThat(value.get("StandardDateTime"), is(standardDateTimeFieldValue));

        LinkedHashMap<String, Object> nestedComplex = mapOf(value.get("NestedComplex"));

        assertThat(nestedComplex.get("DateField"), is(nestedDateFieldValue));
        assertThat(nestedComplex.get("StandardDate"), is(nestedStandardDateFieldValue));
        assertThat(nestedComplex.get("DateTimeField"), is(nestedDateTimeFieldValue));
        assertThat(nestedComplex.get("StandardDateTime"), is(nestedStandardDateTimeFieldValue));
    }

    private void assertSimpleField(CaseViewField caseViewField,
                                   String id,
                                   String displayContextParameter,
                                   String value,
                                   String formattedValue) {
        assertThat(caseViewField.getId(), is(id));
        assertThat(caseViewField.getDisplayContextParameter(),
            displayContextParameter == null ? is(nullValue()) : is(displayContextParameter));
        assertThat(caseViewField.getValue(), value == null ? is(nullValue()) : is(value));
        assertThat(caseViewField.getFormattedValue(), formattedValue == null ? is(nullValue()) : is(formattedValue));
    }

    @SuppressWarnings("unchecked")
    private void assertCollectionField(CaseViewField caseViewField,
                                       String id,
                                       String displayContextParameter,
                                       Object expectedValues[],
                                       Object expectedFormattedValues[]) {
        ArrayList<LinkedHashMap<String, String>> value = (ArrayList<LinkedHashMap<String, String>>) caseViewField.getValue();
        ArrayList<LinkedHashMap<String, String>> formattedValue = (ArrayList<LinkedHashMap<String, String>>) caseViewField.getFormattedValue();

        assertThat(caseViewField.getId(), is(id));
        assertThat(caseViewField.getDisplayContextParameter(),
            displayContextParameter == null ? is(nullValue()) : is(displayContextParameter));

        for (int i = 0; i < expectedValues.length; i++) {
            assertThat(value.get(i).get(CollectionValidator.VALUE),
                expectedValues[i] == null ? is(nullValue()) : is(expectedValues[i]));
            assertThat(formattedValue.get(i).get(CollectionValidator.VALUE),
                expectedFormattedValues[i] == null ? is(nullValue()) : is(expectedFormattedValues[i]));
        }
    }

    @SuppressWarnings("unchecked")
    private LinkedHashMap<String, Object> mapOf(Object object) {
        return (LinkedHashMap<String, Object>) object;
    }

    private void assertCaseDataResultSetSize() {
        final int count = template.queryForObject("SELECT count(1) as n FROM case_data",Integer.class);
        assertEquals("Incorrect case data size", NUMBER_OF_CASES, count);
    }
}
