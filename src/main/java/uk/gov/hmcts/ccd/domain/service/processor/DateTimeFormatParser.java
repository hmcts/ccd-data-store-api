package uk.gov.hmcts.ccd.domain.service.processor;

import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DateTimeFormatParser {

    static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String convertDateTimeToIso8601(String dateTimeFormat, String value) {
        DateTimeFormatter inputFormat = (dateTimeFormat == null) ? DATE_TIME_FORMAT : getDateTimeFormatter(dateTimeFormat);
        return convert(inputFormat, DATE_TIME_FORMAT, value);
    }

    public String convertDateToIso8601(String dateFormat, String value) {
        DateTimeFormatter inputFormat = (dateFormat == null) ? DATE_FORMAT : getDateFormatter(dateFormat);
        return convert(inputFormat, DATE_FORMAT, value);
    }

    public String convertIso8601ToDateTime(String dateTimeFormat, String value) {
        LocalDateTime dateTime = LocalDateTime.parse(value, DATE_TIME_FORMAT);
        return dateTime.format(DateTimeFormatter.ofPattern(dateTimeFormat));
    }

    public String convertIso8601ToDate(String dateFormat, String value) {
        LocalDate date = LocalDate.parse(value, DATE_FORMAT);
        return date.format(DateTimeFormatter.ofPattern(dateFormat));
    }

    private String convert(DateTimeFormatter inputFormat, DateTimeFormatter outputFormat, String value) {
        ParsePosition parsePosition = new ParsePosition(0);
        TemporalAccessor parsed = inputFormat.parseUnresolved(value, parsePosition);
        if (parsed == null || isInvalidParseResult(value, parsePosition)) {
            log.warn("Failed to parse following date value {} with format {} attempting to parse with {}", value, inputFormat, outputFormat);
            parsePosition = new ParsePosition(0);
            parsed = outputFormat.parseUnresolved(value, parsePosition);
            if (parsed == null || isInvalidParseResult(value, parsePosition)) {
                throw new DateTimeParseException(String.format("Failed to parse value '%s' against format '%s'", value, inputFormat), value, 0);
            }
        }

        return outputFormat.format(parsed);
    }

    private DateTimeFormatter getDateTimeFormatter(String dateTimeFormat) {
        return new DateTimeFormatterBuilder()
            .appendPattern(dateTimeFormat)
            .parseDefaulting(ChronoField.YEAR_OF_ERA, LocalDate.now().getYear())
            .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
            .toFormatter();
    }

    private DateTimeFormatter getDateFormatter(String dateFormat) {
        return new DateTimeFormatterBuilder()
            .appendPattern(dateFormat)
            .parseDefaulting(ChronoField.YEAR_OF_ERA, LocalDate.now().getYear())
            .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .toFormatter();
    }

    private boolean isInvalidParseResult(String value, ParsePosition parsePosition) {
        return parsePosition.getIndex() != value.length();
    }
}
