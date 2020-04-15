package uk.gov.hmcts.ccd.domain.service.processor;

import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DateTimeFormatParser {

    static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final int DEFAULT_YEAR = 1970;

    public String convertDateTimeToIso8601(String dateTimeFormat, String value) {
        return convert(Arrays.asList(dateTimeFormat, DATE_TIME_FORMAT), DATE_TIME_FORMAT, value);
    }

    public String convertDateToIso8601(String dateFormat, String value) {
        return convert(Arrays.asList(dateFormat, DATE_FORMAT), DATE_FORMAT, value);
    }

    public String convertIso8601ToDateTime(String dateTimeFormat, String value) {
        LocalDateTime dateTime = LocalDateTime.parse(value);
        return dateTime.format(DateTimeFormatter.ofPattern(dateTimeFormat));
    }

    public String convertIso8601ToDate(String dateFormat, String value) {
        LocalDate date = LocalDate.parse(value, DateTimeFormatter.ofPattern(DATE_FORMAT));
        return date.format(DateTimeFormatter.ofPattern(dateFormat));
    }

    private String convert(List<String> permittedInputFormats, String outputFormat, String value) {
        List<DateTimeFormatter> inputFormatters = permittedInputFormats.stream()
            .filter(Objects::nonNull)
            .map(this::getFormatter)
            .collect(Collectors.toList());
        DateTimeFormatter outputFormatter = getFormatter(outputFormat);
        TemporalAccessor parsed = null;

        Iterator<DateTimeFormatter> iterator = inputFormatters.iterator();
        while (iterator.hasNext()) {
            DateTimeFormatter inputFormatter = iterator.next();

            ParsePosition parsePosition = new ParsePosition(0);
            parsed = inputFormatter.parseUnresolved(value, parsePosition);

            if (parsed != null && isValidParseResult(value, parsePosition)) {
                break;
            } else if (!iterator.hasNext()) {
                throw new DateTimeParseException(
                    String.format("Failed to parse value '%s' against formats: %s",
                        value, String.join(", ", permittedInputFormats)), value, 0);
            }
        }

        return outputFormatter.format(parsed);
    }

    private DateTimeFormatter getFormatter(String dateTimeFormat) {
        return new DateTimeFormatterBuilder()
            .appendPattern(dateTimeFormat)
            .parseDefaulting(ChronoField.YEAR_OF_ERA, DEFAULT_YEAR)
            .parseDefaulting(ChronoField.MONTH_OF_YEAR, 1)
            .parseDefaulting(ChronoField.DAY_OF_MONTH, 1)
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .parseDefaulting(ChronoField.NANO_OF_SECOND, 0)
            .toFormatter();
    }

    private boolean isValidParseResult(String value, ParsePosition parsePosition) {
        return parsePosition.getIndex() == value.length();
    }
}
