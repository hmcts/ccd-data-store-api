package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.FieldTypeDefinition;
import uk.gov.hmcts.ccd.test.CaseFieldDefinitionBuilder;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@DisplayName("DateTimeValidator")
class DateTimeValidatorTest {
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final String FIELD_ID = "TEST_FIELD_ID";
    private static final String DATE_TIME_REGEX = "^(\\d{4}(?!\\d{2}\\b))((-?)((0[1-9]|1[0-2])(\\3([12]\\d|0[1-9]"
        + "|3[01]))?|W([0-4]\\d|5[0-2])(-?[1-7])?|"
        + "(00[1-9]|0[1-9]\\d|[12]\\d{2}|3([0-5]\\d|6[1-6])))([T\\s]((([01]\\d|2[0-3])((:?)[0-5]\\d)?|24\\:?00)"
        + "([\\.,]\\d+(?!:))?)?(\\17[0-5]\\d([\\.,]\\d+)?)?([zZ]|([\\+-])([01]\\d|2[0-3]):?([0-5]\\d)?)?)?)?$";

    @Mock
    private FieldTypeDefinition dateTimeFieldTypeDefinition;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    private DateTimeValidator validator;
    private CaseFieldDefinition caseFieldDefinition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(dateTimeFieldTypeDefinition.getType()).thenReturn(DateTimeValidator.TYPE_ID);
        when(dateTimeFieldTypeDefinition.getRegularExpression()).thenReturn(DATE_TIME_REGEX);
        BaseType.register(new BaseType(dateTimeFieldTypeDefinition));

        validator = new DateTimeValidator();

