package uk.gov.hmcts.ccd.domain.service.stdapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.ccd.domain.model.callbacks.AfterSubmitCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.CallbackResponse;
import uk.gov.hmcts.ccd.domain.model.callbacks.GetCaseCallbackResponse;
import uk.gov.hmcts.ccd.domain.model.definition.CaseDetails;
import uk.gov.hmcts.ccd.domain.model.definition.CaseEventDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.CaseTypeDefinition;
import uk.gov.hmcts.ccd.domain.model.definition.WizardPage;
import uk.gov.hmcts.ccd.domain.service.callbacks.CallbackService;
import uk.gov.hmcts.ccd.domain.service.casedeletion.TimeToLiveService;
import uk.gov.hmcts.ccd.domain.service.common.CaseDataService;
import uk.gov.hmcts.ccd.domain.service.common.CaseTypeService;
import uk.gov.hmcts.ccd.domain.service.common.SecurityValidationService;
import uk.gov.hmcts.ccd.domain.service.processor.GlobalSearchProcessorService;
import uk.gov.hmcts.ccd.domain.types.sanitiser.CaseSanitiser;
import uk.gov.hmcts.ccd.endpoint.std.TestController;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.collect.Maps.newHashMap;
import static java.util.Optional.ofNullable;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType.GET_CASE;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType.MID_EVENT;
import static uk.gov.hmcts.ccd.domain.service.callbacks.CallbackType.SUBMITTED;
import static uk.gov.hmcts.ccd.domain.service.validate.ValidateSignificantDocument.validateSignificantItem;

@Service
public class CallbackInvoker {

    private static final HashMap<String, JsonNode> EMPTY_DATA_CLASSIFICATION = Maps.newHashMap();

    private final CallbackService callbackService;
    private final CaseTypeService caseTypeService;
    private final CaseDataService caseDataService;
    private final CaseSanitiser caseSanitiser;
    private final SecurityValidationService securityValidationService;
    private final GlobalSearchProcessorService globalSearchProcessorService;
    private final TimeToLiveService timeToLiveService;

    @Autowired
    public CallbackInvoker(final CallbackService callbackService,
                           final CaseTypeService caseTypeService,
                           final CaseDataService caseDataService,
                           final CaseSanitiser caseSanitiser,
                           final SecurityValidationService securityValidationService,
                           final GlobalSearchProcessorService globalSearchProcessorService,
                           final TimeToLiveService timeToLiveService) {
        this.callbackService = callbackService;
        this.caseTypeService = caseTypeService;
        this.caseDataService = caseDataService;
        this.caseSanitiser = caseSanitiser;
        this.securityValidationService = securityValidationService;
        this.globalSearchProcessorService = globalSearchProcessorService;
        this.timeToLiveService = timeToLiveService;
    }

    public void invokeAboutToStartCallback(final CaseEventDefinition caseEventDefinition,
                                           final CaseTypeDefinition caseTypeDefinition,
                                           final CaseDetails caseDetails,
                                           final Boolean ignoreWarning) {
        final Optional<CallbackResponse> callbackResponse;
        if (isRetriesDisabled(caseEventDefinition.getRetriesTimeoutAboutToStartEvent())) {
            callbackResponse = callbackService.sendSingleRequest(caseEventDefinition.getCallBackURLAboutToStartEvent(),
                ABOUT_TO_START, caseEventDefinition, null, caseDetails, false);
        } else {
            callbackResponse = callbackService.send(
                caseEventDefinition.getCallBackURLAboutToStartEvent(), ABOUT_TO_START,
                caseEventDefinition, null, caseDetails, false);
        }

        callbackResponse.ifPresent(response -> validateAndSetFromAboutToStartCallback(caseTypeDefinition,
            caseDetails,
            ignoreWarning,
            response));
    }

