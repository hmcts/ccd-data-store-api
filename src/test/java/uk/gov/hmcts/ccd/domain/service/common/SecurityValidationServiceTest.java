package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.service.getcase.AuthorisedGetCaseOperation;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PRIVATE;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.RESTRICTED;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CallbackResponseBuilder.aCallbackResponse;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DataClassificationBuilder.aClassificationBuilder;

class SecurityValidationServiceTest {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private SecurityValidationService securityValidationService;

    @Mock
    private AuthorisedGetCaseOperation authorisedGetCaseOperation;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        securityValidationService = new SecurityValidationService(authorisedGetCaseOperation);
    }

    @Nested
    @DisplayName("Validate security classification case")
    class ValidateDataClassificationCase {

        @Test
        @DisplayName("should decrease security if valid classification level for case")
        void shouldDecreaseSecurityIfValidClassificationLevelForCase() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PRIVATE)
                .withDataClassification(
                    aClassificationBuilder()
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .buildAsMap())
                .build();

            securityValidationService.updateSecurityClassificationIfValid(callbackResponse, caseDetails);

            assertThat(caseDetails.getSecurityClassification(), is(PUBLIC));
        }

        @Test
        @DisplayName("should increase security if valid classification level for case")
        void shouldIncreaseSecurityIfValidClassificationLevelForCase() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PRIVATE)
                .withDataClassification(
                    aClassificationBuilder()
                        .buildAsMap())
                .build();

            securityValidationService.updateSecurityClassificationIfValid(callbackResponse, caseDetails);

            assertThat(caseDetails.getSecurityClassification(), is(PRIVATE));
        }

        @Test
        @DisplayName("should do nothing if case security classification is null")
        void shouldDoNothingIfCaseSecurityClassificationIsNull() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(null)
                .build();

            securityValidationService.updateSecurityClassificationIfValid(callbackResponse, caseDetails);

            assertThat(caseDetails.getSecurityClassification(), is(PUBLIC));
        }

        @Test
        @DisplayName("should decrease security to private if valid classification level for case")
        void shouldLogWarningAndThrowExceptionForInvalidClassifications() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(RESTRICTED)
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PRIVATE)
                .build();

            securityValidationService.updateSecurityClassificationIfValid(callbackResponse, caseDetails);

            assertThat(caseDetails.getSecurityClassification(), is(PRIVATE));
        }
    }

    @Nested
    @DisplayName("Validate data classification simple field")
    class ValidateDataClassificationSimpleField {

        @Test
        @DisplayName("should increase security if valid classification level for case and data nodes")
        void shouldIncreaseSecurityIfValidClassificationLevelForCaseAndDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .buildAsMap())
                .build();
            final Map<String, JsonNode> defaultDataClassification = caseDetails.getDataClassification();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("RESTRICTED"))
                        .buildAsMap())
                .build();

            securityValidationService.setDataClassificationFromCallbackIfValid(callbackResponse, caseDetails,
                defaultDataClassification);

            assertAll(
                () -> assertThat(caseDetails.getDataClassification().size(), is(1)),
                () -> assertThat(caseDetails.getDataClassification(), hasEntry("field1",
                    getTextNode("RESTRICTED")))
            );
        }

        @Test
        @DisplayName("should fail if missing classification for data nodes")
        void shouldFailIfMissingClassificationForDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PUBLIC"))
                        .withData("missingField2", getTextNode("RESTRICTED"))
                        .buildAsMap())
                .build();

            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if extra classification for data nodes")
        void shouldFailIfExtraClassificationForDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PUBLIC"))
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .withData("extraField2", getTextNode("RESTRICTED"))
                        .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName
            ("should fail if neither deduced nor default dataClassification match callbackResponse dataClassification")
        void shouldFailIfNeitherDeducedNorDefaultDataClassificationMatchesCallbackResponseDataClassification() {
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .withData("field2", getTextNode("PRIVATE"))
                        .buildAsMap())
                .build();

            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withReference(1111L)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .buildAsMap())
                .build();

            final CaseDetails defaultCaseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .buildAsMap())
                .build();
            when(authorisedGetCaseOperation.execute(Mockito.anyString())).thenReturn(Optional.of(defaultCaseDetails));

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should PASS if default data classification matches callbackResponse data classification")
        void shouldPassIfDefaultDataClassificationMatchesCallbackResponseDataClassification() {
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .withData("field2", getTextNode("PRIVATE"))
                        .buildAsMap())
                .build();

            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withReference(2222L)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .buildAsMap())
                .build();

            final CaseDetails defaultCaseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .withData("field2", getTextNode("PRIVATE"))
                        .buildAsMap())
                .build();
            when(authorisedGetCaseOperation.execute(Mockito.anyString())).thenReturn(Optional.of(defaultCaseDetails));

            securityValidationService.setDataClassificationFromCallbackIfValid(callbackResponse, caseDetails,
                caseDetails.getDataClassification());
            assertAll(
                () -> assertThat(caseDetails.getDataClassification().size(), is(2)),
                () -> assertThat(caseDetails.getDataClassification(), hasEntry("field1",
                    getTextNode("PRIVATE"))),
                () -> assertThat(caseDetails.getDataClassification(), hasEntry("field2",
                    getTextNode("PRIVATE")))
            );
        }

        @Test
        @DisplayName
            ("should fail if neither deduced nor default dataClassification match callbackResponse dataClassification")
        void shouldFailIfNeitherDeducedNorDefaultDataClassificationMatchesCallbackResponseDataClassification2() {
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .withData("field2", getTextNode("PRIVATE"))
                        .buildAsMap())
                .build();

            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withReference(3333L)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .buildAsMap())
                .build();

            final CaseDetails defaultCaseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .withData("field2", getTextNode("PRIVATE"))
                        .withData("field3", getTextNode("PRIVATE"))
                        .buildAsMap())
                .build();
            when(authorisedGetCaseOperation.execute(Mockito.anyString())).thenReturn(Optional.of(defaultCaseDetails));

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if invalid classification value for callback data nodes")
        void shouldFailIfInvalidClassificationValueForCallbackDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PLOP"))
                        .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if invalid classification level for data nodes")
        void shouldFailIfInvalidClassificationLevelForDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PUBLIC"))
                        .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }
    }

    @Nested
    @DisplayName("Validate data classification complex field")
    class ValidateDataClassificationComplexField {

        @Test
        @DisplayName("should increase security if valid classification level for case and data nodes")
        void shouldIncreaseSecurityIfValidClassificationLevelForCaseAndDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("PRIVATE"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();
            final Map<String, JsonNode> defaultDataClassification = caseDetails.getDataClassification();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("RESTRICTED"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("RESTRICTED"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();

            securityValidationService.setDataClassificationFromCallbackIfValid(callbackResponse, caseDetails,
                defaultDataClassification);

            assertAll(
                () -> assertThat(caseDetails.getDataClassification().size(), is(1)),
                () -> assertThat(caseDetails.getDataClassification().get("complexField1").size(), is(2)),
                () -> assertThat(caseDetails.getDataClassification().get("complexField1").get("classification"),
                    is(getTextNode("RESTRICTED"))),
                () -> assertThat(caseDetails.getDataClassification().get("complexField1").get("value").size(),
                    is(1)),
                () -> assertThat(caseDetails.getDataClassification().get("complexField1").get("value")
                    .get("field2"), is(getTextNode("RESTRICTED")))
            );
        }


        @Test
        @DisplayName("should fail if missing data classification for complex node")
        void shouldFailIfMissingDataClassificationForComplexNode() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("PUBLIC"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("RESTRICTED"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if missing callback classification for complex node")
        void shouldFailIfMissingCallbackClassificationForComplexNode() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("PUBLIC"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("RESTRICTED"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if missing callback value for complex node")
        void shouldFailIfMissingCallbackValueForComplexNode() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("PUBLIC"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .buildAsNode())
                        .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if missing data value for complex node")
        void shouldFailIfMissingDataValueForComplexNode() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .buildAsNode())
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("PUBLIC"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if missing classification for data nodes")
        void shouldFailIfMissingClassificationForDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("PUBLIC"))
                                                    .withData("missingField3", getTextNode("RESTRICTED"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("RESTRICTED"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if extra classification for data nodes")
        void shouldFailIfExtraClassificationForDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("PUBLIC"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("PRIVATE"))
                                                    .withData("extraField3", getTextNode("RESTRICTED"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if invalid classification value for callback data")
        void shouldFailIfInvalidClassificationValueForCallbackData() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("PRIVATE"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PLOP"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("PRIVATE"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if invalid classification value for callback data nodes")
        void shouldFailIfInvalidClassificationValueForCallbackDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("PRIVATE"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("PLOP"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }


        @Test
        @DisplayName("should fail if invalid classification level for data")
        void shouldFailIfInvalidClassificationLevelForData() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("PRIVATE"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PUBLIC"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("PRIVATE"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if invalid classification level for data nodes")
        void shouldFailIfInvalidClassificationLevelForDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("PRIVATE"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("complexField1",
                                  aClassificationBuilder()
                                      .withData("classification", getTextNode("PRIVATE"))
                                      .withData("value",
                                                aClassificationBuilder()
                                                    .withData("field2", getTextNode("PUBLIC"))
                                                    .buildAsNode())
                                      .buildAsNode())
                        .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }
    }

    @Nested
    @DisplayName("Validate data classification collection field")
    class ValidateDataClassificationCollectionField {

        @Test
        @DisplayName("should increase security if valid classification level for case and data nodes")
        void shouldIncreaseSecurityIfValidClassificationLevelForCaseAndDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(
                                                              aClassificationBuilder()
                                                                  .withData("value",
                                                                            aClassificationBuilder()
                                                                                .withData("field2",
                                                                                        getTextNode("PRIVATE"))
                                                                                .buildAsNode())
                                                                  .withData("id", getTextNode("someId1"))
                                                                  .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();
            final Map<String, JsonNode> defaultDataClassification = caseDetails.getDataClassification();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("RESTRICTED"))
                                                .withData("value",
                                                          newArrayList(
                                                              aClassificationBuilder()
                                                                  .withData("value",
                                                                            aClassificationBuilder()
                                                                                .withData("field2",
                                                                                    getTextNode("RESTRICTED"))
                                                                                .buildAsNode())
                                                                  .withData("id", getTextNode("someId1"))
                                                                  .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            securityValidationService.setDataClassificationFromCallbackIfValid(callbackResponse, caseDetails,
                defaultDataClassification);

            assertAll(
                () -> assertThat(caseDetails.getDataClassification().size(), is(1)),
                () -> assertThat(caseDetails.getDataClassification().get("collectionField1").size(), is(2)),
                () -> assertThat(caseDetails.getDataClassification().get("collectionField1")
                    .get("classification"), is(getTextNode("RESTRICTED"))),
                () -> assertThat(caseDetails.getDataClassification().get("collectionField1").get("value")
                    .size(), is(1)),
                () -> assertThat(caseDetails.getDataClassification().get("collectionField1").get("value")
                    .get(0).get("id"), is(getTextNode("someId1"))),
                () -> assertThat(caseDetails.getDataClassification().get("collectionField1").get("value")
                        .get(0).get("value").get("field2"),
                                        is(getTextNode("RESTRICTED")))
            );
        }

        @Test
        @DisplayName("should fail if missing collection item for data nodes")
        void shouldFailIfMissingCollectionItemForDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(
                                                              aClassificationBuilder()
                                                                  .withData("value",
                                                                            aClassificationBuilder()
                                                                                .withData("field2",
                                                                                    getTextNode("PRIVATE"))
                                                                                .buildAsNode())
                                                                  .withData("id", getTextNode("someId1"))
                                                                  .buildAsMap(),
                                                              aClassificationBuilder()
                                                                  .withData("value",
                                                                            aClassificationBuilder()
                                                                                .withData("field3",
                                                                                    getTextNode("PRIVATE"))
                                                                                .buildAsNode())
                                                                  .withData("id", getTextNode("someId2"))
                                                                  .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("RESTRICTED"))
                                                .withData("value",
                                                          newArrayList(
                                                              aClassificationBuilder()
                                                                  .withData("value",
                                                                            aClassificationBuilder()
                                                                                .withData("field2",
                                                                                    getTextNode("PRIVATE"))
                                                                                .buildAsNode())
                                                                  .withData("id", getTextNode("someId1"))
                                                                  .buildAsMap(),
                                                              aClassificationBuilder()
                                                                  .withData("id", getTextNode("someId2"))
                                                                  .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if missing classification for data nodes")
        void shouldFailIfMissingClassificationForDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(
                                                              aClassificationBuilder()
                                                                  .withData("value",
                                                                            aClassificationBuilder()
                                                                                .withData("field2",
                                                                                    getTextNode("PRIVATE"))
                                                                                .withData("missingField3",
                                                                                    getTextNode("RESTRICTED"))
                                                                                .buildAsNode())
                                                                  .withData("id", getTextNode("someId1"))
                                                                  .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(
                                                              aClassificationBuilder()
                                                                  .withData("value",
                                                                            aClassificationBuilder()
                                                                                .withData("field2",
                                                                                    getTextNode("RESTRICTED"))
                                                                                .buildAsNode())
                                                                  .withData("id", getTextNode("someId1"))
                                                                  .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if extra classification for data nodes")
        void shouldFailIfExtraClassificationForDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2",
                                                                                   getTextNode("PRIVATE"))
                                                                               .buildAsNode())
                                                                           .withData("id", getTextNode("someId1"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2",
                                                                                   getTextNode("PRIVATE"))
                                                                               .withData("extraField3",
                                                                                   getTextNode("RESTRICTED"))
                                                                               .buildAsNode())
                                                                           .withData("id",
                                                                               getTextNode("someId1"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if extra collection item")
        void shouldFailIfExtraCollectionItem() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2",
                                                                                   getTextNode("PRIVATE"))
                                                                               .withData("field3",
                                                                                   getTextNode("RESTRICTED"))
                                                                               .buildAsNode())
                                                                           .withData("id",
                                                                               getTextNode("someId1"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2",
                                                                                   getTextNode("PRIVATE"))
                                                                               .withData("field3",
                                                                                   getTextNode("RESTRICTED"))
                                                                               .buildAsNode())
                                                                           .withData("id",
                                                                               getTextNode("someId1"))
                                                                           .buildAsMap(),
                                                                       aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("extraField1",
                                                                                   getTextNode("PRIVATE"))
                                                                               .withData("extraField2",
                                                                                   getTextNode("RESTRICTED"))
                                                                               .buildAsNode())
                                                                           .withData("id",
                                                                               getTextNode("extraItemId"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if invalid classification value for callback data")
        void shouldFailIfInvalidClassificationValueForCaseDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2",
                                                                                   getTextNode("PRIVATE"))
                                                                               .buildAsNode())
                                                                           .withData("id",
                                                                               getTextNode("someId1"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PLOP"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2",
                                                                                   getTextNode("PRIVATE"))
                                                                               .buildAsNode())
                                                                           .withData("id",
                                                                               getTextNode("someId1"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if invalid classification value for callback data nodes")
        void shouldFailIfInvalidClassificationValueForCallbackDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2",
                                                                                   getTextNode("PRIVATE"))
                                                                               .buildAsNode())
                                                                           .withData("id",
                                                                               getTextNode("someId1"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2",
                                                                                   getTextNode("PLOP"))
                                                                               .buildAsNode())
                                                                           .withData("id",
                                                                               getTextNode("someId1"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if invalid classification level for data nodes")
        void shouldFailIfInvalidClassificationLevelForDataNodes() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2",
                                                                                   getTextNode("PRIVATE"))
                                                                               .buildAsNode())
                                                                           .withData("id",
                                                                               getTextNode("someId1"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2",
                                                                                   getTextNode("PUBLIC"))
                                                                               .buildAsNode())
                                                                           .withData("id",
                                                                               getTextNode("someId1"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if invalid classification level for data")
        void shouldFailIfInvalidClassificationLevelForData() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2",
                                                                                   getTextNode("PRIVATE"))
                                                                               .buildAsNode())
                                                                           .withData("id",
                                                                               getTextNode("someId1"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PUBLIC"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2",
                                                                                   getTextNode("PRIVATE"))
                                                                               .buildAsNode())
                                                                           .withData("id",
                                                                               getTextNode("someId1"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            assertThrowsSecurityValidationDueToClassificationException(caseDetails, callbackResponse);
        }
    }

    private void assertThrowsSecurityValidationDueToClassificationException(CaseDetails caseDetails,
                                                                            CallbackResponse callbackResponse) {
        final Map<String, JsonNode> defaultDataClassification = caseDetails.getDataClassification();
        ValidationException validationException = assertThrows(ValidationException.class,
            () -> securityValidationService.setDataClassificationFromCallbackIfValid(callbackResponse,
                caseDetails,
                defaultDataClassification));
        assertEquals("The event cannot be complete due to a callback returned data validation error (c)",
            validationException.getMessage());
    }

    private JsonNode getTextNode(String value) {
        return JSON_NODE_FACTORY.textNode(value);
    }
}
