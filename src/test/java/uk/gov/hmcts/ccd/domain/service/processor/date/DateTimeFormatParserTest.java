package uk.gov.hmcts.ccd.domain.service.processor.date;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataProcessingException;

import java.time.format.DateTimeParseException;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.powermock.api.mockito.PowerMockito.when;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.COLLECTION;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DATE;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DATETIME;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.FieldTypeBuilder.aFieldType;

class DateTimeFormatParserTest {

    private static final String DATETIME_FIELD_TYPE = "DateTime";
    private static final String DATE_FIELD_TYPE = "Date";
    private static final String COLLECTION_FIELD_TYPE = "Collection";
    private static final String COMPLEX_FIELD_TYPE = "Complex";

    @InjectMocks
    private DateTimeFormatParser dateTimeFormatParser;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    @BeforeEach
    void setUp() {
        dateTimeFormatParser = new DateTimeFormatParser();
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.EMPTY_LIST);
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();
        setUpBaseTypes();
    }

    @Test
    void shouldConvertDateTimeToIso8601Format() {
        final String dateTimeFormat = "HHmmssSSS dd/MM/yyyy";
        final String value = "123059123 20/10/2000";
        final String result = dateTimeFormatParser.convertDateTimeToIso8601(dateTimeFormat, value);
        assertAll(
            () -> assertThat(result, is("2000-10-20T12:30:59.123"))
        );
    }

    @Test
    void shouldConvertDateToIso8601() {
        final String dateTimeFormat = "dd/MM/yyyy";
        final String value = "20/10/2000";

        dateTimeFormatParser.convertDateToIso8601(dateTimeFormat, value);
    }

    @Test
    void shouldConvertDateToIso8601Format() {
        final String dateTimeFormat = "dd/MM/yyyy";
        final String value = "20/10/2000";
        final String result = dateTimeFormatParser.convertDateToIso8601(dateTimeFormat, value);
        assertAll(
            () -> assertThat(result, is("2000-10-20"))
        );
    }

    @Test
    void shouldConvertIso8601ToDateTimeFormat1() {
        final String dateTimeFormat = "dd/MM/yyyy HH-mm-ss";
        final String value = "2000-10-20T12:30:59.000";

        final String result = dateTimeFormatParser.convertIso8601ToDateTime(dateTimeFormat, value);

        assertAll(
            () -> assertThat(result, is("20/10/2000 12-30-59"))
        );
    }

    @Test
    void shouldConvertIso8601ToDateTimeFormat2() {
        final String dateTimeFormat = "dd MMM yy H:mm a";
        final String value = "2000-10-20T12:30:59.000";

        final String result = dateTimeFormatParser.convertIso8601ToDateTime(dateTimeFormat, value);

        assertAll(
            () -> assertThat(result, is("20 Oct 00 12:30 PM"))
        );
    }

    @Test
    void shouldConvertIso8601ToDateTimeFormat3() {
        final String dateTimeFormat = "dd/MM/yyyy HH-mm-ss-SSS";
        final String value = "2000-10-20T12:30";

        final String result = dateTimeFormatParser.convertIso8601ToDateTime(dateTimeFormat, value);

        assertAll(
            () -> assertThat(result, is("20/10/2000 12-30-00-000"))
        );
    }

    @Test
    void shouldConvertIso8601ToDateTimeFormat4() {
        final String dateTimeFormat = "dd/MM/yyyy HH-mm-ss-SSS";
        final String value = "2000-10-20T12:30:00";

        final String result = dateTimeFormatParser.convertIso8601ToDateTime(dateTimeFormat, value);

        assertAll(
            () -> assertThat(result, is("20/10/2000 12-30-00-000"))
        );
    }

    @Test
    void shouldConvertIso8601ToDateTimeFormat5() {
        final String dateTimeFormat = "dd/MM/yyyy HH-mm-ss-SSS";
        final String value = "2012-04-21T00:00:00.5";

        final String result = dateTimeFormatParser.convertIso8601ToDateTime(dateTimeFormat, value);

        assertAll(
            () -> assertThat(result, is("21/04/2012 00-00-00-500"))
        );
    }

    @Test
    void shouldConvertIso8601ToDateTimeFormat6() {
        final String dateTimeFormat = "dd/MM/yyyy HH-mm-ss-SSS";
        final String value = "2012-04-21T00:00:30.01";

        final String result = dateTimeFormatParser.convertIso8601ToDateTime(dateTimeFormat, value);

        assertAll(
            () -> assertThat(result, is("21/04/2012 00-00-30-010"))
        );
    }


    @Test
    void shouldConvertIso8601ToDateFormat1() {
        final String dateTimeFormat = "dd/MM/yyyy";
        final String value = "2000-10-20";

        final String result = dateTimeFormatParser.convertIso8601ToDate(dateTimeFormat, value);

        assertAll(
            () -> assertThat(result, is("20/10/2000"))
        );
    }

    @Test
    void shouldConvertIso8601ToDateFormat2() {
        final String dateTimeFormat = "MMM d yy";
        final String value = "2010-10-09";

        final String result = dateTimeFormatParser.convertIso8601ToDate(dateTimeFormat, value);

        assertAll(
            () -> assertThat(result, is("Oct 9 10"))
        );
    }

    @Test
    void shouldAllowIso8601DateValueAsFallbackFormat() {
        final String dateTimeFormat = "MMM d yy";
        final String value = "2010-10-09";

        final String result = dateTimeFormatParser.convertDateToIso8601(dateTimeFormat, value);

        assertAll(
            () -> assertThat(result, is(value))
        );
    }

    @Test
    void shouldAllowIso8601DateTimeValueAsFallbackFormat() {
        final String dateTimeFormat = "MMM d yy";
        final String value = "2000-10-20T12:30:59.000";

        final String result = dateTimeFormatParser.convertDateTimeToIso8601(dateTimeFormat, value);

        assertAll(
            () -> assertThat(result, is(value))
        );
    }

    @Test
    void shouldNotAcceptDateValueInNonCompliantFormat() {
        final String dateTimeFormat = "MMM d yy";
        final String value = "123";

        assertThrows(DateTimeParseException.class, () -> {
            dateTimeFormatParser.convertDateToIso8601(dateTimeFormat, value);
        });
    }

    @Test
    void shouldNotAcceptDateTimeValueInNonCompliantFormat() {
        final String dateTimeFormat = "MMM d yy";
        final String value = "123";

        assertThrows(DateTimeParseException.class, () -> {
            dateTimeFormatParser.convertDateTimeToIso8601(dateTimeFormat, value);
        });
    }

    @Test
    void shouldNotAcceptValueWhichHasNotBeenCompletelyParsed() {
        final String dateTimeFormat = "HHmm";
        final String value = "2020-10-10";

        assertThrows(DateTimeParseException.class, () -> {
            dateTimeFormatParser.convertDateTimeToIso8601(dateTimeFormat, value);
        });
    }

    @Test
    void shouldNotAcceptFallbackValueWhichHasNotBeenCompletelyParsed() {
        final String dateTimeFormat = "HHmm";
        final String value = "2020-10-10123";

        assertThrows(DateTimeParseException.class, () -> {
            dateTimeFormatParser.convertDateTimeToIso8601(dateTimeFormat, value);
        });
    }

    @Test
    void shouldDefaultMonthAndDayTo1WhenNotProvidedInDateFormat() {
        final String dateTimeFormat = "yyyy";
        final String value = "2000";
        final String result = dateTimeFormatParser.convertDateToIso8601(dateTimeFormat, value);
        assertAll(
            () -> assertThat(result, is("2000-01-01"))
        );
    }

    @Test
    void shouldDefaultMonthAndDayTo1WhenNotProvidedInDateTimeFormat() {
        final String dateTimeFormat = "yyyy";
        final String value = "2000";
        final String result = dateTimeFormatParser.convertDateTimeToIso8601(dateTimeFormat, value);
        assertAll(
            () -> assertThat(result, is("2000-01-01T00:00:00.000"))
        );
    }

    @Test
    void shouldDefaultYearWhenNotProvidedInDateFormat() {
        final String dateTimeFormat = "MM";
        final String value = "12";
        final String result = dateTimeFormatParser.convertDateToIso8601(dateTimeFormat, value);
        assertAll(
            () -> assertThat(result, is("1970-12-01"))
        );
    }

    @Test
    void shouldDefaultYearWhenNotProvidedInDateTimeFormat() {
        final String dateTimeFormat = "MM";
        final String value = "12";
        final String result = dateTimeFormatParser.convertDateTimeToIso8601(dateTimeFormat, value);
        assertAll(
            () -> assertThat(result, is("1970-12-01T00:00:00.000"))
        );
    }

    @Test
    void shouldConvertDateTimeToIso8601FormatNullDisplayContextParameter() {
        final String dateTimeFormat = null;
        final String value = "2000-10-20T12:30:59.000";
        final String result = dateTimeFormatParser.convertDateTimeToIso8601(dateTimeFormat, value);
        assertAll(
            () -> assertThat(result, is("2000-10-20T12:30:59.000"))
        );
    }

    @Test
    void shouldConvertDateTimeToIso8601FormatIncorrectDisplayContextParameter() {
        final String dateTimeFormat = "HHmmssSSS dd/MM/yyyy";
        final String value = "2000-10-20T12:30:59.000";
        final String result = dateTimeFormatParser.convertDateTimeToIso8601(dateTimeFormat, value);
        assertAll(
            () -> assertThat(result, is("2000-10-20T12:30:59.000"))
        );
    }

    @Test
    void shouldConvertDateToIso8601FormatNullDisplayContextParameter() {
        final String dateTimeFormat = null;
        final String value = "2000-10-20";
        final String result = dateTimeFormatParser.convertDateToIso8601(dateTimeFormat, value);
        assertAll(
            () -> assertThat(result, is("2000-10-20"))
        );
    }

    @Test
    void shouldConvertDateToIso8601FormatIncorrectDisplayContextParameter() {
        final String dateTimeFormat = "dd/MM/yyyy";
        final String value = "2000-10-20";
        final String result = dateTimeFormatParser.convertDateToIso8601(dateTimeFormat, value);
        assertAll(
            () -> assertThat(result, is("2000-10-20"))
        );
    }

    @Test
    void shouldCreateTextNodeForDate_ToIso() {
        TextNode result = dateTimeFormatParser.valueToTextNode("2010", BaseType.get(DATE), "FieldId", "yyyy", true);

        assertAll(
            () -> assertThat(result.asText(), is("2010-01-01"))
        );
    }

    @Test
    void shouldCreateTextNodeForDateTime_ToIso() {
        TextNode result =
            dateTimeFormatParser.valueToTextNode("2010", BaseType.get(DATETIME), "FieldId", "yyyy", true);

        assertAll(
            () -> assertThat(result.asText(), is("2010-01-01T00:00:00.000"))
        );
    }

    @Test
    void shouldCreateTextNodeForDate_FromIso() {
        TextNode result =
            dateTimeFormatParser.valueToTextNode("2010-01-01", BaseType.get(DATE), "FieldId", "yyyy", false);

        assertAll(
            () -> assertThat(result.asText(), is("2010"))
        );
    }

    @Test
    void shouldCreateTextNodeForDateTime_FromIso() {
        TextNode result = dateTimeFormatParser.valueToTextNode("2010-01-01T00:00:00.000", BaseType.get(DATETIME),
            "FieldId", "yyyy", false);

        assertAll(
            () -> assertThat(result.asText(), is("2010"))
        );
    }

    @Test
    void shouldCreateTextNodeForNullValue() {
        TextNode result = dateTimeFormatParser.valueToTextNode(null, BaseType.get(DATE), "FieldId", "yyyy", true);

        assertAll(
            () -> assertThat(result.asText(), is(nullValue()))
        );
    }

    @Test
    void shouldCreateTextNodeForEmptyValue() {
        TextNode result = dateTimeFormatParser.valueToTextNode("", BaseType.get(DATE), "FieldId", "yyyy", true);

        assertAll(
            () -> assertThat(result.asText(), is(""))
        );
    }

    @Test
    void shouldThrowErrorForInvalidBaseType_ToIso() {
        DataProcessingException exception = assertThrows(DataProcessingException.class,
            () -> dateTimeFormatParser.valueToTextNode("abc", BaseType.get(COLLECTION), "FieldId", "dd/MM/yyyy", true)
        );

        assertAll(
            () -> assertThat(exception.getDetails(),
                is("Unable to process field FieldId of type Collection. Expected type to be either DateTime or Date"))
        );
    }

    @Test
    void shouldThrowErrorDetailingExpectedDateTimeFormat_ToIso() {
        DataProcessingException exception = assertThrows(DataProcessingException.class,
            () -> dateTimeFormatParser.valueToTextNode("abc", BaseType.get(DATETIME), "FieldId", "dd/MM/yyyy", true)
        );

        assertAll(
            () -> assertThat(exception.getDetails(),
                is("Unable to process field FieldId with value abc. Expected format to be either dd/MM/yyyy or "
                    + "yyyy-MM-dd'T'HH:mm:ss.SSS"))
        );
    }

    @Test
    void shouldThrowErrorDetailingExpectedDateFormat_ToIso() {
        DataProcessingException exception = assertThrows(DataProcessingException.class,
            () -> dateTimeFormatParser.valueToTextNode("abc", BaseType.get(DATE), "FieldId", "dd/MM/yyyy", true)
        );

        assertAll(
            () -> assertThat(exception.getDetails(),
                is("Unable to process field FieldId with value abc. Expected format to be either dd/MM/yyyy or "
                    + "yyyy-MM-dd"))
        );
    }

    @Test
    void shouldThrowErrorForInvalidBaseType_FromIso() {
        DataProcessingException exception = assertThrows(DataProcessingException.class,
            () -> dateTimeFormatParser.valueToTextNode("abc", BaseType.get(COLLECTION), "FieldId", "dd/MM/yyyy", false)
        );

        assertAll(
            () -> assertThat(exception.getDetails(),
                is("Unable to process field FieldId of type Collection. Expected type to be either DateTime or Date"))
        );
    }

    @Test
    void shouldThrowErrorDetailingExpectedDateTimeFormat_FromIso() {
        DataProcessingException exception = assertThrows(DataProcessingException.class,
            () -> dateTimeFormatParser.valueToTextNode("abc", BaseType.get(DATETIME), "FieldId", "dd/MM/yyyy", false)
        );

        assertAll(
            () -> assertThat(exception.getDetails(),
                is("Unable to process field FieldId with value abc. Expected format to be either dd/MM/yyyy or "
                    + "yyyy-MM-dd'T'HH:mm:ss.SSS"))
        );
    }

    @Test
    void shouldThrowErrorDetailingExpectedDateFormat_FromIso() {
        DataProcessingException exception = assertThrows(DataProcessingException.class,
            () -> dateTimeFormatParser.valueToTextNode("abc", BaseType.get(DATE), "FieldId", "dd/MM/yyyy", false)
        );

        assertAll(
            () -> assertThat(exception.getDetails(),
                is("Unable to process field FieldId with value abc. Expected format to be either dd/MM/yyyy or "
                    + "yyyy-MM-dd"))
        );
    }

    private FieldTypeDefinition fieldType(String fieldType) {
        return aFieldType().withType(fieldType).build();
    }

    private void setUpBaseTypes() {
        BaseType.register(new BaseType(fieldType(DATE_FIELD_TYPE)));
        BaseType.register(new BaseType(fieldType(DATETIME_FIELD_TYPE)));
        BaseType.register(new BaseType(fieldType(COLLECTION_FIELD_TYPE)));
        BaseType.register(new BaseType(fieldType(COMPLEX_FIELD_TYPE)));
    }
}