    public AboutToSubmitCallbackResponse invokeAboutToSubmitCallback(final CaseEventDefinition caseEventDefinition,
                                                                     final CaseDetails caseDetailsBefore,
                                                                     final CaseDetails caseDetails,
                                                                     final CaseTypeDefinition caseTypeDefinition,
                                                                     final Boolean ignoreWarning) {
        final Optional<CallbackResponse> callbackResponse;
        if (isRetriesDisabled(caseEventDefinition.getRetriesTimeoutURLAboutToSubmitEvent())) {
            callbackResponse = callbackService.sendSingleRequest(caseEventDefinition.getCallBackURLAboutToSubmitEvent(),
                ABOUT_TO_SUBMIT, caseEventDefinition, caseDetailsBefore, caseDetails, ignoreWarning);
        } else {
            callbackResponse = callbackService.send(
                caseEventDefinition.getCallBackURLAboutToSubmitEvent(), ABOUT_TO_SUBMIT,
                caseEventDefinition, caseDetailsBefore, caseDetails, ignoreWarning);
        }

        if (callbackResponse.isPresent()) {
            return validateAndSetFromAboutToSubmitCallback(caseTypeDefinition,
                caseDetails,
                ignoreWarning,
                callbackResponse.get());
        }

        return new AboutToSubmitCallbackResponse();
    }

    public ResponseEntity<AfterSubmitCallbackResponse> invokeSubmittedCallback(final CaseEventDefinition
                                                                                   caseEventDefinition,
                                                                               final CaseDetails caseDetailsBefore,
                                                                               final CaseDetails caseDetails) {
        ResponseEntity<AfterSubmitCallbackResponse> afterSubmitCallbackResponseEntity;
        if (isRetriesDisabled(caseEventDefinition.getRetriesTimeoutURLSubmittedEvent())) {
            afterSubmitCallbackResponseEntity =
                callbackService.sendSingleRequest(caseEventDefinition.getCallBackURLSubmittedEvent(),
                    SUBMITTED, caseEventDefinition,
                    caseDetailsBefore,
                    caseDetails,
                    AfterSubmitCallbackResponse.class);
        } else {
            afterSubmitCallbackResponseEntity = callbackService.send(caseEventDefinition.getCallBackURLSubmittedEvent(),
                SUBMITTED, caseEventDefinition,
                caseDetailsBefore,
                caseDetails,
                AfterSubmitCallbackResponse.class);
        }
        return afterSubmitCallbackResponseEntity;
    }

    public ResponseEntity<GetCaseCallbackResponse> invokeGetCaseCallback(final CaseTypeDefinition caseTypeDefinition,
                                                                         final CaseDetails caseDetails) {
        String url = caseTypeDefinition.getCallbackGetCaseUrl();
        List<Integer> retries = caseTypeDefinition.getRetriesGetCaseUrl();

        CaseEventDefinition caseEventDefinition = new CaseEventDefinition();
        caseEventDefinition.setId("GetCaseCallback");
        caseEventDefinition.setName("GetCaseCallback");

        ResponseEntity<GetCaseCallbackResponse> getCaseCallbackResponseEntity;
        if (isRetriesDisabled(retries)) {
            getCaseCallbackResponseEntity =
                callbackService.sendSingleRequest(url,
                    GET_CASE, caseEventDefinition,
                    null,
                    caseDetails,
                    GetCaseCallbackResponse.class);
        } else {
            getCaseCallbackResponseEntity = callbackService.send(url,
                GET_CASE, caseEventDefinition,
                null,
                caseDetails,
                GetCaseCallbackResponse.class);
        }
        return getCaseCallbackResponseEntity;
    }

