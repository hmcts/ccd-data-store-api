package uk.gov.hmcts.ccd.domain.service.stdapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import uk.gov.hmcts.ccd.ApplicationParams;
import uk.gov.hmcts.ccd.appinsights.AppInsights;
import uk.gov.hmcts.ccd.data.SecurityUtils;
import uk.gov.hmcts.ccd.domain.service.common.PersistenceStrategyResolver;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItem;
import uk.gov.hmcts.ccd.domain.model.callbacks.SignificantItemType;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackService;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackUrlValidator;
import uk.gov.hmcts.ccd.domain.service.casedeletion.TimeToLiveService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityValidationService;
import uk.gov.hmcts.ccd.domain.service.processor.GlobalSearchProcessorService;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.exceptions.ApiException;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PRIVATE;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.PUBLIC;
import static uk.gov.hmcts.ccd.data.casedetails.SecurityClassification.RESTRICTED;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType.GET_CASE;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType.MID_EVENT;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType.SUBMITTED;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CallbackResponseBuilder.aCallbackResponse;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.CaseDetailsBuilder.newCaseDetails;
import static uk.gov.hmcts.ccd.domain.service.common.TestBuildersUtil.DataClassificationBuilder.aClassificationBuilder;

class CallbackInvokerTest {
    private static final JsonNodeFactory JSON_NODE_FACTORY = new JsonNodeFactory(false);
    private static final Boolean IGNORE_WARNING = true;
    private static final String URL_ABOUT_TO_START = "http://about-to-start";
    private static final String URL_ABOUT_TO_SUBMIT = "http://about-to-submit";
    private static final String URL_GET_CASE = "http://get-case";
    private static final String URL_AFTER_SUBMIT = "http://after-submit";
    private static final String URL_MID_EVENT = "http://mid-event";
    private static final Boolean IGNORE_WARNINGS = FALSE;
    private static final List<Integer> RETRIES_DISABLED = Lists.newArrayList(0);

    @Mock
    private CallbackService callbackService;

    @Mock
    private CaseTypeService caseTypeService;

    @Mock
    private TimeToLiveService timeToLiveService;

    @Mock
    private CaseDataService caseDataService;

    @Mock
    private CaseSanitiser caseSanitiser;

    @Mock
    private SecurityValidationService securityValidationService;

    @Mock
    private GlobalSearchProcessorService globalSearchProcessorService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ApplicationParams applicationParams;

    @Mock
    private AppInsights appinsights;

    @Mock
    private PersistenceStrategyResolver persistenceStrategyResolver;

    @InjectMocks
    private CallbackInvoker callbackInvoker;

    private CaseEventDefinition caseEventDefinition;
    private CaseTypeDefinition caseTypeDefinition;
    private CaseDetails caseDetailsBefore;
    private CaseDetails caseDetails;
    private WizardPage wizardPage;
    private InOrder inOrder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setCallBackURLAboutToStartEvent(URL_ABOUT_TO_START);
        caseEventDefinition.setCallBackURLAboutToSubmitEvent(URL_ABOUT_TO_SUBMIT);
        caseEventDefinition.setCallBackURLSubmittedEvent(URL_AFTER_SUBMIT);
        caseTypeDefinition = new CaseTypeDefinition();
        caseDetailsBefore = new CaseDetails();
        caseDetails = new CaseDetails();
        wizardPage = new WizardPage();
        wizardPage.setCallBackURLMidEvent(URL_MID_EVENT);

        doReturn(Optional.empty()).when(callbackService).send(any(), any(), same(caseEventDefinition), any(),
            same(caseDetails), anyBoolean());
        doReturn(Optional.empty()).when(callbackService).send(any(), any(),
            same(caseEventDefinition),
            same(caseDetailsBefore),
            same(caseDetails),
            anyBoolean());

