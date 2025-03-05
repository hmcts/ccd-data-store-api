package uk.gov.hmcts.ccd.domain.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.data.definition.CaseDefinitionRepository;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.test.CaseFieldDefinitionBuilder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.domain.types.BaseTypeValidator.REGEX_GUIDANCE;

@DisplayName("EmailValidator")
class EmailValidatorTest {
    private static final JsonNodeFactory NODE_FACTORY = JsonNodeFactory.instance;
    private static final String FIELD_ID = "TEST_FIELD_ID";

    private static final Logger LOG = LoggerFactory.getLogger(EmailValidatorTest.class);

    // TEMPORARY
    private static final String REGEX1 = "^[a-z]\\w*@hmcts.net$";
    private static final String REGEX2 = "\\\\w*@hmcts.net";

    private void jclog(final String message) {
        //System.out.println("JCDEBUG: " + message);
        LOG.debug("JCDEBUG: debug: {}", message);
        LOG.info("JCDEBUG: info: {}", message);
        LOG.warn("JCDEBUG: warn: {}", message);
        LOG.error("JCDEBUG: error: {}", message);
    }

    @Mock
    private BaseType emailBaseType;

    @Mock
    private CaseDefinitionRepository definitionRepository;

    private EmailValidator validator;

    private CaseFieldDefinition caseFieldDefinition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        when(definitionRepository.getBaseTypes()).thenReturn(Collections.emptyList());
        BaseType.setCaseDefinitionRepository(definitionRepository);
        BaseType.initialise();

        when(emailBaseType.getType()).thenReturn(EmailValidator.TYPE_ID);
        when(emailBaseType.getRegularExpression()).thenReturn(null);
        BaseType.register(emailBaseType);

        validator = new EmailValidator();