        caseFieldDefinition = caseField().build();
    }

    @Nested
    @DisplayName("when valid datetime")
    class WhenValidDateTime {
        @Test
        @DisplayName("should validate date time: 2012-04-21T00:00:00.000")
        void shouldValidateDateTimeWithoutTimeZone() {
            final List<ValidationResult> results = validator.validate(FIELD_ID,
                                                                      NODE_FACTORY.textNode("2012-04-21T00:00:00.000"),
                    caseFieldDefinition);
            assertThat(results, hasSize(0));
        }

        @Test
        @DisplayName("should validate date time: 2012-04-21T00:00:00.000Z")
        void shouldValidateDateTimeWithTimeZoneZ() {
            final List<ValidationResult> results =
                validator.validate(FIELD_ID, NODE_FACTORY.textNode("2012-04-21T00:00:00.000Z"), caseFieldDefinition);
            assertThat(results, hasSize(0));
        }

        @Test
        @DisplayName("should validate date time: 2012-04-21T00:00:00+01:00")
        void shouldValidateDateTimeWithTimeZone() {
            final List<ValidationResult> results =
                validator.validate(FIELD_ID, NODE_FACTORY.textNode("2012-04-21T00:00:00+01:00"), caseFieldDefinition);
            assertThat(results, hasSize(0));
        }

        @Test
        @DisplayName("should validate date time: 2000-02-29T00:00:00Z")
        void shouldValidateDateTimeLeapYear() {
            final List<ValidationResult> results = validator.validate(FIELD_ID,
                                                                      NODE_FACTORY.textNode("2000-02-29T00:00:00Z"),
                    caseFieldDefinition);
            assertThat(results, hasSize(0));
        }
    }

    @Nested
    @DisplayName("when invalid datetime")
    class WhenInvalidDateTime {
        @Test
        @DisplayName("should not validate date time: 3321M1 1AA")
        void shouldNotValidateNotDateTime() {
            final List<ValidationResult> results = validator.validate(FIELD_ID,
                                                                      NODE_FACTORY.textNode("3321M1 1AA"),
                    caseFieldDefinition);
            assertAll(
                () -> assertThat(results, hasSize(1)),
                () -> assertThat(results.get(0).getErrorMessage(),
                                 equalTo("Date or Time entered is not valid"))
            );
        }

        @Test
        @DisplayName("should not validate date time: 1800-14-14T00:00:00")
        void shouldNotValidateNotMonth() {
            final List<ValidationResult> results = validator.validate(FIELD_ID,
                                                                      NODE_FACTORY.textNode("1800-14-14T00:00:00"),
                    caseFieldDefinition);
            assertAll(
                () -> assertThat(results, hasSize(1)),
                () -> assertThat(results.get(0).getErrorMessage(),
                                 equalTo("Date or Time entered is not valid"))
            );
        }

        @Test
        @DisplayName("should not validate date time: 2001-11-31T00:00:00")
        void shouldNotValidateNotDay() {
            final List<ValidationResult> results = validator.validate(FIELD_ID,
                                                                      NODE_FACTORY.textNode("2001-11-31T00:00:00"),
                    caseFieldDefinition);
            assertAll(
                () -> assertThat(results, hasSize(1)),
                () -> assertThat(results.get(0).getErrorMessage(),
                                 equalTo("Date or Time entered is not valid"))
            );
        }

        @Test
        @DisplayName("should not validate date time: 2001-01-01")
        void shouldNotValidateNotTime() {
            final List<ValidationResult> results = validator.validate(FIELD_ID,
                                                                      NODE_FACTORY.textNode("2001-01-01"),
                    caseFieldDefinition);
            assertAll(
                () -> assertThat(results, hasSize(1)),
                () -> assertThat(results.get(0).getErrorMessage(),
                                 equalTo("Date or Time entered is not valid"))
            );
        }

        @Test
        @DisplayName("should not validate date time: 2100-02-29T00:00:00Z")
        void shouldNotValidateNotDayLeapYear() {
            final List<ValidationResult> results = validator.validate(FIELD_ID,
                                                                      NODE_FACTORY.textNode("2100-02-29T00:00:00Z"),
                    caseFieldDefinition);
            assertAll(
                () -> assertThat(results, hasSize(1)),
                () -> assertThat(results.get(0).getErrorMessage(),
                                 equalTo("Date or Time entered is not valid"))
            );
        }
    }

    @Test
    void getType() {
        assertEquals(validator.getType(), BaseType.get("DATETIME"));
    }

    @Test
    void nullValue() {
        assertEquals(0, validator.validate(FIELD_ID, null, null).size());
    }

    @Test
    void checkMax() {
        final String validDateTime = "2001-01-01T00:00:00Z";
        final String invalidDateTime = "2002-01-01T00:00:00Z";
        final String maxDateTime = "2001-12-31T00:00:00+01:00";
        final CaseFieldDefinition caseFieldDefinition = caseField().withMax(datetime(maxDateTime)).build();

        final List<ValidationResult> result01 =
            validator.validate(FIELD_ID, NODE_FACTORY.textNode(validDateTime), caseFieldDefinition);
        assertEquals(0, result01.size(), result01.toString());

        final List<ValidationResult> result02 =
            validator.validate(FIELD_ID, NODE_FACTORY.textNode(maxDateTime), caseFieldDefinition);
        assertEquals(0, result02.size(), result02.toString());

        final List<ValidationResult> result03 =
            validator.validate(FIELD_ID, NODE_FACTORY.textNode(invalidDateTime), caseFieldDefinition);
        assertEquals(1, result03.size(), "Did not catch invalid max-date");
        assertEquals("The date time should be earlier than 2001-12-31T00:00:00",
                     result03.get(0).getErrorMessage(), "Validation message");
    }

    @Test
    void checkMin() {
        final String validDateTime = "2001-12-31T00:00:00Z";
        final String invalidDateTime = "2000-01-01T00:00:00Z";
        final String minDateTime = "2001-01-01T00:00:00Z";
        final CaseFieldDefinition caseFieldDefinition = caseField().withMin(datetime(minDateTime)).build();

        final List<ValidationResult> result01 =
            validator.validate(FIELD_ID, NODE_FACTORY.textNode(validDateTime), caseFieldDefinition);
        assertEquals(0, result01.size(), result01.toString());

        final List<ValidationResult> result02 =
            validator.validate(FIELD_ID, NODE_FACTORY.textNode(minDateTime), caseFieldDefinition);
        assertEquals(0, result02.size(), result02.toString());

        final List<ValidationResult> result03 =
            validator.validate(FIELD_ID, NODE_FACTORY.textNode(invalidDateTime), caseFieldDefinition);
        assertEquals(1, result03.size(), "Did not catch invalid max-date");
        assertEquals("The date time should be later than 2001-01-01T00:00:00",
                     result03.get(0).getErrorMessage(),
                     "Validation message");
    }

    @Test
    void checkMaxMinWithoutRegEx() {
        final String validDateTime = "2001-12-10T00:00:00Z";
        final String invalidMinDateTime = "1999-12-31T00:00:00Z";
        final String invalidMaxDateTime = "2002-01-01T00:00:00Z";
        final String minDateTime = "2001-01-01T00:00:00Z";
        final String maxDateTime = "2001-12-31T00:00:00Z";
        final CaseFieldDefinition caseFieldDefinition = caseField().withMin(datetime(minDateTime))
                                               .withMax(datetime(maxDateTime))
                                               .build();

        final List<ValidationResult> result01 =
            validator.validate(FIELD_ID, NODE_FACTORY.textNode(validDateTime), caseFieldDefinition);
        assertEquals(0, result01.size(), result01.toString());

        final List<ValidationResult> result02 =
            validator.validate(FIELD_ID, NODE_FACTORY.textNode(minDateTime), caseFieldDefinition);
        assertEquals(0, result02.size(), result02.toString());

        final List<ValidationResult> result03 =
            validator.validate(FIELD_ID, NODE_FACTORY.textNode(maxDateTime), caseFieldDefinition);
        assertEquals(0, result03.size(), result03.toString());

        final List<ValidationResult> result04 =
            validator.validate(FIELD_ID, NODE_FACTORY.textNode(invalidMinDateTime), caseFieldDefinition);
        assertEquals(1, result04.size(), "Did not catch invalid min-date");

        final List<ValidationResult> result05 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode(invalidMaxDateTime),
                caseFieldDefinition);
        assertEquals(1, result05.size(), "Did not catch invalid max-date");
    }

    @Test
    void invalidFieldTypeRegEx() {
        final String validDateTime = "2001-12-10T00:00:00Z";
        final CaseFieldDefinition caseFieldDefinition = caseField().withRegExp("InvalidRegEx").build();

        final List<ValidationResult> result = validator.validate(FIELD_ID,
                                                                 NODE_FACTORY.textNode(validDateTime),
                caseFieldDefinition);
        assertEquals(1, result.size(), "RegEx validation failed");
        assertEquals("2001-12-10T00:00:00Z Field Type Regex Failed:InvalidRegEx", result.get(0).getErrorMessage());
        assertEquals(FIELD_ID, result.get(0).getFieldId());
    }

    @Test
    void invalidBaseTypeRegEx() {
        when(dateTimeFieldTypeDefinition.getRegularExpression()).thenReturn("InvalidRegEx");
        BaseType.register(new BaseType(dateTimeFieldTypeDefinition));
        final List<ValidationResult> result = validator.validate(FIELD_ID,
                                                                 NODE_FACTORY.textNode("2001-12-10T00:00:00"),
                caseFieldDefinition);
        assertEquals(1, result.size(), "RegEx validation failed");
        assertEquals("2001-12-10T00:00:00 Date Time Type Regex Failed:InvalidRegEx", result.get(0).getErrorMessage());
        assertEquals(FIELD_ID, result.get(0).getFieldId());
    }


    @Test
    void validRegEx() {
        final String validDateTime = "2001-12-10T00:00:00";
        final String limitedRegex = "^\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2}$";
        final CaseFieldDefinition caseFieldDefinition = caseField().withRegExp(limitedRegex).build();

        final List<ValidationResult> result = validator.validate(FIELD_ID,
                                                                 NODE_FACTORY.textNode(validDateTime),
                caseFieldDefinition);
        assertEquals(0, result.size(), "RegEx validation failed");
    }

    @Test
    void shouldFail_whenValidatingBooleanNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.booleanNode(true), caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("Date or Time entered is not valid"));
    }

    @Test
    void shouldFail_whenDataValueIsBinary() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.binaryNode("Ngitb".getBytes()), caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("Date or Time entered is not valid"));
    }

    @Test
    void shouldFail_whenValidatingArrayNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.arrayNode(), caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("Date or Time entered is not valid"));
    }

    @Test
    void shouldPass_whenValidatingObjectNode() {
        final List<ValidationResult>
            result = validator.validate(FIELD_ID, NODE_FACTORY.objectNode(), caseFieldDefinition);
        assertThat(result, empty());
    }

    @Test
    void shouldFail_whenValidatingPojoNode() {
        final List<ValidationResult>
            result =
            validator.validate(FIELD_ID, NODE_FACTORY.pojoNode(true), caseFieldDefinition);
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getErrorMessage(), is("Date or Time entered is not valid"));
    }

    private CaseFieldDefinitionBuilder caseField() {
        return new CaseFieldDefinitionBuilder(FIELD_ID).withType(DateTimeValidator.TYPE_ID);
    }

    private BigDecimal datetime(final String datetimeString) {
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        try {
            return new BigDecimal(df.parse(datetimeString).getTime());
        } catch (ParseException e) {
            throw new AssertionError(e);
        }
    }
}
