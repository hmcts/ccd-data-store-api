package uk.gov.hmcts.ccd.domain.service.processor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DateTimeFormatParser {

    private static final Logger LOG = LoggerFactory.getLogger(DateTimeFormatParser.class);

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";

    public void parseDateTimeFormat(String dateTimeFormat, String value) throws Exception {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dateTimeFormat);
            formatter.parse(value);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            LOG.error("Error occurred while parsing date time format " + dateTimeFormat, e);
            throw new Exception(dateTimeFormat);
        }
    }

    public String convertDateTimeToIso8601(String dateTimeFormat, String value) {
        DateTimeFormatter inputFormat = DateTimeFormatter.ofPattern(dateTimeFormat);
        DateTimeFormatter outputFormat = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT);

        LocalDateTime dateTime = LocalDateTime.parse(value, inputFormat);
        return dateTime.format(outputFormat);
    }
}
