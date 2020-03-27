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
    void shouldConvertDateToIso8601Format() {
        final String dateTimeFormat = "dd/MM/yyyy";
        final String value = "20/10/2000";
        final String result = dateTimeFormatParser.convertDateToIso8601(dateTimeFormat, value);
        assertAll(
            () -> assertThat(result, is("2000-10-20"))
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
}
