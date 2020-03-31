package uk.gov.hmcts.ccd.domain.service.processor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertAll;

class DateTimeFormatParserTest {

    private DateTimeFormatParser dateTimeFormatParser;

    @BeforeEach
    void setUp() {
        dateTimeFormatParser = new DateTimeFormatParser();
    }

    @Test
    void shouldConvertDateTimeToIso8601Format() {
        final String dateTimeFormat = "HHmmssSSS dd/MM/yyyy";
        final String value = "123059000 20/10/2000";
        final String result = dateTimeFormatParser.convertDateTimeToIso8601(dateTimeFormat, value);
        assertAll(
            () -> assertThat(result, is("2000-10-20T12:30:59.000"))
        );
    }

    @Test
    void shouldConvertDateToIso8601() {
        final String dateTimeFormat = "dd/MM/yyyy";
        final String value = "20/10/2000";

        final String result = dateTimeFormatParser.convertDateToIso8601(dateTimeFormat, value);
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
}
