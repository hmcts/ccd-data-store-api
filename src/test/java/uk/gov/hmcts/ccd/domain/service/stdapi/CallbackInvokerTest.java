package uk.gov.hmcts.ccd.domain.service.stdapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import uk.gov.hmcts.ccd.data.casedetails.SecurityClassification;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEvent;
import uk.gov.hmcts.ccd.domain.model.definition.CaseType;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityValidationService;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;

import java.util.*;

import static java.lang.Boolean.TRUE;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CallbackInvokerTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final Boolean IGNORE_WARNING = true;
    private static final String URL_ABOUT_TO_START = "http://about-to-start";
    private static final String URL_ABOUT_TO_SUBMIT = "http://about-to-submit";
    private static final String URL_AFTER_SUBMIT = "http://after-submit";
    private static final List<Integer> RETRIES_ABOUT_TO_START = Collections.unmodifiableList(Arrays.asList(1, 2, 3));
    private static final List<Integer> RETRIES_ABOUT_TO_SUBMIT = Collections.unmodifiableList(Arrays.asList(4, 5, 6));
    private static final List<Integer> RETRIES_AFTER_SUBMIT = Collections.unmodifiableList(Arrays.asList(7, 8, 9));

    @Mock
    private CallbackService callbackService;

    @Mock
    private CaseTypeService caseTypeService;

    @Mock
    private CaseDataService caseDataService;

    @Mock
    private CaseSanitiser caseSanitiser;

    @Mock
    private SecurityValidationService securityValidationService;

    @InjectMocks
    private CallbackInvoker callbackInvoker;

    private CaseEvent caseEvent;
    private CaseType caseType;
    private CaseDetails caseDetailsBefore;
    private CaseDetails caseDetails;
    private InOrder inOrder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);

        caseEvent = new CaseEvent();
        caseEvent.setCallBackURLAboutToStartEvent(URL_ABOUT_TO_START);
        caseEvent.setRetriesTimeoutAboutToStartEvent(RETRIES_ABOUT_TO_START);
        caseEvent.setCallBackURLAboutToSubmitEvent(URL_ABOUT_TO_SUBMIT);
        caseEvent.setRetriesTimeoutURLAboutToSubmitEvent(RETRIES_ABOUT_TO_SUBMIT);
        caseEvent.setCallBackURLSubmittedEvent(URL_AFTER_SUBMIT);
        caseEvent.setRetriesTimeoutURLSubmittedEvent(RETRIES_AFTER_SUBMIT);
        caseType = new CaseType();
        caseDetailsBefore = new CaseDetails();
        caseDetails = new CaseDetails();

        doReturn(Optional.empty()).when(callbackService).send(any(), any(), same(caseEvent), same(caseDetails));
        doReturn(Optional.empty()).when(callbackService).send(any(),
                                                              any(),
                                                              same(caseEvent),
                                                              same(caseDetailsBefore),
                                                              same(caseDetails));

        inOrder = inOrder(callbackService,
                                  caseTypeService,
                                  caseDataService,
                          securityValidationService,
                                  caseSanitiser);
    }

    @Nested
    @DisplayName("invokeAboutToStartCallback()")
    class AboutToStart {

        @Test
        @DisplayName("should send callback")
        void shouldSendCallback() {
            callbackInvoker.invokeAboutToStartCallback(caseEvent, caseType, caseDetails, IGNORE_WARNING);

            verify(callbackService).send(URL_ABOUT_TO_START, RETRIES_ABOUT_TO_START, caseEvent, caseDetails);
        }
    }

    @Nested
    @DisplayName("invokeAboutToSubmitCallback()")
    class AboutToSubmit {

        @Test
        @DisplayName("should send callback")
        void shouldSendCallback() {
            final Optional<String>
                response =
                callbackInvoker.invokeAboutToSubmitCallback(caseEvent,
                                                            caseDetailsBefore,
                                                            caseDetails,
                                                            caseType,
                                                            IGNORE_WARNING);

            verify(callbackService).send(URL_ABOUT_TO_SUBMIT,
                                         RETRIES_ABOUT_TO_SUBMIT,
                                         caseEvent,
                                         caseDetailsBefore,
                                         caseDetails);
            assertThat(response.isPresent(), is(false));
        }

        @Test
        @DisplayName("should send callback and get state")
        void sendCallbackAndGetState() {
            final String expectedState = "uNiCORn";
            doReturn(Optional.of(mockCallbackResponse(expectedState))).when(callbackService)
                                                                      .send(any(),
                                                                            any(),
                                                                            same(caseEvent),
                                                                            same(caseDetailsBefore),
                                                                            same(caseDetails));
            final Optional<String>
                response =
                callbackInvoker.invokeAboutToSubmitCallback(caseEvent,
                                                            caseDetailsBefore,
                                                            caseDetails,
                                                            caseType,
                                                            IGNORE_WARNING);

            verify(callbackService).send(URL_ABOUT_TO_SUBMIT,
                                         RETRIES_ABOUT_TO_SUBMIT,
                                         caseEvent,
                                         caseDetailsBefore,
                                         caseDetails);
            assertThat(response.get(), is(expectedState));
        }

        @Test
        @DisplayName("should send callback and get no state")
        void sendCallbackAndGetNoState() {
            doReturn(Optional.of(mockCallbackResponseWithNoState())).when(callbackService)
                                                                      .send(any(),
                                                                            any(),
                                                                            same(caseEvent),
                                                                            same(caseDetailsBefore),
                                                                            same(caseDetails));
            final Optional<String>
                response =
                callbackInvoker.invokeAboutToSubmitCallback(caseEvent,
                                                            caseDetailsBefore,
                                                            caseDetails,
                                                            caseType,
                                                            IGNORE_WARNING);

            verify(callbackService).send(URL_ABOUT_TO_SUBMIT,
                                         RETRIES_ABOUT_TO_SUBMIT,
                                         caseEvent,
                                         caseDetailsBefore,
                                         caseDetails);
            assertThat(response.isPresent(), is(false));
        }

        private CallbackResponse mockCallbackResponse(final String state) {
            final CallbackResponse response = new CallbackResponse();
            final Map<String, JsonNode> data = new HashMap<>();
            data.put("state", JsonNodeFactory.instance.textNode(state));
            response.setData(data);
            return response;
        }

        private CallbackResponse mockCallbackResponseWithNoState() {
            return new CallbackResponse();
        }
    }

    @Nested
    @DisplayName("invokeSubmittedCallback()")
    class Submitted {

        @Test
        @DisplayName("should send callback")
        void shouldSendCallback() {
            callbackInvoker.invokeSubmittedCallback(caseEvent, caseDetailsBefore, caseDetails);

            verify(callbackService).send(URL_AFTER_SUBMIT,
                                         RETRIES_AFTER_SUBMIT,
                                         caseEvent,
                                         caseDetailsBefore,
                                         caseDetails,
                                         AfterSubmitCallbackResponse.class);
        }
    }

    @Nested
    @DisplayName("validations")
    class Validations {

        @Nested
        @DisplayName("about to start")
        class AboutToStart {
            @DisplayName("validate call back response and update case details data for about to start")
            @Test
            void validateAndSetDataForAboutToStart() {
                final CallbackResponse callbackResponse = new CallbackResponse();
                final Map<String, JsonNode> data = new HashMap<>();
                data.put("xxx", TextNode.valueOf("ngitb"));
                callbackResponse.setData(data);
                HashMap<String, JsonNode> currentDataClassification = Maps.newHashMap();
                when(caseDataService.getDefaultSecurityClassifications(caseType, data, caseDetails.getDataClassification())).thenReturn(
                    currentDataClassification);
                when(callbackService.send(caseEvent.getCallBackURLAboutToStartEvent(),
                                          caseEvent.getRetriesTimeoutAboutToStartEvent(),
                                          caseEvent,
                                          caseDetails)).thenReturn(Optional.of(callbackResponse));

                callbackInvoker.invokeAboutToStartCallback(caseEvent, caseType, caseDetails, TRUE);

                assertAll(
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(caseTypeService).validateData(callbackResponse.getData(), caseType),
                    () -> inOrder.verify(caseSanitiser).sanitise(caseType, callbackResponse.getData()),
                    () -> inOrder.verify(caseDataService).getDefaultSecurityClassifications(caseType,
                                                                                            caseDetails.getData(),
                                                                                            caseDetails.getDataClassification()),
                    () -> inOrder.verify(securityValidationService, never()).setClassificationFromCallbackIfValid(any(), any(), any())
                );
            }

            @DisplayName("validate call back response and no case details data is updated")
            @Test
            void validateAndDoNotSetData() {
                final CallbackResponse callbackResponse = new CallbackResponse();
                when(callbackService.send(caseEvent.getCallBackURLAboutToStartEvent(),
                                          caseEvent.getRetriesTimeoutAboutToStartEvent(),
                                          caseEvent,
                                          caseDetails)).thenReturn(Optional.of(callbackResponse));

                callbackInvoker.invokeAboutToStartCallback(caseEvent, caseType, caseDetails, TRUE);

                assertAll(
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(caseTypeService, never()).validateData(any(), any()),
                    () -> inOrder.verify(caseSanitiser, never()).sanitise(any(), any()),
                    () -> inOrder.verify(caseDataService, never()).getDefaultSecurityClassifications(any(), any(), any()),
                    () -> inOrder.verify(securityValidationService, never()).setClassificationFromCallbackIfValid(any(), any(), any())
                );
            }

            @DisplayName("validate call back response and there are errors in call back validation when setting data")
            @Test
            void validateAndSetDataMetError() throws ApiException {
                final CallbackResponse callbackResponse = new CallbackResponse();
                when(callbackService.send(caseEvent.getCallBackURLAboutToStartEvent(),
                                          caseEvent.getRetriesTimeoutAboutToStartEvent(),
                                          caseEvent,
                                          caseDetails)).thenReturn(Optional.of(callbackResponse));
                final Map<String, JsonNode> data = new HashMap<>();
                callbackResponse.setData(data);

                final String ErrorMessage = "Royal marriage *!><}{^";
                doThrow(new ApiException(ErrorMessage))
                    .when(callbackService).validateCallbackErrorsAndWarnings(any(), any());

                final ApiException apiException =
                    assertThrows(ApiException.class,
                                 () -> callbackInvoker.invokeAboutToStartCallback(caseEvent, caseType, caseDetails, TRUE));

                assertThat(apiException.getMessage(), is(ErrorMessage));

                assertAll(
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(caseTypeService, never()).validateData(any(), any()),
                    () -> inOrder.verify(caseSanitiser, never()).sanitise(any(), any()),
                    () -> inOrder.verify(caseDataService, never()).getDefaultSecurityClassifications(any(), any(), any()),
                    () -> inOrder.verify(securityValidationService, never()).setClassificationFromCallbackIfValid(any(), any(), any())
                );
            }
        }

        @Nested
        @DisplayName("about to submit")
        class AboutToSubmit {
            final HashMap<String, JsonNode> currentDataClassification = Maps.newHashMap();
            final Map<String, JsonNode> newFieldsDataClassification = Maps.newHashMap();
            final Map<String, JsonNode> allFieldsDataClassification = Maps.newHashMap();
            final CallbackResponse callbackResponse = new CallbackResponse();
            final Map<String, JsonNode> data = new HashMap<>();

            @BeforeEach
            private void setup() {
                caseDetails.setDataClassification(currentDataClassification);
                caseDetails.setData(data);
                callbackResponse.setData(data);
                currentDataClassification.put("currentKey", JSON_NODE_FACTORY.textNode("currentValue"));
                caseDetails.setState("BAYAN");
                newFieldsDataClassification.put("key", JSON_NODE_FACTORY.textNode("value"));

                allFieldsDataClassification.put("key", JSON_NODE_FACTORY.textNode("otherValue"));
                callbackResponse.setSecurityClassification(SecurityClassification.PRIVATE);
                callbackResponse.setDataClassification(allFieldsDataClassification);
                when(callbackService.send(caseEvent.getCallBackURLAboutToSubmitEvent(),
                                          caseEvent.getRetriesTimeoutURLAboutToSubmitEvent(),
                                          caseEvent,
                                          caseDetailsBefore,
                                          caseDetails)).thenReturn(Optional.of(callbackResponse));
                when(caseSanitiser.sanitise(eq(caseType), eq(caseDetails.getData()))).thenReturn(data);
                when(caseDataService.getDefaultSecurityClassifications(eq(caseType), eq(caseDetails.getData()), eq(currentDataClassification))).thenReturn(newFieldsDataClassification);
                when(caseDataService.getDefaultSecurityClassifications(eq(caseType), eq(caseDetails.getData()), eq(Maps.newHashMap()))).thenReturn(allFieldsDataClassification);
            }

            @DisplayName("do not validate call back response if no data security passed back")
            @Test
            void doNotValidateCallbackResponseIfNoDataSecurityPassedBack() {
                callbackResponse.setDataClassification(null);
                data.put("state", TextNode.valueOf("ngitb"));

                callbackInvoker.invokeAboutToSubmitCallback(caseEvent, caseDetailsBefore, caseDetails, caseType, TRUE);

                assertAll(
                    () -> assertThat(caseDetails.getState(), is("ngitb")),
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(caseTypeService).validateData(callbackResponse.getData(), caseType),
                    () -> inOrder.verify(caseSanitiser).sanitise(caseType, callbackResponse.getData()),
                    () -> inOrder.verify(caseDataService, times(1)).getDefaultSecurityClassifications(eq(caseType),
                                                                                                      eq(caseDetails.getData()),
                                                                                                      eq(currentDataClassification)),
                    () -> inOrder.verify(securityValidationService, never()).setClassificationFromCallbackIfValid(any(), any(), any())
                );
            }

            @DisplayName("do not validate call back response if no case security passed back")
            @Test
            void doNotValidateCallbackResponseIfNoCaseSecurityPassedBack() {
                callbackResponse.setSecurityClassification(null);
                data.put("state", TextNode.valueOf("ngitb"));

                callbackInvoker.invokeAboutToSubmitCallback(caseEvent, caseDetailsBefore, caseDetails, caseType, TRUE);

                assertAll(
                    () -> assertThat(caseDetails.getState(), is("ngitb")),
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(caseTypeService).validateData(callbackResponse.getData(), caseType),
                    () -> inOrder.verify(caseSanitiser).sanitise(caseType, callbackResponse.getData()),
                    () -> inOrder.verify(caseDataService, times(1)).getDefaultSecurityClassifications(eq(caseType),
                                                                                                      eq(caseDetails.getData()),
                                                                                                      eq(currentDataClassification)),
                    () -> inOrder.verify(securityValidationService, never()).setClassificationFromCallbackIfValid(any(), any(), any())
                );
            }


            @DisplayName("validate call back response and set case details state for about to submit")
            @Test
            void validateAndSetStateForAboutToSubmit() {
                data.put("state", TextNode.valueOf("ngitb"));

                callbackInvoker.invokeAboutToSubmitCallback(caseEvent, caseDetailsBefore, caseDetails, caseType, TRUE);

                ArgumentCaptor<Map> argumentDataClassification = ArgumentCaptor.forClass(Map.class);
                assertAll(
                    () -> assertThat(caseDetails.getState(), is("ngitb")),
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(caseTypeService).validateData(callbackResponse.getData(), caseType),
                    () -> inOrder.verify(caseSanitiser).sanitise(caseType, callbackResponse.getData()),
                    () -> inOrder.verify(caseDataService, times(2)).getDefaultSecurityClassifications(eq(caseType),
                                                                                            eq(caseDetails.getData()),
                                                                                            argumentDataClassification.capture()),
                    () -> inOrder.verify(securityValidationService).setClassificationFromCallbackIfValid(eq(callbackResponse), eq(caseDetails), eq(allFieldsDataClassification)),
                    () -> assertThat(argumentDataClassification.getAllValues(), contains(currentDataClassification, Maps.newHashMap()))
                );
            }

            @DisplayName("validate call back response and neither case details nor state is updated")
            @Test
            void validateAndDoNotSetStateOrData() {
                callbackInvoker.invokeAboutToSubmitCallback(caseEvent, caseDetailsBefore, caseDetails, caseType, TRUE);

                ArgumentCaptor<Map> argumentDataClassification = ArgumentCaptor.forClass(Map.class);
                assertAll(
                    () -> assertThat(caseDetails.getState(), is("BAYAN")),
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(caseTypeService).validateData(callbackResponse.getData(), caseType),
                    () -> inOrder.verify(caseSanitiser).sanitise(caseType, callbackResponse.getData()),
                    () -> inOrder.verify(caseDataService, times(2)).getDefaultSecurityClassifications(eq(caseType),
                                                                                            eq(caseDetails.getData()),
                                                                                            argumentDataClassification.capture()),
                    () -> inOrder.verify(securityValidationService).setClassificationFromCallbackIfValid(callbackResponse, caseDetails, allFieldsDataClassification),
                    () -> assertThat(argumentDataClassification.getAllValues(), contains(currentDataClassification, Maps.newHashMap()))
                );
            }

            @DisplayName("validate call back response and there are errors in call back validation when setting data and state")
            @Test
            void validateAndSetStateMetError() throws ApiException {
                final String errorMessgae = "Royal carriage is stuck AGAIN!!!!";
                doThrow(new ApiException(errorMessgae))
                    .when(callbackService).validateCallbackErrorsAndWarnings(any(), any());
                final ApiException apiException =
                    assertThrows(ApiException.class,
                                 () -> callbackInvoker.invokeAboutToSubmitCallback(caseEvent, caseDetailsBefore, caseDetails, caseType, TRUE));

                assertAll(
                    () -> assertThat(apiException.getMessage(), is(errorMessgae)),
                    () -> assertThat(caseDetails.getState(), is("BAYAN")),
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(caseTypeService, never()).validateData(any(), any()),
                    () -> inOrder.verify(caseSanitiser, never()).sanitise(any(), any()),
                    () -> inOrder.verify(caseDataService, never()).getDefaultSecurityClassifications(any(), any(), any()),
                    () -> inOrder.verify(securityValidationService, never()).setClassificationFromCallbackIfValid(any(), any(), any())
                );
            }
        }

        @DisplayName("state is filtered in json map")
        @Test
        void filterCaseState() {
            final Map<String, JsonNode> data = new HashMap<>();
            data.put("state", TextNode.valueOf("ngitb"));
            data.put("blah", IntNode.valueOf(678));

            assertThat("Before filter", data.keySet(), hasSize(2));

            final Optional<String> state = callbackInvoker.filterCaseState(data);

            assertAll(() -> assertThat(state.get(), is("ngitb")),
                      () -> assertThat(data.keySet(), hasSize(1)),
                      () -> assertThat(data.get("blah").intValue(), is(678)));
        }

        @DisplayName("state is filtered but state is not returned when it is not a text value")
        @Test
        void filterCaseStateButNotReturned() {
            final Map<String, JsonNode> data = new HashMap<>();
            data.put("state", IntNode.valueOf(678));

            assertThat("Before filter", data.keySet(), hasSize(1));

            final Optional<String> state = callbackInvoker.filterCaseState(data);

            assertAll(() -> assertFalse(state.isPresent()),
                      () -> assertThat(data.keySet(), hasSize(0)));
        }
    }
}