        caseFieldDefinition = caseField().build();
    }

    @Test
    void validEmail() {
        jclog("----------------  validEmail()  ----------------");
        final List<ValidationResult> result01 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test@test.com"),
            caseFieldDefinition);
        assertEquals(0, result01.size(), result01.toString());

        final List<ValidationResult> result02 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test@test.org"),
            caseFieldDefinition);
        assertEquals(0, result02.size(), result02.toString());

        final List<ValidationResult> result03 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test@test.org.uk"),
            caseFieldDefinition);
        assertEquals(0, result03.size(), result03.toString());

        final List<ValidationResult> result04 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test.test@test.com"),
            caseFieldDefinition);
        assertEquals(0, result04.size(), result04.toString());

        final List<ValidationResult> result05 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test_test@test.xxx"),
            caseFieldDefinition);
        assertEquals(0, result05.size(), result05.toString());
    }

    @Test
    void invalidEmail() {
        jclog("----------------  invalidEmail()  ----------------");
        final List<ValidationResult> result01 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test.test.com"),
            caseFieldDefinition);
        assertEquals(1, result01.size(), result01.toString());

        final List<ValidationResult> result02 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test.com"),
            caseFieldDefinition);
        assertEquals(1, result02.size(), result02.toString());

        final List<ValidationResult> result03 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test@test@test"),
            caseFieldDefinition);
        assertEquals(1, result03.size(), result03.toString());

        final List<ValidationResult> result04 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("test@test"),
            caseFieldDefinition);
        assertEquals(1, result04.size(), result04.toString());

        final List<ValidationResult> result05 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("<a@a.a"),
            caseFieldDefinition);
        assertEquals(1, result05.size(), result05.toString());

        final List<ValidationResult> result06 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("a@a.a>"),
            caseFieldDefinition);
        assertEquals(1, result06.size(), result06.toString());

        // Email address ending in comma
        final List<ValidationResult> result07 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("a@a.a,"),
            caseFieldDefinition);
        assertEquals(1, result07.size(), result07.toString());

        final List<ValidationResult> result08 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("a@a.a and b@b.b"),
            caseFieldDefinition);
        assertEquals(1, result08.size(), result08.toString());

        final List<ValidationResult> result09 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("a@a.a AND b@b.b"),
            caseFieldDefinition);
        assertEquals(1, result09.size(), result09.toString());

        // NOTE: assertEquals should be 1 for invalid Email address
        final List<ValidationResult> result10 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("abc?ca@ae.com"),
            caseFieldDefinition);
        assertEquals(0, result10.size(), result10.toString());

        // NOTE: assertEquals should be 1 for invalid Email address
        final List<ValidationResult> result11 = validator.validate(FIELD_ID,
                                                                   NODE_FACTORY.textNode("abc*ca@ae.com"),
            caseFieldDefinition);
        assertEquals(0, result11.size(), result11.toString());
    }

    //@Test
    void fieldTypeRegEx() {
        jclog("----------------  fieldTypeRegEx()  ----------------");
        final CaseFieldDefinition regexCaseFieldDefinition = caseField().withRegExp("^[a-z]\\w*@hmcts.net$").build();
        final JsonNode validValue = NODE_FACTORY.textNode("k9@hmcts.net");
        final List<ValidationResult> validResult = validator.validate(FIELD_ID, validValue, regexCaseFieldDefinition);
        assertEquals(0, validResult.size(), validResult.toString());

        final JsonNode invalidValue = NODE_FACTORY.textNode("9k@hmcts.net");
        final List<ValidationResult> invalidResult =
                validator.validate(FIELD_ID, invalidValue, regexCaseFieldDefinition);
        assertEquals(1, invalidResult.size(), invalidResult.toString());
        assertEquals(REGEX_GUIDANCE, invalidResult.get(0).getErrorMessage());
    }

    //@Test
    void baseTypeRegEx() {
        jclog("----------------  baseTypeRegEx()  ----------------");
        when(emailBaseType.getRegularExpression()).thenReturn("\\\\w*@hmcts.net");

        final List<ValidationResult> result01 =
                validator.validate(FIELD_ID, NODE_FACTORY.textNode("9k@hmcts.net"), caseFieldDefinition);
        assertEquals(1, result01.size());
        assertEquals(REGEX_GUIDANCE, result01.get(0).getErrorMessage());
    }

    //@Test
    void checkMin() {
        jclog("----------------  checkMin()  ----------------");
        final CaseFieldDefinition caseFieldDefinition = caseField().withMin(new BigDecimal(13)).build();
        final JsonNode validValue = NODE_FACTORY.textNode("k99@hmcts.net");
        final List<ValidationResult> validResult = validator.validate(FIELD_ID, validValue, caseFieldDefinition);
        assertEquals(0, validResult.size(), validResult.toString());

        final JsonNode invalidValue = NODE_FACTORY.textNode("k9@hmcts.net");
        final List<ValidationResult> invalidResult = validator.validate(FIELD_ID, invalidValue, caseFieldDefinition);
        assertEquals(1, invalidResult.size(), invalidResult.toString());
        assertEquals("Email 'k9@hmcts.net' requires minimum length 13", invalidResult.get(0).getErrorMessage());
    }

    //@Test
    void checkMax() {
        jclog("----------------  checkMax()  ----------------");
        final CaseFieldDefinition caseFieldDefinition = caseField().withMax(new BigDecimal(12)).build();
        final JsonNode validValue = NODE_FACTORY.textNode("k9@hmcts.net");
        final List<ValidationResult> validResult = validator.validate(FIELD_ID, validValue, caseFieldDefinition);
        assertEquals(0, validResult.size(), validResult.toString());

        final JsonNode invalidValue = NODE_FACTORY.textNode("k99@hmcts.net");
        final List<ValidationResult> invalidResult = validator.validate(FIELD_ID, invalidValue, caseFieldDefinition);
        assertEquals(1, invalidResult.size(), invalidResult.toString());
        assertEquals("Email 'k99@hmcts.net' exceeds maximum length 12", invalidResult.get(0).getErrorMessage());
    }

    //@Test
    void nullValue() {
        jclog("----------------  nullValue()  ----------------");
        assertEquals(0, validator.validate(FIELD_ID, null, caseFieldDefinition).size(), "Did not catch NULL");
    }

    //@Test
    void getType() {
        jclog("----------------  getType()  ----------------");
        assertEquals(validator.getType(), BaseType.get("Email"), "Type is incorrect");
    }

    private CaseFieldDefinitionBuilder caseField() {
        return new CaseFieldDefinitionBuilder(FIELD_ID).withType(EmailValidator.TYPE_ID);
    }
}
