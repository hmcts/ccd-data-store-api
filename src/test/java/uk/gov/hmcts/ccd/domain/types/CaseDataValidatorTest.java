package uk.gov.hmcts.ccd.domain.types;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.ccd.TestFixtures;
import uk.gov.hmcts.ccd.WireMockBaseTest;
import uk.gov.hmcts.ccd.domain.model.definition.CaseFieldDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.TestFixtures.caseDataFromJsonString;

// too many legacy OperatorWrap occurrences on JSON strings so suppress until move to Java12+
@SuppressWarnings("checkstyle:OperatorWrap")
public class CaseDataValidatorTest extends WireMockBaseTest {
    private static final String CASE_FIELD_JSON = "tests/CaseDataValidator_CaseField.json";
    private static final String CASE_FIELD_DYNAMIC_JSON = "tests/CaseDataValidator_DynamicLists.json";

    @Inject
    private CaseDataValidator caseDataValidator;

    private static List<CaseFieldDefinition> caseFields;
    private static List<CaseFieldDefinition> dynamicCaseFields;

    @Mock
    private TextCaseReferenceCaseLinkValidator textCaseReferenceCaseLinkValidator;

    @BeforeAll
    public static void setUpClass() throws IOException {
        caseFields = TestFixtures.getCaseFieldsFromJson(CASE_FIELD_JSON);
        dynamicCaseFields = TestFixtures.getCaseFieldsFromJson(CASE_FIELD_DYNAMIC_JSON);
    }

    @Test
    public void validValueComplex() throws Exception {

        final String DATA =
            """
                {
                  "Person" : {
                    "Name" : "Name",
                    "Address": {
                      "Line1": "Address Line 1",
                      "Line2": "Address Line 2"
                    }
                  }
                }""";

        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> result = caseDataValidator.validate(validationContext);
        assertThat(result).hasSize(0);
    }

    private ValidationContext getValidationContext(Map<String, JsonNode> values) {
        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(caseFields);
        return new ValidationContext(caseTypeDefinition, values);
    }

    private ValidationContext getValidationContextDynamicFields(Map<String, JsonNode> values) {
        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(dynamicCaseFields);
        return new ValidationContext(caseTypeDefinition, values);
    }


    @Test
    public void unrecognisedValueComplex() throws Exception {

        final String DATA =
            """
                {
                  "Person" : {
                    "Name" : "Name",
                    "Address": {
                      "Line1": "Address Line 1",
                      "": "Address Line 2",
                      "Line4": "Address Line 2"
                    }
                  }
                }""";
        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> result = caseDataValidator.validate(validationContext);
        assertThat(result).hasSize(2);
    }

    @Test
    public void shouldPrefixComplexChildrenIDWithPath() throws Exception {

        final String DATA =
            """
                {
                  "Person" : {
                    "Address": {
                      "Line4": "Address Line 2"
                    }
                  }
                }""";
        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> result = caseDataValidator.validate(validationContext);
        assertThat(result).hasSize(1);

        final ValidationResult error = result.get(0);
        assertThat(error.getFieldId()).isEqualTo("Person.Address.Line4");
    }

    @Test
    public void shouldPrefixComplexChildrenIDWithPath_oneLevel() throws Exception {

        final String DATA =
            """
                {
                  "Person" : {
                    "FirstName": "Invalid field",\
                    "Address": {
                      "Line1": "Address Line 1"
                    }
                  }
                }""";
        final Map<String, JsonNode> values = mapper.readValue(DATA, new TypeReference<HashMap<String, JsonNode>>() {
        });
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> result = caseDataValidator.validate(validationContext);
        assertThat(result).hasSize(1);

        final ValidationResult error = result.get(0);
        assertThat(error.getFieldId()).isEqualTo("Person.FirstName");
    }

    @Test
    public void shouldNotPrefixRootFields() throws Exception {

        final String DATA =
            """
                {
                  "PersonGender" : ""
                }""";
        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> result = caseDataValidator.validate(validationContext);
        assertThat(result).hasSize(1);

        final ValidationResult error = result.get(0);
        assertThat(error.getFieldId()).isEqualTo("PersonGender");
    }

