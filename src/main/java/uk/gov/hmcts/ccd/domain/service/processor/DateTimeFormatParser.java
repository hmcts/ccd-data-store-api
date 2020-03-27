package uk.gov.hmcts.ccd.domain.service.processor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import lombok.extern.slf4j.*;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DateTimeFormatParser {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    public String convertDateTimeToIso8601(String dateTimeFormat, String value) {
        LocalDateTime dateTime;
        try {
            dateTimeFormat = ((dateTimeFormat == null) ? DATE_TIME_FORMAT : dateTimeFormat);
            DateTimeFormatter inputFormat = getDateTimeFormatter(dateTimeFormat);
            dateTime = LocalDateTime.parse(value, inputFormat);
        } catch (Exception e) {
            log.warn("Failed to parse following dateTime value {} with format {} attempting to parse with {}", value, dateTimeFormat, DATE_TIME_FORMAT);
            dateTimeFormat = DATE_TIME_FORMAT;
            DateTimeFormatter inputFormat = getDateTimeFormatter(dateTimeFormat);
            dateTime = LocalDateTime.parse(value, inputFormat);
        }
        return dateTime.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
    }

    private DateTimeFormatter getDateTimeFormatter(String dateTimeFormat) {
        return new DateTimeFormatterBuilder()
            .appendPattern(dateTimeFormat)
            .parseDefaulting(ChronoField.YEAR_OF_ERA, LocalDate.now().getYear())
            .parseDefaulting(ChronoField.MONTH_OF_YEAR, LocalDate.now().getMonthValue())
            .parseDefaulting(ChronoField.DAY_OF_MONTH, LocalDate.now().getDayOfMonth())
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .parseDefaulting(ChronoField.MILLI_OF_SECOND, 0)
            .toFormatter();
    }

    public String convertDateToIso8601(String dateFormat, String value) {
        LocalDate date;
        try {
            dateFormat = ((dateFormat == null) ? DATE_FORMAT : dateFormat);
            DateTimeFormatter inputFormat = getDateFormatter(dateFormat);
            date = LocalDate.parse(value, inputFormat);
        } catch (Exception e) {
            log.warn("Failed to parse following date value {} with format {} attempting to parse with {}", value, dateFormat, DATE_FORMAT);
            dateFormat = DATE_FORMAT;
            DateTimeFormatter inputFormat = getDateFormatter(dateFormat);
            date = LocalDate.parse(value, inputFormat);
        }
        return date.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
    }

    private DateTimeFormatter getDateFormatter(String dateFormat) {
        return new DateTimeFormatterBuilder()
                .appendPattern(dateFormat)
                .parseDefaulting(ChronoField.YEAR_OF_ERA, LocalDate.now().getYear())
                .parseDefaulting(ChronoField.MONTH_OF_YEAR, LocalDate.now().getMonthValue())
                .parseDefaulting(ChronoField.DAY_OF_MONTH, LocalDate.now().getDayOfMonth())
                .toFormatter();
    }
}