        inOrder = inOrder(callbackService,
            caseTypeService,
            globalSearchProcessorService,
            caseDataService,
            securityValidationService,
            caseSanitiser,
            timeToLiveService);
    }

    @Nested
    @DisplayName("invokeAboutToStartCallback()")
    class AboutToStart {

        @Test
        @DisplayName("should send callback")
        void shouldSendCallback() {
            callbackInvoker.invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition, caseDetails,
                IGNORE_WARNING);

            verify(callbackService).send(URL_ABOUT_TO_START, ABOUT_TO_START, caseEventDefinition,
                null, caseDetails, false);
            verifyNoMoreInteractions(callbackService);
        }

        @Test
        @DisplayName("should disable callback retries")
        void shouldDisableCallbackRetries() {
            caseEventDefinition.setRetriesTimeoutAboutToStartEvent(RETRIES_DISABLED);

            callbackInvoker.invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition, caseDetails,
                IGNORE_WARNING);

            verify(callbackService, times(1)).sendSingleRequest(URL_ABOUT_TO_START,
                ABOUT_TO_START, caseEventDefinition, null, caseDetails, false);
            verifyNoMoreInteractions(callbackService);
        }

        @Test
        @DisplayName("should handle exception in printCallbackDetails gracefully")
        void shouldHandleExceptionInPrintCallbackDetailsGracefully() throws JsonProcessingException {
            final String testUrl = "https://localhost/about-to-start";
            when(applicationParams.getCcdCallbackLogControl()).thenReturn(Collections.singletonList("*"));
            when(applicationParams.getCallbackAllowedHosts()).thenReturn(Collections.singletonList("localhost"));
            when(applicationParams.getCallbackAllowedHttpHosts()).thenReturn(Collections.singletonList("localhost"));
            when(applicationParams.getCallbackAllowPrivateHosts()).thenReturn(Collections.singletonList("localhost"));
            doNothing().when(appinsights).trackCallbackEvent(any(), anyString(), anyString(), any(Duration.class));

            CallbackService callbackService = new CallbackService(securityUtils,
                restTemplate,
                applicationParams,
                appinsights,
                null,
                objectMapper,
                new CallbackUrlValidator(applicationParams));

            when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("Mocked exception"));
            when(securityUtils.authorizationHeaders()).thenReturn(new HttpHeaders());
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(CallbackResponse.class)))
                .thenReturn(new ResponseEntity<>(new CallbackResponse(), HttpStatus.OK));

            Optional<CallbackResponse> response = callbackService.sendSingleRequest(
                testUrl,
                ABOUT_TO_START,
                caseEventDefinition,
                null,
                caseDetails,
                IGNORE_WARNING
            );

            assertTrue(response.isPresent());
            verify(objectMapper, times(2)).writeValueAsString(any());
            verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class),
                eq(CallbackResponse.class));
        }
    }

    @Nested
    @DisplayName("invokeAboutToSubmitCallback()")
    class AboutToSubmit {

        @Test
        @DisplayName("should send callback")
        void shouldSendCallback() {
            final AboutToSubmitCallbackResponse
                response =
                callbackInvoker.invokeAboutToSubmitCallback(caseEventDefinition,
                    caseDetailsBefore,
                    caseDetails,
                    caseTypeDefinition,
                    IGNORE_WARNING);

            verify(callbackService).send(URL_ABOUT_TO_SUBMIT, ABOUT_TO_SUBMIT,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                IGNORE_WARNING);
            verifyNoMoreInteractions(callbackService);
            assertThat(response.getState().isPresent(), is(false));
        }

        @Test
        @DisplayName("should disable callback retries")
        void shouldDisableCallbackRetries() {
            caseEventDefinition.setRetriesTimeoutURLAboutToSubmitEvent(RETRIES_DISABLED);

            final AboutToSubmitCallbackResponse
                response =
                callbackInvoker.invokeAboutToSubmitCallback(caseEventDefinition,
                    caseDetailsBefore,
                    caseDetails,
                    caseTypeDefinition,
                    IGNORE_WARNING);

            verify(callbackService).sendSingleRequest(URL_ABOUT_TO_SUBMIT, ABOUT_TO_SUBMIT,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                IGNORE_WARNING);
            verifyNoMoreInteractions(callbackService);
            assertThat(response.getState().isPresent(), is(false));
        }

        @Test
        @DisplayName("should send callback and get state")
        void sendCallbackAndGetState() {
            final String expectedState = "uNiCORn";
            doReturn(Optional.of(mockCallbackResponse(expectedState))).when(callbackService)
                .send(any(), any(),
                    same(caseEventDefinition),
                    same(caseDetailsBefore),
                    same(caseDetails),
                    anyBoolean());
            final AboutToSubmitCallbackResponse response =
                callbackInvoker.invokeAboutToSubmitCallback(caseEventDefinition,
                    caseDetailsBefore,
                    caseDetails,
                    caseTypeDefinition,
                    IGNORE_WARNING);

            verify(callbackService).send(URL_ABOUT_TO_SUBMIT, ABOUT_TO_SUBMIT,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                true);
            assertThat(response.getState().get(), is(expectedState));
        }

        @Test
        @DisplayName("should send callback and get no state")
        void sendCallbackAndGetNoState() {
            doReturn(Optional.of(mockCallbackResponseWithNoState())).when(callbackService)
                .send(any(), any(),
                    same(caseEventDefinition),
                    same(caseDetailsBefore),
                    same(caseDetails),
                    anyBoolean());
            final AboutToSubmitCallbackResponse response =
                callbackInvoker.invokeAboutToSubmitCallback(caseEventDefinition,
                    caseDetailsBefore,
                    caseDetails,
                    caseTypeDefinition,
                    IGNORE_WARNING);

            verify(callbackService).send(URL_ABOUT_TO_SUBMIT, ABOUT_TO_SUBMIT,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                true);
            assertThat(response.getState().isPresent(), is(false));
        }

        @Test
        @DisplayName("should send callback and get state and significant Item")
        void sendCallbackAndGetStateAndSignificantDocument() {
            final String expectedState = "uNiCORn";
            doReturn(Optional.of(mockCallbackResponseWithSignificantItem(expectedState))).when(callbackService)
                .send(any(), any(),
                    same(caseEventDefinition),
                    same(caseDetailsBefore),
                    same(caseDetails),
                    anyBoolean());

            final AboutToSubmitCallbackResponse response =
                callbackInvoker.invokeAboutToSubmitCallback(caseEventDefinition,
                    caseDetailsBefore,
                    caseDetails,
                    caseTypeDefinition,
                    IGNORE_WARNING);

            verify(callbackService).send(URL_ABOUT_TO_SUBMIT, ABOUT_TO_SUBMIT,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                true);
            assertThat(response.getState().get(), is(expectedState));
            assertEquals("description", response.getSignificantItem().getDescription());
            assertEquals(SignificantItemType.DOCUMENT.name(), response.getSignificantItem().getType());
            assertEquals("http://www.cnn.com", response.getSignificantItem().getUrl());
        }

        @Test
        @DisplayName("should send callback and get state and significant Item with invalid URL")
        void sendCallbackAndGetStateAndSignificantDocumentWithInvalidURL() {
            final String expectedState = "uNiCORn";
            doReturn(Optional.of(mockCallbackResponseWithSignificantItem(expectedState))).when(callbackService)
                .send(any(), any(),
                    same(caseEventDefinition),
                    same(caseDetailsBefore),
                    same(caseDetails),
                    anyBoolean());

            final AboutToSubmitCallbackResponse response =
                callbackInvoker.invokeAboutToSubmitCallback(caseEventDefinition,
                    caseDetailsBefore,
                    caseDetails,
                    caseTypeDefinition,
                    IGNORE_WARNING);

            verify(callbackService).send(URL_ABOUT_TO_SUBMIT, ABOUT_TO_SUBMIT,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                true);
            assertThat(response.getState().get(), is(expectedState));
            assertEquals("description", response.getSignificantItem().getDescription());
            assertEquals(SignificantItemType.DOCUMENT.name(), response.getSignificantItem().getType());
            assertEquals("http://www.cnn.com", response.getSignificantItem().getUrl());
        }

        @Test
        @DisplayName("should send callback with errors of significant item is incorrect")
        void sendCallbackAndGetStateAndIncorrectSignificantDocument() {
            final String expectedState = "uNiCORn";
            CallbackResponse callbackResponse = mockCallbackResponseWithIncorrectSignificantItem(expectedState);
            doReturn(Optional.of(callbackResponse)).when(callbackService)
                .send(any(), any(),
                    same(caseEventDefinition),
                    same(caseDetailsBefore),
                    same(caseDetails),
                    anyBoolean());
            final AboutToSubmitCallbackResponse
                response =
                callbackInvoker.invokeAboutToSubmitCallback(caseEventDefinition,
                    caseDetailsBefore,
                    caseDetails,
                    caseTypeDefinition,
                    IGNORE_WARNING);

            verify(callbackService).send(URL_ABOUT_TO_SUBMIT, ABOUT_TO_SUBMIT,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                true);
            assertThat(response.getState().get(), is(expectedState));
            assertNull(response.getSignificantItem());
            assertEquals(3, callbackResponse.getErrors().size());

        }

        @Test
        @DisplayName("should decrease security classification successfully")
        void shouldDecreaseSecurityClassificationSuccessfully() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PRIVATE)
                .build();
            final CaseDetails caseDetailsBefore = newCaseDetails()
                .withSecurityClassification(PRIVATE)
                .build();

            CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PUBLIC)
                .withData(aClassificationBuilder().buildAsMap())
                .build();

            doReturn(Optional.of(callbackResponse)).when(callbackService)
                .send(any(), any(),
                    same(caseEventDefinition),
                    same(caseDetailsBefore),
                    same(caseDetails),
                    anyBoolean());

            doAnswer(invocation -> {
                caseDetails.setSecurityClassification(callbackResponse.getSecurityClassification());
                return null;
            }).when(securityValidationService).updateSecurityClassificationIfValid(callbackResponse, caseDetails);

            AboutToSubmitCallbackResponse response = callbackInvoker.invokeAboutToSubmitCallback(
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                caseTypeDefinition,
                IGNORE_WARNING
            );

            assertAll(
                () -> assertThat(caseDetails.getSecurityClassification(), is(PUBLIC)),
                () -> assertNotNull(response),
                () -> assertThat(response.getState().isPresent(), is(false))
            );

            verify(callbackService).send(URL_ABOUT_TO_SUBMIT, ABOUT_TO_SUBMIT,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                true);
            verify(securityValidationService).updateSecurityClassificationIfValid(callbackResponse, caseDetails);
        }

        @Test
        @DisplayName("should increase security classification successfully")
        void shouldIncreaseSecurityClassificationSuccessfully() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .build();
            final CaseDetails caseDetailsBefore = newCaseDetails()
                .withSecurityClassification(PUBLIC)
                .build();

            CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(PRIVATE)
                .withData(aClassificationBuilder().buildAsMap())
                .build();

            doReturn(Optional.of(callbackResponse)).when(callbackService)
                .send(any(), any(),
                    same(caseEventDefinition),
                    same(caseDetailsBefore),
                    same(caseDetails),
                    anyBoolean());

            doAnswer(invocation -> {
                caseDetails.setSecurityClassification(callbackResponse.getSecurityClassification());
                return null;
            }).when(securityValidationService).updateSecurityClassificationIfValid(callbackResponse, caseDetails);

            AboutToSubmitCallbackResponse response = callbackInvoker.invokeAboutToSubmitCallback(
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                caseTypeDefinition,
                IGNORE_WARNING
            );

            assertAll(
                () -> assertThat(caseDetails.getSecurityClassification(), is(PRIVATE)),
                () -> assertNotNull(response),
                () -> assertThat(response.getState().isPresent(), is(false))
            );

            verify(callbackService).send(URL_ABOUT_TO_SUBMIT, ABOUT_TO_SUBMIT,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                true);
            verify(securityValidationService).updateSecurityClassificationIfValid(callbackResponse, caseDetails);
        }

        @Test
        @DisplayName("should not update security classification if callback classification is null")
        void shouldNotUpdateSecurityClassificationIfCallbackClassificationIsNull() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PRIVATE)
                .build();
            final CaseDetails caseDetailsBefore = newCaseDetails()
                .withSecurityClassification(PRIVATE)
                .build();

            CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(null)
                .withData(aClassificationBuilder().buildAsMap())
                .build();

            doReturn(Optional.of(callbackResponse)).when(callbackService)
                .send(any(), any(),
                    same(caseEventDefinition),
                    same(caseDetailsBefore),
                    same(caseDetails),
                    anyBoolean());

            AboutToSubmitCallbackResponse response = callbackInvoker.invokeAboutToSubmitCallback(
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                caseTypeDefinition,
                IGNORE_WARNING
            );

            assertAll(
                () -> assertThat(caseDetails.getSecurityClassification(), is(PRIVATE)),
                () -> assertNotNull(response),
                () -> assertThat(response.getState().isPresent(), is(false))
            );
            verify(securityValidationService, never()).updateSecurityClassificationIfValid(callbackResponse,
                caseDetails);
        }

        @Test
        @DisplayName("should not update security classification if callback data is null")
        void shouldNotUpdateSecurityClassificationIfCallbackDataIsNull() {
            final CaseDetails caseDetails = newCaseDetails()
                .withSecurityClassification(PRIVATE)
                .build();
            final CaseDetails caseDetailsBefore = newCaseDetails()
                .withSecurityClassification(PRIVATE)
                .build();

            CallbackResponse callbackResponse = aCallbackResponse()
                .withSecurityClassification(RESTRICTED)
                .withData(null)
                .build();

            doReturn(Optional.of(callbackResponse)).when(callbackService)
                .send(any(), any(),
                    same(caseEventDefinition),
                    same(caseDetailsBefore),
                    same(caseDetails),
                    anyBoolean());

            AboutToSubmitCallbackResponse response = callbackInvoker.invokeAboutToSubmitCallback(
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                caseTypeDefinition,
                IGNORE_WARNING
            );

            assertAll(
                () -> assertThat(caseDetails.getSecurityClassification(), is(PRIVATE)),
                () -> assertNotNull(response),
                () -> assertThat(response.getState().isPresent(), is(false))
            );
            verify(securityValidationService, never()).updateSecurityClassificationIfValid(callbackResponse,
                caseDetails);
        }

        private CallbackResponse mockCallbackResponse(final String state) {
            final CallbackResponse response = new CallbackResponse();
            final Map<String, JsonNode> data = new HashMap<>();
            data.put("state", JsonNodeFactory.instance.textNode(state));
            response.setData(data);
            return response;
        }

        private CallbackResponse mockCallbackResponseWithSignificantItem(final String state) {
            final CallbackResponse response = new CallbackResponse();
            SignificantItem significantItem = new SignificantItem();
            significantItem.setUrl("http://www.cnn.com");
            significantItem.setDescription("description");
            significantItem.setType(SignificantItemType.DOCUMENT.name());
            response.setSignificantItem(significantItem);
            final Map<String, JsonNode> data = new HashMap<>();
            data.put("state", JsonNodeFactory.instance.textNode(state));
            response.setData(data);
            return response;
        }

        private CallbackResponse mockCallbackResponseWithIncorrectSignificantItem(final String state) {
            final CallbackResponse response = new CallbackResponse();
            SignificantItem significantItem = new SignificantItem();
            significantItem.setUrl("http://www..com");
            significantItem.setDescription("");

            response.setSignificantItem(significantItem);
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
            callbackInvoker.invokeSubmittedCallback(caseEventDefinition, caseDetailsBefore, caseDetails);

            verify(callbackService).send(URL_AFTER_SUBMIT, SUBMITTED,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                AfterSubmitCallbackResponse.class);
            verifyNoMoreInteractions(callbackService);
        }

        @Test
        @DisplayName("should disable callback retries")
        void shouldDisableCallbackRetries() {
            caseEventDefinition.setRetriesTimeoutURLSubmittedEvent(RETRIES_DISABLED);

            callbackInvoker.invokeSubmittedCallback(caseEventDefinition, caseDetailsBefore, caseDetails);

            verify(callbackService).sendSingleRequest(URL_AFTER_SUBMIT, SUBMITTED,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                AfterSubmitCallbackResponse.class);
            verifyNoMoreInteractions(callbackService);
        }

        @Test
        @DisplayName("should return stored after submit response for decentralised case types")
        void shouldReturnStoredAfterSubmitResponseForDecentralisedCaseTypes() {
            when(persistenceStrategyResolver.isDecentralised(caseDetails)).thenReturn(true);

            AfterSubmitCallbackResponse storedResponse = new AfterSubmitCallbackResponse();
            storedResponse.setConfirmationHeader("Header");
            storedResponse.setConfirmationBody("Body");
            caseDetails.setAfterSubmitCallbackResponseEntity(ResponseEntity.ok(storedResponse));

            ResponseEntity<AfterSubmitCallbackResponse> response =
                callbackInvoker.invokeSubmittedCallback(caseEventDefinition, caseDetailsBefore, caseDetails);

            assertAll(
                () -> assertEquals(HttpStatus.OK, response.getStatusCode()),
                () -> assertEquals(storedResponse, response.getBody())
            );
            verifyNoInteractions(callbackService);
        }
    }

    @Nested
    @DisplayName("invokeGetCaseCallback()")
    class GetCase {

        @Test
        @DisplayName("should send callback")
        void shouldSendCallback() {
            caseTypeDefinition.setCallbackGetCaseUrl(URL_GET_CASE);

            callbackInvoker.invokeGetCaseCallback(caseTypeDefinition, caseDetails);

            verify(callbackService).send(eq(URL_GET_CASE), eq(GET_CASE),
                any(CaseEventDefinition.class),
                isNull(),
                eq(caseDetails),
                any(Class.class));
            verifyNoMoreInteractions(callbackService);
        }

        @Test
        @DisplayName("should disable callback retries")
        void shouldDisableCallbackRetries() {
            caseTypeDefinition.setCallbackGetCaseUrl(URL_GET_CASE);
            caseTypeDefinition.setRetriesGetCaseUrl(RETRIES_DISABLED);

            callbackInvoker.invokeGetCaseCallback(caseTypeDefinition, caseDetails);

            verify(callbackService).sendSingleRequest(eq(URL_GET_CASE), eq(GET_CASE),
                any(CaseEventDefinition.class),
                isNull(),
                eq(caseDetails),
                any(Class.class));
            verifyNoMoreInteractions(callbackService);
        }
    }

    @Nested
    @DisplayName("invokeMidEventCallback()")
    class MidEvent {

        @Test
        @DisplayName("should send callback")
        void shouldSendCallback() {
            callbackInvoker.invokeMidEventCallback(wizardPage,
                caseTypeDefinition,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                IGNORE_WARNINGS);

            verify(callbackService).send(URL_MID_EVENT, MID_EVENT, caseEventDefinition, caseDetailsBefore,
                caseDetails, false);
        }

        @Test
        @DisplayName("should disable callback retries")
        void shouldDisableCallbackRetries() {
            wizardPage.setRetriesTimeoutMidEvent(RETRIES_DISABLED);

            callbackInvoker.invokeMidEventCallback(wizardPage,
                caseTypeDefinition,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                IGNORE_WARNINGS);

            verify(callbackService).sendSingleRequest(URL_MID_EVENT, MID_EVENT, caseEventDefinition, caseDetailsBefore,
                caseDetails, false);
            verifyNoMoreInteractions(callbackService);
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
                when(caseDataService.getDefaultSecurityClassifications(caseTypeDefinition,
                    data,
                    caseDetails.getDataClassification())).thenReturn(
                    currentDataClassification);
                when(callbackService.send(caseEventDefinition.getCallBackURLAboutToStartEvent(),
                    ABOUT_TO_START, caseEventDefinition,
                    null,
                    caseDetails,
                    false)).thenReturn(Optional.of(callbackResponse));

                callbackInvoker.invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition, caseDetails, TRUE);

                assertAll(
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(timeToLiveService).verifyTTLContentNotChangedByCallback(
                        any(), eq(callbackResponse.getData())),
                    () -> inOrder.verify(caseTypeService).validateData(callbackResponse.getData(), caseTypeDefinition),
                    () -> inOrder.verify(globalSearchProcessorService, never())
                        .populateGlobalSearchData(caseTypeDefinition,
                        callbackResponse.getData()),
                    () -> inOrder.verify(caseSanitiser).sanitise(caseTypeDefinition, callbackResponse.getData()),
                    () -> inOrder.verify(caseDataService).getDefaultSecurityClassifications(caseTypeDefinition,
                        caseDetails.getData(),
                        caseDetails.getDataClassification()),
                    () -> inOrder.verify(securityValidationService, never())
                        .setDataClassificationFromCallbackIfValid(any(),
                            any(),
                            any())
                );
            }

            @DisplayName("validate call back response and no case details data is updated")
            @Test
            void validateAndDoNotSetData() {
                final CallbackResponse callbackResponse = new CallbackResponse();
                when(callbackService.send(caseEventDefinition.getCallBackURLAboutToStartEvent(),
                    ABOUT_TO_START, caseEventDefinition,
                    null,
                    caseDetails,
                    false)).thenReturn(Optional.of(callbackResponse));

                callbackInvoker.invokeAboutToStartCallback(caseEventDefinition, caseTypeDefinition, caseDetails, TRUE);

                assertAll(
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(caseTypeService, never()).validateData(any(), any()),
                    () -> inOrder.verify(caseSanitiser, never()).sanitise(any(), any()),
                    () -> inOrder.verify(caseDataService, never()).getDefaultSecurityClassifications(any(),
                        any(),
                        any()),
                    () -> inOrder.verify(securityValidationService, never())
                        .setDataClassificationFromCallbackIfValid(any(),
                            any(),
                            any())
                );
            }

            @DisplayName("validate call back response and there are errors in call back validation when setting data")
            @Test
            void validateAndSetDataMetError() throws ApiException {
                final CallbackResponse callbackResponse = new CallbackResponse();
                when(callbackService.send(caseEventDefinition.getCallBackURLAboutToStartEvent(),
                    ABOUT_TO_START, caseEventDefinition,
                    null,
                    caseDetails,
                    false)).thenReturn(Optional.of(callbackResponse));
                final Map<String, JsonNode> data = new HashMap<>();
                callbackResponse.setData(data);

                final String ErrorMessage = "Royal marriage *!><}{^";
                doThrow(new ApiException(ErrorMessage))
                    .when(callbackService).validateCallbackErrorsAndWarnings(any(), any());

                final ApiException apiException =
                    assertThrows(ApiException.class, () ->
                        callbackInvoker.invokeAboutToStartCallback(caseEventDefinition,
                            caseTypeDefinition,
                            caseDetails,
                            TRUE));

                assertThat(apiException.getMessage(), is(ErrorMessage));

                assertAll(
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(caseTypeService, never()).validateData(any(), any()),
                    () -> inOrder.verify(caseSanitiser, never()).sanitise(any(), any()),
                    () -> inOrder.verify(caseDataService, never()).getDefaultSecurityClassifications(any(),
                        any(),
                        any()),
                    () -> inOrder.verify(securityValidationService, never())
                        .setDataClassificationFromCallbackIfValid(any(),
                            any(),
                            any())
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
            public void setup() {
                caseDetails.setDataClassification(currentDataClassification);
                caseDetails.setData(data);
                callbackResponse.setData(data);
                currentDataClassification.put("currentKey", JSON_NODE_FACTORY.textNode("currentValue"));
                caseDetails.setState("BAYAN");
                newFieldsDataClassification.put("key", JSON_NODE_FACTORY.textNode("value"));

                allFieldsDataClassification.put("key", JSON_NODE_FACTORY.textNode("otherValue"));
                callbackResponse.setSecurityClassification(PRIVATE);
                callbackResponse.setDataClassification(allFieldsDataClassification);
                when(callbackService.send(caseEventDefinition.getCallBackURLAboutToSubmitEvent(),
                    ABOUT_TO_SUBMIT, caseEventDefinition,
                    caseDetailsBefore,
                    caseDetails,
                    TRUE)).thenReturn(Optional.of(callbackResponse));
                when(caseSanitiser.sanitise(eq(caseTypeDefinition), eq(caseDetails.getData()))).thenReturn(data);
                when(caseDataService.getDefaultSecurityClassifications(eq(caseTypeDefinition),
                    eq(caseDetails.getData()),
                    eq(currentDataClassification))).thenReturn(
                    newFieldsDataClassification);
                when(caseDataService.getDefaultSecurityClassifications(eq(caseTypeDefinition),
                    eq(caseDetails.getData()),
                    eq(Maps.newHashMap()))).thenReturn(
                    allFieldsDataClassification);
            }

            @DisplayName("do not validate call back response if no data security passed back")
            @Test
            void doNotValidateCallbackResponseIfNoDataSecurityPassedBack() {
                callbackResponse.setDataClassification(null);
                data.put("state", TextNode.valueOf("ngitb"));

                callbackInvoker.invokeAboutToSubmitCallback(caseEventDefinition, caseDetailsBefore, caseDetails,
                    caseTypeDefinition, TRUE);

                assertAll(
                    () -> assertThat(caseDetails.getState(), is("ngitb")),
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(caseTypeService).validateData(callbackResponse.getData(), caseTypeDefinition),
                    () -> inOrder.verify(caseSanitiser).sanitise(caseTypeDefinition, callbackResponse.getData()),
                    () -> inOrder.verify(caseDataService, times(1))
                        .getDefaultSecurityClassifications(eq(caseTypeDefinition),
                            eq(caseDetails.getData()),
                            eq(currentDataClassification)),
                    () -> inOrder.verify(securityValidationService, never())
                        .setDataClassificationFromCallbackIfValid(any(),
                            any(),
                            any())
                );
            }

            @DisplayName("do not validate call back response if no case security passed back")
            @Test
            void doNotValidateCallbackResponseIfNoCaseSecurityPassedBack() {
                callbackResponse.setSecurityClassification(null);
                data.put("state", TextNode.valueOf("ngitb"));
                callbackResponse.setState(null);
                callbackInvoker.invokeAboutToSubmitCallback(caseEventDefinition, caseDetailsBefore, caseDetails,
                    caseTypeDefinition, TRUE);

                assertAll(
                    () -> assertThat(caseDetails.getState(), is("ngitb")),
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(caseTypeService).validateData(callbackResponse.getData(), caseTypeDefinition),
                    () -> inOrder.verify(caseSanitiser).sanitise(caseTypeDefinition, callbackResponse.getData()),
                    () -> inOrder.verify(caseDataService, times(1))
                        .getDefaultSecurityClassifications(eq(caseTypeDefinition),
                            eq(caseDetails.getData()),
                            eq(currentDataClassification)),
                    () -> inOrder.verify(securityValidationService, never())
                        .updateSecurityClassificationIfValid(any(),
                            any()),
                    () -> inOrder.verify(securityValidationService, times(1))
                        .setDataClassificationFromCallbackIfValid(any(),
                            any(),
                            any()),
                    () -> assertEquals(callbackResponse.getState(), "ngitb")
                );
            }

            @DisplayName("do not validate call back response and do not set a sate.")
            @Test
            void doNotValidateCallbackResponseAndDoNotSetSate() {
                callbackResponse.setSecurityClassification(null);
                data.put("state", null);
                callbackResponse.setState(null);
                caseDetails.setState("caseDetailsState");
                when(globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition,
                    data)).thenReturn(callbackResponse.getData());
                callbackInvoker.invokeAboutToSubmitCallback(caseEventDefinition, caseDetailsBefore, caseDetails,
                    caseTypeDefinition, TRUE);


                assertAll(
                    () -> assertThat(caseDetails.getState(), is("caseDetailsState")),
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(caseTypeService).validateData(callbackResponse.getData(), caseTypeDefinition),
                    () -> inOrder.verify(globalSearchProcessorService).populateGlobalSearchData(caseTypeDefinition,
                        callbackResponse.getData()),
                    () -> inOrder.verify(caseSanitiser).sanitise(caseTypeDefinition, callbackResponse.getData()),
                    () -> inOrder.verify(caseDataService, times(1))
                        .getDefaultSecurityClassifications(eq(caseTypeDefinition),
                            eq(caseDetails.getData()),
                            eq(currentDataClassification)),
                    () -> inOrder.verify(securityValidationService, never())
                        .updateSecurityClassificationIfValid(any(),
                            any()),
                    () -> inOrder.verify(securityValidationService, times(1))
                        .setDataClassificationFromCallbackIfValid(any(),
                            any(),
                            any()),
                    () -> assertNull(callbackResponse.getState())
                );
            }

            @DisplayName("validate call back response and set case details state for about to submit")
            @Test
            void validateAndSetStateForAboutToSubmit() {
                data.put("state", TextNode.valueOf("ngitb"));
                callbackResponse.setState("toto");

                callbackInvoker.invokeAboutToSubmitCallback(caseEventDefinition, caseDetailsBefore, caseDetails,
                    caseTypeDefinition, TRUE);

                ArgumentCaptor<Map> argumentDataClassification = ArgumentCaptor.forClass(Map.class);
                assertAll(
                    () -> assertThat(caseDetails.getState(), is("toto")),
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(timeToLiveService).verifyTTLContentNotChangedByCallback(
                        any(), eq(callbackResponse.getData())),
                    () -> inOrder.verify(caseTypeService).validateData(callbackResponse.getData(), caseTypeDefinition),
                    () -> inOrder.verify(caseSanitiser).sanitise(caseTypeDefinition, callbackResponse.getData()),
                    () -> inOrder.verify(caseDataService, times(2))
                        .getDefaultSecurityClassifications(eq(caseTypeDefinition),
                            eq(caseDetails.getData()),
                            argumentDataClassification.capture()),
                    () -> inOrder.verify(securityValidationService).setDataClassificationFromCallbackIfValid(eq(
                        callbackResponse), eq(caseDetails), eq(allFieldsDataClassification)),
                    () -> assertThat(argumentDataClassification.getAllValues(),
                        contains(currentDataClassification, Maps.newHashMap())),
                    () -> assertEquals(callbackResponse.getState(), "toto"),
                    () -> assertThat(caseDetails.getData().containsKey("state"), is(false))
                );
            }

            @DisplayName("validate call back response and neither case details nor state is updated")
            @Test
            void validateAndDoNotSetStateOrData() {
                callbackInvoker.invokeAboutToSubmitCallback(caseEventDefinition, caseDetailsBefore, caseDetails,
                    caseTypeDefinition, TRUE);

                ArgumentCaptor<Map> argumentDataClassification = ArgumentCaptor.forClass(Map.class);
                assertAll(
                    () -> assertThat(caseDetails.getState(), is("BAYAN")),
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(caseTypeService).validateData(callbackResponse.getData(), caseTypeDefinition),
                    () -> inOrder.verify(caseSanitiser).sanitise(caseTypeDefinition, callbackResponse.getData()),
                    () -> inOrder.verify(caseDataService, times(2))
                        .getDefaultSecurityClassifications(eq(caseTypeDefinition),
                            eq(caseDetails.getData()),
                            argumentDataClassification.capture()),
                    () -> inOrder.verify(securityValidationService).setDataClassificationFromCallbackIfValid(
                        callbackResponse,
                        caseDetails,
                        allFieldsDataClassification),
                    () -> assertThat(argumentDataClassification.getAllValues(),
                        contains(currentDataClassification, Maps.newHashMap()))
                );
            }

            @DisplayName("validate call back response and there are errors in call back validation when setting data "
                + "and state")
            @Test
            void validateAndSetStateMetError() throws ApiException {
                final String errorMessage = "Royal carriage is stuck AGAIN!!!!";
                doThrow(new ApiException(errorMessage))
                    .when(callbackService).validateCallbackErrorsAndWarnings(any(), any());
                final ApiException apiException =
                    assertThrows(ApiException.class, () ->
                        callbackInvoker.invokeAboutToSubmitCallback(caseEventDefinition,
                            caseDetailsBefore,
                            caseDetails,
                            caseTypeDefinition,
                            TRUE));

                assertAll(
                    () -> assertThat(apiException.getMessage(), is(errorMessage)),
                    () -> assertThat(caseDetails.getState(), is("BAYAN")),
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, TRUE),
                    () -> inOrder.verify(caseTypeService, never()).validateData(any(), any()),
                    () -> inOrder.verify(caseSanitiser, never()).sanitise(any(), any()),
                    () -> inOrder.verify(caseDataService, never()).getDefaultSecurityClassifications(any(),
                        any(),
                        any()),
                    () -> inOrder.verify(securityValidationService, never())
                        .setDataClassificationFromCallbackIfValid(any(),
                            any(),
                            any())
                );
            }
        }

        @Nested
        @DisplayName("mid event")
        class MidEvent {

            @DisplayName("validate callback response and update case details data for about to start")
            @Test
            void validateAndSetDataForAboutToStart() {
                final CallbackResponse callbackResponse = new CallbackResponse();
                final Map<String, JsonNode> data = new HashMap<>();
                data.put("xxx", TextNode.valueOf("ngitb"));
                callbackResponse.setData(data);
                HashMap<String, JsonNode> currentDataClassification = Maps.newHashMap();
                when(caseDataService.getDefaultSecurityClassifications(caseTypeDefinition, data,
                    caseDetails.getDataClassification())).thenReturn(currentDataClassification);
                when(callbackService.send(wizardPage.getCallBackURLMidEvent(),
                    MID_EVENT, caseEventDefinition,
                    caseDetailsBefore,
                    caseDetails,
                    false)).thenReturn(Optional.of(callbackResponse));

                callbackInvoker.invokeMidEventCallback(wizardPage,
                    caseTypeDefinition,
                    caseEventDefinition,
                    caseDetailsBefore,
                    caseDetails,
                    IGNORE_WARNINGS);

                assertAll(
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, FALSE),
                    () -> inOrder.verify(timeToLiveService).verifyTTLContentNotChangedByCallback(
                        any(), eq(callbackResponse.getData())),
                    () -> inOrder.verify(caseTypeService).validateData(callbackResponse.getData(), caseTypeDefinition),
                    () -> inOrder.verify(globalSearchProcessorService, never())
                        .populateGlobalSearchData(caseTypeDefinition,
                        callbackResponse.getData()),
                    () -> inOrder.verify(caseSanitiser).sanitise(caseTypeDefinition, callbackResponse.getData()),
                    () -> inOrder.verify(caseDataService).getDefaultSecurityClassifications(caseTypeDefinition,
                        caseDetails.getData(),
                        caseDetails.getDataClassification()),
                    () -> inOrder.verify(securityValidationService, never())
                        .setDataClassificationFromCallbackIfValid(any(), any(), any())
                );
            }

            @DisplayName("validate call back response and no case details data is updated")
            @Test
            void validateAndDoNotSetData() {
                final CallbackResponse callbackResponse = new CallbackResponse();
                when(callbackService.send(wizardPage.getCallBackURLMidEvent(),
                    MID_EVENT, caseEventDefinition,
                    caseDetailsBefore,
                    caseDetails,
                    false)).thenReturn(Optional.of(callbackResponse));

                callbackInvoker.invokeMidEventCallback(wizardPage,
                    caseTypeDefinition,
                    caseEventDefinition,
                    caseDetailsBefore,
                    caseDetails,
                    IGNORE_WARNINGS);

                assertAll(
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, FALSE),
                    () -> inOrder.verify(caseTypeService, never()).validateData(any(), any()),
                    () -> inOrder.verify(caseSanitiser, never()).sanitise(any(), any()),
                    () -> inOrder.verify(caseDataService, never()).getDefaultSecurityClassifications(any(), any(),
                        any()),
                    () -> inOrder.verify(securityValidationService, never()).setDataClassificationFromCallbackIfValid(
                        any(), any(), any())
                );
            }

            @DisplayName("validate call back response and there are errors in call back validation when setting data")
            @Test
            void validateAndSetDataMetError() throws ApiException {
                final CallbackResponse callbackResponse = new CallbackResponse();
                when(callbackService.send(wizardPage.getCallBackURLMidEvent(),
                    MID_EVENT, caseEventDefinition,
                    caseDetailsBefore,
                    caseDetails, false)).thenReturn(Optional.of(callbackResponse));
                final Map<String, JsonNode> data = new HashMap<>();
                callbackResponse.setData(data);

                final String ErrorMessage = "Royal marriage *!><}{^";
                doThrow(new ApiException(ErrorMessage))
                    .when(callbackService).validateCallbackErrorsAndWarnings(any(), any());

                final ApiException apiException =
                    assertThrows(ApiException.class,
                        () -> callbackInvoker.invokeMidEventCallback(wizardPage,
                            caseTypeDefinition,
                            caseEventDefinition,
                            caseDetailsBefore,
                            caseDetails,
                            IGNORE_WARNINGS));

                assertThat(apiException.getMessage(), is(ErrorMessage));

                assertAll(
                    () -> inOrder.verify(callbackService).validateCallbackErrorsAndWarnings(callbackResponse, FALSE),
                    () -> inOrder.verify(caseTypeService, never()).validateData(any(), any()),
                    () -> inOrder.verify(caseSanitiser, never()).sanitise(any(), any()),
                    () -> inOrder.verify(caseDataService, never()).getDefaultSecurityClassifications(any(), any(),
                        any()),
                    () -> inOrder.verify(securityValidationService, never()).setDataClassificationFromCallbackIfValid(
                        any(), any(), any())
                );
            }
        }

        @DisplayName("Resolve state inside data section only.")
        @Test
        void resolveStateInsideDataSectionOnly() {
            final CallbackResponse callbackResponse = new CallbackResponse();
            final Map<String, JsonNode> data = new HashMap<>();

            data.put("state", TextNode.valueOf("stateInDataSection"));
            data.put("blah", IntNode.valueOf(678));
            callbackResponse.setData(data);

            assertThat("Before filter", data.keySet(), hasSize(2));

            callbackResponse.updateCallbackStateBasedOnPriority();
            final String stateResult = callbackResponse.getState();

            assertAll(
                () -> assertThat(stateResult, is("stateInDataSection")),
                () -> assertThat(callbackResponse.getData().get("blah").intValue(), is(678))
            );
        }

        @DisplayName("Resolve top level state only.")
        @Test
        void resolveTopLevelStateOnly() {
            final CallbackResponse callbackResponse = new CallbackResponse();
            final Map<String, JsonNode> data = new HashMap<>();

            data.put("blah", IntNode.valueOf(678));
            callbackResponse.setData(data);
            callbackResponse.setState("stateInTopLevel");

            assertThat("Before filter", data.keySet(), hasSize(1));

            callbackResponse.updateCallbackStateBasedOnPriority();
            final String stateResult = callbackResponse.getState();

            assertAll(
                () -> assertThat(stateResult, is("stateInTopLevel")),
                () -> assertThat(callbackResponse.getData().get("blah").intValue(), is(678))
            );
        }

        @DisplayName("Resolve top level state and state inside data section.")
        @Test
        void resolveTopLevelStateAndSateInDataSection() {
            final CallbackResponse callbackResponse = new CallbackResponse();
            final Map<String, JsonNode> data = new HashMap<>();

            data.put("blah", IntNode.valueOf(678));
            data.put("state", TextNode.valueOf("stateInDataSection"));
            callbackResponse.setData(data);
            callbackResponse.setState("stateInTopLevel");

            assertThat("Before filter", data.keySet(), hasSize(2));

            callbackResponse.updateCallbackStateBasedOnPriority();
            final String stateResult = callbackResponse.getState();

            assertAll(
                () -> assertThat(stateResult, is("stateInTopLevel")),
                () -> assertThat(callbackResponse.getData().get("blah").intValue(), is(678))
            );
        }

        @DisplayName("Resolve no defined state.")
        @Test
        void resolveNoDefinedState() {
            final CallbackResponse callbackResponse = new CallbackResponse();
            final Map<String, JsonNode> data = new HashMap<>();

            data.put("blah", IntNode.valueOf(678));
            callbackResponse.setData(data);

            assertThat("Before filter", data.keySet(), hasSize(1));

            callbackResponse.updateCallbackStateBasedOnPriority();
            final String stateResult = callbackResponse.getState();

            assertAll(
                () -> assertEquals(stateResult, null),
                () -> assertThat(callbackResponse.getData().get("blah").intValue(), is(678))
            );
        }

        @DisplayName("Resolve no defined data and top leve state.")
        @Test
        void resolveNoDefinedStateAndDataSectionEmpty() {
            final CallbackResponse callbackResponse = new CallbackResponse();

            callbackResponse.updateCallbackStateBasedOnPriority();
            final String stateResult = callbackResponse.getState();

            assertAll(
                () -> assertEquals(stateResult, null)
            );
        }
    }
}
