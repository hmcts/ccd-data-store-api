package uk.gov.hmcts.ccd.domain.service.processor.date;

import java.text.ParsePosition;
import java.time.DateTimeException;
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

import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.ccd.domain.types.BaseType;
import uk.gov.hmcts.ccd.endpoint.exceptions.DataProcessingException;

import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DATE;
import static uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition.DATETIME;

@Component
@Slf4j
public class DateTimeFormatParser {

    public static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    public static final String DATE_FORMAT = "yyyy-MM-dd";
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

    public TextNode valueToTextNode(String valueToConvert, BaseType baseType, String fieldPath, String format, boolean toIso) {
        if (Strings.isNullOrEmpty(valueToConvert)) {
            return new TextNode(valueToConvert);
        }

        try {
            if (baseType == BaseType.get(DATETIME)) {
                return new TextNode(toIso ? convertDateTimeToIso8601(format, valueToConvert) :
                    convertIso8601ToDateTime(format, valueToConvert));
            } else if (baseType == BaseType.get(DATE)) {
                return new TextNode(toIso ? convertDateToIso8601(format, valueToConvert) :
                    convertIso8601ToDate(format, valueToConvert));
            } else {
                throw new DataProcessingException().withDetails(
                    String.format("Unable to process field %s of type %s. Expected type to be either %s or %s",
                        fieldPath,
                        baseType.getType(),
                        DATETIME,
                        DATE)
                );
            }
        } catch (DateTimeException e) {
            throw new DataProcessingException().withDetails(
                String.format("Unable to process field %s with value %s. Expected format to be either %s or %s",
                    fieldPath,
                    valueToConvert,
                    format,
                    baseType == BaseType.get(DATETIME) ? DATE_TIME_FORMAT : DATE_FORMAT)
            );
        }
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

            if (isValidParseResult(value, parsePosition)) {
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
        return parsePosition.getErrorIndex() != 1 && parsePosition.getIndex() == value.length();
    }
}
