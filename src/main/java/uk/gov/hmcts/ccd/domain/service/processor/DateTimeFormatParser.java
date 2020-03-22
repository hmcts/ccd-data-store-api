package uk.gov.hmcts.ccd.domain.service.processor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

import org.springframework.stereotype.Component;

@Component
public class DateTimeFormatParser {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    public String convertDateTimeToIso8601(String dateTimeFormat, String value) {
        DateTimeFormatter inputFormat = new DateTimeFormatterBuilder()
            .appendPattern(dateTimeFormat)
            .parseDefaulting(ChronoField.YEAR_OF_ERA, LocalDate.now().getYear())
            .parseDefaulting(ChronoField.MONTH_OF_YEAR, LocalDate.now().getMonthValue())
            .parseDefaulting(ChronoField.DAY_OF_MONTH, LocalDate.now().getDayOfMonth())
            .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
            .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
            .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
            .parseDefaulting(ChronoField.MILLI_OF_SECOND, 0)
            .toFormatter();

        LocalDateTime dateTime = LocalDateTime.parse(value, inputFormat);
        return dateTime.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT));
    }

    public String convertDateToIso8601(String dateFormat, String value) {
        DateTimeFormatter inputFormat = new DateTimeFormatterBuilder()
            .appendPattern(dateFormat)
            .parseDefaulting(ChronoField.YEAR_OF_ERA, LocalDate.now().getYear())
            .parseDefaulting(ChronoField.MONTH_OF_YEAR, LocalDate.now().getMonthValue())
            .parseDefaulting(ChronoField.DAY_OF_MONTH, LocalDate.now().getDayOfMonth())
            .toFormatter();

        LocalDate date = LocalDate.parse(value, inputFormat);
        return date.format(DateTimeFormatter.ofPattern(DATE_FORMAT));
    }
}
