package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseField;
import uk.gov.hmcts.ccd.domain.model.definition.FieldType;
import uk.gov.hmcts.ccd.test.CaseFieldBuilder;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.types.BaseTypeValidator.REGEX_GUIDANCE;

@DisplayName("DateValidator")
class DateValidatorTest {
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final String DATE_REGEX = "^(\\d{4})\\D?(0[1-9]|1[0-2])\\D?([12]\\d|0[1-9]|3[01])([zZ]|([\\+-])([01]\\d|2[0-3])\\D?([0-5]\\d)?)?$";
    private static final String LIMITED_REGEX = "^\\d{4}-\\d{2}-\\d{2}$";
    private static final String FIELD_ID = "TEST_FIELD_ID";

    @Mock
    private FieldType dateFieldType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    private DateValidator validator;
    private CaseField caseField;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(dateFieldType.getType()).thenReturn(DateValidator.TYPE_ID);
        when(dateFieldType.getRegularExpression()).thenReturn(DATE_REGEX);
        BaseType.register(new BaseType(dateFieldType));

        validator = new DateValidator();

        caseField = caseField().build();
    }

    @Nested
    @DisplayName("when date is valid")
    class WhenDateValid {
        @Test
        @DisplayName("should accept date without timezone")
        void shouldAcceptDateNoTimezone() {
            final List<ValidationResult> result = validator.validate(FIELD_ID,
                                                                     NODE_FACTORY.textNode("2012-04-21"), caseField);
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("should accept date with timezone Z")
        void shouldAcceptDateWithTimezoneZ() {
            final List<ValidationResult> result = validator.validate(FIELD_ID,
                                                                     NODE_FACTORY.textNode("2012-04-21Z"), caseField);
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("should accept date with timezone")
        void shouldAcceptDateWithTimezone() {
            final List<ValidationResult> result = validator.validate(FIELD_ID,
                                                                     NODE_FACTORY.textNode("2012-04-21+01:00"),
                                                                     caseField);
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("should accept date for leap year: 2000-02-29Z")
        void shouldAcceptDayLeapYear() {
            final List<ValidationResult> result = validator.validate(FIELD_ID,
                                                                     NODE_FACTORY.textNode("2000-02-29Z"), caseField);
            assertEquals(0, result.size());
        }
    }

    @Nested
    @DisplayName("when date is invalid")
    class WhenDateInvalid {
        @Test
        @DisplayName("should not accept date: 3321M1 1AA")
        void shouldNotAcceptNotDate() {
            final List<ValidationResult> result = validator.validate(FIELD_ID,
                                                                     NODE_FACTORY.textNode("3321M1 1AA"), caseField);
            assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals("Date or Time entered is not valid", result.get(0).getErrorMessage())
            );
        }

        @Test
        @DisplayName("should not accept date: 1800-14-14")
        void shouldNotAcceptNotMonth() {
            final List<ValidationResult> result = validator.validate(FIELD_ID,
                                                                     NODE_FACTORY.textNode("1800-14-14"), caseField);
            assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals("Date or Time entered is not valid", result.get(0).getErrorMessage())
            );
        }

        @Test
        @DisplayName("should not accept date: 2001-11-31")
        void shouldNotAcceptNotDay() {
            final List<ValidationResult> result = validator.validate(FIELD_ID,
                                                                     NODE_FACTORY.textNode("2001-11-31"), caseField);
            assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals("Date or Time entered is not valid", result.get(0).getErrorMessage())
            );
        }

        @Test
        @DisplayName("should not accept date with time: 2001-01-01T00:00:00.000Z")
        void shouldNotAcceptDateTime() {
            final List<ValidationResult> result = validator.validate(FIELD_ID,
                                                                     NODE_FACTORY.textNode("2001-01-01T00:00:00.000Z"),
                                                                     caseField);
            assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals("Date or Time entered is not valid",
                                   result.get(0).getErrorMessage())
            );
        }

        @Test
        @DisplayName("should not accept date: 2001-02-29Z")
        void shouldNotAcceptNotDayLeapYear() {
            final List<ValidationResult> result = validator.validate(FIELD_ID,
                                                                     NODE_FACTORY.textNode("2001-02-29Z"), caseField);
            assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertEquals("Date or Time entered is not valid", result.get(0).getErrorMessage())
            );
        }
    }

    @Test
    void getType() {
        assertEquals(validator.getType(), BaseType.get("DATE"), "Type is incorrect");
    }

    @Test
    void nullValue() {
        assertEquals(0, validator.validate(FIELD_ID, null, null).size(), "Did not catch NULL");
    }

    @Test
    void checkMax() {
        final String validDate = "2001-01-01Z";
        final String invalidDate = "2002-01-01Z";
        final String maxDate = "2001-12-31+01:00";
        final CaseField caseField = caseField().withMax(date(maxDate)).build();

        final List<ValidationResult> result01 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode(validDate), caseField);
        assertEquals(0, result01.size(), result01.toString());

        final List<ValidationResult> result02 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode(maxDate), caseField);
        assertEquals(0, result02.size(), result02.toString());

        final List<ValidationResult> result03 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode(invalidDate), caseField);
        assertEquals(1, result03.size(), "Did not catch invalid max-date");
        assertEquals("The date should be earlier than 31-12-2001",
                     result03.get(0).getErrorMessage(),
                     "Validation message");
    }

    @Test
    void checkMin() {
        final String validDate = "2001-12-31Z";
        final String invalidDate = "2000-01-01Z";
        final String minDate = "2001-01-01Z";
        final CaseField caseField = caseField().withMin(date(minDate)).build();

        final List<ValidationResult> result01 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode(validDate), caseField);
        assertEquals(0, result01.size(), result01.toString());

        final List<ValidationResult> result02 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode(minDate), caseField);
        assertEquals(0, result02.size(), result02.toString());

        final List<ValidationResult> result03 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode(invalidDate), caseField);
        assertEquals(1, result03.size(), "Did not catch invalid max-date");
        assertEquals("The date should be later than 01-01-2001",
                     result03.get(0).getErrorMessage(),
                     "Validation message");
    }

    @Test
    void checkMaxMinWithoutRegEx() {
        final String validDate = "2001-12-10Z";
        final String invalidMinDate = "1999-12-31Z";
        final String invalidMaxDate = "2002-01-01Z";
        final String minDate = "2001-01-01Z";
        final String maxDate = "2001-12-31Z";
        final CaseField caseField = caseField().withMin(date(minDate))
                                               .withMax(date(maxDate))
                                               .build();

        final List<ValidationResult> result01 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode(validDate), caseField);
        assertEquals(0, result01.size(), result01.toString());

        final List<ValidationResult> result02 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode(minDate), caseField);
        assertEquals(0, result02.size(), result02.toString());

        final List<ValidationResult> result03 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode(maxDate), caseField);
        assertEquals(0, result03.size(), result03.toString());

        final List<ValidationResult> result04 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode(invalidMinDate), caseField);
        assertEquals(1, result04.size(), "Did not catch invalid min-date");

        final List<ValidationResult> result05 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode(invalidMaxDate), caseField);
        assertEquals(1, result05.size(), "Did not catch invalid max-date");
    }

    @Test
    void invalidFieldTypeRegEx() {
        final String validDate = "2001-12-10Z";

        final CaseField caseField = caseField().withMin(date("2001-01-01"))
                                               .withMax(date("2001-12-10Z"))
                                               .withRegExp("InvalidRegEx")
                                               .build();
        final List<ValidationResult> result = validator.validate(FIELD_ID,
                                                                 NODE_FACTORY.textNode(validDate), caseField);
        assertAll(
            () -> assertEquals(1, result.size(), "RegEx validation failed"),
            () -> assertEquals(REGEX_GUIDANCE, result.get(0).getErrorMessage()),
            () -> assertEquals(FIELD_ID, result.get(0).getFieldId())
        );
    }

    @Test
    void invalidBaseTypeRegEx() {
        when(dateFieldType.getRegularExpression()).thenReturn("InvalidRegEx");
        BaseType.register(new BaseType(dateFieldType));

        final CaseField caseField = caseField().build();
        final List<ValidationResult> result = validator.validate(FIELD_ID,
                                                                 NODE_FACTORY.textNode("2001-12-10"), caseField);
        assertAll(
            () -> assertEquals(1, result.size(), "RegEx validation failed"),
            () -> assertEquals(REGEX_GUIDANCE, result.get(0).getErrorMessage()),
            () -> assertEquals(FIELD_ID, result.get(0).getFieldId())
        );
    }

    @Test
    void validRegEx() {
        final String validDate = "2001-12-10";
        final CaseField caseField = caseField().withRegExp(LIMITED_REGEX).build();

        final List<ValidationResult> result = validator.validate(FIELD_ID,
                                                                 NODE_FACTORY.textNode(validDate), caseField);
        assertEquals(0, result.size(), "RegEx validation failed");
    }

    private BigDecimal date(final String dateString) {
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return new BigDecimal(df.parse(dateString).getTime());
        } catch (ParseException e) {
            throw new AssertionError(e);
        }
    }

    private CaseFieldBuilder caseField() {
        return new CaseFieldBuilder(FIELD_ID).withType(DateValidator.TYPE_ID);
    }
}