    @Test
    public void unknownType() throws Exception {
        final String data =
            """
                {
                  "Person" : {
                    "Name" : "Name",
                    "Address": {
                      "Line1": "Address Line 1",
                      "": "Address Line 2",
                      "Line4": "Address Line 2"
                    }
                  }
                }""";
        final Map<String, JsonNode> values = caseDataFromJsonString(data);
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> result = caseDataValidator.validate(validationContext);
        assertThat(result).hasSize(2);
    }

    @Test
    public void validValueCollection() throws Exception {

        final String DATA =
            """
                {
                  "OtherAddresses" : [
                    {
                      "value":\s
                        {
                          "Line1": "Address Line 1",
                          "Line2": "Address Line 2"
                        }
                    }
                  ]
                }""";

        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> result = caseDataValidator.validate(validationContext);
        assertThat(result).hasSize(0);
    }

    @Test
    public void validDynamicListInCollection() throws Exception {
        final String DATA = """
            {
                    "TextAreaField": "textAreaField1",
                    "TextField": "textField1",
                    "EmailField": "test@hmcts.net",
                    "DynamicListsComplexField": {
                        "DynamicRadioListComplex": {
                            "value": {
                                "code": "JUDGESMITH",
                                "label": "Judge Smith"
                            },
                            "list_items": [{
                                "code": "JUDGEJUDY",
                                "label": "Judge Judy"
                            }, {
                                "code": "JUDGERINDER",
                                "label": "Judge Rinder"
                            }, {
                                "code": "JUDGESMITH",
                                "label": "Judge Smith"
                            }, {
                                "code": "JUDGEDREDD",
                                "label": "Judge Dredd"
                            }]
                        },
                        "DynamicMultiSelectComplex": {
                            "value": [{
                                "code": "MONDAYFIRSTOFMAY",
                                "label": "Monday, May 1st"
                            }, {

                                "code": "THURSDAYFOURTHOFMAY",
                                "label": "Thursday, May 4th"
                            }],
                            "list_items": [{
                                "code": "MONDAYFIRSTOFMAY",
                                "label": "Monday, May 1st"
                            }, {
                                "code": "TUESDAYSECONDOFMAY",
                                "label": "Tuesday, May 2nd"
                            }, {
                                "code": "WEDNESDAYTHIRDOFMAY",
                                "label": "Wednesday, May 3rd"
                            }, {
                                "code": "THURSDAYFOURTHOFMAY",
                                "label": "Thursday, May 4th"
                            }]
                        }


                    },
                    "CollectionDynamicMultiSelectList": [
                        {
                            "id": "MultiSelect1",
                            "value": {
                                "value": [{
                                    "code": "MONDAYFIRSTOFMAY",
                                    "label": "Monday, May 1st"
                                }, {

                                    "code": "THURSDAYFOURTHOFMAY",
                                    "label": "Thursday, May 4th"
                                }],
                                "list_items": [{
                                    "code": "MONDAYFIRSTOFMAY",
                                    "label": "Monday, May 1st"
                                }, {
                                    "code": "TUESDAYSECONDOFMAY",
                                    "label": "Tuesday, May 2nd"
                                }, {
                                    "code": "WEDNESDAYTHIRDOFMAY",
                                    "label": "Wednesday, May 3rd"
                                }, {
                                    "code": "THURSDAYFOURTHOFMAY",
                                    "label": "Thursday, May 4th"
                                }]
                            }
                        },
                        {
                            "id": "MultiSelect2",
                            "value": {
                                "value": [{
                                    "code": "TUESDAYSECONDOFMAY",
                                    "label": "Tuesday, May 2nd"
                                }, {

                                    "code": "WEDNESDAYTHIRDOFMAY",
                                    "label": "Wednesday, May 3rd"
                                }],
                                "list_items": [{
                                    "code": "MONDAYFIRSTOFMAY",
                                    "label": "Monday, May 1st"
                                }, {
                                    "code": "TUESDAYSECONDOFMAY",
                                    "label": "Tuesday, May 2nd"
                                }, {
                                    "code": "WEDNESDAYTHIRDOFMAY",
                                    "label": "Wednesday, May 3rd"
                                }, {
                                    "code": "THURSDAYFOURTHOFMAY",
                                    "label": "Thursday, May 4th"
                                }]
                            }
                        }

                    ],
                    "CollectionDynamicRadioList": [
                        {
                            "id": "RadioList1",
                            "value": {
                                "value": {
                                    "code": "JUDGESMITH",
                                    "label": "Judge Smith"
                                },
                                "list_items": [{
                                    "code": "JUDGEJUDY",
                                    "label": "Judge Judy"
                                }, {
                                    "code": "JUDGERINDER",
                                    "label": "Judge Rinder"
                                }, {
                                    "code": "JUDGESMITH",
                                    "label": "Judge Smith"
                                }, {
                                    "code": "JUDGEDREDD",
                                    "label": "Judge Dredd"
                                }]
                            }
                        },
                        {
                            "id": "RadioList2",
                            "value": {
                                "value": {
                                    "code": "JUDGESMITH",
                                    "label": "Judge Smith"
                                },
                                "list_items": [{
                                    "code": "JUDGEJUDY",
                                    "label": "Judge Judy"
                                }, {
                                    "code": "JUDGERINDER",
                                    "label": "Judge Rinder"
                                }, {
                                    "code": "JUDGESMITH",
                                    "label": "Judge Smith"
                                }, {
                                    "code": "JUDGEDREDD",
                                    "label": "Judge Dredd"
                                }]
                            }
                        }
                    ],
                    "DynamicRadioList": {
                        "value": {
                            "code": "JUDGESMITH",
                            "label": "Judge Smith"
                        },
                        "list_items": [{
                            "code": "JUDGEJUDY",
                            "label": "Judge Judy"
                        }, {
                            "code": "JUDGERINDER",
                            "label": "Judge Rinder"
                        }, {
                            "code": "JUDGESMITH",
                            "label": "Judge Smith"
                        }, {
                            "code": "JUDGEDREDD",
                            "label": "Judge Dredd"
                        }]
                    },
                    "DynamicMultiSelectList": {
                        "value": [{
                            "code": "MONDAYFIRSTOFMAY",
                            "label": "Monday, May 1st"
                        }, {

                            "code": "THURSDAYFOURTHOFMAY",
                            "label": "Thursday, May 4th"
                        }],
                        "list_items": [{
                            "code": "MONDAYFIRSTOFMAY",
                            "label": "Monday, May 1st"
                        }, {
                            "code": "TUESDAYSECONDOFMAY",
                            "label": "Tuesday, May 2nd"
                        }, {
                            "code": "WEDNESDAYTHIRDOFMAY",
                            "label": "Wednesday, May 3rd"
                        }, {
                            "code": "THURSDAYFOURTHOFMAY",
                            "label": "Thursday, May 4th"
                        }]
                    }
                }""";

        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);
        final ValidationContext validationContext = getValidationContextDynamicFields(values);
        final List<ValidationResult> result = caseDataValidator.validate(validationContext);
        assertThat(result).hasSize(0);
    }

    @Test
    public void unknownFieldInCollectionOfComplex() throws Exception {

        final String DATA =
            """
                {
                  "OtherAddresses" : [
                    {
                      "value": \
                        {
                          "UnknownField": "Address Line 1",
                          "Line2": "Address Line 2"
                        }
                    }
                  ]
                }""";

        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);
        assertThat(results).hasSize(1);

        final ValidationResult result = results.getFirst();
        assertThat(result.getFieldId()).isEqualTo("OtherAddresses.0.UnknownField");
    }

    @Test
    public void exceedMaxItemsInCollection() throws Exception {

        final String DATA =
            """
                {
                  "OtherAddresses" : [
                    {
                      "value": \
                        {
                          "Line1": "Address 1 Line 1",
                          "Line2": "Address 1 Line 2"
                        }
                    },
                    {
                      "value": \
                        {
                          "Line1": "Address 2 Line 1",
                          "Line2": "Address 2 Line 2"
                        }
                    }
                  ]
                }""";

        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);
        assertThat(results).hasSize(1);

        final ValidationResult result = results.getFirst();
        assertThat(result.getFieldId()).isEqualTo("OtherAddresses");
    }

    @Test
    public void validCollectionOfSimpleFields() throws Exception {

        final String DATA =
            """
                {
                  "Initials" : [ { "value": "A" }, { "value": "B" } ]
                }""";

        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);
        assertThat(results).hasSize(0);
    }

    @Test
    public void invalidCollectionOfSimpleFields() throws Exception {

        final String DATA =
            """
                {
                  "Initials" : [ { "value": "TooLong" }, { "value": "B" } ]
                }""";

        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);
        assertThat(results).hasSize(1);

        final ValidationResult result = results.getFirst();
        assertThat(result.getFieldId()).isEqualTo("Initials.0");
    }

    @Test
    public void multipleInvalidItemsInCollection() throws Exception {

        final String DATA =
            """
                {
                  "Initials" : [ { "value": "TooLong" }, { "value": "TooLong" } ]
                }""";

        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);
        assertThat(results).hasSize(2);

        final ValidationResult result0 = results.get(0);
        assertThat(result0.getFieldId()).isEqualTo("Initials.0");

        final ValidationResult result1 = results.get(1);
        assertThat(result1.getFieldId()).isEqualTo("Initials.1");
    }

    @Test
    public void invalidCollection_valuesMissing() throws Exception {

        final String DATA =
            """
                {
                  "Initials" : [ { "x": "TooLong" }, { "y": "TooLong" } ]
                }""";

        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);
        assertThat(results).hasSize(2);

        final ValidationResult result0 = results.get(0);
        assertThat(result0.getFieldId()).isEqualTo("Initials.0");

        final ValidationResult result1 = results.get(1);
        assertThat(result1.getFieldId()).isEqualTo("Initials.1");
    }

    @Test
    public void invalidCollection_notObject() throws Exception {

        final String DATA =
            """
                {
                  "Initials" : [ "x", "y" ]
                }""";

        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);
        assertThat(results).hasSize(2);

        final ValidationResult result0 = results.get(0);
        assertThat(result0.getFieldId()).isEqualTo("Initials.0");

        final ValidationResult result1 = results.get(1);
        assertThat(result1.getFieldId()).isEqualTo("Initials.1");
    }

    @Test
    public void shouldFailForPredefinedType() throws Exception {
        final String DATA = """
            {
                    "CaseReference": "1596XXXXX1048-4059XXXOOOO"
                  }""";

        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);
        assertThat(results).hasSize(1);

        final ValidationResult result0 = results.get(0);
        assertThat(result0.getFieldId()).isEqualTo("CaseReference");
        assertThat(result0.getErrorMessage()).isEqualTo(
            "The data entered is not valid for this type of field, please delete and re-enter using only valid"
                + " data");
    }

    @Test
    public void shouldInvokeForPredefinedTypea() throws Exception {
        List<FieldValidator> fieldValidators = new ArrayList<>();
        fieldValidators.add(textCaseReferenceCaseLinkValidator);
        CaseDataValidator caseDataValidator = new CaseDataValidator(fieldValidators);

        when(textCaseReferenceCaseLinkValidator.getCustomTypeId()).thenReturn("TextCaseReference");

        final String DATA = """
            {
                    "CaseReference": "1596104840593131"
                  }""";

        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);

        assertThat(results.size()).isEqualTo(0);
        verify(textCaseReferenceCaseLinkValidator).validate(any(ValidationContext.class));
    }

    @Test
    public void shouldInvokeForPredefinedTypea2() throws Exception {
        List<FieldValidator> fieldValidators = new ArrayList<>();
        fieldValidators.add(textCaseReferenceCaseLinkValidator);
        fieldValidators.add(new CollectionValidator());
        CaseDataValidator caseDataValidator = new CaseDataValidator(fieldValidators);

        when(textCaseReferenceCaseLinkValidator.getCustomTypeId()).thenReturn("TextCaseReference");

        final String DATA =
            """
                {
                  "CaseLink" : [
                    {
                      "value": \
                        {
                          "CaseLink1": "1596104840593131",
                          "CaseLink2": "1596104840593131"
                        }
                    }\
                  ]
                }""";

        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);
        final ValidationContext validationContext = getValidationContext(values);
        final List<ValidationResult> results = caseDataValidator.validate(validationContext);

        assertThat(results).hasSize(0);
        verify(textCaseReferenceCaseLinkValidator, times(2))
            .validate(any(ValidationContext.class));
    }

    @Test
    public void textFieldWithMaxMin() throws Exception {
        final String caseFieldString =
            """
                [{
                  "id": "PersonFirstName",
                  "case_type_id": "TestAddressBookCase",
                  "label": "First name",
                  "field_type": {
                    "type": "Text",
                    "max": 100,
                    "min": 10
                  }
                }]""";
        final List<CaseFieldDefinition> caseFields =
            mapper.readValue(caseFieldString, TypeFactory.defaultInstance().constructCollectionType(List.class,
                CaseFieldDefinition.class));
        final String DATA = "{\"PersonFirstName\" : \"Test Name Test Name\"}";
        final Map<String, JsonNode> values = caseDataFromJsonString(DATA);

        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(caseFields);
        final ValidationContext validationContext = new ValidationContext(caseTypeDefinition, values);
        assertThat(caseDataValidator.validate(validationContext)).hasSize(0);
    }

    /**
     * This test is only meant to ensure that validators are invoked and not to test the TextValidator which has
     * it own test.
     */
    @Test
    public void textFieldWithInvalidMaxMin() throws Exception {
        final String caseFieldString =
            """
                [{
                  "id": "PersonFirstName",
                  "case_type_id": "TestAddressBookCase",
                  "label": "First name",
                  "field_type": {
                    "type": "Text",
                    "max": 10,
                    "min": 5
                  }
                }]""";
        final List<CaseFieldDefinition> caseFields =
            mapper.readValue(caseFieldString, TypeFactory.defaultInstance().constructCollectionType(List.class,
                CaseFieldDefinition.class));

        final String DATA = "{\"PersonFirstName\" : \"Test Name Test Name\"}";
        final Map<String, JsonNode> invalidMaxVal = caseDataFromJsonString(DATA);

        final CaseTypeDefinition caseTypeDefinition = new CaseTypeDefinition();
        caseTypeDefinition.setCaseFieldDefinitions(caseFields);
        final ValidationContext validationContext = new ValidationContext(caseTypeDefinition, invalidMaxVal);
        assertThat(caseDataValidator.validate(validationContext)).hasSize(1);

        final Map<String, JsonNode> invalidMinVal = caseDataFromJsonString("{\"PersonFirstName\" : \"Test\"}");
        final ValidationContext validationContext1 = new ValidationContext(caseTypeDefinition, invalidMinVal);
        assertThat(caseDataValidator.validate(validationContext1)).hasSize(1);
    }

    @Test
    public void fieldTypeWithNoValidator() throws Exception {
        final String DATA =
            """
                {
                  "NoValidatorForFieldType" : [
                    {
                      "value":\s
                        {
                          "Line1": "Address Line 1",
                          "Line2": "Address Line 2"
                        }
                    }
                  ]
                }""";

        final ValidationContext validationContext = getValidationContext(caseDataFromJsonString(DATA));

        Logger logger = (Logger) LoggerFactory.getLogger(CaseDataValidator.class);
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);

        assertThrows(RuntimeException.class, () -> caseDataValidator.validate(validationContext));

        List<ILoggingEvent> loggingEventList = listAppender.list;
        String expectedLogMessage = TestBuildersUtil.formatLogMessage(
                        "CaseField=NoValidatorForFieldType.0.Line1 of baseType=Label doesn't have write access");
        assertAll(
                () -> MatcherAssert.assertThat(loggingEventList.get(0).getLevel(), is(Level.ERROR)),
                () -> MatcherAssert.assertThat(loggingEventList.get(0).getFormattedMessage(), is(expectedLogMessage))
        );
        listAppender.stop();
        logger.detachAndStopAllAppenders();
    }
}
