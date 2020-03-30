package uk.gov.hmcts.ccd.domain.service.processor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DateTimeFormatParser {

    static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");
    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String convertDateTimeToIso8601(String dateTimeFormat, String value) {
        LocalDateTime dateTime;
        try {
            DateTimeFormatter inputFormat = (dateTimeFormat == null) ? DATE_TIME_FORMAT : getDateTimeFormatter(dateTimeFormat);
            dateTime = LocalDateTime.parse(value, inputFormat);
        } catch (Exception e) {
            log.warn("Failed to parse following dateTime value {} with format {} attempting to parse with {}", value, dateTimeFormat, DATE_TIME_FORMAT);
            dateTime = LocalDateTime.parse(value, DATE_TIME_FORMAT);
        }
        return dateTime.format(DATE_TIME_FORMAT);
    }

    public String convertDateToIso8601(String dateFormat, String value) {
        LocalDate date;
        try {
            DateTimeFormatter inputFormat = (dateFormat == null) ? DATE_FORMAT : getDateFormatter(dateFormat);
            date = LocalDate.parse(value, inputFormat);
        } catch (Exception e) {
            log.warn("Failed to parse following date value {} with format {} attempting to parse with {}", value, dateFormat, DATE_FORMAT);
            date = LocalDate.parse(value, DATE_FORMAT);
        }
        return date.format(DATE_FORMAT);
    }

    public String convertIso8601ToDateTime(String dateTimeFormat, String value) {
        LocalDateTime dateTime = LocalDateTime.parse(value, DATE_TIME_FORMAT);
        return dateTime.format(DateTimeFormatter.ofPattern(dateTimeFormat));
    }

    public String convertIso8601ToDate(String dateFormat, String value) {
        LocalDate date = LocalDate.parse(value, DATE_FORMAT);
        return date.format(DateTimeFormatter.ofPattern(dateFormat));
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

    private DateTimeFormatter getDateFormatter(String dateFormat) {
        return new DateTimeFormatterBuilder()
            .appendPattern(dateFormat)
            .parseDefaulting(ChronoField.YEAR_OF_ERA, LocalDate.now().getYear())
            .parseDefaulting(ChronoField.MONTH_OF_YEAR, LocalDate.now().getMonthValue())
            .parseDefaulting(ChronoField.DAY_OF_MONTH, LocalDate.now().getDayOfMonth())
            .toFormatter();
    }
}