    public CaseDetails invokeMidEventCallback(final WizardPage wizardPage,
                                              final CaseTypeDefinition caseTypeDefinition,
                                              final CaseEventDefinition caseEventDefinition,
                                              final CaseDetails caseDetailsBefore,
                                              final CaseDetails caseDetails,
                                              final Boolean ignoreWarning) {

        Optional<CallbackResponse> callbackResponseOptional;
        if (isRetriesDisabled(wizardPage.getRetriesTimeoutMidEvent())) {
            callbackResponseOptional = callbackService.sendSingleRequest(wizardPage.getCallBackURLMidEvent(),
                MID_EVENT,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails, false);
        } else {
            callbackResponseOptional = callbackService.send(wizardPage.getCallBackURLMidEvent(),
                MID_EVENT,
                caseEventDefinition,
                caseDetailsBefore,
                caseDetails, false);
        }

        if (callbackResponseOptional.isPresent()) {
            CallbackResponse callbackResponse = callbackResponseOptional.get();

            callbackService.validateCallbackErrorsAndWarnings(callbackResponse, ignoreWarning);
            if (callbackResponse.getData() != null) {
                validateAndSetData(caseTypeDefinition, caseDetails, callbackResponse.getData());
            }
        }

        return caseDetails;
    }

    private boolean isRetriesDisabled(final List<Integer> retriesTimeouts) {
        return retriesTimeouts != null && retriesTimeouts.size() == 1 && retriesTimeouts.get(0) == 0;
    }

    private void validateAndSetFromAboutToStartCallback(CaseTypeDefinition caseTypeDefinition,
                                                        CaseDetails caseDetails,
                                                        Boolean ignoreWarning,
                                                        CallbackResponse callbackResponse) {
        callbackService.validateCallbackErrorsAndWarnings(callbackResponse, ignoreWarning);

        if (callbackResponse.getData() != null) {
            validateAndSetData(caseTypeDefinition, caseDetails, callbackResponse.getData());
        }
    }

    private static String jcLog(final String message) {
        return TestController.jcLog(message);
    }

