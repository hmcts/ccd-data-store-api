package uk.gov.hmcts.ccd.domain.service.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.endpoint.exceptions.ValidationException;

import static com.google.common.collect.Lists.newArrayList;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PRIVATE;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CallbackResponseBuilder.aCallbackResponse;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.aCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DataClassificationBuilder.aClassificationBuilder;

class SecurityValidationServiceTest {

    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private SecurityValidationService securityValidationService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        securityValidationService = new SecurityValidationService();
    }

    @Nested
    @DisplayName("Validate data classification case")
    class ValidateDataClassificationCase {

        @Test
        @DisplayName("should fail if invalid classification level for case")
        void shouldFailIfInvalidClassificationLevelForCase() {
            final CaseDetails caseDetails = aCaseDetails()
                .withSecurityClassification(PRIVATE)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .buildAsMap())
                .build();

            assertThrowsSecurityValidationForCaseException(caseDetails, callbackResponse);
        }
    }

    @Nested
    @DisplayName("Validate data classification simple field")
    class ValidateDataClassificationSimpleField {

        @Test
        @DisplayName("should increase security if valid classification level for case and data nodes")
        void shouldIncreaseSecurityIfValidClassificationLevelForCaseAndDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("PRIVATE"))
                        .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PRIVATE)
                .withDataClassification(
                    aClassificationBuilder()
                        .withData("field1", getTextNode("RESTRICTED"))
                        .buildAsMap())
                .build();

            securityValidationService.setClassificationFromCallbackIfValid(callbackResponse, caseDetails);

            assertAll(
                () -> Assert.assertThat(caseDetails.getSecurityClassification(), Matchers.is(PRIVATE)),
                () -> Assert.assertThat(caseDetails.getDataClassification().size(), Matchers.is(1)),
                () -> Assert.assertThat(caseDetails.getDataClassification(), hasEntry("field1", getTextNode("RESTRICTED")))
            );
        }

        @Test
        @DisplayName("should fail if missing classification for data nodes")
        void shouldFailIfMissingClassificationForDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
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

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if extra classification for data nodes")
        void shouldFailIfExtraClassificationForDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
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

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if invalid classification value for callback data nodes")
        void shouldFailIfInvalidClassificationValueForCallbackDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
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

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if invalid classification level for data nodes")
        void shouldFailIfInvalidClassificationLevelForDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
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

            assertThrowsSecurityValidationForCaseDataException(caseDetails, callbackResponse, "field1");
        }
    }

    @Nested
    @DisplayName("Validate data classification complex field")
    class ValidateDataClassificationComplexField {

        @Test
        @DisplayName("should increase security if valid classification level for case and data nodes")
        void shouldIncreaseSecurityIfValidClassificationLevelForCaseAndDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
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
                .withSecurityClassification(PRIVATE)
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

            securityValidationService.setClassificationFromCallbackIfValid(callbackResponse, caseDetails);

            assertAll(
                () -> Assert.assertThat(caseDetails.getSecurityClassification(), Matchers.is(PRIVATE)),
                () -> Assert.assertThat(caseDetails.getDataClassification().size(), Matchers.is(1)),
                () -> Assert.assertThat(caseDetails.getDataClassification().get("complexField1").size(), Matchers.is(2)),
                () -> Assert.assertThat(caseDetails.getDataClassification().get("complexField1").get("classification"), Matchers.is(getTextNode("RESTRICTED"))),
                () -> Assert.assertThat(caseDetails.getDataClassification().get("complexField1").get("value").size(), Matchers.is(1)),
                () -> Assert.assertThat(caseDetails.getDataClassification().get("complexField1").get("value").get("field2"), Matchers.is(getTextNode("RESTRICTED")))
            );
        }


        @Test
        @DisplayName("should fail if missing data classification for complex node")
        void shouldFailIfMissingDataClassificationForComplexNode() {
            final CaseDetails caseDetails = aCaseDetails()
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

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if missing callback classification for complex node")
        void shouldFailIfMissingCallbackClassificationForComplexNode() {
            final CaseDetails caseDetails = aCaseDetails()
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

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if missing callback value for complex node")
        void shouldFailIfMissingCallbackValueForComplexNode() {
            final CaseDetails caseDetails = aCaseDetails()
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

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if missing data value for complex node")
        void shouldFailIfMissingDataValueForComplexNode() {
            final CaseDetails caseDetails = aCaseDetails()
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

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if missing classification for data nodes")
        void shouldFailIfMissingClassificationForDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
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

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if extra classification for data nodes")
        void shouldFailIfExtraClassificationForDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
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

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if invalid classification value for callback data")
        void shouldFailIfInvalidClassificationValueForCallbackData() {
            final CaseDetails caseDetails = aCaseDetails()
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

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if invalid classification value for callback data nodes")
        void shouldFailIfInvalidClassificationValueForCallbackDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
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

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }


        @Test
        @DisplayName("should fail if invalid classification level for data")
        void shouldFailIfInvalidClassificationLevelForData() {
            final CaseDetails caseDetails = aCaseDetails()
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

            assertThrowsSecurityValidationForCaseDataException(caseDetails, callbackResponse, "complexField1");
        }

        @Test
        @DisplayName("should fail if invalid classification level for data nodes")
        void shouldFailIfInvalidClassificationLevelForDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
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

            assertThrowsSecurityValidationForCaseDataException(caseDetails, callbackResponse, "field2");
        }
    }

    @Nested
    @DisplayName("Validate data classification collection field")
    class ValidateDataClassificationCollectionField {

        @Test
        @DisplayName("should increase security if valid classification level for case and data nodes")
        void shouldIncreaseSecurityIfValidClassificationLevelForCaseAndDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(
                                                              aClassificationBuilder()
                                                                  .withData("value",
                                                                            aClassificationBuilder()
                                                                                .withData("field2", getTextNode("PRIVATE"))
                                                                                .buildAsNode())
                                                                  .withData("id", getTextNode("someId1"))
                                                                  .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PRIVATE)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("RESTRICTED"))
                                                .withData("value",
                                                          newArrayList(
                                                              aClassificationBuilder()
                                                                  .withData("value",
                                                                            aClassificationBuilder()
                                                                                .withData("field2", getTextNode("RESTRICTED"))
                                                                                .buildAsNode())
                                                                  .withData("id", getTextNode("someId1"))
                                                                  .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            securityValidationService.setClassificationFromCallbackIfValid(callbackResponse, caseDetails);

            assertAll(
                () -> Assert.assertThat(caseDetails.getSecurityClassification(), Matchers.is(PRIVATE)),
                () -> Assert.assertThat(caseDetails.getDataClassification().size(), Matchers.is(1)),
                () -> Assert.assertThat(caseDetails.getDataClassification().get("collectionField1").size(), Matchers.is(2)),
                () -> Assert.assertThat(caseDetails.getDataClassification().get("collectionField1").get("classification"),
                                        Matchers.is(getTextNode("RESTRICTED"))),
                () -> Assert.assertThat(caseDetails.getDataClassification().get("collectionField1").get("value").size(), Matchers.is(1)),
                () -> Assert.assertThat(caseDetails.getDataClassification().get("collectionField1").get("value").get(0).get("id"),
                                        Matchers.is(getTextNode("someId1"))),
                () -> Assert.assertThat(caseDetails.getDataClassification().get("collectionField1").get("value").get(0).get("value").get("field2"),
                                        Matchers.is(getTextNode("RESTRICTED")))
            );
        }

        @Test
        @DisplayName("should fail if missing collection item for data nodes")
        void shouldFailIfMissingCollectionItemForDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(
                                                              aClassificationBuilder()
                                                                  .withData("value",
                                                                            aClassificationBuilder()
                                                                                .withData("field2", getTextNode("PRIVATE"))
                                                                                .buildAsNode())
                                                                  .withData("id", getTextNode("someId1"))
                                                                  .buildAsMap(),
                                                              aClassificationBuilder()
                                                                  .withData("value",
                                                                            aClassificationBuilder()
                                                                                .withData("field3", getTextNode("PRIVATE"))
                                                                                .buildAsNode())
                                                                  .withData("id", getTextNode("someId2"))
                                                                  .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();
            final CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PRIVATE)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("RESTRICTED"))
                                                .withData("value",
                                                          newArrayList(
                                                              aClassificationBuilder()
                                                                  .withData("value",
                                                                            aClassificationBuilder()
                                                                                .withData("field2", getTextNode("PRIVATE"))
                                                                                .buildAsNode())
                                                                  .withData("id", getTextNode("someId1"))
                                                                  .buildAsMap(),
                                                              aClassificationBuilder()
                                                                  .withData("id", getTextNode("someId2"))
                                                                  .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if missing classification for data nodes")
        void shouldFailIfMissingClassificationForDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(
                                                              aClassificationBuilder()
                                                                  .withData("value",
                                                                            aClassificationBuilder()
                                                                                .withData("field2", getTextNode("PRIVATE"))
                                                                                .withData("missingField3", getTextNode("RESTRICTED"))
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
                                                                                .withData("field2", getTextNode("RESTRICTED"))
                                                                                .buildAsNode())
                                                                  .withData("id", getTextNode("someId1"))
                                                                  .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if extra classification for data nodes")
        void shouldFailIfExtraClassificationForDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2", getTextNode("PRIVATE"))
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
                                                                               .withData("field2", getTextNode("PRIVATE"))
                                                                               .withData("extraField3", getTextNode("RESTRICTED"))
                                                                               .buildAsNode())
                                                                           .withData("id", getTextNode("someId1"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if extra collection item")
        void shouldFailIfExtraCollectionItem() {
            final CaseDetails caseDetails = aCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2", getTextNode("PRIVATE"))
                                                                               .withData("field3", getTextNode("RESTRICTED"))
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
                                                                               .withData("field2", getTextNode("PRIVATE"))
                                                                               .withData("field3", getTextNode("RESTRICTED"))
                                                                               .buildAsNode())
                                                                           .withData("id", getTextNode("someId1"))
                                                                           .buildAsMap(),
                                                                       aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("extraField1", getTextNode("PRIVATE"))
                                                                               .withData("extraField2", getTextNode("RESTRICTED"))
                                                                               .buildAsNode())
                                                                           .withData("id", getTextNode("extraItemId"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if invalid classification value for callback data")
        void shouldFailIfInvalidClassificationValueForCaseDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2", getTextNode("PRIVATE"))
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
                                                .withData("classification", getTextNode("PLOP"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2", getTextNode("PRIVATE"))
                                                                               .buildAsNode())
                                                                           .withData("id", getTextNode("someId1"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if invalid classification value for callback data nodes")
        void shouldFailIfInvalidClassificationValueForCallbackDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2", getTextNode("PRIVATE"))
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
                                                                               .withData("field2", getTextNode("PLOP"))
                                                                               .buildAsNode())
                                                                           .withData("id", getTextNode("someId1"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            assertThrowsSecurityValidationOtherException(caseDetails, callbackResponse);
        }

        @Test
        @DisplayName("should fail if invalid classification level for data nodes")
        void shouldFailIfInvalidClassificationLevelForDataNodes() {
            final CaseDetails caseDetails = aCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2", getTextNode("PRIVATE"))
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
                                                                               .withData("field2", getTextNode("PUBLIC"))
                                                                               .buildAsNode())
                                                                           .withData("id", getTextNode("someId1"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            assertThrowsSecurityValidationForCaseDataException(caseDetails, callbackResponse, "field2");
        }

        @Test
        @DisplayName("should fail if invalid classification level for data")
        void shouldFailIfInvalidClassificationLevelForData() {
            final CaseDetails caseDetails = aCaseDetails()
                .withSecurityClassification(PUBLIC)
                .withDataClassification(aClassificationBuilder()
                                            .withData("collectionField1", aClassificationBuilder()
                                                .withData("classification", getTextNode("PRIVATE"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2", getTextNode("PRIVATE"))
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
                                                .withData("classification", getTextNode("PUBLIC"))
                                                .withData("value",
                                                          newArrayList(aClassificationBuilder()
                                                                           .withData("value", aClassificationBuilder()
                                                                               .withData("field2", getTextNode("PRIVATE"))
                                                                               .buildAsNode())
                                                                           .withData("id", getTextNode("someId1"))
                                                                           .buildAsMap()))
                                                .buildAsNode())
                                            .buildAsMap())
                .build();

            assertThrowsSecurityValidationForCaseDataException(caseDetails, callbackResponse, "collectionField1");
        }
    }

    private void assertThrowsSecurityValidationForCaseException(CaseDetails caseDetails, CallbackResponse callbackResponse) {
        ValidationException validationException = assertThrows(ValidationException.class,
                                                             () -> securityValidationService.setClassificationFromCallbackIfValid(callbackResponse, caseDetails));
        assertEquals(String.format("The security level of the case with reference=%s cannot be loosened", caseDetails.getReference()), validationException.getMessage());
    }

    private void assertThrowsSecurityValidationForCaseDataException(CaseDetails caseDetails, CallbackResponse callbackResponse, String fieldName) {
        ValidationException validationException = assertThrows(ValidationException.class,
                                                             () -> securityValidationService.setClassificationFromCallbackIfValid(callbackResponse, caseDetails));
        assertEquals(String.format("The security level of the caseData=%s cannot be loosened", fieldName), validationException.getMessage());
    }

    private void assertThrowsSecurityValidationOtherException(CaseDetails caseDetails, CallbackResponse callbackResponse) {
        ValidationException validationException = assertThrows(ValidationException.class,
                                                             () -> securityValidationService.setClassificationFromCallbackIfValid(callbackResponse, caseDetails));
        assertEquals("The event cannot be completed as something went wrong while updating the security level of the case or some of the case fields", validationException.getMessage());
    }

    private JsonNode getTextNode(String value) {
        return JSON_NODE_FACTORY.textNode(value);
    }
}