    /*
     * ==== Get call start as string. ====
     */
    private String getCallStackString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        new Throwable().printStackTrace(printWriter);
        return stringWriter.toString().replaceAll("[\n\r]", "_");
    }

    private String getCallStackString(final Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString().replaceAll("[\n\r]", "_");
    }

    /*
     * 1. CallbackInvoker.validateAndSetFromAboutToSubmitCallback         (gets to #8 but NOT #9)
     * 2. SecurityValidationService.setClassificationFromCallbackIfValid  (gets to #4 (before validateObject))
     * 3. SecurityValidationService.validateObject
     */
    private AboutToSubmitCallbackResponse validateAndSetFromAboutToSubmitCallback(final CaseTypeDefinition
                                                                                      caseTypeDefinition,
                                                                                  final CaseDetails caseDetails,
                                                                                  final Boolean ignoreWarning,
                                                                                  final CallbackResponse
                                                                                      callbackResponse) {
        jcLog("JCDEBUG3: CallbackInvoker.validateAndSetFromAboutToSubmitCallback #1 -->  (20th May)");
        try {
            final AboutToSubmitCallbackResponse aboutToSubmitCallbackResponse = new AboutToSubmitCallbackResponse();

            jcLog("JCDEBUG3: CallbackInvoker.validateAndSetFromAboutToSubmitCallback #2");
            validateSignificantItem(aboutToSubmitCallbackResponse, callbackResponse);
            jcLog("JCDEBUG3: CallbackInvoker.validateAndSetFromAboutToSubmitCallback #3");
            callbackService.validateCallbackErrorsAndWarnings(callbackResponse, ignoreWarning);
            jcLog("JCDEBUG3: CallbackInvoker.validateAndSetFromAboutToSubmitCallback #4");
            callbackResponse.updateCallbackStateBasedOnPriority();
            jcLog("JCDEBUG3: CallbackInvoker.validateAndSetFromAboutToSubmitCallback #5");
            aboutToSubmitCallbackResponse.setState(Optional.ofNullable(callbackResponse.getState()));
            if (callbackResponse.getState() != null) {
                jcLog("JCDEBUG3: CallbackInvoker.validateAndSetFromAboutToSubmitCallback #6");
                caseDetails.setState(callbackResponse.getState());
            }
            if (callbackResponse.getData() != null) {
                jcLog("JCDEBUG3: CallbackInvoker.validateAndSetFromAboutToSubmitCallback #7.1");
                validateAndSetDataForGlobalSearch(caseTypeDefinition, caseDetails, callbackResponse.getData());
                jcLog("JCDEBUG3: CallbackInvoker.validateAndSetFromAboutToSubmitCallback #7.2");
                if (callbackResponseHasCaseAndDataClassification(callbackResponse)) {
                    jcLog("JCDEBUG3: CallbackInvoker.validateAndSetFromAboutToSubmitCallback #8.1 (yes)");
                    securityValidationService.setClassificationFromCallbackIfValid(
                        callbackResponse,
                        caseDetails,
                        deduceDefaultClassificationForExistingFields(caseTypeDefinition, caseDetails)
                    );
                } else {
                    jcLog("JCDEBUG3: CallbackInvoker.validateAndSetFromAboutToSubmitCallback #8.2 (*NO*)");
                }
            }
            jcLog("JCDEBUG3: CallbackInvoker.validateAndSetFromAboutToSubmitCallback #9 (OK)");
            return aboutToSubmitCallbackResponse;
        } catch (Exception e) {
            jcLog("JCDEBUG3: CallbackInvoker.validateAndSetFromAboutToSubmitCallback #10 *EXCEPTION* "
                + e.getMessage());
            jcLog("JCDEBUG3: CallbackInvoker.validateAndSetFromAboutToSubmitCallback #10 *CALL STACK* = "
                + getCallStackString(e));
            jcLog("JCDEBUG3: CallbackInvoker.validateAndSetFromAboutToSubmitCallback #10 *CALL STACK* = "
                + getCallStackString());
            throw e;
        }
    }


    private boolean callbackResponseHasCaseAndDataClassification(CallbackResponse callbackResponse) {
        return (callbackResponse.getSecurityClassification() != null
            && callbackResponse.getDataClassification() != null) ? true : false;
    }

    private Map<String, JsonNode> deduceDefaultClassificationForExistingFields(CaseTypeDefinition caseTypeDefinition,
                                                                               CaseDetails caseDetails) {
        Map<String, JsonNode> defaultSecurityClassifications = caseDataService.getDefaultSecurityClassifications(
            caseTypeDefinition,
            caseDetails.getData(),
            EMPTY_DATA_CLASSIFICATION);
        return defaultSecurityClassifications;
    }

    private void validateAndSetData(final CaseTypeDefinition caseTypeDefinition,
                                    final CaseDetails caseDetails,
                                    final Map<String, JsonNode> responseData,
                                    final boolean populateGlobalSearch) {
        timeToLiveService.verifyTTLContentNotChangedByCallback(caseDetails.getData(), responseData);
        caseTypeService.validateData(responseData, caseTypeDefinition);

        Map<String, JsonNode> responseDataToSanitise = responseData;

        if (populateGlobalSearch) {
            responseDataToSanitise =
                globalSearchProcessorService.populateGlobalSearchData(caseTypeDefinition, responseData);
        }

        caseDetails.setData(caseSanitiser.sanitise(caseTypeDefinition, responseDataToSanitise));
        deduceDataClassificationForNewFields(caseTypeDefinition, caseDetails);
    }

    private void validateAndSetData(final CaseTypeDefinition caseTypeDefinition,
                                    final CaseDetails caseDetails,
                                    final Map<String, JsonNode> responseData) {
        validateAndSetData(caseTypeDefinition, caseDetails, responseData, false);
    }

    private void validateAndSetDataForGlobalSearch(final CaseTypeDefinition caseTypeDefinition,
                                    final CaseDetails caseDetails,
                                    final Map<String, JsonNode> responseData) {

        validateAndSetData(caseTypeDefinition, caseDetails, responseData, true);
    }

    private void deduceDataClassificationForNewFields(CaseTypeDefinition caseTypeDefinition, CaseDetails caseDetails) {
        Map<String, JsonNode> defaultSecurityClassifications = caseDataService.getDefaultSecurityClassifications(
            caseTypeDefinition,
            caseDetails.getData(),
            ofNullable(caseDetails.getDataClassification()).orElse(
                newHashMap()));
        caseDetails.setDataClassification(defaultSecurityClassifications);
    }
}
